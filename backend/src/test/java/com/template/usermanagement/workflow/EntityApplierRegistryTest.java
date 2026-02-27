package com.template.usermanagement.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityApplierRegistryTest {

    @Mock
    private EntityApplier userApplier;

    @Mock
    private EntityApplier roleApplier;

    @Test
    @DisplayName("getApplier returns correct applier for registered entity type")
    void getApplier_registeredType_shouldReturnApplier() {
        when(userApplier.getEntityType()).thenReturn("USER");
        when(roleApplier.getEntityType()).thenReturn("ROLE");

        EntityApplierRegistry registry = new EntityApplierRegistry(List.of(userApplier, roleApplier));

        EntityApplier result = registry.getApplier("USER");

        assertSame(userApplier, result);
    }

    @Test
    @DisplayName("getApplier returns second applier correctly")
    void getApplier_secondRegisteredType_shouldReturnCorrectApplier() {
        when(userApplier.getEntityType()).thenReturn("USER");
        when(roleApplier.getEntityType()).thenReturn("ROLE");

        EntityApplierRegistry registry = new EntityApplierRegistry(List.of(userApplier, roleApplier));

        EntityApplier result = registry.getApplier("ROLE");

        assertSame(roleApplier, result);
    }

    @Test
    @DisplayName("getApplier throws IllegalArgumentException for unknown entity type")
    void getApplier_unknownType_shouldThrowIllegalArgumentException() {
        when(userApplier.getEntityType()).thenReturn("USER");

        EntityApplierRegistry registry = new EntityApplierRegistry(List.of(userApplier));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.getApplier("UNKNOWN")
        );

        assertTrue(exception.getMessage().contains("UNKNOWN"));
        assertTrue(exception.getMessage().contains("No EntityApplier registered for type"));
    }

    @Test
    @DisplayName("getApplier throws IllegalArgumentException when registry is empty")
    void getApplier_emptyRegistry_shouldThrowIllegalArgumentException() {
        EntityApplierRegistry registry = new EntityApplierRegistry(Collections.emptyList());

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.getApplier("USER")
        );
    }

    @Test
    @DisplayName("Registry constructed with multiple appliers registers all of them")
    void constructor_multipleAppliers_shouldRegisterAll() {
        when(userApplier.getEntityType()).thenReturn("USER");
        when(roleApplier.getEntityType()).thenReturn("ROLE");

        EntityApplierRegistry registry = new EntityApplierRegistry(List.of(userApplier, roleApplier));

        assertDoesNotThrow(() -> registry.getApplier("USER"));
        assertDoesNotThrow(() -> registry.getApplier("ROLE"));
    }
}
