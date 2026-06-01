#!/bin/bash

# AWS ECS Fargate Deployment Script for ModResorts Application
# This script deploys the containerized application to AWS ECS Fargate

set -e
set -o pipefail

echo "=========================================="
echo "ModResorts - AWS ECS Fargate Deployment"
echo "=========================================="
echo ""

# Project configuration
PROJECT_NAME="modresorts"
TASK_FAMILY="modresorts-task"
SERVICE_NAME="modresorts-service"

# Prompt for AWS configuration
echo "=== AWS Configuration ==="
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
read -p "Enter ECS cluster name: " CLUSTER_NAME
echo ""

# Get AWS Account ID
echo "Retrieving AWS Account ID..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "AWS Account ID: $ACCOUNT_ID"
echo ""

# Prompt for network configuration
echo "=== Network Configuration ==="
read -p "Enter VPC ID: " VPC_ID
read -p "Enter Subnet 1 ID: " SUBNET_1
read -p "Enter Subnet 2 ID: " SUBNET_2
read -p "Enter Security Group ID: " SECURITY_GROUP
echo ""

# Prompt for image URI
echo "=== Container Image ==="
read -p "Enter Docker image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): " IMAGE_URI
echo ""

# Prompt for database configuration
echo "=== Database Configuration ==="
read -p "Enter database host (default: localhost): " DB_HOST
DB_HOST=${DB_HOST:-localhost}
read -p "Enter database port (default: 5432): " DB_PORT
DB_PORT=${DB_PORT:-5432}
read -p "Enter database name (default: modresorts): " DB_NAME
DB_NAME=${DB_NAME:-modresorts}
read -p "Enter database user (default: admin): " DB_USER
DB_USER=${DB_USER:-admin}
read -sp "Enter database password: " DB_PASSWORD
echo ""
echo ""

# Check if ECS cluster exists, create if not
echo "=== ECS Cluster Setup ==="
echo "Checking if cluster exists..."
aws ecs describe-clusters --clusters "$CLUSTER_NAME" --region "$AWS_REGION" >/dev/null 2>&1 || {
    echo "Creating ECS cluster: $CLUSTER_NAME"
    aws ecs create-cluster --cluster-name "$CLUSTER_NAME" --region "$AWS_REGION"
    echo "Cluster created successfully"
}
echo "Cluster ready: $CLUSTER_NAME"
echo ""

# Create CloudWatch log group
echo "=== CloudWatch Logs Setup ==="
LOG_GROUP="/ecs/$PROJECT_NAME"
echo "Creating CloudWatch log group: $LOG_GROUP"
aws logs create-log-group --log-group-name "$LOG_GROUP" --region "$AWS_REGION" 2>/dev/null || echo "Log group already exists"
echo ""

# Load balancer configuration
echo "=== Load Balancer Configuration ==="
read -p "Do you need a load balancer for this service? (y/n): " NEED_LB

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo ""
    echo "Creating Application Load Balancer and Target Group..."
    
    # Create ALB
    ALB_NAME="${PROJECT_NAME}-alb"
    echo "Creating ALB: $ALB_NAME"
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name "$ALB_NAME" \
        --subnets "$SUBNET_1" "$SUBNET_2" \
        --security-groups "$SECURITY_GROUP" \
        --scheme internet-facing \
        --type application \
        --ip-address-type ipv4 \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].LoadBalancerArn' \
        --output text)
    
    echo "ALB created: $ALB_ARN"
    
    # Create Target Group with target-type ip (required for Fargate)
    TG_NAME="${PROJECT_NAME}-tg"
    echo "Creating Target Group: $TG_NAME"
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name "$TG_NAME" \
        --protocol HTTP \
        --port 8080 \
        --vpc-id "$VPC_ID" \
        --target-type ip \
        --health-check-enabled \
        --health-check-protocol HTTP \
        --health-check-path "/health" \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --region "$AWS_REGION" \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text)
    
    echo "Target Group created: $TARGET_GROUP_ARN"
    
    # Create Listener
    echo "Creating ALB Listener..."
    aws elbv2 create-listener \
        --load-balancer-arn "$ALB_ARN" \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn="$TARGET_GROUP_ARN" \
        --region "$AWS_REGION" >/dev/null
    
    echo "Listener created successfully"
    
    # Get ALB DNS name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns "$ALB_ARN" \
        --region "$AWS_REGION" \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    echo "ALB DNS Name: $ALB_DNS"
    echo ""
    
    # Update service definition with load balancer
    sed -i "s|{{TARGET_GROUP_ARN}}|$TARGET_GROUP_ARN|g" ecs/service-definition.json
