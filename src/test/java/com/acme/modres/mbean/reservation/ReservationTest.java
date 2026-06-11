package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Reservation class.
 */
class ReservationTest {

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
    }

    @Test
    void defaultConstructor_createsInstance() {
        assertNotNull(reservation);
    }

    @Test
    void defaultConstructor_fromDateIsNull() {
        assertNull(reservation.getFromDate());
    }

    @Test
    void defaultConstructor_toDateIsNull() {
        assertNull(reservation.getToDate());
    }

    @Test
    void parameterizedConstructor_setsFromDate() {
        // Arrange & Act
        Reservation r = new Reservation("01/01/2024", "01/15/2024");

        // Assert
        assertEquals("01/01/2024", r.getFromDate());
    }

    @Test
    void parameterizedConstructor_setsToDate() {
        // Arrange & Act
        Reservation r = new Reservation("01/01/2024", "01/15/2024");

        // Assert
        assertEquals("01/15/2024", r.getToDate());
    }

    @Test
    void setFromDate_updatesFromDate() {
        // Arrange
        reservation.setFromDate("03/01/2024");

        // Act & Assert
        assertEquals("03/01/2024", reservation.getFromDate());
    }

    @Test
    void setToDate_updatesToDate() {
        // Arrange
        reservation.setToDate("03/31/2024");

        // Act & Assert
        assertEquals("03/31/2024", reservation.getToDate());
    }

    @Test
    void setFromDate_withNull_setsNull() {
        reservation.setFromDate(null);
        assertNull(reservation.getFromDate());
    }

    @Test
    void setToDate_withNull_setsNull() {
        reservation.setToDate(null);
        assertNull(reservation.getToDate());
    }

    @Test
    void setFromDate_overwritesPreviousValue() {
        reservation.setFromDate("01/01/2024");
        reservation.setFromDate("02/01/2024");
        assertEquals("02/01/2024", reservation.getFromDate());
    }

    @Test
    void setToDate_overwritesPreviousValue() {
        reservation.setToDate("01/31/2024");
        reservation.setToDate("02/28/2024");
        assertEquals("02/28/2024", reservation.getToDate());
    }

    @Test
    void parameterizedConstructor_withNullDates_setsNullDates() {
        Reservation r = new Reservation(null, null);
        assertNull(r.getFromDate());
        assertNull(r.getToDate());
    }

    @Test
    void getFromDate_afterSetFromDate_returnsCorrectValue() {
        // Arrange
        String expectedDate = "06/15/2024";
        reservation.setFromDate(expectedDate);

        // Act
        String actualDate = reservation.getFromDate();

        // Assert
        assertEquals(expectedDate, actualDate);
    }

    @Test
    void getToDate_afterSetToDate_returnsCorrectValue() {
        // Arrange
        String expectedDate = "06/30/2024";
        reservation.setToDate(expectedDate);

        // Act
        String actualDate = reservation.getToDate();

        // Assert
        assertEquals(expectedDate, actualDate);
    }
}
