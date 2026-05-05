# ModResorts - Cloud-Ready Application

## Overview
ModResorts has been modernized for cloud deployment on AWS with the following improvements:

## Cloud Readiness Fixes Applied

### 1. **Packaging & Deployment**
- ✅ Converted from WAR to executable JAR with embedded Tomcat
- ✅ Added Spring Boot for cloud-native deployment
- ✅ Removed WebSphere and JBoss dependencies

### 2. **File System & Storage**
- ✅ Replaced local file system operations with Amazon S3
- ✅ Eliminated hard-coded file paths
- ✅ Removed temporary file dependencies
- ✅ All file operations now use S3 or classpath resources

### 3. **Session Management**
- ✅ Replaced WebSphere-specific session clustering with Spring Session Redis
- ✅ Externalized session state to Amazon ElastiCache (Redis)
- ✅ Enabled horizontal scaling without session affinity

### 4. **Database & Connection Pooling**
- ✅ Replaced EJB 2.x with Spring Data JPA
- ✅ Implemented HikariCP connection pooling
- ✅ Added try-with-resources for automatic resource management

### 5. **Secrets Management**
- ✅ Migrated hardcoded API keys to AWS Secrets Manager
- ✅ Externalized all credentials and sensitive configuration

### 6. **Time & Date Handling**
- ✅ Migrated from java.util.Date to java.time API
- ✅ Standardized on UTC for all time operations
- ✅ Eliminated timezone dependencies

### 7. **Resource Management**
- ✅ Implemented try-with-resources for all AutoCloseable resources
- ✅ Proper cleanup of S3 clients, database connections, and streams

## Environment Variables

### Required Configuration
```bash
# AWS Configuration
AWS_REGION=us-east-1
S3_BUCKET_NAME=modresorts-data

# Database Configuration (Amazon RDS)
DB_JDBC_URL=jdbc:postgresql://your-rds-endpoint:5432/modresorts
DB_USERNAME=dbuser
DB_PASSWORD=<stored-in-secrets-manager>
DB_DRIVER=org.postgresql.Driver

# Redis Configuration (Amazon ElastiCache)
REDIS_HOST=your-elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=<stored-in-secrets-manager>
REDIS_SSL=true

# Application Configuration
SERVER_PORT=8080
```

### AWS Secrets Manager
The following secrets should be stored in AWS Secrets Manager:
- `weather-api-key`: Weather Underground API key
- `db-password`: Database password
- `redis-password`: Redis password

## Building the Application

```bash
mvn clean package
```

This produces an executable JAR: `target/modresorts-2.0.0.jar`

## Running Locally

```bash
export AWS_REGION=us-east-1
export S3_BUCKET_NAME=modresorts-data
export DB_JDBC_URL=jdbc:h2:mem:testdb
export REDIS_HOST=localhost

java -jar target/modresorts-2.0.0.jar
```

## AWS Deployment Options

### Option 1: Amazon ECS (Elastic Container Service)
1. Build Docker image from the executable JAR
2. Push to Amazon ECR
3. Deploy to ECS Fargate or EC2

### Option 2: Amazon EKS (Elastic Kubernetes Service)
1. Build Docker image
2. Create Kubernetes deployment manifests
3. Deploy to EKS cluster

### Option 3: AWS Elastic Beanstalk
1. Upload the executable JAR
2. Configure environment variables
3. Deploy to Elastic Beanstalk Java platform

## AWS Services Used

- **Amazon S3**: Persistent file storage
- **Amazon RDS**: Managed database with connection pooling
- **Amazon ElastiCache (Redis)**: Distributed session management
- **AWS Secrets Manager**: Secure credential storage
- **Amazon CloudWatch**: Logging and monitoring

## Health Checks

The application exposes health check endpoints for AWS load balancers:
- `GET /actuator/health` - Overall application health
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Application metrics

## Security Considerations

1. All credentials are externalized to AWS Secrets Manager
2. Database connections use SSL/TLS
3. Redis connections support SSL/TLS
4. No hardcoded secrets in source code
5. IAM roles for AWS service access

## Migration Notes

### Removed Dependencies
- `com.ibm.websphere.appserver:was_public` - WebSphere-specific APIs
- `javax:javaee-api` - Replaced with Spring Boot starters

### Added Dependencies
- Spring Boot 2.7.18 with embedded Tomcat
- AWS SDK for Java v2 (S3, Secrets Manager)
- Spring Session Data Redis
- HikariCP (via Spring Boot)

## Troubleshooting

### S3 Access Issues
Ensure the application has IAM permissions:
```json
{
  "Effect": "Allow",
  "Action": ["s3:GetObject", "s3:PutObject"],
  "Resource": "arn:aws:s3:::modresorts-data/*"
}
```

### Secrets Manager Access
Ensure IAM permissions:
```json
{
  "Effect": "Allow",
  "Action": ["secretsmanager:GetSecretValue"],
  "Resource": "arn:aws:secretsmanager:*:*:secret:*"
}
```

### Redis Connection Issues
- Verify ElastiCache security group allows inbound traffic
- Check Redis endpoint and port configuration
- Verify SSL/TLS settings match ElastiCache configuration

## Support

For issues or questions, contact the ModResorts development team.
