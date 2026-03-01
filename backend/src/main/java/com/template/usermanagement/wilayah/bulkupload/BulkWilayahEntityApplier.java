package com.template.usermanagement.wilayah.bulkupload;

import com.template.usermanagement.wilayah.bulkupload.dto.BulkUploadResponse;
import com.template.usermanagement.workflow.EntityApplier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BulkWilayahEntityApplier implements EntityApplier {

    private final BulkUploadService bulkUploadService;

    public BulkWilayahEntityApplier(@Lazy BulkUploadService bulkUploadService) {
        this.bulkUploadService = bulkUploadService;
    }

    @Override
    public String getEntityType() {
        return "BULK_UPLOAD_WILAYAH";
    }

    @Override
    public Long applyCreate(Map<String, Object> payload, String performedBy) {
        Long bulkUploadId = Long.valueOf(payload.get("bulkUploadId").toString());
        bulkUploadService.applyBulkUpload(bulkUploadId, performedBy);
        return bulkUploadId;
    }

    @Override
    public void applyUpdate(Long entityId, Map<String, Object> payload, String performedBy) {
        throw new UnsupportedOperationException("BulkUpload does not support UPDATE");
    }

    @Override
    public void applyDelete(Long entityId, String performedBy) {
        throw new UnsupportedOperationException("BulkUpload does not support DELETE");
    }

    @Override
    public Map<String, Object> getCurrentState(Long id) {
        BulkUploadResponse resp = bulkUploadService.getById(id);
        return Map.of(
                "id", resp.getId(),
                "fileName", resp.getFileName(),
                "status", resp.getStatus(),
                "rowCount", resp.getRowCount(),
                "validCount", resp.getValidCount()
        );
    }
}
