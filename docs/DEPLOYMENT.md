# ModResorts Application - Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
5. [AWS EKS Deployment](#aws-eks-deployment)
6. [Configuration Management](#configuration-management)
7. [Monitoring and Health Checks](#monitoring-and-health-checks)
8. [Troubleshooting](#troubleshooting)
9. [Security Considerations](#security-considerations)
10. [Scaling and Management](#scaling-and-management)

---

## Overview

ModResorts is a Java EE web application built with:
- **Java Version**: Java 8
- **Build Tool**: Maven
- **Package Type**: WAR (Web Application Archive)
- **Application Server**: Apache Tomcat 9
- **Framework**: Java EE 7 (Servlets, Filters)
- **Port**: 8080
- **Health Endpoints**: `/health`, `/health/live`, `/health/ready`

This guide covers containerization and deployment to AWS EKS (Elastic Kubernetes Service).

---

## Prerequisites

### Required Tools

#### For Local Development
- **Docker**: Version 20.10 or higher
  - Installation: https://docs.docker.com/get-docker/
- **Docker Compose**: Version 1.29 or higher
  - Installation: https://docs.docker.com/compose/install/

#### For AWS EKS Deployment
- **AWS CLI**: Version 2.x
  - Installation: https://aws.amazon.com/cli/
  - Configuration: `aws configure`
- **kubectl**: Version 1.24 or higher
  - Installation: https://kubernetes.io/docs/tasks/tools/
- **eksctl** (optional, for cluster creation): Version 0.140 or higher
  - Installation: https://eksctl.io/introduction/#installation

#### For Building
- **Java JDK 8**: Required for local Maven builds
- **Maven 3.6+**: Required for local builds (optional if using Docker)

### AWS Requirements

1. **AWS Account** with appropriate permissions
2. **IAM Permissions** for:
   - ECR (Elastic Container Registry)
   - EKS (Elastic Kubernetes Service)
   - EC2 (for EKS worker nodes)
   - VPC (for networking)
   - IAM (for service roles)

3. **EKS Cluster** (if not already created):
   ```bash
   eksctl create cluster \
     --name modresorts-cluster \
     --region us-east-1 \
     --nodegroup-name standard-workers \
     --node-type t3.medium \
     --nodes 2 \
     --nodes-min 1 \
     --nodes-max 4 \
     --managed
   ```

4. **AWS Load Balancer Controller** installed on EKS cluster:
   - Follow: https://docs.aws.amazon.com/eks/latest/userguide/aws-load-balancer-controller.html

---

## Local Development Setup

### Using Docker Compose

1. **Clone the repository** (if applicable):
   ```bash
   cd /path/to/Backend\ Services
   ```

2. **Build and run with Docker Compose**:
   ```bash
   docker-compose up --build
   ```

3. **Access the application**:
   - Application: http://localhost:8080
   - Health Check: http://localhost:8080/health
   - Liveness Probe: http://localhost:8080/health/live
   - Readiness Probe: http://localhost:8080/health/ready

4. **Stop the application**:
   ```bash
   docker-compose down
   ```

### Manual Docker Build

1. **Build the Docker image**:
   ```bash
   docker build -t modresorts:latest .
   ```

2. **Run the container**:
   ```bash
   docker run -d \
     --name modresorts \
     -p 8080:8080 \
     -e JAVA_OPTS="-Xmx512m -Xms256m" \
     modresorts:latest
   ```

3. **View logs**:
   ```bash
   docker logs -f modresorts
   ```

4. **Stop and remove**:
   ```bash
   docker stop modresorts
   docker rm modresorts
   ```

---

## Building and Pushing Docker Images

### Option 1: AWS ECR (Elastic Container Registry)

#### Linux/macOS
```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

#### Windows
```cmd
scripts\build-push.bat
```

**Follow the prompts**:
1. Select registry type: `1` (AWS ECR)
2. Enter AWS region (e.g., `us-east-1`)
3. Enter AWS Account ID (e.g., `123456789012`)
4. Enter ECR repository name (default: `modresorts`)
5. Enter image tag (default: `latest`)

The script will:
- Authenticate with AWS ECR
- Create ECR repository if it doesn't exist
- Build the Docker image
- Push to ECR

**Image URI Format**:
```
123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest
```

### Option 2: Docker Hub

#### Linux/macOS
```bash
./scripts/build-push.sh
```

#### Windows
```cmd
scripts\build-push.bat
```

**Follow the prompts**:
1. Select registry type: `2` (Docker Hub)
2. Enter Docker Hub username
3. Enter Docker Hub password or access token
4. Enter image tag (default: `latest`)

The script will:
- Authenticate with Docker Hub
- Build the Docker image
- Push to Docker Hub

**Image URI Format**:
```
yourusername/modresorts:latest
```

---

## AWS EKS Deployment

### Prerequisites

1. **EKS Cluster** is running and accessible
2. **kubectl** is configured for your cluster:
   ```bash
   aws eks update-kubeconfig --region us-east-1 --name modresorts-cluster
   ```

3. **AWS Load Balancer Controller** is installed on the cluster

4. **Docker image** is pushed to ECR or Docker Hub

### Deployment Steps

#### Linux/macOS
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows
```cmd
scripts\deploy-image.bat
```

**Follow the prompts**:
1. Enter AWS region (e.g., `us-east-1`)
2. Enter EKS cluster name (e.g., `modresorts-cluster`)
3. Enter full Docker image URI (e.g., `123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:latest`)

The script will:
1. Configure kubectl for your EKS cluster
2. Verify cluster connectivity
3. Update Kubernetes manifests with your image URI
4. Create namespace: `modresorts`
5. Deploy the application (2 replicas)
6. Create ClusterIP service
7. Create ALB Ingress
8. Wait for deployment rollout
9. Display deployment status and access information

### Manual Deployment

If you prefer manual deployment:

1. **Update the deployment manifest**:
   ```bash
   # Edit kubernetes/deployment.yaml
   # Replace {{IMAGE_URI}} with your actual image URI
   ```

2. **Apply manifests**:
   ```bash
   kubectl apply -f kubernetes/namespace.yaml
   kubectl apply -f kubernetes/deployment.yaml
   kubectl apply -f kubernetes/service.yaml
   kubectl apply -f kubernetes/ingress.yaml
   ```

3. **Check deployment status**:
   ```bash
   kubectl get pods -n modresorts
   kubectl get svc -n modresorts
   kubectl get ingress -n modresorts
   ```

### Accessing the Application

1. **Get the Load Balancer URL**:
   ```bash
   kubectl get ingress modresorts-ingress -n modresorts
   ```

2. **Wait for DNS propagation** (may take 2-5 minutes)

3. **Access the application**:
   ```
   http://<load-balancer-dns-name>
   ```

4. **Test health endpoints**:
   ```bash
   curl http://<load-balancer-dns-name>/health
   curl http://<load-balancer-dns-name>/health/live
   curl http://<load-balancer-dns-name>/health/ready
   ```

---

## Configuration Management

### Environment Variables

The application supports the following environment variables:

#### JVM Configuration
- `JAVA_OPTS`: JVM options (default: `-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`)
- `TZ`: Timezone (default: `UTC`)

#### Database Configuration (if needed)
- `DB_HOST`: Database hostname
- `DB_PORT`: Database port
- `DB_NAME`: Database name
- `DB_USER`: Database username
- `DB_PASSWORD`: Database password

### Kubernetes Secrets

For sensitive data, use Kubernetes secrets:

1. **Create a secret**:
   ```bash
   kubectl create secret generic modresorts-secrets \
     --from-literal=db-user=myuser \
     --from-literal=db-password=mypassword \
     -n modresorts
   ```

2. **Reference in deployment** (already configured in deployment.yaml):
   ```yaml
   env:
   - name: DB_USER
     valueFrom:
       secretKeyRef:
         name: modresorts-secrets
         key: db-user
   ```

### ConfigMaps

For non-sensitive configuration:

1. **Create a ConfigMap**:
   ```bash
   kubectl create configmap modresorts-config \
     --from-literal=app.environment=production \
     --from-literal=log.level=INFO \
     -n modresorts
   ```

2. **Reference in deployment**:
   ```yaml
   envFrom:
   - configMapRef:
       name: modresorts-config
   ```

---

## Monitoring and Health Checks

### Health Endpoints

The application provides three health check endpoints:

1. **General Health** (`/health`):
   - Returns comprehensive health information
   - Includes uptime, memory usage, and timestamp
   - Status: 200 (UP) or 503 (DOWN)

2. **Liveness Probe** (`/health/live`):
   - Indicates if the application is running
   - Used by Kubernetes to restart unhealthy pods
   - Status: 200 (UP) or 503 (DOWN)

3. **Readiness Probe** (`/health/ready`):
   - Indicates if the application is ready to serve traffic
   - Used by Kubernetes to route traffic
   - Checks memory usage (<95%)
   - Status: 200 (UP) or 503 (DOWN)

### Kubernetes Probes Configuration

**Liveness Probe**:
- Initial Delay: 90 seconds (allows Tomcat startup)
- Period: 10 seconds
- Timeout: 5 seconds
- Failure Threshold: 3

**Readiness Probe**:
- Initial Delay: 60 seconds
- Period: 10 seconds
- Timeout: 5 seconds
- Failure Threshold: 3

### Viewing Logs

**Pod logs**:
```bash
kubectl logs -f deployment/modresorts -n modresorts
```

**Specific pod**:
```bash
kubectl logs -f <pod-name> -n modresorts
```

**Previous pod logs** (after restart):
```bash
kubectl logs --previous <pod-name> -n modresorts
```

### Monitoring Commands

**Check pod status**:
```bash
kubectl get pods -n modresorts -w
```

**Describe pod** (for troubleshooting):
```bash
kubectl describe pod <pod-name> -n modresorts
```

**Check events**:
```bash
kubectl get events -n modresorts --sort-by='.lastTimestamp'
```

**Resource usage**:
```bash
kubectl top pods -n modresorts
kubectl top nodes
```

---

## Troubleshooting

### Common Issues

#### 1. Pods Not Starting

**Symptoms**: Pods stuck in `Pending`, `CrashLoopBackOff`, or `ImagePullBackOff`

**Diagnosis**:
```bash
kubectl describe pod <pod-name> -n modresorts
kubectl logs <pod-name> -n modresorts
```

**Solutions**:
- **ImagePullBackOff**: Check image URI, ECR permissions, and image existence
- **CrashLoopBackOff**: Check application logs for startup errors
- **Pending**: Check node resources, PVC availability, or scheduling constraints

#### 2. Health Check Failures

**Symptoms**: Pods restarting frequently, readiness probe failures

**Diagnosis**:
```bash
kubectl describe pod <pod-name> -n modresorts
kubectl logs <pod-name> -n modresorts
```

**Solutions**:
- Increase `initialDelaySeconds` if Tomcat takes longer to start
- Check memory limits (may need to increase)
- Verify health endpoint is accessible: `kubectl exec -it <pod-name> -n modresorts -- curl localhost:8080/health`

#### 3. Ingress Not Working

**Symptoms**: Cannot access application via Load Balancer URL

**Diagnosis**:
```bash
kubectl describe ingress modresorts-ingress -n modresorts
kubectl get svc -n modresorts
```

**Solutions**:
- Verify AWS Load Balancer Controller is installed
- Check security groups allow traffic on port 80
- Wait for DNS propagation (2-5 minutes)
- Check ALB target group health in AWS Console

#### 4. Out of Memory Errors

**Symptoms**: Pods restarting with OOMKilled status

**Diagnosis**:
```bash
kubectl describe pod <pod-name> -n modresorts | grep -A 5 "Last State"
```

**Solutions**:
- Increase memory limits in `deployment.yaml`:
  ```yaml
  resources:
    limits:
      memory: "2Gi"
  ```
- Adjust JVM heap size:
  ```yaml
  env:
  - name: JAVA_OPTS
    value: "-Xmx1024m -Xms512m"
  ```

#### 5. Database Connection Issues

**Symptoms**: Application logs show database connection errors

**Solutions**:
- Verify database credentials in secrets
- Check database hostname and port
- Ensure security groups allow traffic from EKS nodes to database
- Test connectivity: `kubectl exec -it <pod-name> -n modresorts -- nc -zv <db-host> <db-port>`

### Debugging Commands

**Execute commands in pod**:
```bash
kubectl exec -it <pod-name> -n modresorts -- /bin/bash
```

**Port forward for local testing**:
```bash
kubectl port-forward deployment/modresorts 8080:8080 -n modresorts
```

**Check service endpoints**:
```bash
kubectl get endpoints -n modresorts
```

**View deployment rollout history**:
```bash
kubectl rollout history deployment/modresorts -n modresorts
```

---

## Security Considerations

### Container Security

1. **Non-root User**: Application runs as `tomcat` user (not root)
2. **Minimal Base Image**: Uses official OpenJDK image
3. **No Unnecessary Tools**: Minimal runtime dependencies

### Kubernetes Security

1. **Namespace Isolation**: Application runs in dedicated `modresorts` namespace
2. **Resource Limits**: CPU and memory limits prevent resource exhaustion
3. **Secrets Management**: Use Kubernetes secrets for sensitive data
4. **Network Policies**: Consider implementing network policies to restrict traffic

### Best Practices

1. **Use Secrets for Credentials**:
   ```bash
   kubectl create secret generic modresorts-secrets \
     --from-literal=db-password=<password> \
     -n modresorts
   ```

2. **Enable RBAC**: Restrict access to Kubernetes resources
3. **Use Private ECR**: Store images in private ECR repositories
4. **Scan Images**: Use AWS ECR image scanning or third-party tools
5. **Update Dependencies**: Regularly update base images and dependencies
6. **Enable Audit Logging**: Enable EKS audit logging for compliance

### Network Security

1. **Security Groups**: Configure AWS security groups to allow only necessary traffic
2. **Private Subnets**: Deploy worker nodes in private subnets
3. **VPC Endpoints**: Use VPC endpoints for AWS services (ECR, S3)
4. **TLS/SSL**: Configure HTTPS on ALB (update ingress annotations)

---

## Scaling and Management

### Horizontal Pod Autoscaling (HPA)

1. **Install Metrics Server** (if not already installed):
   ```bash
   kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
   ```

2. **Create HPA**:
   ```bash
   kubectl autoscale deployment modresorts \
     --cpu-percent=70 \
     --min=2 \
     --max=10 \
     -n modresorts
   ```

3. **Check HPA status**:
   ```bash
   kubectl get hpa -n modresorts
   ```

### Manual Scaling

**Scale up**:
```bash
kubectl scale deployment modresorts --replicas=5 -n modresorts
```

**Scale down**:
```bash
kubectl scale deployment modresorts --replicas=2 -n modresorts
```

### Rolling Updates

1. **Update image**:
   ```bash
   kubectl set image deployment/modresorts \
     modresorts=123456789012.dkr.ecr.us-east-1.amazonaws.com/modresorts:v2.0 \
     -n modresorts
   ```

2. **Monitor rollout**:
   ```bash
   kubectl rollout status deployment/modresorts -n modresorts
   ```

3. **Rollback if needed**:
   ```bash
   kubectl rollout undo deployment/modresorts -n modresorts
   ```

### Cluster Autoscaling

1. **Install Cluster Autoscaler**:
   ```bash
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml
   ```

2. **Configure node group** with min/max nodes in AWS Console or eksctl

### Backup and Disaster Recovery

1. **Backup Kubernetes Resources**:
   ```bash
   kubectl get all -n modresorts -o yaml > modresorts-backup.yaml
   ```

2. **Use Velero** for comprehensive backup:
   - Installation: https://velero.io/docs/main/basic-install/

3. **Database Backups**: Implement regular database backups if applicable

---

## Additional Resources

### Documentation
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Docker Documentation](https://docs.docker.com/)
- [Apache Tomcat Documentation](https://tomcat.apache.org/tomcat-9.0-doc/)

### AWS EKS Best Practices
- [EKS Best Practices Guide](https://aws.github.io/aws-eks-best-practices/)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)

### Monitoring and Observability
- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
- [AWS CloudWatch Container Insights](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights.html)

---

## Support and Maintenance

### Regular Maintenance Tasks

1. **Update Dependencies**: Monthly review of Maven dependencies
2. **Update Base Images**: Quarterly update of Docker base images
3. **Security Patches**: Apply security patches promptly
4. **Log Review**: Weekly review of application logs
5. **Performance Monitoring**: Continuous monitoring of resource usage

### Useful Commands Reference

```bash
# View all resources
kubectl get all -n modresorts

# Describe deployment
kubectl describe deployment modresorts -n modresorts

# View logs
kubectl logs -f deployment/modresorts -n modresorts

# Execute command in pod
kubectl exec -it <pod-name> -n modresorts -- /bin/bash

# Port forward
kubectl port-forward deployment/modresorts 8080:8080 -n modresorts

# Scale deployment
kubectl scale deployment modresorts --replicas=3 -n modresorts

# Update image
kubectl set image deployment/modresorts modresorts=<new-image> -n modresorts

# Rollback deployment
kubectl rollout undo deployment/modresorts -n modresorts

# Delete all resources
kubectl delete namespace modresorts
```

---

## Conclusion

This deployment guide provides comprehensive instructions for containerizing and deploying the ModResorts application to AWS EKS. Follow the steps carefully, and refer to the troubleshooting section for common issues.

For questions or issues, consult the AWS EKS documentation or contact your DevOps team.
