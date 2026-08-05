package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.acme.modres.Constants;

/**
 * DateChecker checks whether a selected date falls within any reserved period.
 *
 * <p>Migrated from legacy {@code java.util.Date} / {@code SimpleDateFormat}
 * (blockers 12 &amp; 13, cr-java-0111) to the {@code java.time} API
 * ({@link LocalDate}, {@link DateTimeFormatter}).  All date comparisons are
 * performed in UTC-equivalent wall-clock terms using {@link LocalDate} so that
 * timezone inconsistencies across cloud regions and containers are eliminated.</p>
 */
public class DateChecker implements Runnable {
  ReservationCheckerData data;
  List<Reservation> reservations;

  public DateChecker(ReservationCheckerData data) {
    this.data = data;
    this.reservations = data.getReservationList().getReservations();
  }

  public void run() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
    for (int i = 0; i < reservations.size(); i++) {
      Reservation reservation = reservations.get(i);
      // Use java.time LocalDate (UTC-standardized) instead of java.util.Date
      LocalDate selectedDate = data.getSelectedDate();

      try {
        LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
        LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);
        if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
          data.setAvailablility(false);
          return;
        }
      } catch (DateTimeParseException ex) {
        ex.printStackTrace();
      }
    }
    data.setAvailablility(true);
  }
}
