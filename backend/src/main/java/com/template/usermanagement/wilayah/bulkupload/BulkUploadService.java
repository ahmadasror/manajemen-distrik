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

        // Collect unique entities from CSV rows
        Map<String, Province> csvProvinces = new LinkedHashMap<>();
        Map<String, State> csvStates = new LinkedHashMap<>();
        Map<String, District> csvDistricts = new LinkedHashMap<>();
        Map<String, SubDistrict> csvSubDistricts = new LinkedHashMap<>();

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

            csvProvinces.computeIfAbsent(provinceId, id -> Province.builder().provinceId(id).name(provinceName).build());
            csvProvinces.get(provinceId).setName(provinceName);

            if (!stateId.isEmpty()) {
                Province prov = csvProvinces.get(provinceId);
                csvStates.computeIfAbsent(stateId, id -> State.builder().stateId(id).name(stateName).province(prov).build());
                csvStates.get(stateId).setName(stateName);
            }

            if (!districtId.isEmpty() && csvStates.containsKey(stateId)) {
                State st = csvStates.get(stateId);
                csvDistricts.computeIfAbsent(districtId, id -> District.builder().districtId(id).name(districtName).state(st).build());
                csvDistricts.get(districtId).setName(districtName);
            }

            if (!subDistrictId.isEmpty() && csvDistricts.containsKey(districtId)) {
                District dist = csvDistricts.get(districtId);
                csvSubDistricts.put(subDistrictId, SubDistrict.builder()
                        .subDistrictId(subDistrictId)
                        .name(subDistrictName)
                        .district(dist)
                        .zipCode(zipCode.isEmpty() ? null : zipCode)
                        .build());
            }
        }

        int insertedCount = 0, updatedCount = 0, skippedCount = 0;

        // --- Provinces: diff ---
        Map<String, Province> existingProvs = new LinkedHashMap<>();
        provinceRepository.findAllById(csvProvinces.keySet()).forEach(p -> existingProvs.put(p.getProvinceId(), p));
        List<Province> provsToSave = new ArrayList<>();
        for (Map.Entry<String, Province> e : csvProvinces.entrySet()) {
            Province existing = existingProvs.get(e.getKey());
            Province csv = e.getValue();
            if (existing == null) {
                provsToSave.add(csv);
                insertedCount++;
            } else if (!Objects.equals(existing.getName(), csv.getName())) {
                existing.setName(csv.getName());
                provsToSave.add(existing);
                insertedCount++;
            } else {
                skippedCount++;
            }
        }
        batchSave(provsToSave, provinceRepository);

        // --- States: diff ---
        Map<String, State> existingStates = new LinkedHashMap<>();
        stateRepository.findAllById(csvStates.keySet()).forEach(s -> existingStates.put(s.getStateId(), s));
        List<State> statesToSave = new ArrayList<>();
        for (Map.Entry<String, State> e : csvStates.entrySet()) {
            State existing = existingStates.get(e.getKey());
            State csv = e.getValue();
            if (existing == null) {
                statesToSave.add(csv);
                insertedCount++;
            } else if (!Objects.equals(existing.getName(), csv.getName())) {
                existing.setName(csv.getName());
                existing.setProvince(csv.getProvince());
                statesToSave.add(existing);
                insertedCount++;
            } else {
                skippedCount++;
            }
        }
        batchSave(statesToSave, stateRepository);

        // --- Districts: diff ---
        Map<String, District> existingDists = new LinkedHashMap<>();
        districtRepository.findAllById(csvDistricts.keySet()).forEach(d -> existingDists.put(d.getDistrictId(), d));
        List<District> distsToSave = new ArrayList<>();
        for (Map.Entry<String, District> e : csvDistricts.entrySet()) {
            District existing = existingDists.get(e.getKey());
            District csv = e.getValue();
            if (existing == null) {
                distsToSave.add(csv);
                insertedCount++;
            } else if (!Objects.equals(existing.getName(), csv.getName())) {
                existing.setName(csv.getName());
                existing.setState(csv.getState());
                distsToSave.add(existing);
                insertedCount++;
            } else {
                skippedCount++;
            }
        }
        batchSave(distsToSave, districtRepository);

        // --- SubDistricts: diff (with zipCode) ---
        Map<String, SubDistrict> existingSubs = new LinkedHashMap<>();
        subDistrictRepository.findAllById(csvSubDistricts.keySet()).forEach(s -> existingSubs.put(s.getSubDistrictId(), s));
        List<SubDistrict> subsToSave = new ArrayList<>();
        for (Map.Entry<String, SubDistrict> e : csvSubDistricts.entrySet()) {
            SubDistrict existing = existingSubs.get(e.getKey());
            SubDistrict csv = e.getValue();
            if (existing == null) {
                subsToSave.add(csv);
                insertedCount++;
            } else {
                boolean nameDiff = !Objects.equals(existing.getName(), csv.getName());
                boolean zipDiff = !Objects.equals(existing.getZipCode(), csv.getZipCode());
                if (nameDiff) {
                    existing.setName(csv.getName());
                    existing.setZipCode(csv.getZipCode());
                    existing.setDistrict(csv.getDistrict());
                    subsToSave.add(existing);
                    insertedCount++;
                } else if (zipDiff) {
                    existing.setZipCode(csv.getZipCode());
                    subsToSave.add(existing);
                    updatedCount++;
                } else {
                    skippedCount++;
                }
            }
        }
        batchSave(subsToSave, subDistrictRepository);

        bu.setStatus("APPLIED");
        bu.setSummary(Map.of(
                "provinces", provsToSave.size(),
                "states", statesToSave.size(),
                "districts", distsToSave.size(),
                "subDistricts", subsToSave.size(),
                "inserted", insertedCount,
                "updated", updatedCount,
                "skipped", skippedCount
        ));
        bulkUploadRepository.save(bu);

        auditTrailService.recordAudit("BULK_UPLOAD_WILAYAH", bulkUploadId, "BULK_APPLY_WILAYAH",
                null, Map.of("rowsApplied", validRows.size(), "fileName", bu.getFileName(),
                        "inserted", insertedCount, "updated", updatedCount, "skipped", skippedCount),
                performedBy, null);

        log.info("[BulkUpload] Applied {} valid rows from upload {} (inserted={}, updated={}, skipped={})",
                validRows.size(), bulkUploadId, insertedCount, updatedCount, skippedCount);
        return validRows.size();
    }

    private <T> void batchSave(List<T> entities, org.springframework.data.jpa.repository.JpaRepository<T, ?> repo) {
        for (int i = 0; i < entities.size(); i += 500) {
            repo.saveAll(entities.subList(i, Math.min(i + 500, entities.size())));
        }
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
