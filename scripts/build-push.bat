@echo off
setlocal enabledelayedexpansion

REM =============================================================================
REM build-push.bat - Build and Push Docker Image for ModResorts (Windows)
REM Java 8 Maven WAR application
REM =============================================================================

set PROJECT_NAME=modresorts
set DOCKERFILE_PATH=Dockerfile
set BUILD_CONTEXT=.

echo ==============================================
echo   ModResorts Docker Build ^& Push Script
echo ==============================================
echo.

REM Prompt for image tag
set /p IMAGE_TAG_INPUT="Enter image tag [latest]: "
if "!IMAGE_TAG_INPUT!"=="" (
    set IMAGE_TAG=latest
) else (
    set IMAGE_TAG=!IMAGE_TAG_INPUT!
)
echo Using image tag: !IMAGE_TAG!
echo.

REM Registry selection
echo Select container registry:
echo   1. AWS ECR (Elastic Container Registry)
echo   2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice [1]: "
if "!REGISTRY_CHOICE!"=="" set REGISTRY_CHOICE=1

if "!REGISTRY_CHOICE!"=="1" goto ECR_SETUP
if "!REGISTRY_CHOICE!"=="2" goto DOCKERHUB_SETUP
echo ERROR: Invalid registry choice. Please enter 1 or 2.
exit /b 1

:ECR_SETUP
echo.
echo --- AWS ECR Configuration ---
set /p AWS_REGION="Enter AWS Region [us-east-1]: "
if "!AWS_REGION!"=="" set AWS_REGION=us-east-1

set /p ACCOUNT_ID="Enter AWS Account ID (leave blank to auto-detect): "
if "!ACCOUNT_ID!"=="" (
    echo Fetching AWS Account ID...
    for /f "tokens=*" %%i in ('aws sts get-caller-identity --query Account --output text 2^>^&1') do set ACCOUNT_ID=%%i
    echo Account ID: !ACCOUNT_ID!
)

set /p ECR_REPO_INPUT="Enter ECR repository name [%PROJECT_NAME%]: "
if "!ECR_REPO_INPUT!"=="" (
    set ECR_REPO=%PROJECT_NAME%
) else (
    set ECR_REPO=!ECR_REPO_INPUT!
)

set REGISTRY_URL=!ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!

echo.
echo Logging in to AWS ECR...
aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
if !ERRORLEVEL! neq 0 (
    echo ERROR: ECR login failed. Check your AWS credentials and region.
    exit /b 1
)
echo ECR login successful.

echo Checking if ECR repository '!ECR_REPO!' exists...
aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Repository not found. Creating ECR repository '!ECR_REPO!'...
    aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
    if !ERRORLEVEL! neq 0 (
        echo ERROR: Failed to create ECR repository.
        exit /b 1
    )
    echo ECR repository created successfully.
)
goto BUILD_IMAGE

:DOCKERHUB_SETUP
echo.
echo --- Docker Hub Configuration ---
set /p DOCKER_USERNAME="Enter Docker Hub username: "
set /p DOCKER_PASSWORD="Enter Docker Hub password/token: "
set /p DOCKER_NAMESPACE_INPUT="Enter Docker Hub namespace/org [!DOCKER_USERNAME!]: "
if "!DOCKER_NAMESPACE_INPUT!"=="" (
    set DOCKER_NAMESPACE=!DOCKER_USERNAME!
) else (
    set DOCKER_NAMESPACE=!DOCKER_NAMESPACE_INPUT!
)

set FULL_IMAGE_NAME=!DOCKER_NAMESPACE!/%PROJECT_NAME%:!IMAGE_TAG!

echo.
echo Logging in to Docker Hub...
echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker Hub login failed. Check your credentials.
    exit /b 1
)
echo Docker Hub login successful.
goto BUILD_IMAGE

:BUILD_IMAGE
echo.
echo ==============================================
echo   Building Docker Image
echo ==============================================
echo Image: !FULL_IMAGE_NAME!
echo Dockerfile: %DOCKERFILE_PATH%
echo Build context: %BUILD_CONTEXT%
echo.

docker build -f %DOCKERFILE_PATH% -t !FULL_IMAGE_NAME! %BUILD_CONTEXT%
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker build failed.
    exit /b 1
)
echo.
echo Docker build completed successfully.

echo.
echo ==============================================
echo   Pushing Docker Image
echo ==============================================
echo Pushing: !FULL_IMAGE_NAME!
echo.

docker push !FULL_IMAGE_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Docker push failed.
    exit /b 1
)

echo.
echo ==============================================
echo   Build ^& Push Complete!
echo ==============================================
echo Image successfully pushed: !FULL_IMAGE_NAME!
echo.
echo To deploy to AWS ECS Fargate, run:
echo   scripts\deploy-image.bat
echo.

endlocal
