package com.acme.modres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WelcomeServlet class.
 */
@ExtendWith(MockitoExtension.class)
class WelcomeServletTest {

    private WelcomeServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new WelcomeServlet();
    }

    @Test
    void doGet_setsContentTypePlain() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).setContentType("text/plain");
    }

    @Test
    void doGet_writesEnjoyMessage() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        assertTrue(stringWriter.toString().contains("Enjoy!"));
    }

    @Test
    void doGet_responseWriterIsUsed() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).getWriter();
    }

    @Test
    void doGet_doesNotThrowException() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Act & Assert
        assertDoesNotThrow(() -> servlet.doGet(request, response));
    }

    @Test
    void servlet_isHttpServlet() {
        // Assert - no stubbing needed for this test
        assertTrue(servlet instanceof jakarta.servlet.http.HttpServlet);
    }
}
