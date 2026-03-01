package com.template.usermanagement.wilayah;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Wilayah Repositories")
class WilayahRepositoryTest {

    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private StateRepository stateRepository;
    @Autowired private DistrictRepository districtRepository;
    @Autowired private SubDistrictRepository subDistrictRepository;

    private Province aceh;
    private Province jawa;
    private State simeulue;
    private State jakarta;
    private District teupahSelatan;
    private District jakartaPusat;
    private SubDistrict desaLatiung;
    private SubDistrict melayu;

    @BeforeEach
    void setUp() {
        // Province
        aceh = provinceRepository.save(Province.builder().provinceId("1100").name("Aceh").build());
        jawa = provinceRepository.save(Province.builder().provinceId("3100").name("Jawa Barat").build());

        // State
        simeulue = stateRepository.save(State.builder().stateId("1101").name("Kab. Simeulue").province(aceh).build());
        jakarta = stateRepository.save(State.builder().stateId("3101").name("Kota Jakarta").province(jawa).build());

        // District
        teupahSelatan = districtRepository.save(District.builder().districtId("1101010").name("Teupah Selatan").state(simeulue).build());
        jakartaPusat = districtRepository.save(District.builder().districtId("3101010").name("Jakarta Pusat").state(jakarta).build());

        // SubDistrict
        desaLatiung = subDistrictRepository.save(SubDistrict.builder()
                .subDistrictId("1101010001").name("Desa Latiung").district(teupahSelatan).zipCode("23891").build());
        melayu = subDistrictRepository.save(SubDistrict.builder()
                .subDistrictId("3101010001").name("Gambir Melayu").district(jakartaPusat).zipCode("10110").build());
    }

    // ── ProvinceRepository ────────────────────────────────────────────────────

    @Nested
    @DisplayName("ProvinceRepository")
    class ProvinceRepo {

        @Test
        @DisplayName("findByNameContainingIgnoreCase — case insensitive match")
        void findByNameContaining_caseInsensitive() {
            Page<Province> result = provinceRepository.findByNameContainingIgnoreCase("aceh", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getProvinceId()).isEqualTo("1100");
        }

        @Test
        @DisplayName("findByNameContainingIgnoreCase — uppercase search matches lowercase name")
        void findByNameContaining_uppercase() {
            Page<Province> result = provinceRepository.findByNameContainingIgnoreCase("JAWA", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Jawa Barat");
        }

        @Test
        @DisplayName("findByNameContainingIgnoreCase — no match returns empty")
        void findByNameContaining_noMatch() {
            Page<Province> result = provinceRepository.findByNameContainingIgnoreCase("Sulawesi", PageRequest.of(0, 10));
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("findAll returns all provinces")
        void findAll_returnsAll() {
            assertThat(provinceRepository.findAll()).hasSize(2);
        }
    }

    // ── StateRepository ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("StateRepository")
    class StateRepo {

        @Test
        @DisplayName("findByProvinceProvinceId — returns states in given province")
        void findByProvinceProvinceId() {
            Page<State> result = stateRepository.findByProvinceProvinceId("1100", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStateId()).isEqualTo("1101");
        }

        @Test
        @DisplayName("findByProvinceProvinceId — wrong province returns empty")
        void findByProvinceProvinceId_wrongProvince() {
            Page<State> result = stateRepository.findByProvinceProvinceId("9999", PageRequest.of(0, 10));
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("findByProvinceProvinceIdAndNameContainingIgnoreCase — filters correctly")
        void findByProvinceAndNameContaining() {
            Page<State> result = stateRepository.findByProvinceProvinceIdAndNameContainingIgnoreCase("1100", "simeulue", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStateId()).isEqualTo("1101");
        }

        @Test
        @DisplayName("findByProvinceProvinceIdAndNameContainingIgnoreCase — province mismatch returns empty")
        void findByProvinceAndNameContaining_provinceMismatch() {
            // "Kota Jakarta" is in province 3100, not 1100
            Page<State> result = stateRepository.findByProvinceProvinceIdAndNameContainingIgnoreCase("1100", "jakarta", PageRequest.of(0, 10));
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("findByNameContainingIgnoreCase — cross-province search")
        void findByNameContaining() {
            Page<State> result = stateRepository.findByNameContainingIgnoreCase("kota", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).containsIgnoringCase("kota");
        }
    }

    // ── DistrictRepository ────────────────────────────────────────────────────

    @Nested
    @DisplayName("DistrictRepository")
    class DistrictRepo {

        @Test
        @DisplayName("findByStateStateId — returns districts in given state")
        void findByStateStateId() {
            Page<District> result = districtRepository.findByStateStateId("1101", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getDistrictId()).isEqualTo("1101010");
        }

        @Test
        @DisplayName("findByStateStateIdAndNameContainingIgnoreCase — filters within state")
        void findByStateAndNameContaining() {
            Page<District> result = districtRepository.findByStateStateIdAndNameContainingIgnoreCase("1101", "teupah", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("findByNameContainingIgnoreCase — finds across all states")
        void findByNameContaining() {
            Page<District> result = districtRepository.findByNameContainingIgnoreCase("jakarta", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).containsIgnoringCase("jakarta");
        }
    }

    // ── SubDistrictRepository ─────────────────────────────────────────────────

    @Nested
    @DisplayName("SubDistrictRepository")
    class SubDistrictRepo {

        @Test
        @DisplayName("findByDistrictDistrictId — returns subdistricts in given district")
        void findByDistrictDistrictId() {
            Page<SubDistrict> result = subDistrictRepository.findByDistrictDistrictId("1101010", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getSubDistrictId()).isEqualTo("1101010001");
        }

        @Test
        @DisplayName("findByDistrictDistrictIdAndNameContainingIgnoreCase — filters within district")
        void findByDistrictAndNameContaining() {
            Page<SubDistrict> result = subDistrictRepository.findByDistrictDistrictIdAndNameContainingIgnoreCase("1101010", "latiung", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("findByZipCode — exact match")
        void findByZipCode_exact() {
            Page<SubDistrict> result = subDistrictRepository.findByZipCode("23891", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getZipCode()).isEqualTo("23891");
        }

        @Test
        @DisplayName("findByZipCode — no match returns empty")
        void findByZipCode_noMatch() {
            Page<SubDistrict> result = subDistrictRepository.findByZipCode("99999", PageRequest.of(0, 10));
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("findByNameContainingIgnoreCase — cross-district search")
        void findByNameContaining() {
            Page<SubDistrict> result = subDistrictRepository.findByNameContainingIgnoreCase("melayu", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).containsIgnoringCase("melayu");
        }

        @Test
        @DisplayName("findByNameContainingIgnoreCase — partial match")
        void findByNameContaining_partial() {
            Page<SubDistrict> result = subDistrictRepository.findByNameContainingIgnoreCase("desa", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("multiple results returned with pagination")
        void findAll_withPagination() {
            // Add another subdistrict so we have 3 total
            subDistrictRepository.save(SubDistrict.builder()
                    .subDistrictId("1101010002").name("Desa Bajau").district(teupahSelatan).zipCode("23891").build());

            Page<SubDistrict> page0 = subDistrictRepository.findAll(PageRequest.of(0, 2));
            assertThat(page0.getTotalElements()).isEqualTo(3);
            assertThat(page0.getContent()).hasSize(2);
        }
    }
}
