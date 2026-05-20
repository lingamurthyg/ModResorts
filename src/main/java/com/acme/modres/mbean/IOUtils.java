package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

/**
 * Cloud-ready IOUtils that reads from classpath resources instead of local file system.
 * For cloud deployments, configuration files should be packaged in the application JAR
 * or loaded from external configuration services like AWS Systems Manager Parameter Store.
 */
public final class IOUtils {

  /**
   * Load configuration from classpath resources instead of creating temporary files.
   * This approach is cloud-native and works in containerized environments.
   */
  public static InputStream getResourceAsStream(String path) {
    InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      throw new IllegalArgumentException("Resource not found in classpath: " + path);
    }
    return stream;
  }

  public static OpMetadataList getOpListFromConfig() {
    try (InputStream stream = getResourceAsStream("ops.json");
         JsonInputStream is = new JsonInputStream(stream)) {
      OpMetadataList opList = new OpMetadataList(); // empty default
      opList = (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    try (InputStream stream = getResourceAsStream("reservations.json");
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
