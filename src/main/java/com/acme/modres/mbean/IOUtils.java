package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.regions.Region;

public final class IOUtils {

  // AWS S3 configuration from environment variables
  private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "modresorts-data");
  private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
  private static final boolean USE_S3 = Boolean.parseBoolean(System.getenv().getOrDefault("USE_S3_STORAGE", "false"));

  /**
   * Get input stream from classpath resource or S3 based on configuration
   */
  private static InputStream getResourceStream(String resourcePath) throws IOException {
    if (USE_S3) {
      // Load from S3
      try (S3Client s3Client = S3Client.builder()
              .region(Region.of(AWS_REGION))
              .build()) {
        
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(S3_BUCKET_NAME)
            .key("config/" + resourcePath)
            .build();
        
        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
        // Read entire content into memory to avoid S3 client closure issues
        byte[] content = s3Object.readAllBytes();
        return new java.io.ByteArrayInputStream(content);
      }
    } else {
      // Load from classpath (default for cloud deployments with config in container)
      InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(resourcePath);
      if (stream == null) {
        throw new IOException("Resource not found: " + resourcePath);
      }
      return stream;
    }
  }

  public static OpMetadataList getOpListFromConfig() {
    try (InputStream inputStream = getResourceStream("ops.json");
         JsonInputStream is = new JsonInputStream(inputStream)) {
      OpMetadataList opList = new OpMetadataList(); // empty default
      opList = (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    try (InputStream inputStream = getResourceStream("reservations.json");
         JsonInputStream is = new JsonInputStream(inputStream)) {
      ReservationList reservationList = new ReservationList(); // empty default
      reservationList = (ReservationList) is.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
