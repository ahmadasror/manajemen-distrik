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
     */
    @Query("SELECT sd FROM SubDistrict sd " +
           "WHERE (:name    IS NULL OR LOWER(sd.name)    LIKE LOWER(CONCAT('%', :name,    '%'))) " +
           "AND   (:zipCode IS NULL OR sd.zipCode        = :zipCode) " +
           "AND   (:districtId  IS NULL OR sd.district.districtId               = :districtId) " +
           "AND   (:stateId     IS NULL OR sd.district.state.stateId            = :stateId) " +
           "AND   (:provinceId  IS NULL OR sd.district.state.province.provinceId = :provinceId)")
    Page<SubDistrict> inquiry(@Param("name")       String name,
                              @Param("zipCode")    String zipCode,
                              @Param("districtId") String districtId,
                              @Param("stateId")    String stateId,
                              @Param("provinceId") String provinceId,
                              Pageable pageable);
}
