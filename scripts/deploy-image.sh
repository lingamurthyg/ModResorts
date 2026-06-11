#!/bin/bash
# =============================================================================
# deploy-image.sh - Deploy ModResorts to AWS ECS Fargate
# =============================================================================
set -e
set -o pipefail

SERVICE_NAME="modresorts-service"
TASK_FAMILY="modresorts-task"
PROJECT_NAME="modresorts"
APP_PORT=8080
LOG_GROUP="/ecs/modresorts"

echo "=============================================="
echo "  ModResorts - AWS ECS Fargate Deployment"
echo "=============================================="
echo ""

# ---- Collect configuration ----
read -p "Enter AWS Region [us-east-1]: " AWS_REGION
AWS_REGION="${AWS_REGION:-us-east-1}"

read -p "Enter ECS Cluster name [modresorts-cluster]: " CLUSTER_NAME
CLUSTER_NAME="${CLUSTER_NAME:-modresorts-cluster}"

read -p "Enter VPC ID (e.g. vpc-xxxxxxxx): " VPC_ID
if [ -z "$VPC_ID" ]; then
  echo "ERROR: VPC ID is required."
  exit 1
fi

read -p "Enter Subnet IDs (comma-separated, e.g. subnet-aaa,subnet-bbb): " SUBNETS_INPUT
if [ -z "$SUBNETS_INPUT" ]; then
  echo "ERROR: At least one subnet ID is required."
  exit 1
fi

read -p "Enter Security Group ID (e.g. sg-xxxxxxxx): " SECURITY_GROUP
if [ -z "$SECURITY_GROUP" ]; then
  echo "ERROR: Security Group ID is required."
  exit 1
fi

read -p "Enter full ECR Image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Image URI is required."
  exit 1
fi

# Parse subnets into JSON array
IFS=',' read -ra SUBNET_ARRAY <<< "$SUBNETS_INPUT"
SUBNET_JSON="["
for i in "${!SUBNET_ARRAY[@]}"; do
  SUBNET_TRIMMED=$(echo "${SUBNET_ARRAY[$i]}" | xargs)
  if [ $i -gt 0 ]; then SUBNET_JSON+=","; fi
  SUBNET_JSON+="\"$SUBNET_TRIMMED\""
done
SUBNET_JSON+="]"

# ---- Get AWS Account ID ----
echo ""
echo "Fetching AWS Account ID..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "Account ID: $ACCOUNT_ID"

# ---- Ensure CloudWatch Log Group exists ----
echo ""
echo "Ensuring CloudWatch log group '$LOG_GROUP' exists..."
aws logs create-log-group --log-group-name "$LOG_GROUP" --region "$AWS_REGION" 2>/dev/null || true
echo "Log group ready."

# ---- Check / Create ECS Cluster ----
echo ""
echo "Checking ECS cluster '$CLUSTER_NAME'..."
CLUSTER_STATUS=$(aws ecs describe-clusters --clusters "$CLUSTER_NAME" --region "$AWS_REGION" \
  --query "clusters[0].status" --output text 2>/dev/null || echo "MISSING")

if [ "$CLUSTER_STATUS" != "ACTIVE" ]; then
  echo "Cluster not found or inactive. Creating cluster '$CLUSTER_NAME'..."
  aws ecs create-cluster --cluster-name "$CLUSTER_NAME" --region "$AWS_REGION"
  echo "Cluster created."
else
  echo "Cluster '$CLUSTER_NAME' is active."
fi

# ---- Load Balancer ----
echo ""
read -p "Do you need an Application Load Balancer for this service? (y/n) [n]: " NEED_LB
NEED_LB="${NEED_LB:-n}"
USE_LB=false
TARGET_GROUP_ARN=""

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
  USE_LB=true
  echo ""
  echo "Creating Application Load Balancer..."

  # Build subnet list for ALB (space-separated)
  SUBNET_SPACE="${SUBNETS_INPUT//,/ }"

  ALB_NAME="${PROJECT_NAME}-alb"
  ALB_ARN=$(aws elbv2 create-load-balancer \
    --name "$ALB_NAME" \
    --subnets $SUBNET_SPACE \
    --security-groups "$SECURITY_GROUP" \
    --scheme internet-facing \
    --type application \
    --region "$AWS_REGION" \
    --query "LoadBalancers[0].LoadBalancerArn" \
    --output text)
  echo "ALB created: $ALB_ARN"

  echo "Creating Target Group (type: ip for Fargate awsvpc)..."
  TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
    --name "${PROJECT_NAME}-tg" \
    --protocol HTTP \
    --port "$APP_PORT" \
    --vpc-id "$VPC_ID" \
    --target-type ip \
    --health-check-path "/health" \
    --health-check-interval-seconds 30 \
    --healthy-threshold-count 2 \
    --unhealthy-threshold-count 3 \
    --region "$AWS_REGION" \
    --query "TargetGroups[0].TargetGroupArn" \
    --output text)
  echo "Target Group created: $TARGET_GROUP_ARN"

  echo "Creating ALB Listener on port 80..."
  aws elbv2 create-listener \
    --load-balancer-arn "$ALB_ARN" \
    --protocol HTTP \
    --port 80 \
    --default-actions "Type=forward,TargetGroupArn=$TARGET_GROUP_ARN" \
    --region "$AWS_REGION" > /dev/null
  echo "Listener created."

  ALB_DNS=$(aws elbv2 describe-load-balancers \
    --load-balancer-arns "$ALB_ARN" \
    --region "$AWS_REGION" \
    --query "LoadBalancers[0].DNSName" \
    --output text)
fi

