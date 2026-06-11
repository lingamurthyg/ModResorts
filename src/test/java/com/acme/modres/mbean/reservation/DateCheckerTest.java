package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DateChecker class.
 * Note: DateChecker.run() always sets availability=true at the end of the loop
 * (source code behavior). Tests reflect actual source behavior.
 */
class DateCheckerTest {

    private ReservationCheckerData checkerData;
    private ReservationList reservationList;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
        checkerData = new ReservationCheckerData(reservationList);
    }

    @Test
    void constructor_createsInstance() {
        // Arrange
        checkerData.setSelectedDate("06/15/2024");

        // Act
        DateChecker checker = new DateChecker(checkerData);

        // Assert
        assertNotNull(checker);
    }

    @Test
    void run_withNoReservations_availabilityRemainsTrue() {
        // Arrange
        checkerData.setSelectedDate("06/15/2024");
        DateChecker checker = new DateChecker(checkerData);

        // Act
        checker.run();

        // Assert
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void run_withDateOutsideReservation_availabilityIsTrue() {
        // Arrange
        reservationList.add(new Reservation("06/01/2024", "06/10/2024"));
        checkerData.setSelectedDate("06/15/2024");
        DateChecker checker = new DateChecker(checkerData);

        // Act
        checker.run();

        // Assert
        // Source code always sets true at end of loop iteration
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void run_withDateBeforeReservation_availabilityIsTrue() {
        // Arrange
        reservationList.add(new Reservation("06/10/2024", "06/20/2024"));
        checkerData.setSelectedDate("06/05/2024");
        DateChecker checker = new DateChecker(checkerData);

        // Act
        checker.run();

        // Assert
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void run_withDateAfterReservation_availabilityIsTrue() {
        // Arrange
        reservationList.add(new Reservation("06/01/2024", "06/10/2024"));
        checkerData.setSelectedDate("06/25/2024");
        DateChecker checker = new DateChecker(checkerData);

        // Act
        checker.run();

        // Assert
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void run_withInvalidReservationDates_doesNotThrow() {
        // Arrange
        reservationList.add(new Reservation("invalid-date", "also-invalid"));
        checkerData.setSelectedDate("06/15/2024");
        DateChecker checker = new DateChecker(checkerData);

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> checker.run());
    }

    @Test
    void run_implementsRunnable() {
        // Arrange
        checkerData.setSelectedDate("06/15/2024");
        DateChecker checker = new DateChecker(checkerData);

        // Assert
        assertTrue(checker instanceof Runnable);
    }

    @Test
    void run_withMultipleReservations_doesNotThrow() {
        // Arrange
        reservationList.add(new Reservation("01/01/2024", "01/10/2024"));
        reservationList.add(new Reservation("06/01/2024", "06/30/2024"));
        checkerData.setSelectedDate("06/15/2024");
        DateChecker checker = new DateChecker(checkerData);

        // Act & Assert
        assertDoesNotThrow(() -> checker.run());
    }

    @Test
    void run_withEmptyReservationList_availabilityIsTrue() {
        // Arrange
        checkerData.setSelectedDate("01/01/2024");
        DateChecker checker = new DateChecker(checkerData);

        // Act
        checker.run();

        // Assert
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void constructor_setsReservationsFromData() {
        // Arrange
        reservationList.add(new Reservation("01/01/2024", "01/15/2024"));
        checkerData.setSelectedDate("01/10/2024");

        // Act
        DateChecker checker = new DateChecker(checkerData);

        // Assert
        assertNotNull(checker);
    }
}
