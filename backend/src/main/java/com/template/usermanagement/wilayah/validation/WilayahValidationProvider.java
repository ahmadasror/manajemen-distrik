package com.template.usermanagement.wilayah.validation;

import com.template.usermanagement.wilayah.dto.ValidationResult;

/**
 * Strategy interface for wilayah validation providers.
 * Implementations validate a region name (and optional zip code) against
 * an external data source.
 *
 * <p>Select the active provider via {@code wilayah.validation.provider} in application.yml.
 * Available values: {@code nominatim}, {@code wikipedia}.
 */
public interface WilayahValidationProvider {

    /** Unique identifier used in {@code wilayah.validation.provider} config. */
    String getProviderName();

    /**
     * Validate a subdistrict/village name against the external source.
     *
     * @param name         subdistrict or village name (required)
     * @param zipCode      local zip code to compare (nullable)
     * @param provinceName province context hint (nullable)
     * @param stateName    kab/kota context hint (nullable)
     * @param districtName kecamatan context hint (nullable)
     * @return {@link ValidationResult} — never null
     */
    ValidationResult validate(String name, String zipCode,
                              String provinceName, String stateName, String districtName);
}
