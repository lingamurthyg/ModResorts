package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SSLUtils class.
 */
class SSLUtilsTest {

    @Test
    void constructor_createsInstance() {
        // Act
        SSLUtils sslUtils = new SSLUtils();

        // Assert
        assertNotNull(sslUtils);
    }

    @Test
    void class_isInstantiable() {
        // Act & Assert
        assertDoesNotThrow(() -> new SSLUtils());
    }

    @Test
    void instance_isNotNull() {
        // Act
        SSLUtils sslUtils = new SSLUtils();

        // Assert
        assertNotNull(sslUtils);
    }
}
