package com.template.usermanagement.wilayah.dto;

import com.template.usermanagement.wilayah.SubDistrict;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubDistrictResponse {

    private String subDistrictId;
    private String name;
    private String districtId;
    private String districtName;
    private String stateId;
    private String stateName;
    private String provinceId;
    private String provinceName;
    private String zipCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubDistrictResponse from(SubDistrict sd) {
        String districtId = null, districtName = null, stateId = null, stateName = null, provinceId = null, provinceName = null;
        if (sd.getDistrict() != null) {
            districtId = sd.getDistrict().getDistrictId();
            districtName = sd.getDistrict().getName();
            if (sd.getDistrict().getState() != null) {
                stateId = sd.getDistrict().getState().getStateId();
                stateName = sd.getDistrict().getState().getName();
                if (sd.getDistrict().getState().getProvince() != null) {
                    provinceId = sd.getDistrict().getState().getProvince().getProvinceId();
                    provinceName = sd.getDistrict().getState().getProvince().getName();
                }
            }
        }
        return SubDistrictResponse.builder()
                .subDistrictId(sd.getSubDistrictId())
                .name(sd.getName())
                .districtId(districtId)
                .districtName(districtName)
                .stateId(stateId)
                .stateName(stateName)
                .provinceId(provinceId)
                .provinceName(provinceName)
                .zipCode(sd.getZipCode())
                .createdAt(sd.getCreatedAt())
                .updatedAt(sd.getUpdatedAt())
                .build();
    }
}
