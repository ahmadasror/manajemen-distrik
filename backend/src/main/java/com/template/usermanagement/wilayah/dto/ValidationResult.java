package com.template.usermanagement.wilayah.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationResult {

    private boolean found;
    /** VALID | PARTIAL_ZIP | PARTIAL_NAME | INVALID */
    private String status;

    /** 0–100 */
    private Integer nameSimilarity;
    private boolean zipCodeMatch;

    private String nominatimName;
    private String nominatimDisplayName;
    private String nominatimType;
    private String nominatimProvince;
    private String nominatimCounty;
    private String nominatimZipCode;

    private String localZipCode;

    private Double lat;
    private Double lon;

    private String source;
}
