package com.acme.modres.exception;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExceptionHandler class.
 */
class ExceptionHandlerTest {

    private static final Logger logger = Logger.getLogger(ExceptionHandlerTest.class.getName());

    @Test
    void handleException_withNullException_throwsServletException() {
        // Arrange
        String errorMsg = "Test error message";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class,
                () -> ExceptionHandler.handleException(null, errorMsg, logger));
        assertNotNull(thrown);
    }

    @Test
    void handleException_withNullException_servletExceptionContainsMessage() {
        // Arrange
        String errorMsg = "Test error message";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class,
                () -> ExceptionHandler.handleException(null, errorMsg, logger));
        assertEquals(errorMsg, thrown.getMessage());
    }

    @Test
    void handleException_withNonNullException_throwsServletException() {
        // Arrange
        Exception cause = new RuntimeException("Root cause");
        String errorMsg = "Wrapped error message";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class,
                () -> ExceptionHandler.handleException(cause, errorMsg, logger));
        assertNotNull(thrown);
    }

    @Test
    void handleException_withNonNullException_servletExceptionContainsMessage() {
        // Arrange
        Exception cause = new RuntimeException("Root cause");
        String errorMsg = "Wrapped error message";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class,
                () -> ExceptionHandler.handleException(cause, errorMsg, logger));
        assertEquals(errorMsg, thrown.getMessage());
    }

    @Test
    void handleException_withNonNullException_servletExceptionContainsCause() {
        // Arrange
        Exception cause = new RuntimeException("Root cause");
        String errorMsg = "Wrapped error message";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class,
                () -> ExceptionHandler.handleException(cause, errorMsg, logger));
        assertEquals(cause, thrown.getCause());
    }

    @Test
    void handleException_withIOException_throwsServletException() {
        // Arrange
        Exception cause = new java.io.IOException("IO error");
        String errorMsg = "IO error occurred";

        // Act & Assert
        assertThrows(ServletException.class,
                () -> ExceptionHandler.handleException(cause, errorMsg, logger));
    }

    @Test
    void handleException_withEmptyMessage_throwsServletException() {
        // Arrange
        String errorMsg = "";

        // Act & Assert
        assertThrows(ServletException.class,
                () -> ExceptionHandler.handleException(null, errorMsg, logger));
    }

    @Test
    void handleException_withNullException_noCauseInServletException() {
        // Arrange
        String errorMsg = "No cause error";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class,
                () -> ExceptionHandler.handleException(null, errorMsg, logger));
        assertNull(thrown.getCause());
    }
}
