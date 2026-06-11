package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomPermission class.
 */
class CustomPermissionTest {

    @Test
    void constructor_withName_createsInstance() {
        // Act
        CustomPermission permission = new CustomPermission("test.permission");

        // Assert
        assertNotNull(permission);
    }

    @Test
    void constructor_withNameAndActions_createsInstance() {
        // Act
        CustomPermission permission = new CustomPermission("test.permission", "read,write");

        // Assert
        assertNotNull(permission);
    }

    @Test
    void constructor_withName_getName_returnsCorrectName() {
        // Arrange
        String permissionName = "com.acme.modres.test";

        // Act
        CustomPermission permission = new CustomPermission(permissionName);

        // Assert
        assertEquals(permissionName, permission.getName());
    }

    @Test
    void constructor_withNameAndActions_getName_returnsCorrectName() {
        // Arrange
        String permissionName = "com.acme.modres.test";

        // Act
        CustomPermission permission = new CustomPermission(permissionName, "read");

        // Assert
        assertEquals(permissionName, permission.getName());
    }

    @Test
    void constructor_withNameAndActions_getActions_returnsEmptyOrActions() {
        // Act
        CustomPermission permission = new CustomPermission("test.permission", "read");

        // Assert - BasicPermission ignores actions, returns empty string
        assertNotNull(permission.getActions());
    }

    @Test
    void constructor_withWildcardName_createsInstance() {
        // Act
        CustomPermission permission = new CustomPermission("com.acme.*");

        // Assert
        assertNotNull(permission);
    }

    @Test
    void implies_samePermission_returnsTrue() {
        // Arrange
        CustomPermission p1 = new CustomPermission("test.permission");
        CustomPermission p2 = new CustomPermission("test.permission");

        // Act & Assert
        assertTrue(p1.implies(p2));
    }

    @Test
    void implies_differentPermission_returnsFalse() {
        // Arrange
        CustomPermission p1 = new CustomPermission("test.permission.one");
        CustomPermission p2 = new CustomPermission("test.permission.two");

        // Act & Assert
        assertFalse(p1.implies(p2));
    }

    @Test
    void equals_samePermission_returnsTrue() {
        // Arrange
        CustomPermission p1 = new CustomPermission("test.permission");
        CustomPermission p2 = new CustomPermission("test.permission");

        // Act & Assert
        assertEquals(p1, p2);
    }

    @Test
    void hashCode_samePermission_sameHashCode() {
        // Arrange
        CustomPermission p1 = new CustomPermission("test.permission");
        CustomPermission p2 = new CustomPermission("test.permission");

        // Act & Assert
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void constructor_withEmptyActions_createsInstance() {
        // Act
        CustomPermission permission = new CustomPermission("test.permission", "");

        // Assert
        assertNotNull(permission);
    }
}
