package com.template.usermanagement.wilayah.bulkupload.dto;

import com.template.usermanagement.wilayah.bulkupload.BulkUpload;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class BulkUploadResponse {

    private Long id;
    private String entityType;
    private String fileName;
    private Integer rowCount;
    private Integer validCount;
    private Integer errorCount;
    private String status;
    private Map<String, Object> summary;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BulkUploadResponse from(BulkUpload b) {
        return BulkUploadResponse.builder()
                .id(b.getId())
                .entityType(b.getEntityType())
                .fileName(b.getFileName())
                .rowCount(b.getRowCount())
                .validCount(b.getValidCount())
                .errorCount(b.getErrorCount())
                .status(b.getStatus())
                .summary(b.getSummary())
                .uploadedBy(b.getUploadedBy() != null ? b.getUploadedBy().getUsername() : null)
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
