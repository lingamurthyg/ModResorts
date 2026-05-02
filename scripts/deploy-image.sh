#!/bin/bash
set -e
set -o pipefail

# Deploy ModResorts Application to AWS EKS
# This script configures kubectl and deploys the application to EKS

echo "=========================================="
echo "ModResorts - AWS EKS Deployment Script"
echo "=========================================="
echo ""

# Prompt for AWS region
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
    echo "Error: AWS region is required"
    exit 1
fi

# Prompt for EKS cluster name
read -p "Enter EKS cluster name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
    echo "Error: EKS cluster name is required"
    exit 1
fi

# Prompt for Docker image URI
echo ""
echo "Enter the full Docker image URI (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest)"
read -p "Image URI: " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
    echo "Error: Image URI is required"
    exit 1
fi

echo ""
echo "Configuration:"
echo "  AWS Region: $AWS_REGION"
echo "  EKS Cluster: $CLUSTER_NAME"
echo "  Image URI: $IMAGE_URI"
echo ""

# Configure kubectl for EKS
echo "=========================================="
echo "Configuring kubectl for EKS cluster..."
echo "=========================================="
echo ""

aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

if [ $? -ne 0 ]; then
    echo "Error: Failed to configure kubectl for EKS cluster"
    exit 1
fi

echo "kubectl configured successfully"
echo ""

# Verify cluster connectivity
echo "Verifying cluster connectivity..."
kubectl cluster-info

if [ $? -ne 0 ]; then
    echo "Error: Cannot connect to EKS cluster"
    exit 1
fi

echo ""
echo "Cluster connectivity verified"
echo ""

# Update deployment manifest with image URI
echo "=========================================="
echo "Updating Kubernetes manifests..."
echo "=========================================="
echo ""

# Create temporary directory for processed manifests
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# Copy manifests to temp directory
cp -r kubernetes/* "$TEMP_DIR/"

# Replace IMAGE_URI placeholder
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TEMP_DIR/deployment.yaml"

echo "Manifests updated successfully"
echo ""

# Apply Kubernetes manifests
echo "=========================================="
echo "Deploying to EKS cluster..."
echo "=========================================="
echo ""

# Apply namespace
echo "Creating namespace..."
kubectl apply -f "$TEMP_DIR/namespace.yaml"

if [ $? -ne 0 ]; then
    echo "Error: Failed to create namespace"
    exit 1
fi

echo "Namespace created successfully"
echo ""

# Apply deployment
echo "Creating deployment..."
kubectl apply -f "$TEMP_DIR/deployment.yaml"

if [ $? -ne 0 ]; then
    echo "Error: Failed to create deployment"
    exit 1
fi

echo "Deployment created successfully"
echo ""

# Apply service
echo "Creating service..."
kubectl apply -f "$TEMP_DIR/service.yaml"

if [ $? -ne 0 ]; then
    echo "Error: Failed to create service"
    exit 1
fi

echo "Service created successfully"
echo ""

# Apply ingress
echo "Creating ingress..."
kubectl apply -f "$TEMP_DIR/ingress.yaml"

if [ $? -ne 0 ]; then
    echo "Error: Failed to create ingress"
    exit 1
fi

echo "Ingress created successfully"
echo ""

# Wait for deployment rollout
echo "=========================================="
echo "Waiting for deployment to complete..."
echo "=========================================="
echo ""

kubectl rollout status deployment/modresorts -n modresorts --timeout=5m

if [ $? -ne 0 ]; then
    echo "Warning: Deployment rollout did not complete within timeout"
    echo "Check deployment status with: kubectl get pods -n modresorts"
else
    echo ""
    echo "Deployment rollout completed successfully"
fi

echo ""

# Display deployment status
echo "=========================================="
echo "Deployment Status"
echo "=========================================="
echo ""

echo "Pods:"
kubectl get pods -n modresorts
echo ""

echo "Services:"
kubectl get svc -n modresorts
echo ""

echo "Ingress:"
kubectl get ingress -n modresorts
echo ""

# Get ingress URL
echo "=========================================="
echo "Application Access Information"
echo "=========================================="
echo ""

INGRESS_ADDRESS=$(kubectl get ingress modresorts-ingress -n modresorts -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)

if [ -n "$INGRESS_ADDRESS" ]; then
    echo "Application URL: http://$INGRESS_ADDRESS"
    echo ""
    echo "Note: It may take a few minutes for the Load Balancer to become available"
    echo "      and DNS to propagate."
else
    echo "Ingress is being provisioned. Check status with:"
    echo "  kubectl get ingress -n modresorts"
fi

echo ""
echo "=========================================="
echo "Deployment completed successfully!"
echo "=========================================="
echo ""

echo "Useful commands:"
echo "  View pods:        kubectl get pods -n modresorts"
echo "  View logs:        kubectl logs -f deployment/modresorts -n modresorts"
echo "  View services:    kubectl get svc -n modresorts"
echo "  View ingress:     kubectl get ingress -n modresorts"
echo "  Describe pod:     kubectl describe pod <pod-name> -n modresorts"
echo ""

echo "To rollback deployment:"
echo "  kubectl rollout undo deployment/modresorts -n modresorts"
echo ""
