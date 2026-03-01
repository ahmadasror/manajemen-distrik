package com.template.usermanagement.wilayah;

import com.template.usermanagement.audit.AuditTrailService;
import com.template.usermanagement.common.ResourceNotFoundException;
import com.template.usermanagement.wilayah.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WilayahService")
class WilayahServiceTest {

    @Mock private ProvinceRepository provinceRepository;
    @Mock private StateRepository stateRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private SubDistrictRepository subDistrictRepository;
    @Mock private AuditTrailService auditTrailService;

    @InjectMocks private WilayahService wilayahService;

    // ── fixtures ──────────────────────────────────────────────────────────────

    private static Province province(String id, String name) {
        Province p = Province.builder().provinceId(id).name(name).build();
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    private static State state(String id, String name, Province province) {
        State s = State.builder().stateId(id).name(name).province(province).build();
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private static District district(String id, String name, State state) {
        District d = District.builder().districtId(id).name(name).state(state).build();
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return d;
    }

    private static SubDistrict subDistrict(String id, String name, District district, String zip) {
        SubDistrict sd = SubDistrict.builder()
                .subDistrictId(id).name(name).district(district).zipCode(zip).build();
        sd.setCreatedAt(LocalDateTime.now());
        sd.setUpdatedAt(LocalDateTime.now());
        return sd;
    }

    private static WilayahRequest request(String id, String name, String parentId) {
        WilayahRequest r = new WilayahRequest();
        r.setId(id);
        r.setName(name);
        r.setParentId(parentId);
        return r;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Province
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("searchProvinces")
    class SearchProvinces {

        @Test
        @DisplayName("no filter → findAll")
        void noFilter_callsFindAll() {
            Province p = province("1100", "Aceh");
            Page<Province> page = new PageImpl<>(List.of(p));
            when(provinceRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<ProvinceResponse> result = wilayahService.searchProvinces(null, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getProvinceId()).isEqualTo("1100");
            verify(provinceRepository).findAll(any(Pageable.class));
            verify(provinceRepository, never()).findByNameContainingIgnoreCase(anyString(), any());
        }

        @Test
        @DisplayName("blank search → findAll")
        void blankSearch_callsFindAll() {
            when(provinceRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
            wilayahService.searchProvinces("   ", PageRequest.of(0, 10));
            verify(provinceRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("with search → findByNameContainingIgnoreCase")
        void withSearch_callsContaining() {
            Province p = province("1100", "Aceh");
            Page<Province> page = new PageImpl<>(List.of(p));
            when(provinceRepository.findByNameContainingIgnoreCase(eq("aceh"), any(Pageable.class))).thenReturn(page);

            Page<ProvinceResponse> result = wilayahService.searchProvinces("aceh", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            verify(provinceRepository).findByNameContainingIgnoreCase(eq("aceh"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getProvince")
    class GetProvince {

        @Test
        @DisplayName("found → returns response")
        void found_returnsResponse() {
            Province p = province("1100", "Aceh");
            when(provinceRepository.findById("1100")).thenReturn(Optional.of(p));

            ProvinceResponse resp = wilayahService.getProvince("1100");

            assertThat(resp.getProvinceId()).isEqualTo("1100");
            assertThat(resp.getName()).isEqualTo("Aceh");
        }

        @Test
        @DisplayName("not found → throws ResourceNotFoundException")
        void notFound_throwsException() {
            when(provinceRepository.findById("9999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> wilayahService.getProvince("9999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Province not found");
        }
    }

    @Nested
    @DisplayName("createProvince")
    class CreateProvince {

        @Test
        @DisplayName("saves province and records audit")
        void savesAndAudits() {
            WilayahRequest req = request("1100", "Aceh", null);
            Province saved = province("1100", "Aceh");
            when(provinceRepository.save(any(Province.class))).thenReturn(saved);

            ProvinceResponse resp = wilayahService.createProvince(req, "admin");

            assertThat(resp.getProvinceId()).isEqualTo("1100");
            assertThat(resp.getName()).isEqualTo("Aceh");

            ArgumentCaptor<Province> captor = ArgumentCaptor.forClass(Province.class);
            verify(provinceRepository).save(captor.capture());
            assertThat(captor.getValue().getProvinceId()).isEqualTo("1100");

            verify(auditTrailService).recordAudit(
                    eq("PROVINCE"), isNull(), eq("CREATE_PROVINCE"),
                    isNull(), anyMap(), eq("admin"), isNull());
        }
    }

    @Nested
    @DisplayName("updateProvince")
    class UpdateProvince {

        @Test
        @DisplayName("updates name and records audit")
        void updatesNameAndAudits() {
            Province existing = province("1100", "Aceh Lama");
            when(provinceRepository.findById("1100")).thenReturn(Optional.of(existing));
            when(provinceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WilayahRequest req = request("1100", "Aceh Baru", null);
            ProvinceResponse resp = wilayahService.updateProvince("1100", req, "admin");

            assertThat(resp.getName()).isEqualTo("Aceh Baru");
            verify(auditTrailService).recordAudit(
                    eq("PROVINCE"), isNull(), eq("UPDATE_PROVINCE"),
                    argThat(m -> "Aceh Lama".equals(m.get("name"))),
                    argThat(m -> "Aceh Baru".equals(m.get("name"))),
                    eq("admin"), isNull());
        }

        @Test
        @DisplayName("not found → throws exception")
        void notFound_throwsException() {
            when(provinceRepository.findById("9999")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> wilayahService.updateProvince("9999", request("9999", "X", null), "admin"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteProvince")
    class DeleteProvince {

        @Test
        @DisplayName("deletes and records audit")
        void deletesAndAudits() {
            Province p = province("1100", "Aceh");
            when(provinceRepository.findById("1100")).thenReturn(Optional.of(p));

            wilayahService.deleteProvince("1100", "admin");

            verify(provinceRepository).delete(p);
            verify(auditTrailService).recordAudit(
                    eq("PROVINCE"), isNull(), eq("DELETE_PROVINCE"),
                    anyMap(), isNull(), eq("admin"), isNull());
        }

        @Test
        @DisplayName("not found → throws exception")
        void notFound_throwsException() {
            when(provinceRepository.findById("9999")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> wilayahService.deleteProvince("9999", "admin"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("searchStates")
    class SearchStates {

        @Test
        @DisplayName("no filter → findAll")
        void noFilter_findAll() {
            when(stateRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
            wilayahService.searchStates(null, null, PageRequest.of(0, 10));
            verify(stateRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("provinceId only → findByProvinceProvinceId")
        void provinceIdOnly_findByProvince() {
            when(stateRepository.findByProvinceProvinceId(eq("1100"), any())).thenReturn(Page.empty());
            wilayahService.searchStates("1100", null, PageRequest.of(0, 10));
            verify(stateRepository).findByProvinceProvinceId(eq("1100"), any());
        }

        @Test
        @DisplayName("provinceId + search → findByProvinceProvinceIdAndNameContaining")
        void provinceIdAndSearch() {
            when(stateRepository.findByProvinceProvinceIdAndNameContainingIgnoreCase(eq("1100"), eq("kota"), any())).thenReturn(Page.empty());
            wilayahService.searchStates("1100", "kota", PageRequest.of(0, 10));
            verify(stateRepository).findByProvinceProvinceIdAndNameContainingIgnoreCase(eq("1100"), eq("kota"), any());
        }

        @Test
        @DisplayName("search only → findByNameContaining")
        void searchOnly() {
            when(stateRepository.findByNameContainingIgnoreCase(eq("jakarta"), any())).thenReturn(Page.empty());
            wilayahService.searchStates(null, "jakarta", PageRequest.of(0, 10));
            verify(stateRepository).findByNameContainingIgnoreCase(eq("jakarta"), any());
        }
    }

    @Nested
    @DisplayName("createState")
    class CreateState {

        @Test
        @DisplayName("saves state with province FK and records audit")
        void savesAndAudits() {
            Province prov = province("1100", "Aceh");
            when(provinceRepository.findById("1100")).thenReturn(Optional.of(prov));
            State saved = state("1101", "Kab. Simeulue", prov);
            when(stateRepository.save(any())).thenReturn(saved);

            WilayahRequest req = request("1101", "Kab. Simeulue", "1100");
            StateResponse resp = wilayahService.createState(req, "admin");

            assertThat(resp.getStateId()).isEqualTo("1101");
            assertThat(resp.getProvinceId()).isEqualTo("1100");
            verify(auditTrailService).recordAudit(eq("STATE"), isNull(), eq("CREATE_STATE"), isNull(), anyMap(), eq("admin"), isNull());
        }

        @Test
        @DisplayName("parent province not found → throws exception")
        void parentNotFound_throwsException() {
            when(provinceRepository.findById("9999")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> wilayahService.createState(request("X", "X", "9999"), "admin"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteState")
    class DeleteState {

        @Test
        @DisplayName("deletes and records audit")
        void deletesAndAudits() {
            Province prov = province("1100", "Aceh");
            State s = state("1101", "Kab. Simeulue", prov);
            when(stateRepository.findById("1101")).thenReturn(Optional.of(s));

            wilayahService.deleteState("1101", "admin");

            verify(stateRepository).delete(s);
            verify(auditTrailService).recordAudit(eq("STATE"), isNull(), eq("DELETE_STATE"), anyMap(), isNull(), eq("admin"), isNull());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // District
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("searchDistricts")
    class SearchDistricts {

        @Test
        @DisplayName("stateId + search → findByStateStateIdAndNameContaining")
        void stateAndSearch() {
            when(districtRepository.findByStateStateIdAndNameContainingIgnoreCase(eq("1101"), eq("sungai"), any())).thenReturn(Page.empty());
            wilayahService.searchDistricts("1101", "sungai", PageRequest.of(0, 10));
            verify(districtRepository).findByStateStateIdAndNameContainingIgnoreCase(eq("1101"), eq("sungai"), any());
        }

        @Test
        @DisplayName("stateId only → findByStateStateId")
        void stateIdOnly() {
            when(districtRepository.findByStateStateId(eq("1101"), any())).thenReturn(Page.empty());
            wilayahService.searchDistricts("1101", null, PageRequest.of(0, 10));
            verify(districtRepository).findByStateStateId(eq("1101"), any());
        }
    }

    @Nested
    @DisplayName("createDistrict")
    class CreateDistrict {

        @Test
        @DisplayName("saves district with state FK and records audit")
        void savesAndAudits() {
            Province prov = province("1100", "Aceh");
            State st = state("1101", "Kab. Simeulue", prov);
            when(stateRepository.findById("1101")).thenReturn(Optional.of(st));
            District saved = district("1101010", "Teupah Selatan", st);
            when(districtRepository.save(any())).thenReturn(saved);

            WilayahRequest req = request("1101010", "Teupah Selatan", "1101");
            DistrictResponse resp = wilayahService.createDistrict(req, "admin");

            assertThat(resp.getDistrictId()).isEqualTo("1101010");
            assertThat(resp.getStateId()).isEqualTo("1101");
            verify(auditTrailService).recordAudit(eq("DISTRICT"), isNull(), eq("CREATE_DISTRICT"), isNull(), anyMap(), eq("admin"), isNull());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SubDistrict
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("searchSubDistricts")
    class SearchSubDistricts {

        @Test
        @DisplayName("zipCode filter takes precedence → findByZipCode")
        void zipCode_takesprecedence() {
            when(subDistrictRepository.findByZipCode(eq("23891"), any())).thenReturn(Page.empty());
            wilayahService.searchSubDistricts("1101010", "23891", "Desa", PageRequest.of(0, 50));
            verify(subDistrictRepository).findByZipCode(eq("23891"), any());
            verify(subDistrictRepository, never()).findByDistrictDistrictIdAndNameContainingIgnoreCase(any(), any(), any());
        }

        @Test
        @DisplayName("districtId + search → findByDistrictDistrictIdAndNameContaining")
        void districtAndSearch() {
            when(subDistrictRepository.findByDistrictDistrictIdAndNameContainingIgnoreCase(eq("1101010"), eq("Desa"), any())).thenReturn(Page.empty());
            wilayahService.searchSubDistricts("1101010", null, "Desa", PageRequest.of(0, 50));
            verify(subDistrictRepository).findByDistrictDistrictIdAndNameContainingIgnoreCase(eq("1101010"), eq("Desa"), any());
        }

        @Test
        @DisplayName("name search only → findByNameContaining")
        void nameSearchOnly() {
            when(subDistrictRepository.findByNameContainingIgnoreCase(eq("Latiung"), any())).thenReturn(Page.empty());
            wilayahService.searchSubDistricts(null, null, "Latiung", PageRequest.of(0, 50));
            verify(subDistrictRepository).findByNameContainingIgnoreCase(eq("Latiung"), any());
        }
    }

    @Nested
    @DisplayName("createSubDistrict")
    class CreateSubDistrict {

        @Test
        @DisplayName("saves subdistrict with district FK and zipCode")
        void savesWithZipCode() {
            Province prov = province("1100", "Aceh");
            State st = state("1101", "Kab. Simeulue", prov);
            District dist = district("1101010", "Teupah Selatan", st);
            when(districtRepository.findById("1101010")).thenReturn(Optional.of(dist));
            SubDistrict saved = subDistrict("1101010001", "Desa Latiung", dist, "23891");
            when(subDistrictRepository.save(any())).thenReturn(saved);

            WilayahRequest req = request("1101010001", "Desa Latiung", "1101010");
            req.setZipCode("23891");
            SubDistrictResponse resp = wilayahService.createSubDistrict(req, "admin");

            assertThat(resp.getSubDistrictId()).isEqualTo("1101010001");
            assertThat(resp.getZipCode()).isEqualTo("23891");
            assertThat(resp.getDistrictId()).isEqualTo("1101010");
            verify(auditTrailService).recordAudit(eq("SUBDISTRICT"), isNull(), eq("CREATE_SUBDISTRICT"), isNull(), anyMap(), eq("admin"), isNull());
        }

        @Test
        @DisplayName("parent district not found → throws exception")
        void parentNotFound_throwsException() {
            when(districtRepository.findById("9999")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> wilayahService.createSubDistrict(request("X", "X", "9999"), "admin"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateSubDistrict")
    class UpdateSubDistrict {

        @Test
        @DisplayName("updates name and zipCode, records audit")
        void updatesAndAudits() {
            Province prov = province("1100", "Aceh");
            State st = state("1101", "Kab. Simeulue", prov);
            District dist = district("1101010", "Teupah Selatan", st);
            SubDistrict existing = subDistrict("1101010001", "Desa Lama", dist, "11111");
            when(subDistrictRepository.findById("1101010001")).thenReturn(Optional.of(existing));
            when(subDistrictRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WilayahRequest req = request("1101010001", "Desa Baru", null);
            req.setZipCode("22222");
            SubDistrictResponse resp = wilayahService.updateSubDistrict("1101010001", req, "admin");

            assertThat(resp.getName()).isEqualTo("Desa Baru");
            assertThat(resp.getZipCode()).isEqualTo("22222");
            verify(auditTrailService).recordAudit(eq("SUBDISTRICT"), isNull(), eq("UPDATE_SUBDISTRICT"), anyMap(), anyMap(), eq("admin"), isNull());
        }
    }

    @Nested
    @DisplayName("deleteSubDistrict")
    class DeleteSubDistrict {

        @Test
        @DisplayName("deletes and records audit")
        void deletesAndAudits() {
            Province prov = province("1100", "Aceh");
            State st = state("1101", "Kab. Simeulue", prov);
            District dist = district("1101010", "Teupah Selatan", st);
            SubDistrict sd = subDistrict("1101010001", "Desa Latiung", dist, "23891");
            when(subDistrictRepository.findById("1101010001")).thenReturn(Optional.of(sd));

            wilayahService.deleteSubDistrict("1101010001", "admin");

            verify(subDistrictRepository).delete(sd);
            verify(auditTrailService).recordAudit(eq("SUBDISTRICT"), isNull(), eq("DELETE_SUBDISTRICT"), anyMap(), isNull(), eq("admin"), isNull());
        }
    }
}
