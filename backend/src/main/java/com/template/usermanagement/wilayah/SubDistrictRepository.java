package com.template.usermanagement.wilayah;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubDistrictRepository extends JpaRepository<SubDistrict, String> {

    Page<SubDistrict> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<SubDistrict> findByDistrictDistrictIdAndNameContainingIgnoreCase(String districtId, String name, Pageable pageable);

    Page<SubDistrict> findByDistrictDistrictId(String districtId, Pageable pageable);

    Page<SubDistrict> findByZipCode(String zipCode, Pageable pageable);

    boolean existsByNameIgnoreCaseAndDistrictDistrictId(String name, String districtId);

    boolean existsByNameIgnoreCaseAndDistrictDistrictIdAndSubDistrictIdNot(String name, String districtId, String subDistrictId);

    /**
     * Flexible inquiry query — all filters are optional and combined with AND.
     * Null/blank params are ignored (match everything for that dimension).
     *
     * <p>Uses COALESCE instead of IS NULL to force PostgreSQL to infer varchar
     * type for null parameters; otherwise the LOWER() call fails with
     * "function lower(bytea) does not exist".
     */
    @Query("SELECT sd FROM SubDistrict sd " +
           "WHERE (COALESCE(:name,       '') = '' OR LOWER(sd.name)  LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND   (COALESCE(:zipCode,    '') = '' OR sd.zipCode      = :zipCode) " +
           "AND   (COALESCE(:districtId, '') = '' OR sd.district.districtId               = :districtId) " +
           "AND   (COALESCE(:stateId,    '') = '' OR sd.district.state.stateId            = :stateId) " +
           "AND   (COALESCE(:provinceId, '') = '' OR sd.district.state.province.provinceId = :provinceId)")
    Page<SubDistrict> inquiry(@Param("name")       String name,
                              @Param("zipCode")    String zipCode,
                              @Param("districtId") String districtId,
                              @Param("stateId")    String stateId,
                              @Param("provinceId") String provinceId,
                              Pageable pageable);
}
