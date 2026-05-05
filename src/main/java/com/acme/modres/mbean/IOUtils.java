package com.acme.modres.mbean;

import java.io.IOException;
import java.io.InputStream;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;

/**
 * Cloud-ready IOUtils that uses classpath resources instead of local file system.
 * For persistent storage needs, use Amazon S3 via AWS SDK.
 */
public final class IOUtils {

  /**
   * Load resource from classpath instead of creating temporary files.
   * This approach is cloud-compatible and doesn't rely on ephemeral local storage.
   */
  public static InputStream getResourceAsStream(String path) {
    InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      throw new RuntimeException("Resource not found in classpath: " + path);
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
