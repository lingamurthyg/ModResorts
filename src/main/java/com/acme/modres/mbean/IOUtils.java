package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

/**
 * Cloud-ready IOUtils that reads from classpath resources instead of local file system.
 * For persistent storage needs, use Amazon S3 via AWS SDK.
 */
public final class IOUtils {

  /**
   * Read resource from classpath - cloud-compatible approach
   * Resources are packaged within the JAR and available in all cloud environments
   */
  public static InputStream getResourceAsStream(String path) {
    InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      throw new RuntimeException("Resource not found in classpath: " + path);
    }
    return stream;
  }

  public static OpMetadataList getOpListFromConfig() {
    try (InputStream resourceStream = getResourceAsStream("ops.json");
         JsonInputStream is = new JsonInputStream(resourceStream)) {
      OpMetadataList opList = new OpMetadataList(); // empty default
      opList = (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    try (InputStream resourceStream = getResourceAsStream("reservations.json");
         JsonInputStream is = new JsonInputStream(resourceStream)) {
      ReservationList reservationList = new ReservationList(); // empty default
      reservationList = (ReservationList) is.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
