package com.template.usermanagement.wilayah.bulkupload;

import com.template.usermanagement.audit.AuditTrailService;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.common.ResourceNotFoundException;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserRepository;
import com.template.usermanagement.wilayah.*;
import com.template.usermanagement.wilayah.bulkupload.dto.BulkUploadResponse;
import com.template.usermanagement.workflow.PendingAction;
import com.template.usermanagement.workflow.PendingActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadService {

    private final BulkUploadRepository bulkUploadRepository;
    private final BulkUploadRowRepository bulkUploadRowRepository;
    private final UserRepository userRepository;
    private final PendingActionService pendingActionService;
    private final AuditTrailService auditTrailService;
    private final ProvinceRepository provinceRepository;
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final SubDistrictRepository subDistrictRepository;

    private static final List<String> EXPECTED_HEADERS = List.of(
            "ProvinceID", "ProvinceName", "StateID", "StateName",
            "DistrictID", "DistrictName", "SubDistrictID", "SubDistrictName", "ZipCode"
    );

    @Transactional
    public BulkUpload parseCsvAndStage(MultipartFile file, Long uploadedById) {
        User uploader = userRepository.findById(uploadedById).orElseThrow();

        BulkUpload bulkUpload = BulkUpload.builder()
                .entityType("BULK_UPLOAD_WILAYAH")
                .fileName(file.getOriginalFilename())
                .uploadedBy(uploader)
                .build();
        bulkUpload = bulkUploadRepository.save(bulkUpload);

        int rowCount = 0, validCount = 0, errorCount = 0;
        List<BulkUploadRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessException("CSV file is empty", ErrorCode.BULK_UPLOAD_PARSE_ERROR);
            }
            String[] headers = parseCsvLine(headerLine);
            validateHeaders(headers);

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;
                rowCount++;

                String errorMsg = null;
                Map<String, Object> data = new LinkedHashMap<>();
                boolean valid = true;

                try {
                    String[] values = parseCsvLine(line);
                    if (values.length < 9) {
                        valid = false;
                        errorMsg = "Insufficient columns: expected 9, got " + values.length;
                    } else {
                        data.put("provinceId", values[0].trim());
                        data.put("provinceName", values[1].trim());
                        data.put("stateId", values[2].trim());
                        data.put("stateName", values[3].trim());
                        data.put("districtId", values[4].trim());
                        data.put("districtName", values[5].trim());
                        data.put("subDistrictId", values[6].trim());
                        data.put("subDistrictName", values[7].trim());
                        data.put("zipCode", values[8].trim());

                        // Basic validation
                        if (data.get("provinceId").toString().isEmpty() || data.get("subDistrictId").toString().isEmpty()) {
                            valid = false;
                            errorMsg = "ProvinceID or SubDistrictID is empty";
                        }
                    }
                } catch (Exception e) {
                    valid = false;
                    errorMsg = "Parse error: " + e.getMessage();
                }

                if (valid) validCount++; else errorCount++;

                BulkUpload finalBulkUpload = bulkUpload;
                rows.add(BulkUploadRow.builder()
                        .bulkUpload(finalBulkUpload)
                        .rowNumber(lineNum)
                        .data(data.isEmpty() ? Map.of("raw", line) : data)
                        .isValid(valid)
                        .errorMessage(errorMsg)
                        .build());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Failed to parse CSV: " + e.getMessage(), ErrorCode.BULK_UPLOAD_PARSE_ERROR);
        }

        // Save rows in batches
        for (int i = 0; i < rows.size(); i += 500) {
            bulkUploadRowRepository.saveAll(rows.subList(i, Math.min(i + 500, rows.size())));
        }

        bulkUpload.setRowCount(rowCount);
        bulkUpload.setValidCount(validCount);
        bulkUpload.setErrorCount(errorCount);
        bulkUpload = bulkUploadRepository.save(bulkUpload);

        log.info("[BulkUpload] Staged {} rows ({} valid, {} error) from {}", rowCount, validCount, errorCount, file.getOriginalFilename());
        return bulkUpload;
    }

    @Transactional
    public PendingAction submitForApproval(Long bulkUploadId, Long makerId) {
        BulkUpload bu = findById(bulkUploadId);
        if (!"STAGED".equals(bu.getStatus())) {
            throw new BusinessException("BulkUpload is not in STAGED status", ErrorCode.BULK_UPLOAD_INVALID_STATUS);
        }

        Map<String, Object> payload = Map.of(
                "bulkUploadId", bu.getId(),
                "rowCount", bu.getRowCount(),
                "validCount", bu.getValidCount(),
                "fileName", bu.getFileName()
        );

        return pendingActionService.createPendingAction("BULK_UPLOAD_WILAYAH", bu.getId(), "CREATE", payload, null, makerId);
    }

    @Transactional
    public int applyBulkUpload(Long bulkUploadId, String performedBy) {
        BulkUpload bu = findById(bulkUploadId);

        List<BulkUploadRow> validRows = bulkUploadRowRepository.findByBulkUploadIdAndIsValidTrue(bulkUploadId);

        Map<String, Province> provinces = new LinkedHashMap<>();
        Map<String, State> states = new LinkedHashMap<>();
        Map<String, District> districts = new LinkedHashMap<>();
        List<SubDistrict> subDistricts = new ArrayList<>();

        for (BulkUploadRow row : validRows) {
            Map<String, Object> d = row.getData();
            String provinceId = str(d, "provinceId");
            String provinceName = str(d, "provinceName");
            String stateId = str(d, "stateId");
            String stateName = str(d, "stateName");
            String districtId = str(d, "districtId");
            String districtName = str(d, "districtName");
            String subDistrictId = str(d, "subDistrictId");
            String subDistrictName = str(d, "subDistrictName");
            String zipCode = str(d, "zipCode");

            provinces.computeIfAbsent(provinceId, id -> Province.builder().provinceId(id).name(provinceName).build());
            provinces.get(provinceId).setName(provinceName);

            if (!stateId.isEmpty()) {
                Province prov = provinces.get(provinceId);
                states.computeIfAbsent(stateId, id -> State.builder().stateId(id).name(stateName).province(prov).build());
                states.get(stateId).setName(stateName);
            }

            if (!districtId.isEmpty() && states.containsKey(stateId)) {
                State st = states.get(stateId);
                districts.computeIfAbsent(districtId, id -> District.builder().districtId(id).name(districtName).state(st).build());
                districts.get(districtId).setName(districtName);
            }

            if (!subDistrictId.isEmpty() && districts.containsKey(districtId)) {
                District dist = districts.get(districtId);
                subDistricts.add(SubDistrict.builder()
                        .subDistrictId(subDistrictId)
                        .name(subDistrictName)
                        .district(dist)
                        .zipCode(zipCode.isEmpty() ? null : zipCode)
                        .build());
            }
        }

        // Batch upsert in hierarchy order
        List<Province> provList = new ArrayList<>(provinces.values());
        for (int i = 0; i < provList.size(); i += 500) {
            provinceRepository.saveAll(provList.subList(i, Math.min(i + 500, provList.size())));
        }
        List<State> stateList = new ArrayList<>(states.values());
        for (int i = 0; i < stateList.size(); i += 500) {
            stateRepository.saveAll(stateList.subList(i, Math.min(i + 500, stateList.size())));
        }
        List<District> distList = new ArrayList<>(districts.values());
        for (int i = 0; i < distList.size(); i += 500) {
            districtRepository.saveAll(distList.subList(i, Math.min(i + 500, distList.size())));
        }
        for (int i = 0; i < subDistricts.size(); i += 500) {
            subDistrictRepository.saveAll(subDistricts.subList(i, Math.min(i + 500, subDistricts.size())));
        }

        bu.setStatus("APPLIED");
        bu.setSummary(Map.of(
                "provinces", provList.size(),
                "states", stateList.size(),
                "districts", distList.size(),
                "subDistricts", subDistricts.size()
        ));
        bulkUploadRepository.save(bu);

        auditTrailService.recordAudit("BULK_UPLOAD_WILAYAH", bulkUploadId, "BULK_APPLY_WILAYAH",
                null, Map.of("rowsApplied", validRows.size(), "fileName", bu.getFileName()),
                performedBy, null);

        log.info("[BulkUpload] Applied {} valid rows from upload {}", validRows.size(), bulkUploadId);
        return validRows.size();
    }

    @Transactional(readOnly = true)
    public BulkUploadResponse getById(Long id) {
        return BulkUploadResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<BulkUploadRow> getRows(Long id) {
        findById(id); // existence check
        return bulkUploadRowRepository.findByBulkUploadId(id);
    }

    private BulkUpload findById(Long id) {
        return bulkUploadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BulkUpload not found", ErrorCode.BULK_UPLOAD_NOT_FOUND));
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private void validateHeaders(String[] headers) {
        if (headers.length < EXPECTED_HEADERS.size()) {
            throw new BusinessException("Invalid CSV headers: expected " + EXPECTED_HEADERS, ErrorCode.BULK_UPLOAD_PARSE_ERROR);
        }
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            if (!EXPECTED_HEADERS.get(i).equalsIgnoreCase(headers[i].trim())) {
                throw new BusinessException(
                        "Invalid header at column " + (i + 1) + ": expected '" + EXPECTED_HEADERS.get(i) + "' but got '" + headers[i] + "'",
                        ErrorCode.BULK_UPLOAD_PARSE_ERROR);
            }
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}
