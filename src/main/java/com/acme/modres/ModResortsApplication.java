package com.acme.modres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * Spring Boot Application class for cloud-native deployment
 * Enables embedded Tomcat server for containerization on AWS ECS/EKS/Fargate
 */
@SpringBootApplication
@ServletComponentScan
public class ModResortsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModResortsApplication.class, args);
    }
}