# ---- Prepare task definition JSON ----
echo ""
echo "Preparing ECS task definition..."
TASK_DEF_FILE=$(mktemp /tmp/task-def-XXXXXX.json)
cp ecs/task-definition.json "$TASK_DEF_FILE"

sed -i "s|{{IMAGE_URI}}|${IMAGE_URI}|g" "$TASK_DEF_FILE"
sed -i "s|{{AWS_REGION}}|${AWS_REGION}|g" "$TASK_DEF_FILE"
sed -i "s|{{ACCOUNT_ID}}|${ACCOUNT_ID}|g" "$TASK_DEF_FILE"

# ---- Register Task Definition ----
echo "Registering ECS task definition..."
TASK_DEF_ARN=$(aws ecs register-task-definition \
  --cli-input-json "file://${TASK_DEF_FILE}" \
  --region "$AWS_REGION" \
  --query "taskDefinition.taskDefinitionArn" \
  --output text)
echo "Task definition registered: $TASK_DEF_ARN"
rm -f "$TASK_DEF_FILE"

# ---- Prepare service definition JSON ----
echo ""
echo "Preparing ECS service definition..."
SERVICE_DEF_FILE=$(mktemp /tmp/service-def-XXXXXX.json)
cp ecs/service-definition.json "$SERVICE_DEF_FILE"

sed -i "s|{{CLUSTER_NAME}}|${CLUSTER_NAME}|g" "$SERVICE_DEF_FILE"
sed -i "s|{{SECURITY_GROUP}}|${SECURITY_GROUP}|g" "$SERVICE_DEF_FILE"

# Replace subnet placeholders
SUBNET_1=$(echo "${SUBNET_ARRAY[0]}" | xargs)
SUBNET_2=$(echo "${SUBNET_ARRAY[1]:-${SUBNET_ARRAY[0]}}" | xargs)
sed -i "s|{{SUBNET_1}}|${SUBNET_1}|g" "$SERVICE_DEF_FILE"
sed -i "s|{{SUBNET_2}}|${SUBNET_2}|g" "$SERVICE_DEF_FILE"

# ---- Handle Load Balancer in service definition ----
if [ "$USE_LB" = true ]; then
  # Inject loadBalancers and healthCheckGracePeriodSeconds into service definition
  python3 - <<PYEOF
import json, sys

with open("$SERVICE_DEF_FILE", "r") as f:
    svc = json.load(f)

svc["loadBalancers"] = [{
    "targetGroupArn": "$TARGET_GROUP_ARN",
    "containerName": "$PROJECT_NAME",
    "containerPort": $APP_PORT
}]
svc["healthCheckGracePeriodSeconds"] = 300

with open("$SERVICE_DEF_FILE", "w") as f:
    json.dump(svc, f, indent=2)
PYEOF
fi

# ---- Check if service exists ----
echo ""
echo "Checking if ECS service '$SERVICE_NAME' exists..."
EXISTING_SERVICE=$(aws ecs describe-services \
  --cluster "$CLUSTER_NAME" \
  --services "$SERVICE_NAME" \
  --region "$AWS_REGION" \
  --query "services[?status!='INACTIVE'].serviceName" \
  --output text 2>/dev/null || echo "")

if [ -z "$EXISTING_SERVICE" ] || [ "$EXISTING_SERVICE" = "None" ]; then
  echo "Service does not exist. Creating new ECS service '$SERVICE_NAME'..."
  aws ecs create-service \
    --cli-input-json "file://${SERVICE_DEF_FILE}" \
    --region "$AWS_REGION"
  echo "ECS service created."
else
  echo "Service '$SERVICE_NAME' exists. Updating service with new task definition..."
  aws ecs update-service \
    --cluster "$CLUSTER_NAME" \
    --service "$SERVICE_NAME" \
    --task-definition "$TASK_DEF_ARN" \
    --region "$AWS_REGION" > /dev/null
  echo "ECS service updated."
fi

rm -f "$SERVICE_DEF_FILE"

# ---- Wait for service stability ----
echo ""
echo "Waiting for ECS service to become stable (this may take a few minutes)..."
aws ecs wait services-stable \
  --cluster "$CLUSTER_NAME" \
  --services "$SERVICE_NAME" \
  --region "$AWS_REGION"
echo "Service is stable."

# ---- Verify deployment ----
echo ""
echo "=============================================="
echo "  Deployment Verification"
echo "=============================================="
aws ecs describe-services \
  --cluster "$CLUSTER_NAME" \
  --services "$SERVICE_NAME" \
  --region "$AWS_REGION" \
  --query "services[0].{ServiceName:serviceName,Status:status,DesiredCount:desiredCount,RunningCount:runningCount,PendingCount:pendingCount}" \
  --output table

echo ""
echo "CloudWatch Log Group: $LOG_GROUP"
echo "  View logs: aws logs tail $LOG_GROUP --follow --region $AWS_REGION"
echo ""

if [ "$USE_LB" = true ]; then
  echo "Application Load Balancer DNS: http://$ALB_DNS"
  echo "Health Check URL: http://$ALB_DNS/health"
fi

echo ""
echo "=============================================="
echo "  Deployment Complete!"
echo "=============================================="
echo ""
echo "Troubleshooting tips:"
echo "  - View stopped tasks: aws ecs list-tasks --cluster $CLUSTER_NAME --desired-status STOPPED --region $AWS_REGION"
echo "  - Describe task failures: aws ecs describe-tasks --cluster $CLUSTER_NAME --tasks <task-arn> --region $AWS_REGION"
echo "  - Check CloudWatch logs: aws logs tail $LOG_GROUP --follow --region $AWS_REGION"
echo ""
