# Multi-stage Dockerfile for ModResorts Java WAR Application
# Stage 1: Build the application
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /workspace

# Copy Maven configuration files first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code and web content
COPY src ./src
COPY WebContent ./WebContent

# Build the WAR file
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image with Tomcat
FROM openjdk:8-jdk

# Install Tomcat 9
ENV CATALINA_HOME=/usr/local/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH
ENV TOMCAT_VERSION=9.0.82

RUN mkdir -p "$CATALINA_HOME" && \
    apt-get update && \
    apt-get install -y wget && \
    wget -O /tmp/tomcat.tar.gz "https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" && \
    tar -xzf /tmp/tomcat.tar.gz -C "$CATALINA_HOME" --strip-components=1 && \
    rm /tmp/tomcat.tar.gz && \
    rm -rf "$CATALINA_HOME/webapps/*" && \
    apt-get remove -y wget && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r tomcat && useradd -r -g tomcat tomcat && \
    chown -R tomcat:tomcat "$CATALINA_HOME"

# Copy WAR file from builder stage
COPY --from=builder /workspace/target/*.war $CATALINA_HOME/webapps/ROOT.war

# Set ownership
RUN chown tomcat:tomcat $CATALINA_HOME/webapps/ROOT.war

# Switch to non-root user
USER tomcat

# Expose application port
EXPOSE 8080

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Set timezone
ENV TZ=UTC

# Start Tomcat
CMD ["catalina.sh", "run"]
