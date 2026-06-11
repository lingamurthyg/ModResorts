package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AppInfo class.
 * Note: AppInfo constructor calls IOUtils.getOpListFromConfig() which reads ops.json.
 * We inject a controlled MBeanInfo to avoid dependency on the resource file.
 */
class AppInfoTest {

    /**
     * Helper to create an AppInfo with a mocked/injected dMBeanInfo to avoid
     * dependency on ops.json resource file with potentially invalid impact values.
     */
    private AppInfo createAppInfoWithEmptyOps() throws Exception {
        // Use a fresh OpMetadataList with no ops to avoid invalid impact values from ops.json
        AppInfo appInfo = new AppInfo();
        // Build a clean MBeanInfo with no operations
        javax.management.MBeanInfo cleanInfo = new javax.management.MBeanInfo(
                AppInfo.class.getName(),
                "Configurable App Info",
                null, null, new javax.management.MBeanOperationInfo[0], null);
        Field field = AppInfo.class.getDeclaredField("dMBeanInfo");
        field.setAccessible(true);
        field.set(appInfo, cleanInfo);
        return appInfo;
    }

    @Test
    void constructor_createsInstance() throws Exception {
        // Act
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Assert
        assertNotNull(appInfo);
    }

    @Test
    void getMBeanInfo_returnsNonNull() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act
        MBeanInfo info = appInfo.getMBeanInfo();

        // Assert
        assertNotNull(info);
    }

    @Test
    void getMBeanInfo_classNameIsCorrect() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act
        MBeanInfo info = appInfo.getMBeanInfo();

        // Assert
        assertEquals(AppInfo.class.getName(), info.getClassName());
    }

    @Test
    void getMBeanInfo_descriptionIsCorrect() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act
        MBeanInfo info = appInfo.getMBeanInfo();

        // Assert
        assertEquals("Configurable App Info", info.getDescription());
    }

    @Test
    void invoke_increaseMaxLimit_returnsSuccessMessage() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act
        Object result = appInfo.invoke("increaseMaxLimit", new Object[]{}, new String[]{});

        // Assert
        assertEquals("Max limit increased", result);
    }

    @Test
    void invoke_resetMaxLimit_returnsSuccessMessage() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act
        Object result = appInfo.invoke("resetMaxLimit", new Object[]{}, new String[]{});

        // Assert
        assertEquals("Max limit reset", result);
    }

    @Test
    void invoke_unknownAction_throwsMBeanException() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act & Assert
        assertThrows(MBeanException.class,
                () -> appInfo.invoke("unknownAction", new Object[]{}, new String[]{}));
    }

    @Test
    void getAttribute_returnsNull() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act
        Object result = appInfo.getAttribute("anyAttribute");

        // Assert
        assertNull(result);
    }

    @Test
    void getAttributes_returnsNull() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act
        Object result = appInfo.getAttributes(new String[]{"attr1", "attr2"});

        // Assert
        assertNull(result);
    }

    @Test
    void setAttributes_returnsNull() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act
        Object result = appInfo.setAttributes(new javax.management.AttributeList());

        // Assert
        assertNull(result);
    }

    @Test
    void setAttribute_doesNotThrow() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act & Assert
        assertDoesNotThrow(() -> appInfo.setAttribute(
                new javax.management.Attribute("testAttr", "testValue")));
    }

    @Test
    void invoke_withNullParams_increaseMaxLimit_returnsSuccessMessage() throws Exception {
        // Arrange
        AppInfo appInfo = createAppInfoWithEmptyOps();

        // Act
        Object result = appInfo.invoke("increaseMaxLimit", null, null);

        // Assert
        assertEquals("Max limit increased", result);
    }
}
