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
 * Unit tests for UpperServlet class.
 */
@ExtendWith(MockitoExtension.class)
class UpperServletTest {

    private UpperServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new UpperServlet();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void doGet_withValidInput_convertsToUpperCase() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn("hello");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        assertTrue(stringWriter.toString().contains("HELLO"));
    }

    @Test
    void doGet_withNullInput_usesEmptyString() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn(null);

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        assertNotNull(stringWriter.toString());
    }

    @Test
    void doGet_withEmptyInput_returnsEmptyUpperCase() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn("");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        assertNotNull(stringWriter.toString());
    }

    @Test
    void doGet_withLowerCaseInput_convertsToUpperCase() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn("world");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        assertTrue(stringWriter.toString().contains("WORLD"));
    }

    @Test
    void doGet_withMixedCaseInput_convertsToUpperCase() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn("HeLLo WoRLd");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        assertTrue(stringWriter.toString().contains("HELLO WORLD"));
    }

    @Test
    void doGet_withHtmlSpecialChars_encodesHtml() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn("<script>");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        String output = stringWriter.toString();
        assertFalse(output.contains("<script>"));
        assertTrue(output.contains("&lt;SCRIPT&gt;"));
    }

    @Test
    void doGet_withAmpersand_encodesAmpersand() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn("a&b");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        String output = stringWriter.toString();
        assertTrue(output.contains("&amp;"));
    }

    @Test
    void doGet_setsContentTypeHtml() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn("test");

        // Act
        servlet.doGet(request, response);

        // Assert
        verify(response).setContentType("text/html");
    }

    @Test
    void doGet_withDoubleQuote_encodesQuote() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn("say \"hello\"");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        String output = stringWriter.toString();
        assertTrue(output.contains("&quot;"));
    }

    @Test
    void doGet_withSingleQuote_encodesSingleQuote() throws Exception {
        // Arrange
        when(request.getParameter("input")).thenReturn("it's");

        // Act
        servlet.doGet(request, response);

        // Assert
        printWriter.flush();
        String output = stringWriter.toString();
        assertTrue(output.contains("&#x27;"));
    }
}
