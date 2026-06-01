package com.acme.modres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Spring Boot application entry point for ModResorts.
 * Configured for cloud deployment on AWS with:
 * - Embedded Tomcat server (no external app server needed)
 * - HikariCP connection pooling for AWS RDS
 * - Redis session management for Amazon ElastiCache
 * - AWS SDK integration for S3 and Secrets Manager
 */
@SpringBootApplication
@ServletComponentScan // Enable @WebServlet annotations
@EnableRedisHttpSession // Enable distributed session management with Redis
public class ModResortsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModResortsApplication.class, args);
  }

  /**
   * Configure HikariCP DataSource for connection pooling with AWS RDS.
   * Configuration is externalized via environment variables for cloud deployment.
   */
  @Bean
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    
    // Read database configuration from environment variables
    config.setJdbcUrl(System.getenv().getOrDefault("DB_URL", 
        "jdbc:postgresql://localhost:5432/modresorts"));
    config.setUsername(System.getenv().getOrDefault("DB_USERNAME", "modresorts"));
    config.setPassword(System.getenv().getOrDefault("DB_PASSWORD", "password"));
    config.setDriverClassName(System.getenv().getOrDefault("DB_DRIVER", 
        "org.postgresql.Driver"));
    
    // HikariCP optimal settings for cloud environments
    config.setMaximumPoolSize(Integer.parseInt(
        System.getenv().getOrDefault("DB_POOL_SIZE", "10")));
    config.setMinimumIdle(Integer.parseInt(
        System.getenv().getOrDefault("DB_MIN_IDLE", "2")));
    config.setConnectionTimeout(Long.parseLong(
        System.getenv().getOrDefault("DB_CONN_TIMEOUT", "30000")));
    config.setIdleTimeout(Long.parseLong(
        System.getenv().getOrDefault("DB_IDLE_TIMEOUT", "600000")));
    config.setMaxLifetime(Long.parseLong(
        System.getenv().getOrDefault("DB_MAX_LIFETIME", "1800000")));
    
    // Connection pool name for monitoring
    config.setPoolName("ModResortsHikariPool");
    
    // Enable connection testing
    config.setConnectionTestQuery("SELECT 1");
    
    return new HikariDataSource(config);
  }
}
