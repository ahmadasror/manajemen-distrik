package com.template.usermanagement.wilayah;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<District, String> {

    Page<District> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<District> findByStateStateIdAndNameContainingIgnoreCase(String stateId, String name, Pageable pageable);

    Page<District> findByStateStateId(String stateId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndStateStateId(String name, String stateId);

    boolean existsByNameIgnoreCaseAndStateStateIdAndDistrictIdNot(String name, String stateId, String districtId);
}
