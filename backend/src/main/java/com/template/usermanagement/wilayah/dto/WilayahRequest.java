package com.template.usermanagement.wilayah.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WilayahRequest {

    @NotBlank(message = "ID is required")
    @Size(max = 10, message = "ID must not exceed 10 characters")
    private String id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    // Parent ID — used when creating State (provinceId), District (stateId), SubDistrict (districtId)
    private String parentId;

    // Only for SubDistrict
    @Size(max = 10, message = "Zip code must not exceed 10 characters")
    private String zipCode;
}
