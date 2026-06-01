# ModResorts - AWS ECS Fargate Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
5. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
6. [ECS Fargate Setup](#ecs-fargate-setup)
7. [ECS Task Definition Explained](#ecs-task-definition-explained)
8. [ECS Service Configuration](#ecs-service-configuration)
9. [Deployment Walkthrough](#deployment-walkthrough)
10. [Troubleshooting](#troubleshooting)
11. [Scaling and Management](#scaling-and-management)
12. [Security Considerations](#security-considerations)

---

## Overview

ModResorts is a Java EE 7 web application packaged as a WAR file. This guide covers containerization and deployment to AWS ECS Fargate, a serverless container orchestration platform.

**Application Details:**
- **Technology Stack**: Java 8, Maven, Java EE 7
- **Package Type**: WAR (Web Application Archive)
- **Runtime**: Apache Tomcat 9.0
- **Application Port**: 8080
- **Health Endpoint**: `/health` and `/actuator/health`

---

## Prerequisites

### Required Software
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 1.29 or higher
- **AWS CLI**: Version 2.x
- **Maven**: Version 3.6 or higher (for local builds)
- **Java JDK**: Version 8 or higher

### AWS Account Requirements
- Active AWS account with appropriate permissions
- IAM user with permissions for:
  - ECS (Elastic Container Service)
  - ECR (Elastic Container Registry)
  - EC2 (for VPC, subnets, security groups)
  - IAM (for role creation)
  - CloudWatch Logs
  - Elastic Load Balancing (optional)

### Installation Instructions

#### Docker Installation
```bash
# Linux (Ubuntu/Debian)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# macOS
brew install docker

# Windows
# Download Docker Desktop from https://www.docker.com/products/docker-desktop
```

#### AWS CLI Installation
```bash
# Linux/macOS
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Windows
# Download installer from https://awscli.amazonaws.com/AWSCLIV2.msi

# Configure AWS CLI
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Default region, Output format
```

---

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd BackendServices
```

### 2. Build the Application Locally
```bash
# Using Maven
mvn clean package

# The WAR file will be created at: target/modresorts-2.0.0.war
```

### 3. Run with Docker Compose
```bash
# Build and start the application
docker-compose up --build

# Access the application
# http://localhost:8080

# Health check
curl http://localhost:8080/health

# Stop the application
docker-compose down
```

### 4. Environment Variables
Configure the following environment variables in `docker-compose.yml`:

```yaml
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m
  - DB_HOST=your-database-host
  - DB_PORT=5432
  - DB_NAME=modresorts
  - DB_USER=admin
  - DB_PASSWORD=your-password
```

---

## Building and Pushing Docker Images

### Option 1: Using build-push.sh (Linux/macOS)

```bash
cd BackendServices
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

**Script Workflow:**
1. Prompts for image tag (default: latest)
2. Asks to select registry (AWS ECR or Docker Hub)
3. Collects registry credentials
4. Builds Docker image
5. Authenticates with registry
6. Pushes image to registry

### Option 2: Using build-push.bat (Windows)

```cmd
cd BackendServices
scripts\build-push.bat
```

### Manual Build and Push

#### AWS ECR
```bash
# Set variables
AWS_REGION=us-east-1
AWS_ACCOUNT_ID=123456789012
ECR_REPO=modresorts
IMAGE_TAG=latest

# Authenticate with ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Create ECR repository (if not exists)
aws ecr create-repository --repository-name $ECR_REPO --region $AWS_REGION

# Build image
docker build -t modresorts:$IMAGE_TAG .

# Tag image
docker tag modresorts:$IMAGE_TAG \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG

# Push image
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
```

#### Docker Hub
```bash
# Login to Docker Hub
docker login -u your-username

# Build and tag
docker build -t your-username/modresorts:latest .

# Push
docker push your-username/modresorts:latest
```

---

## AWS ECS Fargate Prerequisites

### 1. VPC and Networking Setup

#### Create VPC (if not exists)
```bash
# Create VPC
VPC_ID=$(aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --region us-east-1 \
  --query 'Vpc.VpcId' \
  --output text)

# Enable DNS hostnames
aws ec2 modify-vpc-attribute \
  --vpc-id $VPC_ID \
  --enable-dns-hostnames

# Create Internet Gateway
IGW_ID=$(aws ec2 create-internet-gateway \
  --region us-east-1 \
  --query 'InternetGateway.InternetGatewayId' \
  --output text)

# Attach Internet Gateway to VPC
aws ec2 attach-internet-gateway \
  --vpc-id $VPC_ID \
  --internet-gateway-id $IGW_ID
```

#### Create Subnets
```bash
# Create public subnet 1 (us-east-1a)
SUBNET_1=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --query 'Subnet.SubnetId' \
  --output text)

# Create public subnet 2 (us-east-1b)
SUBNET_2=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --query 'Subnet.SubnetId' \
  --output text)

# Enable auto-assign public IP
aws ec2 modify-subnet-attribute \
  --subnet-id $SUBNET_1 \
  --map-public-ip-on-launch

aws ec2 modify-subnet-attribute \
  --subnet-id $SUBNET_2 \
  --map-public-ip-on-launch

# Create route table
ROUTE_TABLE_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --query 'RouteTable.RouteTableId' \
  --output text)

# Add route to Internet Gateway
aws ec2 create-route \
  --route-table-id $ROUTE_TABLE_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $IGW_ID

# Associate route table with subnets
aws ec2 associate-route-table \
  --subnet-id $SUBNET_1 \
  --route-table-id $ROUTE_TABLE_ID

aws ec2 associate-route-table \
  --subnet-id $SUBNET_2 \
  --route-table-id $ROUTE_TABLE_ID
```

#### Create Security Group
```bash
# Create security group
SECURITY_GROUP_ID=$(aws ec2 create-security-group \
  --group-name modresorts-sg \
  --description "Security group for ModResorts ECS tasks" \
  --vpc-id $VPC_ID \
  --query 'GroupId' \
  --output text)

# Allow inbound HTTP traffic (port 80)
aws ec2 authorize-security-group-ingress \
  --group-id $SECURITY_GROUP_ID \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# Allow inbound traffic on application port (8080)
aws ec2 authorize-security-group-ingress \
  --group-id $SECURITY_GROUP_ID \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow all outbound traffic (default)
```

### 2. IAM Roles Setup

#### ECS Task Execution Role
This role allows ECS to pull images from ECR and write logs to CloudWatch.

```bash
# Create trust policy document
cat > ecs-task-execution-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach AWS managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ECS Task Role (Optional)
This role grants permissions to the application running in the container.

```bash
# Create task role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach policies as needed (e.g., S3, DynamoDB access)
# Example: S3 read access
aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
```

### 3. CloudWatch Logs Setup

```bash
# Create log group
aws logs create-log-group \
  --log-group-name /ecs/modresorts \
  --region us-east-1

# Set retention policy (optional)
aws logs put-retention-policy \
  --log-group-name /ecs/modresorts \
  --retention-in-days 7 \
  --region us-east-1
```

---

## ECS Fargate Setup

### Understanding ECS Components

1. **Cluster**: Logical grouping of tasks or services
2. **Task Definition**: Blueprint for your application (like a Dockerfile for ECS)
3. **Service**: Maintains desired number of tasks running
4. **Task**: Running instance of a task definition

### Create ECS Cluster

```bash
# Create Fargate cluster
aws ecs create-cluster \
  --cluster-name modresorts-cluster \
  --region us-east-1
```

---

## ECS Task Definition Explained

The task definition (`ecs/task-definition.json`) defines how your container runs.

### Key Components

#### 1. Launch Type Configuration
```json
{
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc"
}
```
- **FARGATE**: Serverless compute engine
- **awsvpc**: Each task gets its own ENI (Elastic Network Interface)

#### 2. CPU and Memory
```json
{
  "cpu": "512",
  "memory": "1024"
}
```

**Valid Fargate CPU/Memory Combinations:**

| CPU (vCPU) | Memory (MB) |
|------------|-------------|
| 256 (.25)  | 512, 1024, 2048 |
| 512 (.5)   | 1024, 2048, 3072, 4096 |
| 1024 (1)   | 2048-8192 (increments of 1024) |
| 2048 (2)   | 4096-16384 (increments of 1024) |
| 4096 (4)   | 8192-30720 (increments of 1024) |

#### 3. Execution Role
```json
{
  "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole"
}
```
Allows ECS to:
- Pull images from ECR
- Write logs to CloudWatch
- Retrieve secrets from Secrets Manager

#### 4. Container Definition
```json
{
  "containerDefinitions": [
    {
      "name": "modresorts",
      "image": "IMAGE_URI",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [...],
      "logConfiguration": {...}
    }
  ]
}
```

#### 5. Logging Configuration
```json
{
  "logConfiguration": {
    "logDriver": "awslogs",
    "options": {
      "awslogs-group": "/ecs/modresorts",
      "awslogs-region": "us-east-1",
      "awslogs-stream-prefix": "ecs"
    }
  }
}
```

---

## ECS Service Configuration

The service definition (`ecs/service-definition.json`) manages task lifecycle.

### Key Components

#### 1. Launch Type
```json
{
  "launchType": "FARGATE"
}
```

#### 2. Network Configuration
```json
{
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-xxx", "subnet-yyy"],
      "securityGroups": ["sg-xxx"],
      "assignPublicIp": "ENABLED"
    }
  }
}
```
- **subnets**: At least 2 subnets in different AZs for high availability
- **securityGroups**: Controls inbound/outbound traffic
- **assignPublicIp**: Required if tasks need internet access

#### 3. Deployment Configuration
```json
{
  "deploymentConfiguration": {
    "maximumPercent": 200,
    "minimumHealthyPercent": 50
  }
}
```
- **maximumPercent**: Maximum tasks during deployment (200% = double capacity)
- **minimumHealthyPercent**: Minimum healthy tasks during deployment

#### 4. Load Balancer (Optional)
```json
{
  "loadBalancers": [
    {
      "targetGroupArn": "arn:aws:elasticloadbalancing:...",
      "containerName": "modresorts",
      "containerPort": 8080
    }
  ],
  "healthCheckGracePeriodSeconds": 300
}
```

---

## Deployment Walkthrough

### Step-by-Step Deployment

#### Step 1: Build and Push Image
```bash
# Run build script
./scripts/build-push.sh

# Select registry (1 for ECR, 2 for Docker Hub)
# Enter credentials and configuration
# Script will build and push the image
```

#### Step 2: Deploy to ECS
```bash
# Run deployment script
./scripts/deploy-image.sh

# Follow prompts:
# - AWS region
# - ECS cluster name
# - VPC and subnet IDs
# - Security group ID
# - Docker image URI
# - Database configuration
# - Load balancer (y/n)
```

#### Step 3: Monitor Deployment
```bash
# Check service status
aws ecs describe-services \
  --cluster modresorts-cluster \
  --services modresorts-service \
  --region us-east-1

# View running tasks
aws ecs list-tasks \
  --cluster modresorts-cluster \
  --service-name modresorts-service \
  --region us-east-1

# View logs
aws logs tail /ecs/modresorts --follow --region us-east-1
```

#### Step 4: Verify Application
```bash
# If using load balancer
curl http://<ALB-DNS-NAME>/health

# Expected response:
# {"status":"UP","application":"ModResorts"}
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Task Fails to Start

**Symptom**: Tasks transition to STOPPED state immediately

**Possible Causes**:
- Invalid CPU/memory combination
- Image pull errors
- Missing IAM permissions

**Solution**:
```bash
# Check task stopped reason
aws ecs describe-tasks \
  --cluster modresorts-cluster \
  --tasks <task-id> \
  --region us-east-1 \
  --query 'tasks[0].stoppedReason'

# Check CloudWatch logs
aws logs tail /ecs/modresorts --region us-east-1
```

#### 2. Cannot Pull Image from ECR

**Symptom**: "CannotPullContainerError"

**Solution**:
```bash
# Verify execution role has ECR permissions
aws iam get-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-name AmazonECSTaskExecutionRolePolicy

# Verify image exists in ECR
aws ecr describe-images \
  --repository-name modresorts \
  --region us-east-1
```

#### 3. Network Connectivity Issues

**Symptom**: Tasks cannot reach internet or other services

**Solution**:
```bash
# Verify security group allows outbound traffic
aws ec2 describe-security-groups \
  --group-ids <security-group-id>

# Verify subnets have route to Internet Gateway
aws ec2 describe-route-tables \
  --filters "Name=association.subnet-id,Values=<subnet-id>"

# Ensure assignPublicIp is ENABLED
```

#### 4. Health Check Failures

**Symptom**: Tasks fail health checks and restart

**Solution**:
```bash
# Test health endpoint locally
docker run -p 8080:8080 modresorts:latest
curl http://localhost:8080/health

# Increase health check grace period
# Edit service-definition.json:
"healthCheckGracePeriodSeconds": 300

# Check application logs
aws logs tail /ecs/modresorts --follow
```

#### 5. Invalid CPU/Memory Configuration

**Symptom**: "Invalid CPU or memory value specified"

**Solution**:
Use valid Fargate combinations:
```json
{
  "cpu": "512",
  "memory": "1024"
}
```

#### 6. Service Update Fails

**Symptom**: Service update stuck or fails

**Solution**:
```bash
# Force new deployment
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --force-new-deployment \
  --region us-east-1

# Check service events
aws ecs describe-services \
  --cluster modresorts-cluster \
  --services modresorts-service \
  --region us-east-1 \
  --query 'services[0].events[0:10]'
```

---

## Scaling and Management

### Manual Scaling

```bash
# Scale service to 5 tasks
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --desired-count 5 \
  --region us-east-1
```

### Auto Scaling

#### 1. Register Scalable Target
```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/modresorts-cluster/modresorts-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10 \
  --region us-east-1
```

#### 2. Create Scaling Policy (CPU-based)
```bash
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/modresorts-cluster/modresorts-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleInCooldown": 300,
    "ScaleOutCooldown": 60
  }' \
  --region us-east-1
```

### Blue/Green Deployments

```bash
# Create new task definition revision
aws ecs register-task-definition \
  --cli-input-json file://ecs/task-definition.json

# Update service with new task definition
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --task-definition modresorts-task:2 \
  --region us-east-1

# ECS will gradually replace old tasks with new ones
```

### Rolling Updates

Configure in service definition:
```json
{
  "deploymentConfiguration": {
    "maximumPercent": 200,
    "minimumHealthyPercent": 100
  }
}
```
- Ensures zero downtime during updates
- Starts new tasks before stopping old ones

---

## Security Considerations

### 1. Container Security

#### Use Non-Root User
The Dockerfile already includes:
```dockerfile
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
```

#### Scan Images for Vulnerabilities
```bash
# Using AWS ECR image scanning
aws ecr start-image-scan \
  --repository-name modresorts \
  --image-id imageTag=latest \
  --region us-east-1

# View scan results
aws ecr describe-image-scan-findings \
  --repository-name modresorts \
  --image-id imageTag=latest \
  --region us-east-1
```

### 2. Network Security

#### Security Group Best Practices
```bash
# Restrict inbound traffic to specific sources
aws ec2 authorize-security-group-ingress \
  --group-id $SECURITY_GROUP_ID \
  --protocol tcp \
  --port 8080 \
  --source-group $ALB_SECURITY_GROUP_ID

# Remove public access if using ALB
aws ec2 revoke-security-group-ingress \
  --group-id $SECURITY_GROUP_ID \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0
```

#### Use Private Subnets (Production)
- Place ECS tasks in private subnets
- Use NAT Gateway for outbound internet access
- Only ALB in public subnets

### 3. Secrets Management

#### Use AWS Secrets Manager
```bash
# Create secret
aws secretsmanager create-secret \
  --name modresorts/db-password \
  --secret-string "your-secure-password" \
  --region us-east-1

# Reference in task definition
{
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:modresorts/db-password"
    }
  ]
}
```

### 4. IAM Best Practices

- Use least privilege principle
- Separate execution role and task role
- Regularly rotate credentials
- Enable CloudTrail for audit logging

### 5. Logging and Monitoring

#### Enable Container Insights
```bash
aws ecs update-cluster-settings \
  --cluster modresorts-cluster \
  --settings name=containerInsights,value=enabled \
  --region us-east-1
```

#### Set Up CloudWatch Alarms
```bash
# CPU utilization alarm
aws cloudwatch put-metric-alarm \
  --alarm-name modresorts-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=ServiceName,Value=modresorts-service Name=ClusterName,Value=modresorts-cluster
```

---

## Technology-Specific Notes

### Java EE / Tomcat Considerations

#### 1. JVM Memory Settings
The Dockerfile includes optimized JVM settings:
```dockerfile
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

**Recommendations**:
- Set `-Xmx` to ~75% of container memory
- Use `-XX:+UseContainerSupport` for container awareness
- Monitor heap usage with CloudWatch

#### 2. Tomcat Configuration
Default Tomcat settings are suitable for most cases. For customization:

```bash
# Create custom server.xml
# Mount as volume in docker-compose.yml or copy in Dockerfile
COPY server.xml /usr/local/tomcat/conf/server.xml
```

#### 3. Session Management
For multi-instance deployments:
- Use sticky sessions (ALB session affinity)
- Or implement distributed session management (Redis, DynamoDB)

#### 4. Graceful Shutdown
Tomcat handles SIGTERM gracefully. Ensure:
```json
{
  "stopTimeout": 120
}
```
in task definition to allow time for in-flight requests.

### Database Connectivity

#### Connection Pooling
Configure in `context.xml`:
```xml
<Resource name="jdbc/ModResortsDB"
          auth="Container"
          type="javax.sql.DataSource"
          maxTotal="20"
          maxIdle="10"
          maxWaitMillis="10000"
          username="${DB_USER}"
          password="${DB_PASSWORD}"
          driverClassName="org.postgresql.Driver"
          url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"/>
```

#### Use RDS for Production
```bash
# Create RDS instance
aws rds create-db-instance \
  --db-instance-identifier modresorts-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username admin \
  --master-user-password <password> \
  --allocated-storage 20 \
  --vpc-security-group-ids $SECURITY_GROUP_ID \
  --db-subnet-group-name <subnet-group>
```

---

## Additional Resources

### AWS Documentation
- [ECS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [ECS Task Definitions](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html)
- [ECS Service Definition](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service_definition_parameters.html)

### Monitoring and Logging
- [CloudWatch Container Insights](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights.html)
- [ECS CloudWatch Metrics](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/cloudwatch-metrics.html)

### Best Practices
- [ECS Best Practices Guide](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/intro.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)

---

## Support and Maintenance

### Regular Maintenance Tasks

1. **Update Base Images**
   ```bash
   # Rebuild with latest base image
   docker build --no-cache -t modresorts:latest .
   ```

2. **Rotate Secrets**
   ```bash
   # Update secrets in Secrets Manager
   aws secretsmanager update-secret \
     --secret-id modresorts/db-password \
     --secret-string "new-password"
   ```

3. **Review Logs**
   ```bash
   # Check for errors
   aws logs filter-log-events \
     --log-group-name /ecs/modresorts \
     --filter-pattern "ERROR" \
     --region us-east-1
   ```

4. **Monitor Costs**
   ```bash
   # View ECS costs
   aws ce get-cost-and-usage \
     --time-period Start=2024-01-01,End=2024-01-31 \
     --granularity MONTHLY \
     --metrics BlendedCost \
     --filter file://ecs-cost-filter.json
   ```

### Getting Help

- **AWS Support**: https://console.aws.amazon.com/support/
- **ECS Forums**: https://forums.aws.amazon.com/forum.jspa?forumID=187
- **Stack Overflow**: Tag questions with `amazon-ecs` and `aws-fargate`

---

## Conclusion

This guide provides comprehensive instructions for deploying ModResorts to AWS ECS Fargate. Follow the steps carefully, and refer to the troubleshooting section for common issues.

For production deployments, ensure you:
- Use private subnets for ECS tasks
- Implement proper secrets management
- Enable monitoring and alerting
- Set up auto-scaling
- Regularly update and patch containers
- Follow security best practices

**Happy Deploying! 🚀**
