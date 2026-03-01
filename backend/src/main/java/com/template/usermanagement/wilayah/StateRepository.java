package com.template.usermanagement.wilayah;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateRepository extends JpaRepository<State, String> {

    Page<State> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<State> findByProvinceProvinceIdAndNameContainingIgnoreCase(String provinceId, String name, Pageable pageable);

    Page<State> findByProvinceProvinceId(String provinceId, Pageable pageable);

    boolean existsByNameIgnoreCaseAndProvinceProvinceId(String name, String provinceId);

    boolean existsByNameIgnoreCaseAndProvinceProvinceIdAndStateIdNot(String name, String provinceId, String stateId);
}
