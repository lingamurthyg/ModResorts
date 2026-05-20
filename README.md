# ModResorts Backend - Cloud-Ready Application

## Overview
This application has been modernized for cloud deployment on AWS. It is now packaged as an executable JAR with embedded Tomcat and uses cloud-native patterns.

## Cloud Readiness Features

### 1. **Executable JAR Packaging**
- Converted from WAR to executable JAR with embedded Tomcat
- No external application server required
- Ready for containerization on AWS ECS, EKS, or Fargate

### 2. **AWS S3 Integration**
- Replaced local file system operations with Amazon S3
- All file writes and reads use S3 for durable, scalable storage
- Configured via environment variables: `S3_BUCKET_NAME`, `AWS_REGION`

### 3. **AWS Secrets Manager**
- Hardcoded API keys replaced with AWS Secrets Manager
- Automatic secret retrieval and caching
- Fallback to environment variables for local development

### 4. **Distributed Session Management**
- Integrated Spring Session with Redis (Amazon ElastiCache)
- Removed WebSphere-specific session clustering
- Enables horizontal scaling across multiple instances

### 5. **Connection Pooling**
- Replaced direct JDBC connections with HikariCP
- Optimized for cloud database services (AWS RDS)
- Configurable pool sizes via environment variables

### 6. **12-Factor App Compliance**
- All configuration externalized to environment variables
- No hardcoded credentials or file paths
- Stateless application design

### 7. **Modern Java Time API**
- Migrated from `java.util.Date` to `java.time.LocalDate`
- UTC standardization for distributed systems
- Eliminates timezone-related issues

### 8. **Resource Management**
- Try-with-resources pattern for automatic cleanup
- Prevents resource leaks in containerized environments
- Proper connection, stream, and file handle management

## Environment Variables

### Required
- `AWS_REGION`: AWS region (default: us-east-1)
- `S3_BUCKET_NAME`: S3 bucket for file storage (default: modresorts-data)
- `DATABASE_URL`: JDBC connection string
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password
- `REDIS_HOST`: Redis host for session management
- `REDIS_PORT`: Redis port (default: 6379)

### Optional
- `PORT`: Application port (default: 8080)
- `WEATHER_API_KEY`: Weather API key (fallback if Secrets Manager unavailable)
- `DB_POOL_SIZE`: Database connection pool size (default: 10)
- `DB_POOL_MIN_IDLE`: Minimum idle connections (default: 2)

## Building the Application

```bash
mvn clean package
```

This produces an executable JAR: `target/modresorts-2.0.0.jar`

## Running Locally

```bash
export AWS_REGION=us-east-1
export S3_BUCKET_NAME=my-bucket
export DATABASE_URL=jdbc:postgresql://localhost:5432/modresorts
export DATABASE_USERNAME=modresorts
export DATABASE_PASSWORD=changeme
export REDIS_HOST=localhost
export REDIS_PORT=6379

java -jar target/modresorts-2.0.0.jar
```

## AWS Deployment

### Prerequisites
1. **S3 Bucket**: Create an S3 bucket for file storage
2. **RDS Database**: PostgreSQL or MySQL instance
3. **ElastiCache Redis**: For distributed session management
4. **Secrets Manager**: Store API keys and credentials
5. **IAM Role**: Grant permissions for S3, Secrets Manager, and RDS

### IAM Permissions Required
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::modresorts-data/*",
        "arn:aws:s3:::modresorts-data"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:modresorts/*"
    }
  ]
}
```

## Health Check Endpoint

The application exposes health check endpoints for AWS load balancers:

- **Health**: `http://localhost:8080/actuator/health`
- **Info**: `http://localhost:8080/actuator/info`

## Migration from WebSphere

### Removed Dependencies
- `com.ibm.websphere.appserver:was_public`
- `javax:javaee-api` (replaced with Spring Boot starters)

### Replaced Components
- **EJB 2.x** → Spring Components with `@Component`
- **WebSphere Session Management** → Spring Session Redis
- **Direct JDBC** → HikariCP Connection Pooling
- **Local File System** → Amazon S3
- **Hardcoded Secrets** → AWS Secrets Manager

## Troubleshooting

### S3 Access Issues
- Verify IAM role has S3 permissions
- Check bucket name and region configuration
- Ensure bucket exists and is accessible

### Database Connection Issues
- Verify RDS security group allows inbound connections
- Check DATABASE_URL format
- Verify HikariCP pool configuration

### Session Management Issues
- Verify Redis/ElastiCache is accessible
- Check REDIS_HOST and REDIS_PORT
- Ensure security group allows Redis port (6379)

## Next Steps

This application is now ready for containerization. The next phase will create:
- Dockerfile for container image
- Kubernetes manifests for EKS deployment
- Terraform/CloudFormation for infrastructure provisioning
