package com.template.usermanagement.wilayah;

import com.template.usermanagement.config.SystemSettingService;
import com.template.usermanagement.wilayah.dto.ValidationResult;
import com.template.usermanagement.wilayah.validation.GoogleSearchValidationProvider;
import com.template.usermanagement.wilayah.validation.WilayahValidationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Facade that chains validation providers based on the configured mode.
 *
 * <h3>Mode: {@code free} (default)</h3>
 * <ol>
 *   <li>Wikipedia → if {@code found=true}, return</li>
 *   <li>Nominatim (full) → return</li>
 * </ol>
 *
 * <h3>Mode: {@code paid}</h3>
 * <ol>
 *   <li>Wikipedia → if {@code found=true}, return</li>
 *   <li>Nominatim (full) → if {@code found=true}, return</li>
 *   <li>Google Custom Search → return (last resort)</li>
 * </ol>
 *
 * <p>The mode is read from {@code system_settings} table (key: {@code validation.mode})
 * at each request, so it can be changed at runtime without restart.
 */
@Slf4j
@Service
public class WilayahValidationService {

    private final Map<String, WilayahValidationProvider> providersByName;
    private final GoogleSearchValidationProvider googleProvider;
    private final SystemSettingService settingService;

    /** Free chain order: wikipedia first, nominatim second */
    private static final List<String> FREE_CHAIN = List.of("wikipedia", "nominatim");

    public WilayahValidationService(List<WilayahValidationProvider> providers,
                                    GoogleSearchValidationProvider googleProvider,
                                    SystemSettingService settingService) {
        this.providersByName = providers.stream()
                .collect(Collectors.toMap(
                        WilayahValidationProvider::getProviderName,
                        Function.identity()));
        this.googleProvider = googleProvider;
        this.settingService = settingService;

        log.info("[WilayahValidation] Available providers: {}", providersByName.keySet());
    }

    public ValidationResult validate(String name, String zipCode,
                                     String provinceName, String stateName, String districtName) {
        String mode = getMode();
        boolean isPaid = "paid".equalsIgnoreCase(mode);
        log.debug("[WilayahValidation] mode={}, isPaid={}", mode, isPaid);

        // Try free providers in order
        ValidationResult lastResult = null;
        for (String providerName : FREE_CHAIN) {
            WilayahValidationProvider provider = providersByName.get(providerName);
            if (provider == null) continue;

            log.debug("[WilayahValidation] trying provider: {}", providerName);
            ValidationResult result = provider.validate(name, zipCode, provinceName, stateName, districtName);

            if (result.isFound()) {
                log.debug("[WilayahValidation] {} returned found=true, status={}", providerName, result.getStatus());
                return result;
            }
            lastResult = result;
        }

        // Paid mode: try Google as last resort
        if (isPaid && googleProvider.isConfigured()) {
            log.debug("[WilayahValidation] free providers exhausted — trying Google");
            ValidationResult googleResult = googleProvider.validate(name, zipCode, provinceName, stateName, districtName);
            if (googleResult.isFound()) {
                return googleResult;
            }
            lastResult = googleResult;
        } else if (isPaid && !googleProvider.isConfigured()) {
            log.warn("[WilayahValidation] paid mode but Google API not configured");
        }

        // Nothing found — return last result (or a generic not-found)
        return lastResult != null ? lastResult : ValidationResult.builder()
                .found(false).status("INVALID")
                .localZipCode(zipCode)
                .source("No provider found result")
                .fieldSources(Map.of())
                .build();
    }

    /** Returns current mode from DB settings, defaults to "free". */
    private String getMode() {
        String mode = settingService.getValue("validation.mode");
        return mode != null && !mode.isBlank() ? mode : "free";
    }

    /** Returns the current validation mode (for diagnostics / frontend display). */
    public String getActiveMode() {
        return getMode();
    }
}
