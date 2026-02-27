package com.template.usermanagement.workflow.dto;

import com.template.usermanagement.workflow.PendingAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class PendingActionResponse {

    private Long id;
    private String entityType;
    private Long entityId;
    private String actionType;
    private Map<String, Object> payload;
    private Map<String, Object> previousState;
    private String status;
    private Long makerId;
    private String makerUsername;
    private Long checkerId;
    private String checkerUsername;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PendingActionResponse from(PendingAction pa) {
        return PendingActionResponse.builder()
                .id(pa.getId())
                .entityType(pa.getEntityType())
                .entityId(pa.getEntityId())
                .actionType(pa.getActionType())
                .payload(pa.getPayload())
                .previousState(pa.getPreviousState())
                .status(pa.getStatus())
                .makerId(pa.getMaker().getId())
                .makerUsername(pa.getMaker().getUsername())
                .checkerId(pa.getChecker() != null ? pa.getChecker().getId() : null)
                .checkerUsername(pa.getChecker() != null ? pa.getChecker().getUsername() : null)
                .remarks(pa.getRemarks())
                .createdAt(pa.getCreatedAt())
                .updatedAt(pa.getUpdatedAt())
                .build();
    }
}
