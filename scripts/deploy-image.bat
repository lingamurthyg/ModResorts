@echo off
setlocal enabledelayedexpansion

REM Deploy ModResorts Application to AWS EKS
REM This script configures kubectl and deploys the application to EKS

echo ==========================================
echo ModResorts - AWS EKS Deployment Script
echo ==========================================
echo.

REM Prompt for AWS region
set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
if "!AWS_REGION!"=="" (
    echo Error: AWS region is required
    exit /b 1
)

REM Prompt for EKS cluster name
set /p CLUSTER_NAME="Enter EKS cluster name: "
if "!CLUSTER_NAME!"=="" (
    echo Error: EKS cluster name is required
    exit /b 1
)

REM Prompt for Docker image URI
echo.
echo Enter the full Docker image URI (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest)
set /p IMAGE_URI="Image URI: "
if "!IMAGE_URI!"=="" (
    echo Error: Image URI is required
    exit /b 1
)

echo.
echo Configuration:
echo   AWS Region: !AWS_REGION!
echo   EKS Cluster: !CLUSTER_NAME!
echo   Image URI: !IMAGE_URI!
echo.

REM Configure kubectl for EKS
echo ==========================================
echo Configuring kubectl for EKS cluster...
echo ==========================================
echo.

aws eks update-kubeconfig --region "!AWS_REGION!" --name "!CLUSTER_NAME!"

if !ERRORLEVEL! neq 0 (
    echo Error: Failed to configure kubectl for EKS cluster
    exit /b 1
)

echo kubectl configured successfully
echo.

REM Verify cluster connectivity
echo Verifying cluster connectivity...
kubectl cluster-info

if !ERRORLEVEL! neq 0 (
    echo Error: Cannot connect to EKS cluster
    exit /b 1
)

echo.
echo Cluster connectivity verified
echo.

REM Update deployment manifest with image URI
echo ==========================================
echo Updating Kubernetes manifests...
echo ==========================================
echo.

REM Create temporary directory for processed manifests
set TEMP_DIR=%TEMP%\modresorts-deploy-%RANDOM%
mkdir "!TEMP_DIR!"

REM Copy manifests to temp directory
xcopy /E /I /Q kubernetes "!TEMP_DIR!" >nul

REM Replace IMAGE_URI placeholder using PowerShell
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

echo Manifests updated successfully
echo.

REM Apply Kubernetes manifests
echo ==========================================
echo Deploying to EKS cluster...
echo ==========================================
echo.

REM Apply namespace
echo Creating namespace...
kubectl apply -f "!TEMP_DIR!\namespace.yaml"

if !ERRORLEVEL! neq 0 (
    echo Error: Failed to create namespace
    rmdir /S /Q "!TEMP_DIR!"
    exit /b 1
)

echo Namespace created successfully
echo.

REM Apply deployment
echo Creating deployment...
kubectl apply -f "!TEMP_DIR!\deployment.yaml"

if !ERRORLEVEL! neq 0 (
    echo Error: Failed to create deployment
    rmdir /S /Q "!TEMP_DIR!"
    exit /b 1
)

echo Deployment created successfully
echo.

REM Apply service
echo Creating service...
kubectl apply -f "!TEMP_DIR!\service.yaml"

if !ERRORLEVEL! neq 0 (
    echo Error: Failed to create service
    rmdir /S /Q "!TEMP_DIR!"
    exit /b 1
)

echo Service created successfully
echo.

REM Apply ingress
echo Creating ingress...
kubectl apply -f "!TEMP_DIR!\ingress.yaml"

if !ERRORLEVEL! neq 0 (
    echo Error: Failed to create ingress
    rmdir /S /Q "!TEMP_DIR!"
    exit /b 1
)

echo Ingress created successfully
echo.

REM Clean up temp directory
rmdir /S /Q "!TEMP_DIR!"

REM Wait for deployment rollout
echo ==========================================
echo Waiting for deployment to complete...
echo ==========================================
echo.

kubectl rollout status deployment/modresorts -n modresorts --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo Warning: Deployment rollout did not complete within timeout
    echo Check deployment status with: kubectl get pods -n modresorts
) else (
    echo.
    echo Deployment rollout completed successfully
)

echo.

REM Display deployment status
echo ==========================================
echo Deployment Status
echo ==========================================
echo.

echo Pods:
kubectl get pods -n modresorts
echo.

echo Services:
kubectl get svc -n modresorts
echo.

echo Ingress:
kubectl get ingress -n modresorts
echo.

REM Get ingress URL
echo ==========================================
echo Application Access Information
echo ==========================================
echo.

for /f "delims=" %%i in ('kubectl get ingress modresorts-ingress -n modresorts -o jsonpath^="{.status.loadBalancer.ingress[0].hostname}" 2^>nul') do set INGRESS_ADDRESS=%%i

if not "!INGRESS_ADDRESS!"=="" (
    echo Application URL: http://!INGRESS_ADDRESS!
    echo.
    echo Note: It may take a few minutes for the Load Balancer to become available
    echo       and DNS to propagate.
) else (
    echo Ingress is being provisioned. Check status with:
    echo   kubectl get ingress -n modresorts
)

echo.
echo ==========================================
echo Deployment completed successfully!
echo ==========================================
echo.

echo Useful commands:
echo   View pods:        kubectl get pods -n modresorts
echo   View logs:        kubectl logs -f deployment/modresorts -n modresorts
echo   View services:    kubectl get svc -n modresorts
echo   View ingress:     kubectl get ingress -n modresorts
echo   Describe pod:     kubectl describe pod ^<pod-name^> -n modresorts
echo.

echo To rollback deployment:
echo   kubectl rollout undo deployment/modresorts -n modresorts
echo.

endlocal
