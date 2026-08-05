package com.acme.modres.mbean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * IOUtils provides cloud-native I/O helpers.
 *
 * <p>Temporary file creation and local file writes have been replaced with
 * Amazon S3 operations to ensure data durability across container restarts
 * and to eliminate reliance on ephemeral local temporary directories.</p>
 *
 * <p>The S3 bucket name is read from the {@code S3_BUCKET_NAME} environment
 * variable (defaulting to {@code modresorts-data}) so that no file-system
 * paths are hard-coded in the application.</p>
 */
public final class IOUtils {

  /** S3 bucket name sourced from environment variable – no hard-coded paths. */
  private static final String S3_BUCKET_NAME = System.getenv("S3_BUCKET_NAME") != null
      ? System.getenv("S3_BUCKET_NAME")
      : "modresorts-data";

  // Private constructor – utility class
  private IOUtils() {}

  /**
   * Reads a classpath resource and returns its content as a byte array.
   * Falls back to Amazon S3 when the resource is not found on the classpath,
   * replacing the previous pattern of writing to a local temporary file
   * (File.createTempFile) which relied on ephemeral local storage.
   */
  public static byte[] readResourceBytes(String resourceName) throws IOException {
    // Primary: load from classpath (works for bundled resources)
    try (InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(resourceName)) {
      if (stream != null) {
        return stream.readAllBytes();
      }
    }

    // Fallback: load from Amazon S3 for externally managed configuration files.
    // Uses try-with-resources to ensure the S3 response stream is always closed.
    try (S3Client s3 = S3Client.create();
         ResponseInputStream<GetObjectResponse> s3Stream = s3.getObject(
             GetObjectRequest.builder()
                 .bucket(S3_BUCKET_NAME)
                 .key("config/" + resourceName)
                 .build())) {
      return s3Stream.readAllBytes();
    } catch (NoSuchKeyException e) {
      throw new IOException("Resource not found in classpath or S3: " + resourceName, e);
    }
  }

  /**
   * Writes data to Amazon S3 under the {@code config/} prefix, replacing the
   * previous local FileOutputStream write that stored data in an ephemeral
   * temporary directory and was lost on container restart.
   */
  public static void writeToS3(String resourceName, byte[] data) throws IOException {
    try (S3Client s3 = S3Client.create()) {
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(S3_BUCKET_NAME)
          .key("config/" + resourceName)
          .build();
      s3.putObject(putRequest, RequestBody.fromBytes(data));
    }
  }

  public static OpMetadataList getOpListFromConfig() {
    try {
      byte[] bytes = readResourceBytes("ops.json");
      // Wrap bytes in a stream compatible with JsonInputStream
      try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
           JsonInputStream is = new JsonInputStream(bais)) {
        OpMetadataList opList = new OpMetadataList();
        opList = (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
        return opList;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    try {
      byte[] bytes = readResourceBytes("reservations.json");
      try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
           JsonInputStream is = new JsonInputStream(bais)) {
        ReservationList reservationList = new ReservationList();
        reservationList = (ReservationList) is.parseJsonAs(ReservationList.class);
        return reservationList;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
