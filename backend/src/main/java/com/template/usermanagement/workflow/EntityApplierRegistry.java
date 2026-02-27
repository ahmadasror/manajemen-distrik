package com.template.usermanagement.workflow;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EntityApplierRegistry {

    private final Map<String, EntityApplier> appliers = new HashMap<>();

    public EntityApplierRegistry(List<EntityApplier> applierList) {
        for (EntityApplier applier : applierList) {
            appliers.put(applier.getEntityType(), applier);
        }
    }

    public EntityApplier getApplier(String entityType) {
        EntityApplier applier = appliers.get(entityType);
        if (applier == null) {
            throw new IllegalArgumentException("No EntityApplier registered for type: " + entityType);
        }
        return applier;
    }
}
