package com.template.usermanagement.wilayah.validation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the wilayah validation provider.
 *
 * <pre>
 * wilayah:
 *   validation:
 *     provider: wikipedia   # options: wikipedia | nominatim
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "wilayah.validation")
public class WilayahValidationProperties {

    /**
     * Name of the active validation provider.
     * Must match one of the registered {@link WilayahValidationProvider#getProviderName()} values.
     * Defaults to {@code wikipedia}.
     */
    private String provider = "wikipedia";
}
