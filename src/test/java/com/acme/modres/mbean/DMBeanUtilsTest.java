package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import javax.management.MBeanOperationInfo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DMBeanUtils class.
 */
class DMBeanUtilsTest {

    @Test
    void getOps_withNullOpList_returnsNull() {
        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(null);

        // Assert
        assertNull(result);
    }

    @Test
    void getOps_withEmptyOpList_returnsNull() {
        // Arrange
        OpMetadataList emptyList = new OpMetadataList();

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(emptyList);

        // Assert
        assertNull(result);
    }

    @Test
    void getOps_withNullInternalList_returnsNull() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.setOpMetadatList(null);

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNull(result);
    }

    @Test
    void getOps_withSingleOperation_returnsArrayOfSizeOne() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("testOp", "Test operation", "void", MBeanOperationInfo.ACTION));

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    void getOps_withSingleOperation_correctName() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("myOperation", "My operation", "void", MBeanOperationInfo.ACTION));

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals("myOperation", result[0].getName());
    }

    @Test
    void getOps_withSingleOperation_correctDescription() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op", "My description", "void", MBeanOperationInfo.ACTION));

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals("My description", result[0].getDescription());
    }

    @Test
    void getOps_withMultipleOperations_returnsCorrectSize() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc1", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("op2", "desc2", "String", MBeanOperationInfo.INFO));
        opList.add(new OpMetadata("op3", "desc3", "int", MBeanOperationInfo.UNKNOWN));

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void getOps_withMultipleOperations_preservesOrder() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("firstOp", "desc1", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("secondOp", "desc2", "void", MBeanOperationInfo.ACTION));

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals("firstOp", result[0].getName());
        assertEquals("secondOp", result[1].getName());
    }

    @Test
    void getOps_withOperation_correctReturnType() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op", "desc", "java.lang.String", MBeanOperationInfo.ACTION));

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals("java.lang.String", result[0].getReturnType());
    }

    @Test
    void getOps_withOperation_correctImpact() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op", "desc", "void", MBeanOperationInfo.ACTION_INFO));

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals(MBeanOperationInfo.ACTION_INFO, result[0].getImpact());
    }
}
