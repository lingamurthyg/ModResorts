@echo off
setlocal enabledelayedexpansion

REM AWS ECS Fargate Deployment Script for ModResorts Application (Windows)
REM This script deploys the containerized application to AWS ECS Fargate

echo ==========================================
echo ModResorts - AWS ECS Fargate Deployment
echo ==========================================
echo.

REM Project configuration
set PROJECT_NAME=modresorts
set TASK_FAMILY=modresorts-task
set SERVICE_NAME=modresorts-service

REM Prompt for AWS configuration
echo === AWS Configuration ===
set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter ECS cluster name: "
echo.

REM Get AWS Account ID
echo Retrieving AWS Account ID...
for /f "delims=" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
echo AWS Account ID: !ACCOUNT_ID!
echo.

REM Prompt for network configuration
echo === Network Configuration ===
set /p VPC_ID="Enter VPC ID: "
set /p SUBNET_1="Enter Subnet 1 ID: "
set /p SUBNET_2="Enter Subnet 2 ID: "
set /p SECURITY_GROUP="Enter Security Group ID: "
echo.

REM Prompt for image URI
echo === Container Image ===
set /p IMAGE_URI="Enter Docker image URI: "
echo.

REM Prompt for database configuration
echo === Database Configuration ===
set /p DB_HOST="Enter database host (default: localhost): "
if "!DB_HOST!"=="" set DB_HOST=localhost
set /p DB_PORT="Enter database port (default: 5432): "
if "!DB_PORT!"=="" set DB_PORT=5432
set /p DB_NAME="Enter database name (default: modresorts): "
if "!DB_NAME!"=="" set DB_NAME=modresorts
set /p DB_USER="Enter database user (default: admin): "
if "!DB_USER!"=="" set DB_USER=admin
set /p DB_PASSWORD="Enter database password: "
echo.

REM Check if ECS cluster exists, create if not
echo === ECS Cluster Setup ===
echo Checking if cluster exists...
aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Creating ECS cluster: !CLUSTER_NAME!
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create cluster
        exit /b 1
    )
    echo Cluster created successfully
)
echo Cluster ready: !CLUSTER_NAME!
echo.

REM Create CloudWatch log group
echo === CloudWatch Logs Setup ===
set LOG_GROUP=/ecs/!PROJECT_NAME!
echo Creating CloudWatch log group: !LOG_GROUP!
aws logs create-log-group --log-group-name !LOG_GROUP! --region !AWS_REGION! 2>nul
echo.

REM Load balancer configuration
echo === Load Balancer Configuration ===
set /p NEED_LB="Do you need a load balancer for this service? (y/n): "

if /i "!NEED_LB!"=="y" (
    echo.
    echo Creating Application Load Balancer and Target Group...
    
    REM Create ALB
    set ALB_NAME=!PROJECT_NAME!-alb
    echo Creating ALB: !ALB_NAME!
    for /f "delims=" %%i in ('aws elbv2 create-load-balancer --name !ALB_NAME! --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --scheme internet-facing --type application --ip-address-type ipv4 --region !AWS_REGION! --query LoadBalancers[0].LoadBalancerArn --output text') do set ALB_ARN=%%i
    echo ALB created: !ALB_ARN!
    
    REM Create Target Group
    set TG_NAME=!PROJECT_NAME!-tg
    echo Creating Target Group: !TG_NAME!
    for /f "delims=" %%i in ('aws elbv2 create-target-group --name !TG_NAME! --protocol HTTP --port 8080 --vpc-id !VPC_ID! --target-type ip --health-check-enabled --health-check-protocol HTTP --health-check-path /health --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region !AWS_REGION! --query TargetGroups[0].TargetGroupArn --output text') do set TARGET_GROUP_ARN=%%i
    echo Target Group created: !TARGET_GROUP_ARN!
    
    REM Create Listener
    echo Creating ALB Listener...
    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn=!TARGET_GROUP_ARN! --region !AWS_REGION! >nul
    echo Listener created successfully
    
    REM Get ALB DNS name
    for /f "delims=" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --region !AWS_REGION! --query LoadBalancers[0].DNSName --output text') do set ALB_DNS=%%i
    echo ALB DNS Name: !ALB_DNS!
    echo.
    
    REM Update service definition with load balancer
    powershell -Command "(Get-Content ecs\service-definition.json) -replace '{{TARGET_GROUP_ARN}}', '!TARGET_GROUP_ARN!' | Set-Content ecs\service-definition.json"
) else (
    echo Skipping load balancer setup
    echo.
)

REM Replace placeholders in task definition
echo === Preparing Task Definition ===
copy ecs\task-definition.json ecs\task-definition-temp.json >nul

powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_USER}}', '!DB_USER!' | Set-Content ecs\task-definition-temp.json"
powershell -Command "(Get-Content ecs\task-definition-temp.json) -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content ecs\task-definition-temp.json"

echo Task definition prepared
echo.

REM Register task definition
echo === Registering Task Definition ===
for /f "delims=" %%i in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition-temp.json --region !AWS_REGION! --query taskDefinition.taskDefinitionArn --output text') do set TASK_DEF_ARN=%%i
echo Task definition registered: !TASK_DEF_ARN!
echo.

REM Clean up temp file
del ecs\task-definition-temp.json

REM Replace placeholders in service definition
echo === Preparing Service Definition ===
copy ecs\service-definition.json ecs\service-definition-temp.json >nul

powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content ecs\service-definition-temp.json"
powershell -Command "(Get-Content ecs\service-definition-temp.json) -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content ecs\service-definition-temp.json"

echo Service definition prepared
echo.

REM Check if service exists
echo === Checking Service Status ===
for /f "delims=" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query services[?status==`ACTIVE`].serviceName --output text 2^>nul') do set SERVICE_EXISTS=%%i

if "!SERVICE_EXISTS!"=="" (
    echo Service does not exist. Creating new service...
    aws ecs create-service --cli-input-json file://ecs/service-definition-temp.json --region !AWS_REGION! >nul
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create service
        del ecs\service-definition-temp.json
        exit /b 1
    )
    echo Service created: !SERVICE_NAME!
) else (
    echo Service exists. Updating service...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --desired-count 2 --region !AWS_REGION! >nul
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to update service
        del ecs\service-definition-temp.json
        exit /b 1
    )
    echo Service updated: !SERVICE_NAME!
)

echo.

REM Clean up temp file
del ecs\service-definition-temp.json

REM Wait for service to stabilize
echo === Waiting for Service Stability ===
echo This may take a few minutes...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!
echo Service is stable
echo.

REM Verify deployment
echo === Deployment Verification ===
aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query services[0].[serviceName,status,runningCount,desiredCount] --output table

echo.
echo ==========================================
echo DEPLOYMENT SUCCESSFUL!
echo ==========================================
echo Service Name: !SERVICE_NAME!
echo Cluster: !CLUSTER_NAME!
echo Region: !AWS_REGION!
echo Task Definition: !TASK_DEF_ARN!
echo CloudWatch Logs: !LOG_GROUP!

if /i "!NEED_LB!"=="y" (
    echo.
    echo Load Balancer DNS: !ALB_DNS!
    echo Application URL: http://!ALB_DNS!
    echo.
    echo Note: It may take a few minutes for the ALB to become healthy
)

echo.
echo To view logs:
echo   aws logs tail !LOG_GROUP! --follow --region !AWS_REGION!
echo.
echo To check service status:
echo   aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!
echo ==========================================

endlocal
