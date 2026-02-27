package com.template.usermanagement.audit.dto;

import com.template.usermanagement.audit.AuditTrail;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AuditTrailResponse {

    private Long id;
    private String entityType;
    private Long entityId;
    private String action;
    private Map<String, Object> beforeState;
    private Map<String, Object> afterState;
    private List<String> changedFields;
    private String performedBy;
    private String ipAddress;
    private String correlationId;
    private Long pendingActionId;
    private LocalDateTime createdAt;

    public static AuditTrailResponse from(AuditTrail audit) {
        return AuditTrailResponse.builder()
                .id(audit.getId())
                .entityType(audit.getEntityType())
                .entityId(audit.getEntityId())
                .action(audit.getAction())
                .beforeState(audit.getBeforeState())
                .afterState(audit.getAfterState())
                .changedFields(audit.getChangedFields())
                .performedBy(audit.getPerformedBy())
                .ipAddress(audit.getIpAddress())
                .correlationId(audit.getCorrelationId())
                .pendingActionId(audit.getPendingActionId())
                .createdAt(audit.getCreatedAt())
                .build();
    }
}
