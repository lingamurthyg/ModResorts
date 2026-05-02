package com.acme.modres.db;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaced @Singleton with @ApplicationScoped for container-portable state management
 * For distributed caching across container instances, integrate with Redis/ElastiCache
 * using environment variables: REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
 */
@ApplicationScoped
public class ModResortsCustomerInformation {
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  // Removing DB connection for ease of demo setup
  // @Resource(lookup = "jdbc/ModResortsJndi")
  private DataSource dataSource;
  
  // Replace singleton state with distributed cache-ready structure
  // In production, replace this with Redis/ElastiCache client
  private Map<String, ArrayList<String>> customerCache = new ConcurrentHashMap<>();
  
  @PostConstruct
  public void init() {
    // Initialize distributed cache connection if environment variables are set
    String redisHost = System.getenv("REDIS_HOST");
    String redisPort = System.getenv("REDIS_PORT");
    
    if (redisHost != null && redisPort != null) {
      // TODO: Initialize Redis/ElastiCache client for distributed caching
      // Example: redisClient = new JedisPool(redisHost, Integer.parseInt(redisPort));
      System.out.println("Distributed cache configuration detected: " + redisHost + ":" + redisPort);
    }
  }

  public ArrayList<String> getCustomerInformation() {
    // Check cache first (in production, this would be Redis/ElastiCache)
    String cacheKey = "all_customers";
    if (customerCache.containsKey(cacheKey)) {
      return customerCache.get(cacheKey);
    }
    
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    ArrayList<String> customerInfo = new ArrayList<>();

    try {
      // Get a connection from the injected data source
      conn = dataSource.getConnection();
      // Create a prepared statement
      stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
      // Execute the query
      rs = stmt.executeQuery();

      // Process the results
      while (rs.next()) {
        String info = rs.getString("INFO");
        customerInfo.add(info);
      }
      
      // Store in cache (in production, use Redis with TTL)
      customerCache.put(cacheKey, customerInfo);

    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      // Close the result set, statement, and connection
      try {
        if (rs != null)
          rs.close();
        if (stmt != null)
          stmt.close();
        if (conn != null)
          conn.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    return customerInfo;
  }
  
  /**
   * Clear cache entry - in production, this would clear from Redis
   */
  public void clearCache() {
    customerCache.clear();
  }
}
