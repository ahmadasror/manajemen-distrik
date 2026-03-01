package com.template.usermanagement.wilayah.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

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

    /** Primary provider used for this validation (e.g. "Wikipedia (id.wikipedia.org)"). */
    private String source;

    /**
     * Per-field source attribution for tracing which data provider supplied each value.
     * Keys: name, zipCode, province, county, district, type, coordinates.
     * Values: provider label, e.g. "Wikipedia", "Nominatim", "Nominatim (fallback)".
     */
    private Map<String, String> fieldSources;
}
