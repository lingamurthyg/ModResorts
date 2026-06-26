package com.acme.modres;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acme.modres.mbean.IOUtils;
import com.acme.modres.mbean.reservation.DateChecker;
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.mbean.reservation.Reservation;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.regions.Region;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private static InitialContext context;

  private ReservationCheckerData reservationCheckerData;
  
  // AWS S3 configuration from environment variables
  private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "modresorts-data");
  private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");

  @Override
  public void init() {
    // load reserved dates
    this.reservationCheckerData = new ReservationCheckerData(IOUtils.getReservationListFromConfig());
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
   */
  protected int exportRevervations(String selectedDateStr) {
    // Use try-with-resources for automatic resource management
    try (S3Client s3Client = S3Client.builder()
            .region(Region.of(AWS_REGION))
            .build()) {
      
      // Read reservations.json from classpath
      InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("reservations.json");
      if (resourceStream == null) {
        logger.severe("reservations.json not found in classpath");
        return -1;
      }
      
      // Read the content into a byte array using try-with-resources
      byte[] fileContent;
      try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = resourceStream.read(data, 0, data.length)) != -1) {
          buffer.write(data, 0, nRead);
        }
        fileContent = buffer.toByteArray();
      } finally {
        resourceStream.close();
      }

      // Upload to S3
      String s3Key = "exports/reservations-" + selectedDateStr + ".json";
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key(s3Key)
          .contentType("application/json")
          .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));
      
      logger.info("Successfully exported reservations to S3: s3://" + S3_BUCKET_NAME + "/" + s3Key);
      return 0;
      
    } catch (IOException e) {
      logger.severe("IOException during S3 export: " + e.getMessage());
      e.printStackTrace();
      return -1;
    } catch (Exception e) {
      logger.severe("Error during S3 export: " + e.getMessage());
      e.printStackTrace();
      return -1;
    }
  }

}
