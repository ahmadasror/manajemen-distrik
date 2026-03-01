package com.template.usermanagement.wilayah.validation;

import com.template.usermanagement.wilayah.dto.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validation provider backed by OpenStreetMap Nominatim.
 *
 * <p>All fields originate from Nominatim (reflected in {@code fieldSources}).
 *
 * <p>Activate via: {@code wilayah.validation.provider: nominatim}
 */
@Slf4j
@Component
public class NominatimValidationProvider extends AbstractValidationProvider {

    static final String PROVIDER_NAME = "nominatim";
    static final String LABEL         = "Nominatim";
    private static final String USER_AGENT = "ManajemenDistrik/1.0 (educational project)";

    final RestTemplate restTemplate;

    public NominatimValidationProvider(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public ValidationResult validate(String name, String zipCode,
                                     String provinceName, String stateName, String districtName) {
        String query = buildQuery(name, districtName, stateName, provinceName);
        log.debug("[Nominatim] query: {}", query);

        List<Map<String, Object>> results = searchNominatim(restTemplate, query, 3);
        if (results.isEmpty()) {
            return ValidationResult.builder()
                    .found(false).status("INVALID")
                    .localZipCode(zipCode)
                    .source("OpenStreetMap Nominatim")
                    .fieldSources(Map.of())
                    .build();
        }

        Map<String, Object> best   = nominatimBestMatch(name, results);
        String nominatimName       = str(best, "name");
        double similarity          = computeSimilarity(name, nominatimName);
        int    similarityPct       = (int) (similarity * 100);
        String placeId             = String.valueOf(best.get("place_id"));
        String nominatimZip        = getNominatimPostcode(restTemplate, placeId, "Nominatim");

        boolean hasLocalZip = zipCode != null && !zipCode.isBlank();
        String  normNomZip  = nominatimZip != null ? nominatimZip.trim() : "";
        boolean zipMatch    = hasLocalZip && zipCode.trim().equals(normNomZip);
        boolean nameValid   = similarity >= SIMILARITY_THRESHOLD;

        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) best.getOrDefault("address", Map.of());

        Map<String, String> fieldSources = buildFieldSources(nominatimName, normNomZip,
                str(address, "state"), address);

        return ValidationResult.builder()
                .found(true)
                .status(determineStatus(nameValid, hasLocalZip, zipMatch))
                .nameSimilarity(similarityPct)
                .zipCodeMatch(zipMatch)
                .nominatimName(nominatimName)
                .nominatimDisplayName(str(best, "display_name"))
                .nominatimType(str(best, "type"))
                .nominatimProvince(str(address, "state"))
                .nominatimCounty(address.containsKey("county") ? str(address, "county")
                               : address.containsKey("city")   ? str(address, "city") : null)
                .nominatimZipCode(normNomZip.isEmpty() ? null : normNomZip)
                .localZipCode(zipCode)
                .lat(parseDouble(best.get("lat")))
                .lon(parseDouble(best.get("lon")))
                .source("OpenStreetMap Nominatim")
                .fieldSources(fieldSources)
                .build();
    }

    private Map<String, String> buildFieldSources(String name, String zip,
                                                   String province, Map<String, Object> address) {
        Map<String, String> fs = new LinkedHashMap<>();
        if (name     != null)                        fs.put("name",        LABEL);
        if (!zip.isEmpty())                          fs.put("zipCode",     LABEL);
        if (province != null)                        fs.put("province",    LABEL);
        String county = address.containsKey("county") ? str(address, "county")
                      : address.containsKey("city")   ? str(address, "city") : null;
        if (county   != null)                        fs.put("county",      LABEL);
        if (str(address, "suburb") != null
         || str(address, "village") != null)         fs.put("district",    LABEL);
        fs.put("coordinates", LABEL);
        return fs;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String buildQuery(String name, String district, String state, String province) {
        StringBuilder sb = new StringBuilder(name);
        if (district != null && !district.isBlank()) sb.append(", ").append(district);
        if (state    != null && !state.isBlank())    sb.append(", ").append(state);
        if (province != null && !province.isBlank()) sb.append(", ").append(province);
        sb.append(", Indonesia");
        return sb.toString();
    }
}
