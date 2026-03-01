package com.template.usermanagement.wilayah;

import com.template.usermanagement.wilayah.dto.ValidationResult;
import com.template.usermanagement.wilayah.validation.WilayahValidationProperties;
import com.template.usermanagement.wilayah.validation.WilayahValidationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Facade that delegates to the configured {@link WilayahValidationProvider}.
 *
 * <p>The active provider is selected at startup from {@code wilayah.validation.provider}
 * in application.yml. All registered {@code @Component} implementations of
 * {@link WilayahValidationProvider} are auto-discovered.
 */
@Slf4j
@Service
public class WilayahValidationService {

    private final WilayahValidationProvider activeProvider;

    public WilayahValidationService(List<WilayahValidationProvider> providers,
                                    WilayahValidationProperties props) {
        Map<String, WilayahValidationProvider> byName = providers.stream()
                .collect(Collectors.toMap(
                        WilayahValidationProvider::getProviderName,
                        Function.identity()));

        String configured = props.getProvider();
        if (!byName.containsKey(configured)) {
            throw new IllegalStateException(
                    "Unknown wilayah validation provider: '" + configured +
                    "'. Available: " + byName.keySet());
        }

        this.activeProvider = byName.get(configured);
        log.info("[WilayahValidation] Active provider: {}", activeProvider.getProviderName());
    }

    public ValidationResult validate(String name, String zipCode,
                                     String provinceName, String stateName, String districtName) {
        return activeProvider.validate(name, zipCode, provinceName, stateName, districtName);
    }

    /** Returns the name of the currently active provider (e.g. for diagnostics). */
    public String getActiveProviderName() {
        return activeProvider.getProviderName();
    }
}
