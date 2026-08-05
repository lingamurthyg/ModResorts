package com.acme.modres.db;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * ModResortsCustomerInformation provides customer data access.
 *
 * <p>Migrated from EJB 2.x ({@code @Singleton}/{@code @Startup}) to a Spring
 * Boot {@code @Service} component (blocker-8 / blocker-9, cr-java-0085).
 * EJB 2.x has heavy container dependencies and poor cloud compatibility;
 * Spring Boot services are lightweight, container-friendly, and integrate
 * natively with AWS managed services (RDS, etc.).</p>
 *
 * <p>The {@link DataSource} is injected by Spring (via {@code @Autowired})
 * rather than via JNDI {@code @Resource}, enabling use with HikariCP or any
 * Spring-managed connection pool backed by Amazon RDS.</p>
 */
@Service
public class ModResortsCustomerInformation {

  private static final String SELECT_CUSTOMERS_QUERY = "SELECT INFO FROM CUSTOMER";

  @Autowired(required = false)
  private DataSource dataSource;

  public ArrayList<String> getCustomerInformation() {
    ArrayList<String> customerInfo = new ArrayList<>();

    if (dataSource == null) {
      return customerInfo;
    }

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(SELECT_CUSTOMERS_QUERY);
         ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        String info = rs.getString("INFO");
        customerInfo.add(info);
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }
    return customerInfo;
  }
}
