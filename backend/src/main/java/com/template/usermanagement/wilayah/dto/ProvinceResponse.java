package com.template.usermanagement.wilayah.dto;

import com.template.usermanagement.wilayah.Province;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProvinceResponse {

    private String provinceId;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProvinceResponse from(Province p) {
        return ProvinceResponse.builder()
                .provinceId(p.getProvinceId())
                .name(p.getName())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
