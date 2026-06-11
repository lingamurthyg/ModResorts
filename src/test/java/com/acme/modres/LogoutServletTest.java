package com.acme.modres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LogoutServlet class.
 */
@ExtendWith(MockitoExtension.class)
class LogoutServletTest {

    private LogoutServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        servlet = new LogoutServlet();
    }

    @Test
    void doGet_withActiveSession_invalidatesSession() throws Exception {
        // Arrange
        when(request.getSession(false)).thenReturn(session);

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(session).invalidate();
    }

    @Test
    void doGet_withNoSession_doesNotThrow() throws Exception {
        // Arrange
        when(request.getSession(false)).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> servlet.doGet(request, response));
    }

    @Test
    void doGet_redirectsToLoginPage() throws Exception {
        // Arrange
        when(request.getSession(false)).thenReturn(null);

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).sendRedirect("login.jsp");
    }

    @Test
    void doGet_withActiveSession_redirectsToLoginPage() throws Exception {
        // Arrange
        when(request.getSession(false)).thenReturn(session);

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).sendRedirect("login.jsp");
    }

    @Test
    void doGet_withNullSession_doesNotCallInvalidate() throws Exception {
        // Arrange
        when(request.getSession(false)).thenReturn(null);

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(session, never()).invalidate();
    }

    @Test
    void servlet_isHttpServlet() {
        assertTrue(servlet instanceof jakarta.servlet.http.HttpServlet);
    }

    @Test
    void doGet_callsGetSessionWithFalse() throws Exception {
        // Arrange
        when(request.getSession(false)).thenReturn(null);

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(request).getSession(false);
    }
}
