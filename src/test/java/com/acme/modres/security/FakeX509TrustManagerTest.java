package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FakeX509TrustManager class.
 */
class FakeX509TrustManagerTest {

    @Test
    void constructor_createsInstance() {
        // Act
        FakeX509TrustManager trustManager = new FakeX509TrustManager();

        // Assert
        assertNotNull(trustManager);
    }

    @Test
    void instance_isNotNull() {
        // Act
        FakeX509TrustManager trustManager = new FakeX509TrustManager();

        // Assert
        assertNotNull(trustManager);
    }

    @Test
    void class_isInstantiable() {
        // Act & Assert
        assertDoesNotThrow(() -> new FakeX509TrustManager());
    }
}
