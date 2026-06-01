# ModResorts - Cloud-Ready Application

## Overview
ModResorts has been modernized for cloud deployment on AWS. The application is now packaged as an executable JAR with embedded Tomcat, eliminating the need for external application servers.

## Cloud Readiness Improvements

### 1. **Packaging: WAR → Executable JAR**
- Converted from WAR to executable JAR with embedded Tomcat
- No external application server (JBoss/WebSphere) required
- Ready for containerization on Amazon ECS, EKS, or AWS Fargate

### 2. **File System → Amazon S3**
- Replaced all local file system operations with Amazon S3
- Eliminated hard-coded file paths
- Removed temporary file dependencies
- Data persists across container restarts

### 3. **Secrets Management → AWS Secrets Manager**
- Migrated hardcoded API keys to AWS Secrets Manager
- Automatic secret rotation support
- Fallback to environment variables for local development

### 4. **EJB 2.x → Spring Boot + HikariCP**
- Replaced EJB 2.x with Spring Data JPA
- Integrated HikariCP for connection pooling
- Compatible with Amazon RDS (PostgreSQL, MySQL, Aurora)

### 5. **Session Management → Amazon ElastiCache (Redis)**
- Externalized session state to Redis
- Removed WebSphere clustering dependencies
- Supports horizontal scaling across multiple instances

### 6. **Date/Time → java.time API (UTC)**
- Migrated from java.util.Date to java.time API
- Standardized on UTC for all operations
- Eliminates timezone issues in distributed environments

### 7. **Resource Management**
- Implemented try-with-resources throughout
- Prevents resource leaks in containerized environments
- Automatic cleanup of connections, streams, and file handles

## Environment Variables

### Required Configuration
```bash
# Database (AWS RDS)
DB_URL=jdbc:postgresql://your-rds-endpoint:5432/modresorts
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password
DB_DRIVER=org.postgresql.Driver

# Redis (Amazon ElastiCache)
REDIS_HOST=your-elasticache-endpoint
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
REDIS_SSL=true

# AWS Configuration
AWS_REGION=us-east-1
S3_BUCKET_NAME=your-s3-bucket-name

# Server Configuration
PORT=8080
```

### Optional Configuration
```bash
# Database Connection Pool
DB_POOL_SIZE=10
DB_MIN_IDLE=2
DB_CONN_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
```

## AWS Secrets Manager Setup

Create a secret in AWS Secrets Manager for the Weather API key:
```bash
aws secretsmanager create-secret \
  --name modresorts/weather-api-key \
  --secret-string "your-weather-api-key" \
  --region us-east-1
```

## Building the Application

```bash
mvn clean package
```

This produces an executable JAR: `target/modresorts-2.0.0.jar`

## Running Locally

```bash
# Set environment variables
export DB_URL=jdbc:postgresql://localhost:5432/modresorts
export DB_USERNAME=modresorts
export DB_PASSWORD=password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export S3_BUCKET_NAME=modresorts-local

# Run the application
java -jar target/modresorts-2.0.0.jar
```

## AWS Deployment Options

### Option 1: Amazon ECS/Fargate
1. Build Docker image (separate workflow)
2. Push to Amazon ECR
3. Deploy to ECS with Fargate launch type
4. Configure task definition with environment variables
5. Use IAM roles for AWS service access

### Option 2: Amazon EKS
1. Build Docker image (separate workflow)
2. Push to Amazon ECR
3. Deploy to EKS cluster using Kubernetes manifests
4. Use ConfigMaps and Secrets for configuration
5. Use IAM roles for service accounts (IRSA)

### Option 3: AWS Elastic Beanstalk
1. Upload JAR file directly
2. Configure environment variables in Beanstalk console
3. Beanstalk handles load balancing and auto-scaling

## AWS Services Integration

### Amazon RDS
- Use PostgreSQL or MySQL compatible database
- Enable automated backups
- Configure security groups for VPC access
- Use IAM database authentication (optional)

### Amazon ElastiCache (Redis)
- Create Redis cluster in same VPC
- Enable encryption in transit
- Configure security groups
- Use cluster mode for high availability

### Amazon S3
- Create bucket for application data
- Enable versioning for data protection
- Configure lifecycle policies
- Use IAM roles for access (no hardcoded credentials)

### AWS Secrets Manager
- Store all sensitive credentials
- Enable automatic rotation
- Use IAM policies for access control
- Integrate with application via AWS SDK

## Health Checks

The application exposes Spring Boot Actuator endpoints:
- `/actuator/health` - Overall health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

## Monitoring and Logging

- Logs are written to stdout/stderr (container-friendly)
- Use Amazon CloudWatch Logs for log aggregation
- Use Amazon CloudWatch Metrics for monitoring
- Configure CloudWatch alarms for critical metrics

## Security Considerations

1. **No hardcoded credentials** - All secrets in AWS Secrets Manager
2. **IAM roles** - Use IAM roles instead of access keys
3. **VPC security groups** - Restrict network access
4. **Encryption** - Enable encryption at rest and in transit
5. **HTTPS** - Use Application Load Balancer with SSL/TLS

## Migration Checklist

- [x] Convert WAR to executable JAR
- [x] Replace file system operations with S3
- [x] Migrate secrets to AWS Secrets Manager
- [x] Replace EJB with Spring Boot
- [x] Implement HikariCP connection pooling
- [x] Externalize session to Redis
- [x] Remove WebSphere dependencies
- [x] Migrate to java.time API
- [x] Implement try-with-resources
- [x] Externalize all configuration

## Next Steps

1. **Containerization** (separate workflow)
   - Create Dockerfile
   - Build container image
   - Push to Amazon ECR

2. **Infrastructure as Code** (separate workflow)
   - Create Terraform/CloudFormation templates
   - Define VPC, subnets, security groups
   - Provision RDS, ElastiCache, S3

3. **CI/CD Pipeline** (separate workflow)
   - Set up AWS CodePipeline
   - Configure automated testing
   - Implement blue/green deployments

## Support

For issues or questions, contact the cloud migration team.
