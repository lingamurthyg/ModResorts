package com.acme.modres;

import com.acme.modres.mbean.reservation.Reservation;
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.mbean.reservation.ReservationList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AvailabilityCheckerServlet class.
 */
@ExtendWith(MockitoExtension.class)
class AvailabilityCheckerServletTest {

    private AvailabilityCheckerServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new AvailabilityCheckerServlet();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Inject a ReservationCheckerData with an empty list
        ReservationList reservationList = new ReservationList();
        ReservationCheckerData checkerData = new ReservationCheckerData(reservationList);
        Field field = AvailabilityCheckerServlet.class.getDeclaredField("reservationCheckerData");
        field.setAccessible(true);
        field.set(servlet, checkerData);
    }

    @Test
    void doGet_withValidDate_returns200StatusCode() throws Exception {
        // Arrange
        when(request.getParameter("date")).thenReturn("06/15/2024");

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).setStatus(200);
    }

    @Test
    void doGet_withInvalidDate_returns500StatusCode() throws Exception {
        // Arrange
        when(request.getParameter("date")).thenReturn("invalid-date");

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).setStatus(500);
    }

    @Test
    void doGet_withNullDate_returns500StatusCode() throws Exception {
        // Arrange
        when(request.getParameter("date")).thenReturn(null);

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).setStatus(500);
    }

    @Test
    void doGet_withValidDate_setsContentTypeJson() throws Exception {
        // Arrange
        when(request.getParameter("date")).thenReturn("06/15/2024");

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).setContentType("application/json");
    }

    @Test
    void doGet_withValidDate_setsCharacterEncodingUtf8() throws Exception {
        // Arrange
        when(request.getParameter("date")).thenReturn("06/15/2024");

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    void doGet_withValidDate_writesAvailabilityJson() throws Exception {
        // Arrange
        when(request.getParameter("date")).thenReturn("06/15/2024");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        String output = stringWriter.toString();
        assertTrue(output.contains("availability"));
    }

    @Test
    void doGet_withDateInsideReservation_returns201StatusCode() throws Exception {
        // Arrange
        ReservationList reservationList = new ReservationList();
        reservationList.add(new Reservation("06/01/2024", "06/30/2024"));
        ReservationCheckerData checkerData = new ReservationCheckerData(reservationList);
        Field field = AvailabilityCheckerServlet.class.getDeclaredField("reservationCheckerData");
        field.setAccessible(true);
        field.set(servlet, checkerData);

        when(request.getParameter("date")).thenReturn("06/15/2024");

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).setStatus(201);
    }

    @Test
    void doPost_delegatesToDoGet() throws Exception {
        // Arrange
        when(request.getParameter("date")).thenReturn("06/15/2024");

        // Act
        servlet.doPost(request, response);

        // Assert
        verify(response).setStatus(200);
    }

    @Test
    void doGet_withAvailableDate_outputContainsTrue() throws Exception {
        // Arrange
        when(request.getParameter("date")).thenReturn("06/15/2024");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        String output = stringWriter.toString();
        assertTrue(output.contains("true"));
    }
}
