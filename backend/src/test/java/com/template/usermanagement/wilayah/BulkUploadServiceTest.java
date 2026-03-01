package com.template.usermanagement.wilayah;

import com.template.usermanagement.TestFixtures;
import com.template.usermanagement.audit.AuditTrailService;
import com.template.usermanagement.common.BusinessException;
import com.template.usermanagement.common.ErrorCode;
import com.template.usermanagement.user.User;
import com.template.usermanagement.user.UserRepository;
import com.template.usermanagement.wilayah.bulkupload.*;
import com.template.usermanagement.wilayah.bulkupload.dto.BulkUploadResponse;
import com.template.usermanagement.workflow.PendingAction;
import com.template.usermanagement.workflow.PendingActionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BulkUploadService")
class BulkUploadServiceTest {

    @Mock private BulkUploadRepository bulkUploadRepository;
    @Mock private BulkUploadRowRepository bulkUploadRowRepository;
    @Mock private UserRepository userRepository;
    @Mock private PendingActionService pendingActionService;
    @Mock private AuditTrailService auditTrailService;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private StateRepository stateRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private SubDistrictRepository subDistrictRepository;

    @InjectMocks private BulkUploadService bulkUploadService;

    private User uploader;

    @BeforeEach
    void setUp() {
        uploader = TestFixtures.createUser(1L, "admin", "ADMIN");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static final String VALID_CSV_HEADER =
            "\"ProvinceID\",\"ProvinceName\",\"StateID\",\"StateName\",\"DistrictID\",\"DistrictName\",\"SubDistrictID\",\"SubDistrictName\",\"ZipCode\"";

    private static String validCsvWith(String... dataRows) {
        String rows = String.join("\n", dataRows);
        return VALID_CSV_HEADER + "\n" + rows;
    }

    private static MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "test.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private BulkUpload stagedUpload(Long id, int rowCount, int validCount, int errorCount) {
        BulkUpload bu = BulkUpload.builder()
                .entityType("BULK_UPLOAD_WILAYAH")
                .fileName("test.csv")
                .rowCount(rowCount)
                .validCount(validCount)
                .errorCount(errorCount)
                .status("STAGED")
                .uploadedBy(uploader)
                .build();
        bu.setId(id);
        bu.setCreatedAt(LocalDateTime.now());
        bu.setUpdatedAt(LocalDateTime.now());
        return bu;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // parseCsvAndStage
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parseCsvAndStage")
    class ParseCsvAndStage {

        @BeforeEach
        void mockSave() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(uploader));
            // Return the entity passed to save (first call = BulkUpload, subsequent = rows)
            when(bulkUploadRepository.save(any(BulkUpload.class)))
                    .thenAnswer(inv -> {
                        BulkUpload bu = inv.getArgument(0);
                        bu.setId(1L);
                        return bu;
                    });
        }

        @Test
        @DisplayName("valid CSV → stages rows with correct counts")
        void validCsv_stagesRows() throws Exception {
            String csv = validCsvWith(
                    "\"1100\",\"Aceh\",\"1101\",\"Kab. Simeulue\",\"1101010\",\"Teupah Selatan\",\"1101010001\",\"Desa Latiung\",\"23891\"",
                    "\"1100\",\"Aceh\",\"1101\",\"Kab. Simeulue\",\"1101010\",\"Teupah Selatan\",\"1101010002\",\"Desa Bajau\",\"23891\""
            );

            BulkUpload result = bulkUploadService.parseCsvAndStage(csvFile(csv), 1L);

            assertThat(result.getRowCount()).isEqualTo(2);
            assertThat(result.getValidCount()).isEqualTo(2);
            assertThat(result.getErrorCount()).isEqualTo(0);
            assertThat(result.getStatus()).isEqualTo("STAGED");
            verify(bulkUploadRowRepository, atLeastOnce()).saveAll(anyList());
        }

