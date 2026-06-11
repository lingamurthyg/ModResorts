package com.acme.modres.mbean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OpMetadataList class.
 */
class OpMetadataListTest {

    private OpMetadataList opMetadataList;

    @BeforeEach
    void setUp() {
        opMetadataList = new OpMetadataList();
    }

    @Test
    void defaultConstructor_createsInstance() {
        assertNotNull(opMetadataList);
    }

    @Test
    void defaultConstructor_emptyList() {
        assertNotNull(opMetadataList.getOpMetadatList());
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void add_singleOpMetadata_listSizeIsOne() {
        // Arrange
        OpMetadata meta = new OpMetadata("op1", "desc1", "void", 1);

        // Act
        opMetadataList.add(meta);

        // Assert
        assertEquals(1, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void add_multipleOpMetadata_listSizeIsCorrect() {
        // Arrange
        OpMetadata meta1 = new OpMetadata("op1", "desc1", "void", 1);
        OpMetadata meta2 = new OpMetadata("op2", "desc2", "String", 2);
        OpMetadata meta3 = new OpMetadata("op3", "desc3", "int", 3);

        // Act
        opMetadataList.add(meta1);
        opMetadataList.add(meta2);
        opMetadataList.add(meta3);

        // Assert
        assertEquals(3, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void add_opMetadata_containsAddedElement() {
        // Arrange
        OpMetadata meta = new OpMetadata("op1", "desc1", "void", 1);

        // Act
        opMetadataList.add(meta);

        // Assert
        assertTrue(opMetadataList.getOpMetadatList().contains(meta));
    }

    @Test
    void getOpMetadatList_returnsNonNull() {
        assertNotNull(opMetadataList.getOpMetadatList());
    }

    @Test
    void setOpMetadatList_replacesExistingList() {
        // Arrange
        List<OpMetadata> newList = new ArrayList<>();
        newList.add(new OpMetadata("newOp", "newDesc", "void", 0));

        // Act
        opMetadataList.setOpMetadatList(newList);

        // Assert
        assertEquals(1, opMetadataList.getOpMetadatList().size());
        assertEquals("newOp", opMetadataList.getOpMetadatList().get(0).getName());
    }

    @Test
    void setOpMetadatList_withEmptyList_listIsEmpty() {
        // Arrange
        opMetadataList.add(new OpMetadata("op1", "desc1", "void", 1));
        List<OpMetadata> emptyList = new ArrayList<>();

        // Act
        opMetadataList.setOpMetadatList(emptyList);

        // Assert
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void setOpMetadatList_withNull_setsNull() {
        // Act
        opMetadataList.setOpMetadatList(null);

        // Assert
        assertNull(opMetadataList.getOpMetadatList());
    }

    @Test
    void add_preservesOrder() {
        // Arrange
        OpMetadata meta1 = new OpMetadata("first", "desc1", "void", 1);
        OpMetadata meta2 = new OpMetadata("second", "desc2", "void", 2);

        // Act
        opMetadataList.add(meta1);
        opMetadataList.add(meta2);

        // Assert
        assertEquals("first", opMetadataList.getOpMetadatList().get(0).getName());
        assertEquals("second", opMetadataList.getOpMetadatList().get(1).getName());
    }
}
