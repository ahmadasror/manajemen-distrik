package com.template.usermanagement.wilayah.dto;

import com.template.usermanagement.wilayah.State;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StateResponse {

    private String stateId;
    private String name;
    private String provinceId;
    private String provinceName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StateResponse from(State s) {
        return StateResponse.builder()
                .stateId(s.getStateId())
                .name(s.getName())
                .provinceId(s.getProvince() != null ? s.getProvince().getProvinceId() : null)
                .provinceName(s.getProvince() != null ? s.getProvince().getName() : null)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
