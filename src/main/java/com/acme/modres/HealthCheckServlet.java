package com.acme.modres;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;

/**
 * Health check endpoint for container orchestration platforms (ECS, EKS, Kubernetes)
 * Provides liveness and readiness probe endpoints
 */
@WebServlet(urlPatterns = {"/health", "/health/live", "/health/ready"})
public class HealthCheckServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String path = request.getServletPath();
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            if ("/health/live".equals(path)) {
                handleLivenessProbe(response);
            } else if ("/health/ready".equals(path)) {
                handleReadinessProbe(response);
            } else {
                handleGeneralHealth(response);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            PrintWriter out = response.getWriter();
            out.print("{\"status\":\"DOWN\",\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * Liveness probe - indicates if the application is running
     * Returns 200 if the JVM is alive
     */
    private void handleLivenessProbe(HttpServletResponse response) throws IOException {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeMXBean.getUptime();
        
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();
        out.print("{\"status\":\"UP\",\"type\":\"liveness\",\"uptime\":" + uptime + "}");
    }
    
    /**
     * Readiness probe - indicates if the application is ready to serve traffic
     * Checks basic application health indicators
     */
    private void handleReadinessProbe(HttpServletResponse response) throws IOException {
        boolean isReady = checkReadiness();
        
        if (isReady) {
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            out.print("{\"status\":\"UP\",\"type\":\"readiness\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            PrintWriter out = response.getWriter();
            out.print("{\"status\":\"DOWN\",\"type\":\"readiness\"}");
        }
    }
    
    /**
     * General health check - provides comprehensive health information
     */
    private void handleGeneralHealth(HttpServletResponse response) throws IOException {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        long uptime = runtimeMXBean.getUptime();
        long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
        
        boolean isHealthy = checkReadiness();
        
        response.setStatus(isHealthy ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        PrintWriter out = response.getWriter();
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"status\":\"").append(isHealthy ? "UP" : "DOWN").append("\",");
        json.append("\"uptime\":").append(uptime).append(",");
        json.append("\"memory\":{");
        json.append("\"used\":").append(usedMemory).append(",");
        json.append("\"max\":").append(maxMemory);
        json.append("},");
        json.append("\"timestamp\":").append(System.currentTimeMillis());
        json.append("}");
        
        out.print(json.toString());
    }
    
    /**
     * Check if the application is ready to serve requests
     * Add custom readiness checks here (database connectivity, external services, etc.)
     */
    private boolean checkReadiness() {
        try {
            // Basic readiness check - ensure JVM is responsive
            // Add additional checks as needed:
            // - Database connection pool availability
            // - External service connectivity
            // - Required resources availability
            
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
            
            // Check if memory usage is below 95%
            double memoryUsagePercent = (double) usedMemory / maxMemory;
            if (memoryUsagePercent > 0.95) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Support HEAD requests for health checks
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