        @Test
        @DisplayName("row with missing columns → counted as error")
        void missingColumns_countedAsError() throws Exception {
            String csv = validCsvWith(
                    "\"1100\",\"Aceh\",\"1101\",\"Kab. Simeulue\"" // only 4 columns
            );

            BulkUpload result = bulkUploadService.parseCsvAndStage(csvFile(csv), 1L);

            assertThat(result.getRowCount()).isEqualTo(1);
            assertThat(result.getValidCount()).isEqualTo(0);
            assertThat(result.getErrorCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("row with empty ProvinceID → counted as error")
        void emptyProvinceId_countedAsError() throws Exception {
            String csv = validCsvWith(
                    "\"\",\"Aceh\",\"1101\",\"Kab. Simeulue\",\"1101010\",\"Teupah Selatan\",\"1101010001\",\"Desa Latiung\",\"23891\""
            );

            BulkUpload result = bulkUploadService.parseCsvAndStage(csvFile(csv), 1L);

            assertThat(result.getErrorCount()).isEqualTo(1);
            assertThat(result.getValidCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("invalid headers → throws BusinessException with BULK_UPLOAD_PARSE_ERROR")
        void invalidHeaders_throwsException() {
            String csv = "Wrong,Headers,Here\n1100,Aceh,1101,Kab";

            assertThatThrownBy(() -> bulkUploadService.parseCsvAndStage(csvFile(csv), 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_UPLOAD_PARSE_ERROR);
        }

        @Test
        @DisplayName("empty file → throws BusinessException with BULK_UPLOAD_PARSE_ERROR")
        void emptyFile_throwsException() {
            assertThatThrownBy(() -> bulkUploadService.parseCsvAndStage(csvFile(""), 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_UPLOAD_PARSE_ERROR);
        }

        @Test
        @DisplayName("mixed valid/invalid rows → counts correctly")
        void mixedRows_countsCorrectly() throws Exception {
            String csv = validCsvWith(
                    "\"1100\",\"Aceh\",\"1101\",\"Kab. Simeulue\",\"1101010\",\"Teupah Selatan\",\"1101010001\",\"Desa Latiung\",\"23891\"",
                    "\"1100\",\"Aceh\",\"1101\"",  // too few columns → error
                    "\"1100\",\"Aceh\",\"1101\",\"Kab. Simeulue\",\"1101010\",\"Teupah Selatan\",\"1101010003\",\"Desa Valid\",\"23891\""
            );

            BulkUpload result = bulkUploadService.parseCsvAndStage(csvFile(csv), 1L);

            assertThat(result.getRowCount()).isEqualTo(3);
            assertThat(result.getValidCount()).isEqualTo(2);
            assertThat(result.getErrorCount()).isEqualTo(1);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // submitForApproval
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("submitForApproval")
    class SubmitForApproval {

        @Test
        @DisplayName("STAGED status → creates pending action with correct payload")
        void staged_createsPendingAction() {
            BulkUpload bu = stagedUpload(5L, 100, 98, 2);
            when(bulkUploadRepository.findById(5L)).thenReturn(Optional.of(bu));

            PendingAction pa = new PendingAction();
            pa.setId(99L);
            when(pendingActionService.createPendingAction(
                    eq("BULK_UPLOAD_WILAYAH"), eq(5L), eq("CREATE"), anyMap(), isNull(), eq(1L)))
                    .thenReturn(pa);

            PendingAction result = bulkUploadService.submitForApproval(5L, 1L);

            assertThat(result.getId()).isEqualTo(99L);

            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(pendingActionService).createPendingAction(
                    eq("BULK_UPLOAD_WILAYAH"), eq(5L), eq("CREATE"),
                    payloadCaptor.capture(), isNull(), eq(1L));

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("bulkUploadId")).isEqualTo(5L);
            assertThat(payload.get("rowCount")).isEqualTo(100);
            assertThat(payload.get("validCount")).isEqualTo(98);
        }

        @Test
        @DisplayName("non-STAGED status → throws BusinessException with BULK_UPLOAD_INVALID_STATUS")
        void nonStaged_throwsException() {
            BulkUpload bu = stagedUpload(5L, 100, 100, 0);
            bu.setStatus("APPLIED"); // Already applied
            when(bulkUploadRepository.findById(5L)).thenReturn(Optional.of(bu));

            assertThatThrownBy(() -> bulkUploadService.submitForApproval(5L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BULK_UPLOAD_INVALID_STATUS);
        }

        @Test
        @DisplayName("not found → throws ResourceNotFoundException")
        void notFound_throwsException() {
            when(bulkUploadRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> bulkUploadService.submitForApproval(99L, 1L))
                    .isInstanceOf(com.template.usermanagement.common.ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // applyBulkUpload
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("applyBulkUpload")
    class ApplyBulkUpload {

        @Test
        @DisplayName("applies valid rows and sets status to APPLIED")
        void appliesRowsAndSetsApplied() {
            BulkUpload bu = stagedUpload(5L, 2, 2, 0);
            when(bulkUploadRepository.findById(5L)).thenReturn(Optional.of(bu));

            BulkUploadRow row1 = BulkUploadRow.builder()
                    .id(1L).bulkUpload(bu).rowNumber(2).isValid(true)
                    .data(Map.of(
                            "provinceId", "1100", "provinceName", "Aceh",
                            "stateId", "1101", "stateName", "Kab. Simeulue",
                            "districtId", "1101010", "districtName", "Teupah Selatan",
                            "subDistrictId", "1101010001", "subDistrictName", "Desa Latiung",
                            "zipCode", "23891"
                    )).build();
            BulkUploadRow row2 = BulkUploadRow.builder()
                    .id(2L).bulkUpload(bu).rowNumber(3).isValid(true)
                    .data(Map.of(
                            "provinceId", "1100", "provinceName", "Aceh",
                            "stateId", "1101", "stateName", "Kab. Simeulue",
                            "districtId", "1101010", "districtName", "Teupah Selatan",
                            "subDistrictId", "1101010002", "subDistrictName", "Desa Bajau",
                            "zipCode", "23891"
                    )).build();

            when(bulkUploadRowRepository.findByBulkUploadIdAndIsValidTrue(5L)).thenReturn(List.of(row1, row2));
            when(provinceRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(stateRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(districtRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(subDistrictRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(bulkUploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int applied = bulkUploadService.applyBulkUpload(5L, "checker");

            assertThat(applied).isEqualTo(2);
            assertThat(bu.getStatus()).isEqualTo("APPLIED");
            assertThat(bu.getSummary()).isNotNull();
            assertThat(bu.getSummary().get("subDistricts")).isEqualTo(2);

            verify(provinceRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1)); // 1 unique province
            verify(stateRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));    // 1 unique state
            verify(districtRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1)); // 1 unique district
            verify(subDistrictRepository, atLeastOnce()).saveAll(anyList());

            verify(auditTrailService).recordAudit(
                    eq("BULK_UPLOAD_WILAYAH"), eq(5L), eq("BULK_APPLY_WILAYAH"),
                    isNull(), argThat(m -> Integer.valueOf(2).equals(m.get("rowsApplied"))),
                    eq("checker"), isNull());
        }

        @Test
        @DisplayName("no valid rows → applies nothing, still sets APPLIED")
        void noValidRows_setsApplied() {
            BulkUpload bu = stagedUpload(5L, 1, 0, 1);
            when(bulkUploadRepository.findById(5L)).thenReturn(Optional.of(bu));
            when(bulkUploadRowRepository.findByBulkUploadIdAndIsValidTrue(5L)).thenReturn(Collections.emptyList());
            when(bulkUploadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int applied = bulkUploadService.applyBulkUpload(5L, "checker");

            assertThat(applied).isEqualTo(0);
            assertThat(bu.getStatus()).isEqualTo("APPLIED");
            verify(provinceRepository, never()).saveAll(anyList());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getById
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("found → returns response")
        void found_returnsResponse() {
            BulkUpload bu = stagedUpload(10L, 50, 49, 1);
            when(bulkUploadRepository.findById(10L)).thenReturn(Optional.of(bu));

            BulkUploadResponse resp = bulkUploadService.getById(10L);

            assertThat(resp.getId()).isEqualTo(10L);
            assertThat(resp.getRowCount()).isEqualTo(50);
            assertThat(resp.getStatus()).isEqualTo("STAGED");
        }

        @Test
        @DisplayName("not found → throws ResourceNotFoundException")
        void notFound_throwsException() {
            when(bulkUploadRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> bulkUploadService.getById(99L))
                    .isInstanceOf(com.template.usermanagement.common.ResourceNotFoundException.class);
        }
    }
}
