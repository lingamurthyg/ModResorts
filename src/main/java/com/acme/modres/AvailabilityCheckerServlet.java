package com.acme.modres;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acme.modres.mbean.IOUtils;
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.mbean.reservation.Reservation;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private static InitialContext context;

  private ReservationCheckerData reservationCheckerData;
  
  // AWS S3 configuration from environment variables
  private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "modresorts-data");
  private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
  
  private S3Client s3Client;

  @Override
  public void init() {
    // Initialize AWS S3 client
    s3Client = S3Client.builder()
        .region(Region.of(AWS_REGION))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
    
    // load reserved dates
    this.reservationCheckerData = new ReservationCheckerData(IOUtils.getReservationListFromConfig());
  }
  
  @Override
  public void destroy() {
    // Close S3 client to prevent resource leaks
    if (s3Client != null) {
      s3Client.close();
    }
    super.destroy();
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    String methodName = "doGet";
    logger.entering(AvailabilityCheckerServlet.class.getName(), methodName);
    int statusCode = 200;

    String selectedDateStr = request.getParameter("date");
    boolean parsedDate = reservationCheckerData.setSelectedDate(selectedDateStr);
    if (!parsedDate || reservationCheckerData.getReservationList() == null) {
      statusCode = 500;
      reservationCheckerData.setAvailablility(false);
    } else {
      List<Reservation> reservations = reservationCheckerData.getReservationList().getReservations();
      boolean isAvailible = true;

      // Use java.time API instead of java.util.Date for cloud-native time handling
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      
      for (Reservation reservation : reservations) {
        try {
          LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
          LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);
          LocalDate selectedDate = LocalDate.parse(selectedDateStr, formatter);

          if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
            isAvailible = false;
            break;
          }
        } catch (DateTimeParseException ex) {
          logger.severe("Failed to parse date: " + ex.getMessage());
          ex.printStackTrace();
        }
      }

      reservationCheckerData.setAvailablility(isAvailible);

      // Adjust the status code based on availability
      if (!isAvailible) {
        statusCode = 201;
      }
    }

    // Send the response
    PrintWriter out = response.getWriter();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    out.print("{\"availability\": \"" + String.valueOf(reservationCheckerData.isAvailible()) + "\"}");
    response.setStatus(statusCode);
  }

  /**
   * Returns the weather information for a given city
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    doGet(request, response);
  }

  /**
   * Export reservations to Amazon S3 instead of local file system
   * This ensures data durability and availability in cloud environments
   */
  protected int exportRevervations(String selectedDateStr) {
    String s3Key = "reservations/" + selectedDateStr + "/reservations.json";
    
    // Use try-with-resources to ensure proper resource management
    try (InputStream reservationStream = IOUtils.class.getClassLoader().getResourceAsStream("reservations.json")) {
      
      if (reservationStream == null) {
        logger.severe("reservations.json not found in classpath");
        return -1;
      }
      
      // Read the reservation data
      byte[] reservationData = reservationStream.readAllBytes();
      
      // Upload to S3 for durable storage
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(s3Key)
          .contentType("application/json")
          .build();
      
      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(reservationData));
      
      logger.info("Successfully exported reservations to S3: s3://" + S3_BUCKET_NAME + "/" + s3Key);
      
      // Verify the upload by checking if object exists
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(s3Key)
          .build();
      
      try (InputStream verifyStream = s3Client.getObject(getObjectRequest)) {
        if (verifyStream != null) {
          return 0; // Success
        }
      }
      
    } catch (IOException e) {
      logger.severe("Failed to export reservations to S3: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      logger.severe("Unexpected error during S3 export: " + e.getMessage());
      e.printStackTrace();
    }
    
    return -1;
  }

}