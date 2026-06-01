package com.acme.modres.mbean.reservation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.acme.modres.Constants;

/**
 * Cloud-ready reservation checker using java.time API for consistent date handling.
 * Standardized on UTC to eliminate timezone issues in distributed cloud environments.
 */
public class ReservationCheckerData {
  private ReservationList reservations;
  private LocalDate selectedDate;
  private boolean available;

  public ReservationCheckerData(ReservationList reservations) {
    this.reservations = reservations;
    this.available = true;
  }

  public ReservationList getReservationList() {
    return reservations;
  }

  /**
   * Get selected date as LocalDate (java.time API)
   */
  public LocalDate getSelectedDateAsLocalDate() {
    return selectedDate;
  }

  /**
   * Set selected date from string using java.time API
   */
  public boolean setSelectedDate(String dateStr) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      selectedDate = LocalDate.parse(dateStr, formatter);
      return true;
    } catch (DateTimeParseException e) {
      System.err.println("Failed to parse date: " + dateStr);
      return false;
    }
  }

  public boolean isAvailible() {
    return available;
  }

  public void setAvailablility(boolean available) {
    this.available = available;
  }
}
