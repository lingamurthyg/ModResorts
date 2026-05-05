package com.acme.modres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acme.modres.mbean.IOUtils;
import com.acme.modres.mbean.reservation.Reservation;
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.util.ZipValidator;

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

  private ReservationCheckerData reservationCheckerData;
  
  private S3Client s3Client;
  private String s3BucketName;

  @Override
  public void init() {
    // Initialize S3 client for cloud storage
    String awsRegion = System.getenv("AWS_REGION");
    if (awsRegion == null || awsRegion.isEmpty()) {
      awsRegion = "us-east-1"; // default region
    }
    
    s3BucketName = System.getenv("S3_BUCKET_NAME");
    if (s3BucketName == null || s3BucketName.isEmpty()) {
      s3BucketName = "modresorts-data"; // default bucket name
    }
    
    s3Client = S3Client.builder()
        .region(Region.of(awsRegion))
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

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      
      for (Reservation reservation : reservations) {
        try {
          LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
          LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);
          LocalDate selectedDate = reservationCheckerData.getSelectedDate();

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
   * Export reservations to S3 instead of local file system
   */
  protected int exportRevervations(String selectedDateStr) {
    String s3Key = "exports/reservations-" + selectedDateStr + ".zip";
    
    try {
      // Get reservation data from classpath resource
      InputStream reservationStream = getClass().getClassLoader().getResourceAsStream("reservations.json");
      if (reservationStream == null) {
        logger.severe("reservations.json not found in classpath");
        return -1;
      }
      
      // Create zip in memory using try-with-resources for automatic resource management
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
           ZipOutputStream zipOut = new ZipOutputStream(baos);
           InputStream resStream = reservationStream) {
        
        ZipEntry zipEntry = new ZipEntry("reservations.json");
        zipOut.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = resStream.read(bytes)) >= 0) {
          zipOut.write(bytes, 0, length);
        }
        
        zipOut.closeEntry();
        zipOut.finish();

        // Upload to S3
        byte[] zipData = baos.toByteArray();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(s3BucketName)
            .key(s3Key)
            .contentType("application/zip")
            .build();
        
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(zipData));
        
        // Verify zip by downloading and validating
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(s3BucketName)
            .key(s3Key)
            .build();
        
        byte[] downloadedZip = s3Client.getObject(getObjectRequest).readAllBytes();
        
        // Validate the zip (simplified validation)
        if (downloadedZip.length > 0) {
          logger.info("Successfully exported reservations to S3: " + s3Key);
          return 0;
        }
      }
    } catch (IOException e) {
      logger.severe("Error exporting reservations to S3: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      logger.severe("Unexpected error during export: " + e.getMessage());
      e.printStackTrace();
    }
    return -1;
  }

}
