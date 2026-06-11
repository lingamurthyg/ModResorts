package com.acme.modres.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Service class.
 */
class ServiceTest {

    private Service service;

    @BeforeEach
    void setUp() {
        service = new Service();
    }

    @Test
    void constructor_createsInstance() {
        assertNotNull(service);
    }

    @Test
    void operationConstant_hasCorrectValue() {
        assertEquals("my-operation", Service.OPERATION);
    }

    @Test
    void operationConstant_isNotNull() {
        assertNotNull(Service.OPERATION);
    }

    @Test
    void operation_executesWithoutException() {
        // Act & Assert
        assertDoesNotThrow(() -> service.operation());
    }

    @Test
    void operation_canBeCalledMultipleTimes() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            service.operation();
            service.operation();
            service.operation();
        });
    }

    @Test
    void operationConstant_isStatic() {
        // Verify static access works
        assertEquals("my-operation", Service.OPERATION);
    }
}
