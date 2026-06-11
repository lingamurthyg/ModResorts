package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReservationCheckerData class.
 */
class ReservationCheckerDataTest {

    private ReservationCheckerData checkerData;
    private ReservationList reservationList;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
        checkerData = new ReservationCheckerData(reservationList);
    }

    @Test
    void constructor_createsInstance() {
        assertNotNull(checkerData);
    }

    @Test
    void constructor_setsReservationList() {
        assertNotNull(checkerData.getReservationList());
    }

    @Test
    void constructor_defaultAvailabilityIsTrue() {
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void getReservationList_returnsCorrectList() {
        assertEquals(reservationList, checkerData.getReservationList());
    }

    @Test
    void setSelectedDate_withValidDate_returnsTrue() {
        // Act
        boolean result = checkerData.setSelectedDate("01/15/2024");

        // Assert
        assertTrue(result);
    }

    @Test
    void setSelectedDate_withValidDate_setsDate() {
        // Act
        checkerData.setSelectedDate("01/15/2024");

        // Assert
        assertNotNull(checkerData.getSelectedDate());
    }

    @Test
    void setSelectedDate_withValidDate_correctDateValue() {
        // Act
        checkerData.setSelectedDate("01/15/2024");

        // Assert
        LocalDate expected = LocalDate.of(2024, 1, 15);
        assertEquals(expected, checkerData.getSelectedDate());
    }

    @Test
    void setSelectedDate_withInvalidDate_returnsFalse() {
        // Act
        boolean result = checkerData.setSelectedDate("not-a-date");

        // Assert
        assertFalse(result);
    }

    @Test
    void setSelectedDate_withNullDate_returnsFalse() {
        // Act
        boolean result = checkerData.setSelectedDate(null);

        // Assert
        assertFalse(result);
    }

    @Test
    void setSelectedDate_withEmptyString_returnsFalse() {
        // Act
        boolean result = checkerData.setSelectedDate("");

        // Assert
        assertFalse(result);
    }

    @Test
    void setSelectedDate_withWrongFormat_returnsFalse() {
        // Act
        boolean result = checkerData.setSelectedDate("2024-01-15");

        // Assert
        assertFalse(result);
    }

    @Test
    void setAvailablility_withFalse_setsUnavailable() {
        // Act
        checkerData.setAvailablility(false);

        // Assert
        assertFalse(checkerData.isAvailible());
    }

    @Test
    void setAvailablility_withTrue_setsAvailable() {
        // Arrange
        checkerData.setAvailablility(false);

        // Act
        checkerData.setAvailablility(true);

        // Assert
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void isAvailible_defaultIsTrue() {
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void getSelectedDate_beforeSetting_returnsNull() {
        assertNull(checkerData.getSelectedDate());
    }

    @Test
    void setSelectedDate_withLeapYearDate_returnsTrue() {
        // Act
        boolean result = checkerData.setSelectedDate("02/29/2024");

        // Assert
        assertTrue(result);
    }

    @Test
    void setSelectedDate_withEndOfYearDate_returnsTrue() {
        // Act
        boolean result = checkerData.setSelectedDate("12/31/2024");

        // Assert
        assertTrue(result);
    }

    @Test
    void constructor_withNullReservationList_setsNullList() {
        // Act
        ReservationCheckerData data = new ReservationCheckerData(null);

        // Assert
        assertNull(data.getReservationList());
    }
}
