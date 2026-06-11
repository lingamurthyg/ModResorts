# =============================================================================
# Multi-stage Dockerfile for ModResorts Java EE Web Application
# Build Tool: Maven | Java Version: 8 | Packaging: WAR
# Runtime: Apache Tomcat on eclipse-temurin:8-jdk
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Builder - Maven build stage
# -----------------------------------------------------------------------------
FROM maven:3.9.4-eclipse-temurin-8 AS builder

WORKDIR /workspace

# Copy Maven build descriptor first for dependency caching
COPY pom.xml .

# Download all dependencies (cached layer if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy the full project source
COPY src/ src/
COPY WebContent/ WebContent/

# Build the WAR artifact (skip tests for Docker build)
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 2: Runtime - Tomcat on eclipse-temurin:8-jdk (explicit base image)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:8-jdk

# Install Tomcat
ENV TOMCAT_VERSION=9.0.85
ENV CATALINA_HOME=/opt/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends \
        tar \
        wget \
    && wget -q "https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
         -O /tmp/tomcat.tar.gz \
    && mkdir -p "$CATALINA_HOME" \
    && tar -xzf /tmp/tomcat.tar.gz -C "$CATALINA_HOME" --strip-components=1 \
    && rm /tmp/tomcat.tar.gz \
    && apt-get remove -y wget \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf "$CATALINA_HOME/webapps/ROOT" \
    && rm -rf "$CATALINA_HOME/webapps/examples" \
    && rm -rf "$CATALINA_HOME/webapps/docs" \
    && rm -rf "$CATALINA_HOME/webapps/host-manager" \
    && rm -rf "$CATALINA_HOME/webapps/manager"

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser

# Set up application directories
RUN mkdir -p /app/logs \
    && chown -R appuser:appuser /app \
    && chown -R appuser:appuser "$CATALINA_HOME"

# Copy the built WAR from builder stage and deploy as ROOT
COPY --from=builder /workspace/target/modresorts-2.0.0.war "$CATALINA_HOME/webapps/ROOT.war"

# Set ownership
RUN chown appuser:appuser "$CATALINA_HOME/webapps/ROOT.war"

# JVM environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UnlockExperimentalVMOptions \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"

ENV TZ=UTC
ENV CATALINA_OPTS="$JAVA_OPTS"

# Application port
EXPOSE 8080

# Switch to non-root user
USER appuser

# Graceful shutdown support
STOPSIGNAL SIGTERM

# Start Tomcat
CMD ["catalina.sh", "run"]
