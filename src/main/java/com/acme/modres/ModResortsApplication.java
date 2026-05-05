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
 * Enables cloud-native deployment with embedded Tomcat, Redis session management,
 * and HikariCP connection pooling.
 */
@SpringBootApplication
@ServletComponentScan // Enable @WebServlet scanning
@EnableRedisHttpSession // Enable distributed session management with Redis
public class ModResortsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModResortsApplication.class, args);
  }

  /**
   * Configure HikariCP DataSource for connection pooling.
   * Configuration is externalized via environment variables for cloud deployment.
   */
  @Bean
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    
    // Read database configuration from environment variables
    String jdbcUrl = System.getenv("DB_JDBC_URL");
    String username = System.getenv("DB_USERNAME");
    String password = System.getenv("DB_PASSWORD");
    
    // Set defaults for local development
    if (jdbcUrl == null || jdbcUrl.isEmpty()) {
      jdbcUrl = "jdbc:h2:mem:testdb";
    }
    if (username == null || username.isEmpty()) {
      username = "sa";
    }
    if (password == null || password.isEmpty()) {
      password = "";
    }
    
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(username);
    config.setPassword(password);
    
    // HikariCP optimal settings for cloud environments
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);
    
    return new HikariDataSource(config);
  }
}
