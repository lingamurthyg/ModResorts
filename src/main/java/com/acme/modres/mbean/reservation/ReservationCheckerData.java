package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.acme.modres.Constants;

/**
 * ReservationCheckerData holds the state for a single availability check.
 *
 * <p>Migrated from legacy {@code java.util.Date} / {@code SimpleDateFormat}
 * (blocker-14, cr-java-0111) to the {@code java.time} API ({@link LocalDate},
 * {@link DateTimeFormatter}).  Using {@link LocalDate} eliminates timezone
 * inconsistencies across cloud regions and containers, standardising all
 * date handling on UTC-equivalent wall-clock dates.</p>
 */
public class ReservationCheckerData {
  private ReservationList reservations;
  // Replaced java.util.Date with java.time.LocalDate (UTC-standardized)
  private LocalDate selectedDate;
  private boolean available;

  public ReservationCheckerData(ReservationList reservations) {
    this.reservations = reservations;
    this.available = true;
  }

  public ReservationList getReservationList() {
    return reservations;
  }

  public LocalDate getSelectedDate() {
    return selectedDate;
  }

  /**
   * Parses the date string using the application date format and stores it as
   * a {@link LocalDate} (UTC-standardized), replacing the previous
   * {@code SimpleDateFormat} / {@code java.util.Date} pattern.
   */
  public boolean setSelectedDate(String dateStr) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      selectedDate = LocalDate.parse(dateStr, formatter);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

  public boolean isAvailible() {
    return available;
  }

  public void setAvailablility(boolean available) {
    this.available = available;
  }
}
