package com.acme.modres.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Migrated from EJB 2.x to Spring Data with HikariCP connection pooling.
 * This is cloud-ready and compatible with AWS RDS and containerized environments.
 */
@Repository
public class ModResortsCustomerInformation {
  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  // Spring-managed DataSource with HikariCP connection pooling
  @Autowired
  private DataSource dataSource;

  /**
   * Retrieve customer information using connection pooling.
   * Uses try-with-resources for automatic resource management to prevent leaks.
   */
  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();

    // Try-with-resources ensures all resources are properly closed
    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
         ResultSet rs = stmt.executeQuery()) {

      // Process the results
      while (rs.next()) {
        String info = rs.getString("INFO");
        customerInfo.add(info);
      }

    } catch (SQLException e) {
      // Log the error and rethrow as runtime exception for proper error handling
      System.err.println("Database error while retrieving customer information: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Failed to retrieve customer information", e);
    }
    
    return customerInfo;
  }
}
