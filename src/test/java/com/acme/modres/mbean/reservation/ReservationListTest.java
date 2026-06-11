package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReservationList class.
 */
class ReservationListTest {

    private ReservationList reservationList;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
    }

    @Test
    void defaultConstructor_createsInstance() {
        assertNotNull(reservationList);
    }

    @Test
    void defaultConstructor_emptyReservations() {
        assertNotNull(reservationList.getReservations());
        assertTrue(reservationList.getReservations().isEmpty());
    }

    @Test
    void parameterizedConstructor_setsReservations() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(new Reservation("01/01/2024", "01/15/2024"));

        // Act
        ReservationList list = new ReservationList(reservations);

        // Assert
        assertEquals(1, list.getReservations().size());
    }

    @Test
    void add_singleReservation_listSizeIsOne() {
        // Arrange
        Reservation r = new Reservation("01/01/2024", "01/15/2024");

        // Act
        reservationList.add(r);

        // Assert
        assertEquals(1, reservationList.getReservations().size());
    }

    @Test
    void add_multipleReservations_listSizeIsCorrect() {
        // Arrange
        Reservation r1 = new Reservation("01/01/2024", "01/15/2024");
        Reservation r2 = new Reservation("02/01/2024", "02/15/2024");
        Reservation r3 = new Reservation("03/01/2024", "03/15/2024");

        // Act
        reservationList.add(r1);
        reservationList.add(r2);
        reservationList.add(r3);

        // Assert
        assertEquals(3, reservationList.getReservations().size());
    }

    @Test
    void add_reservation_containsAddedElement() {
        // Arrange
        Reservation r = new Reservation("01/01/2024", "01/15/2024");

        // Act
        reservationList.add(r);

        // Assert
        assertTrue(reservationList.getReservations().contains(r));
    }

    @Test
    void getReservations_returnsNonNull() {
        assertNotNull(reservationList.getReservations());
    }

    @Test
    void add_preservesOrder() {
        // Arrange
        Reservation r1 = new Reservation("01/01/2024", "01/15/2024");
        Reservation r2 = new Reservation("02/01/2024", "02/15/2024");

        // Act
        reservationList.add(r1);
        reservationList.add(r2);

        // Assert
        assertEquals("01/01/2024", reservationList.getReservations().get(0).getFromDate());
        assertEquals("02/01/2024", reservationList.getReservations().get(1).getFromDate());
    }

    @Test
    void parameterizedConstructor_withEmptyList_emptyReservations() {
        // Arrange
        List<Reservation> emptyList = new ArrayList<>();

        // Act
        ReservationList list = new ReservationList(emptyList);

        // Assert
        assertTrue(list.getReservations().isEmpty());
    }

    @Test
    void parameterizedConstructor_withMultipleReservations_correctSize() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(new Reservation("01/01/2024", "01/15/2024"));
        reservations.add(new Reservation("02/01/2024", "02/15/2024"));

        // Act
        ReservationList list = new ReservationList(reservations);

        // Assert
        assertEquals(2, list.getReservations().size());
    }
}
