package com.acme.modres.mbean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpMetadata class.
 */
class OpMetadataTest {

    private OpMetadata opMetadata;

    @BeforeEach
    void setUp() {
        opMetadata = new OpMetadata();
    }

    @Test
    void defaultConstructor_createsInstance() {
        assertNotNull(opMetadata);
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        // Arrange & Act
        OpMetadata meta = new OpMetadata("testOp", "Test description", "void", 1);

        // Assert
        assertEquals("testOp", meta.getName());
        assertEquals("Test description", meta.getDescription());
        assertEquals("void", meta.getType());
        assertEquals(1, meta.getImpact());
    }

    @Test
    void getName_afterSetName_returnsCorrectValue() {
        // Arrange
        opMetadata.setName("myOperation");

        // Act & Assert
        assertEquals("myOperation", opMetadata.getName());
    }

    @Test
    void getDescription_afterSetDescription_returnsCorrectValue() {
        // Arrange
        opMetadata.setDescription("My operation description");

        // Act & Assert
        assertEquals("My operation description", opMetadata.getDescription());
    }

    @Test
    void getType_afterSetType_returnsCorrectValue() {
        // Arrange
        opMetadata.setType("java.lang.String");

        // Act & Assert
        assertEquals("java.lang.String", opMetadata.getType());
    }

    @Test
    void getImpact_afterSetImpact_returnsCorrectValue() {
        // Arrange
        opMetadata.setImpact(2);

        // Act & Assert
        assertEquals(2, opMetadata.getImpact());
    }

    @Test
    void getName_defaultConstructor_returnsNull() {
        assertNull(opMetadata.getName());
    }

    @Test
    void getDescription_defaultConstructor_returnsNull() {
        assertNull(opMetadata.getDescription());
    }

    @Test
    void getType_defaultConstructor_returnsNull() {
        assertNull(opMetadata.getType());
    }

    @Test
    void getImpact_defaultConstructor_returnsZero() {
        assertEquals(0, opMetadata.getImpact());
    }

    @Test
    void setName_withNull_setsNull() {
        opMetadata.setName(null);
        assertNull(opMetadata.getName());
    }

    @Test
    void setDescription_withNull_setsNull() {
        opMetadata.setDescription(null);
        assertNull(opMetadata.getDescription());
    }

    @Test
    void setType_withNull_setsNull() {
        opMetadata.setType(null);
        assertNull(opMetadata.getType());
    }

    @Test
    void setImpact_withNegativeValue_setsNegativeValue() {
        opMetadata.setImpact(-1);
        assertEquals(-1, opMetadata.getImpact());
    }

    @Test
    void parameterizedConstructor_withNullValues_setsNullFields() {
        OpMetadata meta = new OpMetadata(null, null, null, 0);
        assertNull(meta.getName());
        assertNull(meta.getDescription());
        assertNull(meta.getType());
        assertEquals(0, meta.getImpact());
    }

    @Test
    void setName_overwritesPreviousValue() {
        opMetadata.setName("first");
        opMetadata.setName("second");
        assertEquals("second", opMetadata.getName());
    }

    @Test
    void setImpact_withZero_setsZero() {
        opMetadata.setImpact(0);
        assertEquals(0, opMetadata.getImpact());
    }
}
