package com.template.usermanagement.wilayah.dto;

import com.template.usermanagement.wilayah.District;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DistrictResponse {

    private String districtId;
    private String name;
    private String stateId;
    private String stateName;
    private String provinceId;
    private String provinceName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DistrictResponse from(District d) {
        String provinceId = null, provinceName = null;
        if (d.getState() != null && d.getState().getProvince() != null) {
            provinceId = d.getState().getProvince().getProvinceId();
            provinceName = d.getState().getProvince().getName();
        }
        return DistrictResponse.builder()
                .districtId(d.getDistrictId())
                .name(d.getName())
                .stateId(d.getState() != null ? d.getState().getStateId() : null)
                .stateName(d.getState() != null ? d.getState().getName() : null)
                .provinceId(provinceId)
                .provinceName(provinceName)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
