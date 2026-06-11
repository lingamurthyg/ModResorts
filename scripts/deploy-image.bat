@echo off
setlocal enabledelayedexpansion

REM =============================================================================
REM deploy-image.bat - Deploy ModResorts to AWS ECS Fargate (Windows)
REM =============================================================================

set SERVICE_NAME=modresorts-service
set TASK_FAMILY=modresorts-task
set PROJECT_NAME=modresorts
set APP_PORT=8080
set LOG_GROUP=/ecs/modresorts

echo ==============================================
echo   ModResorts - AWS ECS Fargate Deployment
echo ==============================================
echo.

REM ---- Collect configuration ----
set /p AWS_REGION="Enter AWS Region [us-east-1]: "
if "!AWS_REGION!"=="" set AWS_REGION=us-east-1

set /p CLUSTER_NAME="Enter ECS Cluster name [modresorts-cluster]: "
if "!CLUSTER_NAME!"=="" set CLUSTER_NAME=modresorts-cluster

set /p VPC_ID="Enter VPC ID (e.g. vpc-xxxxxxxx): "
if "!VPC_ID!"=="" (
    echo ERROR: VPC ID is required.
    exit /b 1
)

set /p SUBNETS_INPUT="Enter Subnet IDs (comma-separated, e.g. subnet-aaa,subnet-bbb): "
if "!SUBNETS_INPUT!"=="" (
    echo ERROR: At least one subnet ID is required.
    exit /b 1
)

set /p SECURITY_GROUP="Enter Security Group ID (e.g. sg-xxxxxxxx): "
if "!SECURITY_GROUP!"=="" (
    echo ERROR: Security Group ID is required.
    exit /b 1
)

set /p IMAGE_URI="Enter full ECR Image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest): "
if "!IMAGE_URI!"=="" (
    echo ERROR: Image URI is required.
    exit /b 1
)

REM ---- Parse subnets ----
set SUBNET_1=
set SUBNET_2=
set SUBNET_COUNT=0
for %%s in (!SUBNETS_INPUT!) do (
    set /a SUBNET_COUNT+=1
    if !SUBNET_COUNT!==1 set SUBNET_1=%%s
    if !SUBNET_COUNT!==2 set SUBNET_2=%%s
)
if "!SUBNET_2!"=="" set SUBNET_2=!SUBNET_1!

REM ---- Get AWS Account ID ----
echo.
echo Fetching AWS Account ID...
for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text 2^>^&1') do set ACCOUNT_ID=%%i
echo Account ID: !ACCOUNT_ID!

REM ---- Ensure CloudWatch Log Group exists ----
echo.
echo Ensuring CloudWatch log group exists...
aws logs create-log-group --log-group-name %LOG_GROUP% --region !AWS_REGION! >nul 2>&1
echo Log group ready.

REM ---- Check / Create ECS Cluster ----
echo.
echo Checking ECS cluster '!CLUSTER_NAME!'...
for /f "tokens=*" %%i in ('aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! --query "clusters[0].status" --output text 2^>^&1') do set CLUSTER_STATUS=%%i

if not "!CLUSTER_STATUS!"=="ACTIVE" (
    echo Cluster not found or inactive. Creating cluster '!CLUSTER_NAME!'...
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    echo Cluster created.
) else (
    echo Cluster '!CLUSTER_NAME!' is active.
)

REM ---- Load Balancer ----
echo.
set /p NEED_LB="Do you need an Application Load Balancer for this service? (y/n) [n]: "
if "!NEED_LB!"=="" set NEED_LB=n
set USE_LB=false
set TARGET_GROUP_ARN=

if /i "!NEED_LB!"=="y" (
    set USE_LB=true
    echo.
    echo Creating Application Load Balancer...

    set ALB_NAME=%PROJECT_NAME%-alb
    for /f "tokens=*" %%i in ('aws elbv2 create-load-balancer --name !ALB_NAME! --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --scheme internet-facing --type application --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text 2^>^&1') do set ALB_ARN=%%i
    echo ALB created: !ALB_ARN!

    echo Creating Target Group...
    for /f "tokens=*" %%i in ('aws elbv2 create-target-group --name %PROJECT_NAME%-tg --protocol HTTP --port %APP_PORT% --vpc-id !VPC_ID! --target-type ip --health-check-path /health --health-check-interval-seconds 30 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text 2^>^&1') do set TARGET_GROUP_ARN=%%i
    echo Target Group created: !TARGET_GROUP_ARN!

    echo Creating ALB Listener on port 80...
    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions "Type=forward,TargetGroupArn=!TARGET_GROUP_ARN!" --region !AWS_REGION! >nul
    echo Listener created.

    for /f "tokens=*" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --region !AWS_REGION! --query "LoadBalancers[0].DNSName" --output text 2^>^&1') do set ALB_DNS=%%i
)

