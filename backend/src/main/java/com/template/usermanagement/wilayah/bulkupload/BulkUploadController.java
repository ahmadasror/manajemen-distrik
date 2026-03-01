package com.template.usermanagement.wilayah.bulkupload;

import com.template.usermanagement.common.ApiResponse;
import com.template.usermanagement.security.UserDetailsImpl;
import com.template.usermanagement.wilayah.bulkupload.dto.BulkUploadResponse;
import com.template.usermanagement.workflow.PendingAction;
import com.template.usermanagement.workflow.dto.PendingActionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bulk-uploads")
@RequiredArgsConstructor
public class BulkUploadController {

    private final BulkUploadService bulkUploadService;

    @PostMapping(value = "/wilayah", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAKER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadWilayah(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        BulkUpload staged = bulkUploadService.parseCsvAndStage(file, userDetails.getId());
        PendingAction pa = bulkUploadService.submitForApproval(staged.getId(), userDetails.getId());
        Map<String, Object> result = Map.of(
                "bulkUploadId", staged.getId(),
                "pendingActionId", pa.getId(),
                "rowCount", staged.getRowCount(),
                "validCount", staged.getValidCount(),
                "errorCount", staged.getErrorCount(),
                "fileName", staged.getFileName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Bulk upload staged and submitted for approval", result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BulkUploadResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bulkUploadService.getById(id)));
    }

    @GetMapping("/{id}/rows")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRows(@PathVariable Long id) {
        List<Map<String, Object>> rows = bulkUploadService.getRows(id).stream()
                .map(r -> Map.of(
                        "id", (Object) r.getId(),
                        "rowNumber", r.getRowNumber(),
                        "data", r.getData(),
                        "isValid", r.getIsValid(),
                        "errorMessage", r.getErrorMessage() != null ? r.getErrorMessage() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(rows));
    }
}