else
    echo "Skipping load balancer setup"
    echo ""
    
    # Remove loadBalancers section from service definition
    python3 -c "
import json
with open('ecs/service-definition.json', 'r') as f:
    data = json.load(f)
data.pop('loadBalancers', None)
data.pop('healthCheckGracePeriodSeconds', None)
with open('ecs/service-definition.json', 'w') as f:
    json.dump(data, f, indent=2)
" 2>/dev/null || {
    # Fallback if python3 not available
    echo "Warning: Could not remove loadBalancers section. Please edit ecs/service-definition.json manually."
}
fi

# Replace placeholders in task definition
echo "=== Preparing Task Definition ==="
cp ecs/task-definition.json ecs/task-definition-temp.json

sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" ecs/task-definition-temp.json
sed -i "s|{{AWS_REGION}}|$AWS_REGION|g" ecs/task-definition-temp.json
sed -i "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" ecs/task-definition-temp.json
sed -i "s|{{DB_HOST}}|$DB_HOST|g" ecs/task-definition-temp.json
sed -i "s|{{DB_PORT}}|$DB_PORT|g" ecs/task-definition-temp.json
sed -i "s|{{DB_NAME}}|$DB_NAME|g" ecs/task-definition-temp.json
sed -i "s|{{DB_USER}}|$DB_USER|g" ecs/task-definition-temp.json
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" ecs/task-definition-temp.json

echo "Task definition prepared"
echo ""

# Register task definition
echo "=== Registering Task Definition ==="
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://ecs/task-definition-temp.json \
    --region "$AWS_REGION" \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

echo "Task definition registered: $TASK_DEF_ARN"
echo ""

# Clean up temp file
rm -f ecs/task-definition-temp.json

# Replace placeholders in service definition
echo "=== Preparing Service Definition ==="
cp ecs/service-definition.json ecs/service-definition-temp.json

sed -i "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_1}}|$SUBNET_1|g" ecs/service-definition-temp.json
sed -i "s|{{SUBNET_2}}|$SUBNET_2|g" ecs/service-definition-temp.json
sed -i "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" ecs/service-definition-temp.json

echo "Service definition prepared"
echo ""

# Check if service exists
echo "=== Checking Service Status ==="
SERVICE_EXISTS=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[?status==`ACTIVE`].serviceName' \
    --output text 2>/dev/null || echo "")

if [ -z "$SERVICE_EXISTS" ]; then
    echo "Service does not exist. Creating new service..."
    aws ecs create-service \
        --cli-input-json file://ecs/service-definition-temp.json \
        --region "$AWS_REGION" >/dev/null
    echo "Service created: $SERVICE_NAME"
else
    echo "Service exists. Updating service..."
    aws ecs update-service \
        --cluster "$CLUSTER_NAME" \
        --service "$SERVICE_NAME" \
        --task-definition "$TASK_DEF_ARN" \
        --desired-count 2 \
        --region "$AWS_REGION" >/dev/null
    echo "Service updated: $SERVICE_NAME"
fi

echo ""

# Clean up temp file
rm -f ecs/service-definition-temp.json

# Wait for service to stabilize
echo "=== Waiting for Service Stability ==="
echo "This may take a few minutes..."
aws ecs wait services-stable \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION"

echo "Service is stable"
echo ""

# Verify deployment
echo "=== Deployment Verification ==="
aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$AWS_REGION" \
    --query 'services[0].[serviceName,status,runningCount,desiredCount]' \
    --output table

echo ""
echo "=========================================="
echo "DEPLOYMENT SUCCESSFUL!"
echo "=========================================="
echo "Service Name: $SERVICE_NAME"
echo "Cluster: $CLUSTER_NAME"
echo "Region: $AWS_REGION"
echo "Task Definition: $TASK_DEF_ARN"
echo "CloudWatch Logs: $LOG_GROUP"

if [[ "$NEED_LB" =~ ^[Yy]$ ]]; then
    echo ""
    echo "Load Balancer DNS: $ALB_DNS"
    echo "Application URL: http://$ALB_DNS"
    echo ""
    echo "Note: It may take a few minutes for the ALB to become healthy"
fi

echo ""
echo "To view logs:"
echo "  aws logs tail $LOG_GROUP --follow --region $AWS_REGION"
echo ""
echo "To check service status:"
echo "  aws ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE_NAME --region $AWS_REGION"
echo "=========================================="
