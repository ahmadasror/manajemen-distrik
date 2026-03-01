package com.template.usermanagement.wilayah;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubDistrictRepository extends JpaRepository<SubDistrict, String> {

    Page<SubDistrict> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<SubDistrict> findByDistrictDistrictIdAndNameContainingIgnoreCase(String districtId, String name, Pageable pageable);

    Page<SubDistrict> findByDistrictDistrictId(String districtId, Pageable pageable);

    Page<SubDistrict> findByZipCode(String zipCode, Pageable pageable);

    boolean existsByNameIgnoreCaseAndDistrictDistrictId(String name, String districtId);

    boolean existsByNameIgnoreCaseAndDistrictDistrictIdAndSubDistrictIdNot(String name, String districtId, String subDistrictId);
}
