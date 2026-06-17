package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Cloud-native utility class for loading configuration data.
 * Uses Amazon S3 for durable storage instead of local file system.
 * Falls back to classpath resources for development/testing.
 */
public final class IOUtils {

  private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "modresorts-data");
  private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
  private static final boolean USE_S3 = Boolean.parseBoolean(System.getenv().getOrDefault("USE_S3_STORAGE", "false"));
  
  private static S3Client s3Client;
  
  static {
    if (USE_S3) {
      s3Client = S3Client.builder()
          .region(Region.of(AWS_REGION))
          .credentialsProvider(DefaultCredentialsProvider.create())
          .build();
    }
  }

  /**
   * Get input stream from S3 or classpath resource
   * @param path Resource path
   * @return InputStream for the resource
   */
  private static InputStream getResourceStream(String path) throws IOException {
    if (USE_S3 && s3Client != null) {
      try {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(S3_BUCKET_NAME)
            .key("config/" + path)
            .build();
        
        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
        return s3Object;
      } catch (Exception e) {
        System.err.println("Failed to load from S3, falling back to classpath: " + e.getMessage());
        // Fall back to classpath
      }
    }
    
    // Load from classpath (for development or when S3 is not available)
    InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      throw new IOException("Resource not found: " + path);
    }
    return stream;
  }

  /**
   * Load operations metadata from cloud storage or classpath
   * @return OpMetadataList containing operation metadata
   */
  public static OpMetadataList getOpListFromConfig() {
    // Use try-with-resources for automatic resource management (fixes resource leak)
    try (InputStream stream = getResourceStream("ops.json");
         JsonInputStream is = new JsonInputStream(stream)) {
      
      OpMetadataList opList = new OpMetadataList(); // empty default
      opList = (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Load reservation list from cloud storage or classpath
   * @return ReservationList containing reservation data
   */
  public static ReservationList getReservationListFromConfig() {
    // Use try-with-resources for automatic resource management (fixes resource leak)
    try (InputStream stream = getResourceStream("reservations.json");
         JsonInputStream is = new JsonInputStream(stream)) {
      
      ReservationList reservationList = new ReservationList(); // empty default
      reservationList = (ReservationList) is.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}