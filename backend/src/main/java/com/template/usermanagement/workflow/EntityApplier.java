package com.template.usermanagement.workflow;

import java.util.Map;

public interface EntityApplier {

    String getEntityType();

    Long applyCreate(Map<String, Object> payload, String performedBy);

    void applyUpdate(Long entityId, Map<String, Object> payload, String performedBy);

    void applyDelete(Long entityId, String performedBy);

    Map<String, Object> getCurrentState(Long entityId);
}