REM ---- Prepare task definition ----
echo.
echo Preparing ECS task definition...
copy ecs\task-definition.json ecs\task-definition-deploy.json >nul

powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{AWS_REGION}}', '!AWS_REGION!' | Set-Content ecs\task-definition-deploy.json"
powershell -Command "(Get-Content ecs\task-definition-deploy.json) -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content ecs\task-definition-deploy.json"

REM ---- Register Task Definition ----
echo Registering ECS task definition...
for /f "tokens=*" %%i in ('aws ecs register-task-definition --cli-input-json file://ecs/task-definition-deploy.json --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text 2^>^&1') do set TASK_DEF_ARN=%%i
echo Task definition registered: !TASK_DEF_ARN!
del ecs\task-definition-deploy.json >nul 2>&1

REM ---- Prepare service definition ----
echo.
echo Preparing ECS service definition...
copy ecs\service-definition.json ecs\service-definition-deploy.json >nul

powershell -Command "(Get-Content ecs\service-definition-deploy.json) -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' | Set-Content ecs\service-definition-deploy.json"
powershell -Command "(Get-Content ecs\service-definition-deploy.json) -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content ecs\service-definition-deploy.json"
powershell -Command "(Get-Content ecs\service-definition-deploy.json) -replace '{{SUBNET_1}}', '!SUBNET_1!' | Set-Content ecs\service-definition-deploy.json"
powershell -Command "(Get-Content ecs\service-definition-deploy.json) -replace '{{SUBNET_2}}', '!SUBNET_2!' | Set-Content ecs\service-definition-deploy.json"

REM ---- Handle Load Balancer in service definition ----
if "!USE_LB!"=="true" (
    powershell -Command "$svc = Get-Content ecs\service-definition-deploy.json | ConvertFrom-Json; $lb = @{targetGroupArn='!TARGET_GROUP_ARN!'; containerName='%PROJECT_NAME%'; containerPort=%APP_PORT%}; $svc | Add-Member -NotePropertyName loadBalancers -NotePropertyValue @($lb) -Force; $svc | Add-Member -NotePropertyName healthCheckGracePeriodSeconds -NotePropertyValue 300 -Force; $svc | ConvertTo-Json -Depth 10 | Set-Content ecs\service-definition-deploy.json"
)

REM ---- Check if service exists ----
echo.
echo Checking if ECS service '!SERVICE_NAME!' exists...
for /f "tokens=*" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[?status!='INACTIVE'].serviceName" --output text 2^>^&1') do set EXISTING_SERVICE=%%i

if "!EXISTING_SERVICE!"=="" (
    echo Service does not exist. Creating new ECS service '!SERVICE_NAME!'...
    aws ecs create-service --cli-input-json file://ecs/service-definition-deploy.json --region !AWS_REGION!
    echo ECS service created.
) else (
    echo Service '!SERVICE_NAME!' exists. Updating service with new task definition...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --region !AWS_REGION! >nul
    echo ECS service updated.
)

del ecs\service-definition-deploy.json >nul 2>&1

REM ---- Wait for service stability ----
echo.
echo Waiting for ECS service to become stable (this may take a few minutes)...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!
if !ERRORLEVEL! neq 0 (
    echo WARNING: Service stability wait timed out. Check ECS console for details.
) else (
    echo Service is stable.
)

REM ---- Verify deployment ----
echo.
echo ==============================================
echo   Deployment Verification
echo ==============================================
aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].{ServiceName:serviceName,Status:status,DesiredCount:desiredCount,RunningCount:runningCount,PendingCount:pendingCount}" --output table

echo.
echo CloudWatch Log Group: %LOG_GROUP%
echo   View logs: aws logs tail %LOG_GROUP% --follow --region !AWS_REGION!
echo.

if "!USE_LB!"=="true" (
    echo Application Load Balancer DNS: http://!ALB_DNS!
    echo Health Check URL: http://!ALB_DNS!/health
)

echo.
echo ==============================================
echo   Deployment Complete!
echo ==============================================
echo.
echo Troubleshooting tips:
echo   - View stopped tasks: aws ecs list-tasks --cluster !CLUSTER_NAME! --desired-status STOPPED --region !AWS_REGION!
echo   - Check CloudWatch logs: aws logs tail %LOG_GROUP% --follow --region !AWS_REGION!
echo.

endlocal
