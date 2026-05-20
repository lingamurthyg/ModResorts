package com.acme.modres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * Spring Boot application entry point for ModResorts.
 * This enables the application to run as an executable JAR with embedded Tomcat,
 * making it cloud-ready for deployment on AWS ECS, EKS, or Fargate.
 */
@SpringBootApplication
@ServletComponentScan // Enable scanning for @WebServlet annotations
public class ModResortsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModResortsApplication.class, args);
    }
}
