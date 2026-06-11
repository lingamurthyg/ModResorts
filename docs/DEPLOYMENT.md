# ModResorts - AWS ECS Fargate Deployment Guide

## Overview

This guide covers the complete deployment of the **ModResorts** Java EE web application to **AWS ECS Fargate**. ModResorts is a Java 8 Maven WAR application packaged with Apache Tomcat, containerized using a multi-stage Docker build.

- **Application**: ModResorts (modresorts-2.0.0.war)
- **Java Version**: 8
- **Build Tool**: Maven
- **Packaging**: WAR (deployed on Apache Tomcat 9)
- **Application Port**: 8080
- **Health Endpoint**: `/health`
- **Target Platform**: AWS ECS Fargate

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Structure](#project-structure)
3. [Local Development with Docker Compose](#local-development-with-docker-compose)
4. [Build and Push Docker Image](#build-and-push-docker-image)
5. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
6. [ECS Task Definition Explained](#ecs-task-definition-explained)
7. [ECS Service Configuration](#ecs-service-configuration)
8. [ECS Fargate Deployment Walkthrough](#ecs-fargate-deployment-walkthrough)
9. [ECS-Specific Troubleshooting](#ecs-specific-troubleshooting)
10. [ECS Fargate Scaling and Management](#ecs-fargate-scaling-and-management)
11. [Configuration Management](#configuration-management)
12. [Security Considerations](#security-considerations)
13. [Java-Specific Notes](#java-specific-notes)

---

## Prerequisites

### Local Development Tools
- **Docker** 20.10+ and **Docker Compose** v2+
- **Java 8 JDK** (for local builds)
- **Apache Maven** 3.8+ (for local builds)

### AWS Deployment Tools
- **AWS CLI** v2 configured with appropriate permissions
- **AWS Account** with ECS, ECR, IAM, CloudWatch, and VPC access

### AWS IAM Permissions Required
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:*",
        "ecr:*",
        "iam:PassRole",
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "elasticloadbalancing:*",
        "ec2:DescribeVpcs",
        "ec2:DescribeSubnets",
        "ec2:DescribeSecurityGroups"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## Project Structure

```
BackendServices/
├── Dockerfile                    # Multi-stage Docker build
├── docker-compose.yml            # Local development compose file
├── .dockerignore                 # Docker build exclusions
├── pom.xml                       # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/acme/modres/ # Java source files
│       └── resources/            # Application resources
├── WebContent/                   # Web assets (HTML, JS, CSS, WEB-INF)
├── ecs/
│   ├── task-definition.json      # ECS Fargate task definition
│   └── service-definition.json   # ECS service definition
├── scripts/
│   ├── build-push.sh             # Linux/macOS build & push script
│   ├── build-push.bat            # Windows build & push script
│   ├── deploy-image.sh           # Linux/macOS ECS deploy script
│   └── deploy-image.bat          # Windows ECS deploy script
└── docs/
    └── DEPLOYMENT.md             # This file
```

---

## Local Development with Docker Compose

### Quick Start

```bash
# Build and start the application
docker compose up --build

# Run in background
docker compose up --build -d

# View logs
docker compose logs -f modresorts

# Stop the application
docker compose down
```

### Access the Application

| Endpoint | URL |
|----------|-----|
| Application Home | http://localhost:8080/ |
| Health Check | http://localhost:8080/health |
| Weather API | http://localhost:8080/resorts/weather?selectedCity=Paris |
| Availability | http://localhost:8080/resorts/availability?date=12/25/2024 |

### Environment Variables (Local)

Create a `.env` file in the project root for local overrides:

```env
WEATHER_API_KEY=your_weather_api_key_here
JNDI_PROVIDER_URL=
SERVER_DISPLAY_NAME=modresorts-local
SERVER_FULL_NAME=modresorts-local-container
```

---

## Build and Push Docker Image

### Linux/macOS

```bash
# Make script executable
chmod +x scripts/build-push.sh

# Run from project root
./scripts/build-push.sh
```

The script will prompt you to:
1. Enter an image tag (default: `latest`)
2. Select registry type (AWS ECR or Docker Hub)
3. Provide registry credentials and details

### Windows

```cmd
scripts\build-push.bat
```

### Manual Docker Build

```bash
# Build image
docker build -t modresorts:latest .

# Tag for ECR
docker tag modresorts:latest 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789.dkr.ecr.us-east-1.amazonaws.com
docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest
```

---

## AWS ECS Fargate Prerequisites

### 1. VPC and Networking

Ensure you have a VPC with:
- **At least 2 public or private subnets** in different Availability Zones
- **Internet Gateway** (for public subnets) or **NAT Gateway** (for private subnets)
- **Security Group** allowing inbound traffic on port 8080

```bash
# Create security group (example)
aws ec2 create-security-group \
  --group-name modresorts-sg \
  --description "ModResorts ECS Security Group" \
  --vpc-id vpc-xxxxxxxx

# Allow inbound HTTP on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow inbound HTTP on port 80 (if using ALB)
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxx \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0
```

### 2. IAM Roles

#### ECS Task Execution Role
This role allows ECS to pull images from ECR and write logs to CloudWatch.

```bash
# Create the execution role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'

# Attach the managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ECS Task Role (Optional)
For application-level AWS API access (e.g., S3, DynamoDB):

```bash
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'
```

### 3. ECR Repository

```bash
# Create ECR repository
aws ecr create-repository \
  --repository-name modresorts \
  --region us-east-1

# Get repository URI
aws ecr describe-repositories \
  --repository-names modresorts \
  --query "repositories[0].repositoryUri" \
  --output text
```

### 4. CloudWatch Log Group

```bash
aws logs create-log-group \
  --log-group-name /ecs/modresorts \
  --region us-east-1
```

---

## ECS Task Definition Explained

The task definition (`ecs/task-definition.json`) configures how the container runs on Fargate:

### Key Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| `family` | `modresorts-task` | Task definition family name |
| `requiresCompatibilities` | `["FARGATE"]` | Fargate launch type |
| `networkMode` | `awsvpc` | Required for Fargate |
| `cpu` | `"512"` | 0.5 vCPU |
| `memory` | `"1024"` | 1 GB RAM |
| `executionRoleArn` | ECS execution role | For ECR pull and CloudWatch logs |

### Valid Fargate CPU/Memory Combinations

| CPU | Valid Memory Options |
|-----|---------------------|
| 256 (.25 vCPU) | 512, 1024, 2048 MB |
| **512 (.5 vCPU)** | **1024, 2048, 3072, 4096 MB** ← Default |
| 1024 (1 vCPU) | 2048–8192 MB |
| 2048 (2 vCPU) | 4096–16384 MB |
| 4096 (4 vCPU) | 8192–30720 MB |

### Container Environment Variables

| Variable | Description |
|----------|-------------|
| `JAVA_OPTS` | JVM tuning flags |
| `TZ` | Timezone (UTC) |
| `WEATHER_API_KEY` | Weather Underground API key |
| `JNDI_PROVIDER_URL` | JNDI provider URL (if needed) |
| `SERVER_DISPLAY_NAME` | Server display name |
| `SERVER_FULL_NAME` | Server full name |

### Logging Configuration

Logs are sent to CloudWatch Logs:
- **Log Group**: `/ecs/modresorts`
- **Log Driver**: `awslogs`
- **Stream Prefix**: `ecs`

---

## ECS Service Configuration

The service definition (`ecs/service-definition.json`) manages how tasks are scheduled:

| Parameter | Value | Description |
|-----------|-------|-------------|
| `serviceName` | `modresorts-service` | ECS service name |
| `launchType` | `FARGATE` | Serverless compute |
| `desiredCount` | `2` | Number of running tasks |
| `maximumPercent` | `200` | Max tasks during deployment |
| `minimumHealthyPercent` | `50` | Min healthy tasks during deployment |
| `assignPublicIp` | `ENABLED` | Public IP for Fargate tasks |

---

## ECS Fargate Deployment Walkthrough

### Step 1: Configure AWS CLI

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### Step 2: Build and Push Image

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
# Select: 1 (AWS ECR)
# Enter: region, account ID, repository name, tag
```

### Step 3: Deploy to ECS Fargate

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The deploy script will prompt for:
- AWS Region
- ECS Cluster name
- VPC ID
- Subnet IDs (comma-separated)
- Security Group ID
- ECR Image URI
- Load Balancer preference (y/n)

### Step 4: Verify Deployment

```bash
# Check service status
aws ecs describe-services \
  --cluster modresorts-cluster \
  --services modresorts-service \
  --region us-east-1

# List running tasks
aws ecs list-tasks \
  --cluster modresorts-cluster \
  --service-name modresorts-service \
  --region us-east-1

# View application logs
aws logs tail /ecs/modresorts --follow --region us-east-1
```

### Step 5: Access the Application

If using a Load Balancer:
```
http://<ALB-DNS-NAME>/
http://<ALB-DNS-NAME>/health
```

If using direct task IP (development only):
```
http://<TASK-PUBLIC-IP>:8080/
http://<TASK-PUBLIC-IP>:8080/health
```

---

## ECS-Specific Troubleshooting

### Task Fails to Start

```bash
# List stopped tasks
aws ecs list-tasks \
  --cluster modresorts-cluster \
  --desired-status STOPPED \
  --region us-east-1

# Describe stopped task for failure reason
aws ecs describe-tasks \
  --cluster modresorts-cluster \
  --tasks <task-arn> \
  --region us-east-1 \
  --query "tasks[0].{StopCode:stopCode,StoppedReason:stoppedReason,Containers:containers[*].{Name:name,Reason:reason,ExitCode:exitCode}}"
```

### Common Error: "CannotPullContainerError"
- **Cause**: ECS cannot pull image from ECR
- **Fix**: Verify `executionRoleArn` has `AmazonECSTaskExecutionRolePolicy` attached
- **Fix**: Ensure task is in a subnet with internet access (public subnet with IGW, or private with NAT)

### Common Error: "ResourceInitializationError"
- **Cause**: Fargate cannot initialize the task
- **Fix**: Check VPC endpoints or NAT Gateway for private subnets
- **Fix**: Verify security group allows outbound HTTPS (443) for ECR/CloudWatch

### Common Error: Invalid CPU/Memory Combination
- **Cause**: Invalid Fargate CPU/memory values in task definition
- **Fix**: Use valid combinations (e.g., cpu: "512", memory: "1024")

### Application Not Responding
```bash
# Check task health
aws ecs describe-tasks \
  --cluster modresorts-cluster \
  --tasks <task-arn> \
  --region us-east-1

# Check CloudWatch logs for application errors
aws logs tail /ecs/modresorts --follow --region us-east-1

# Check if Tomcat started successfully
aws logs filter-log-events \
  --log-group-name /ecs/modresorts \
  --filter-pattern "Server startup" \
  --region us-east-1
```

### Network Connectivity Issues
- Verify security group allows inbound on port 8080
- Verify `assignPublicIp: ENABLED` for public subnets
- Check that subnets have route to internet (IGW or NAT)

### JVM Out of Memory
- Increase task memory in task definition (e.g., from 1024 to 2048)
- Adjust `JAVA_OPTS`: `-Xmx768m -Xms256m` for 1024MB task
- Monitor with CloudWatch Container Insights

---

## ECS Fargate Scaling and Management

### Manual Scaling

```bash
# Scale up to 4 tasks
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --desired-count 4 \
  --region us-east-1
```

### Auto Scaling

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/modresorts-cluster/modresorts-service \
  --min-capacity 1 \
  --max-capacity 10

# Create CPU-based scaling policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --scalable-dimension ecs:service:DesiredCount \
  --resource-id service/modresorts-cluster/modresorts-service \
  --policy-name modresorts-cpu-scaling \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleInCooldown": 300,
    "ScaleOutCooldown": 60
  }'
```

### Blue/Green Deployments

For zero-downtime deployments, use AWS CodeDeploy with ECS:

```bash
# Update service with new task definition (rolling update)
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --task-definition modresorts-task:NEW_REVISION \
  --deployment-configuration "maximumPercent=200,minimumHealthyPercent=100" \
  --region us-east-1
```

### Force New Deployment

```bash
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --force-new-deployment \
  --region us-east-1
```

---

## Configuration Management

### Environment Variables in ECS

Update environment variables in the task definition and re-register:

```bash
# Edit ecs/task-definition.json to update environment variables
# Then register new revision
aws ecs register-task-definition \
  --cli-input-json file://ecs/task-definition.json \
  --region us-east-1

# Update service to use new task definition
aws ecs update-service \
  --cluster modresorts-cluster \
  --service modresorts-service \
  --task-definition modresorts-task \
  --region us-east-1
```

### AWS Secrets Manager (Recommended for Sensitive Values)

```bash
# Store secret
aws secretsmanager create-secret \
  --name modresorts/weather-api-key \
  --secret-string "your-api-key-here" \
  --region us-east-1
```

Add to task definition `secrets` section:
```json
"secrets": [
  {
    "name": "WEATHER_API_KEY",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789:secret:modresorts/weather-api-key"
  }
]
```

---

## Security Considerations

### Container Security
- Application runs as non-root user (`appuser`)
- No unnecessary packages installed in runtime image
- Minimal attack surface with multi-stage build

### Network Security
- Use private subnets with NAT Gateway for production
- Restrict security group inbound rules to ALB only
- Enable VPC Flow Logs for network monitoring

### Image Security
- Regularly update base image (`eclipse-temurin:8-jdk`)
- Scan images with Amazon ECR image scanning:
  ```bash
  aws ecr start-image-scan \
    --repository-name modresorts \
    --image-id imageTag=latest \
    --region us-east-1
  ```
- Use specific image tags (not `latest`) in production

### IAM Security
- Follow least-privilege principle for task roles
- Rotate AWS credentials regularly
- Use IAM roles for service accounts (not access keys)

---

## Java-Specific Notes

### JVM Configuration for Containers

The application uses container-aware JVM flags:

```
-XX:+UseContainerSupport          # Respect container CPU/memory limits
-XX:MaxRAMPercentage=75.0         # Use 75% of container memory for heap
-XX:+UnlockExperimentalVMOptions  # Enable experimental JVM options
-Djava.security.egd=file:/dev/./urandom  # Faster random number generation
-Dfile.encoding=UTF-8             # Consistent character encoding
-Duser.timezone=UTC               # Consistent timezone
```

### Tomcat Configuration

- **Version**: Apache Tomcat 9.0.85
- **WAR Deployment**: `modresorts-2.0.0.war` deployed as `ROOT.war`
- **Context Path**: `/` (root context)
- **Startup Time**: Allow 60+ seconds for JVM and Tomcat initialization

### Application Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Application home page |
| `/health` | GET | Health check (returns `{"status":"UP"}`) |
| `/resorts/weather` | GET | Weather data (`?selectedCity=Paris`) |
| `/resorts/availability` | GET | Availability check (`?date=MM/DD/YYYY`) |
| `/resorts/welcome` | GET | Welcome message |
| `/upper` | GET | Upper case utility |

### Monitoring with CloudWatch

Enable Container Insights for detailed metrics:

```bash
aws ecs update-cluster-settings \
  --cluster modresorts-cluster \
  --settings name=containerInsights,value=enabled \
  --region us-east-1
```

Key metrics to monitor:
- `CPUUtilization` - JVM CPU usage
- `MemoryUtilization` - JVM heap + off-heap memory
- `RunningTaskCount` - Active task count
- Application logs in `/ecs/modresorts`

### Java 8 Upgrade Path

Consider upgrading to Java 11 or 17 for:
- Better container support (`-XX:+UseContainerSupport` is default in Java 11+)
- Improved G1GC garbage collector
- Better performance and security patches
- Long-term support (LTS) versions

To upgrade, update `pom.xml`:
```xml
<properties>
  <maven.compiler.source>11</maven.compiler.source>
  <maven.compiler.target>11</maven.compiler.target>
</properties>
```

And update `Dockerfile` builder stage:
```dockerfile
FROM maven:3.9.4-eclipse-temurin-11 AS builder
```
