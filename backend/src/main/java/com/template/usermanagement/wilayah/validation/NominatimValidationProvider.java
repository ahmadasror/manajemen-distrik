package com.template.usermanagement.wilayah.validation;

import com.template.usermanagement.wilayah.dto.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Validation provider backed by OpenStreetMap Nominatim.
 *
 * <p>Fair-use policy: max 1 request/second → 1100 ms sleep before the
 * /details call. Requires no API key.
 *
 * <p>Activate via: {@code wilayah.validation.provider: nominatim}
 */
@Slf4j
@Component
public class NominatimValidationProvider extends AbstractValidationProvider {

    private static final String PROVIDER_NAME   = "nominatim";
    private static final String NOMINATIM_SEARCH  = "https://nominatim.openstreetmap.org/search";
    private static final String NOMINATIM_DETAILS = "https://nominatim.openstreetmap.org/details";
    private static final String USER_AGENT        = "ManajemenDistrik/1.0 (educational project)";

    private final RestTemplate restTemplate;

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

        List<Map<String, Object>> results = search(query);
        if (results.isEmpty()) {
            return ValidationResult.builder()
                    .found(false).status("INVALID")
                    .localZipCode(zipCode)
                    .source("OpenStreetMap Nominatim")
                    .build();
        }

        Map<String, Object> best   = findBest(name, results);
        String nominatimName       = str(best, "name");
        double similarity          = computeSimilarity(name, nominatimName);
        int    similarityPct       = (int) (similarity * 100);
        String placeId             = String.valueOf(best.get("place_id"));
        String nominatimZip        = getPostcode(placeId);

        boolean hasLocalZip = zipCode != null && !zipCode.isBlank();
        String  normNomZip  = nominatimZip != null ? nominatimZip.trim() : "";
        boolean zipMatch    = hasLocalZip && zipCode.trim().equals(normNomZip);
        boolean nameValid   = similarity >= SIMILARITY_THRESHOLD;

        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) best.getOrDefault("address", Map.of());

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
                .build();
    }

    // ─── Nominatim search ─────────────────────────────────────────────────────

    private List<Map<String, Object>> search(String query) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_SEARCH)
                    .queryParam("q", "{q}")
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("limit", "3")
                    .queryParam("countrycodes", "id")
                    .buildAndExpand(query)
                    .toUri();

            log.debug("[Nominatim] URI: {}", uri);
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> body = resp.getBody();
            log.debug("[Nominatim] {} result(s)", body != null ? body.size() : 0);
            return body != null ? body : List.of();
        } catch (Exception e) {
            log.warn("[Nominatim] search error: {}", e.getMessage());
            return List.of();
        }
    }

    private String getPostcode(String placeId) {
        try {
            Thread.sleep(1100); // Nominatim fair-use: max 1 req/sec

            URI uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_DETAILS)
                    .queryParam("place_id", "{id}")
                    .queryParam("format", "json")
                    .buildAndExpand(placeId)
                    .toUri();

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            if (resp.getBody() != null) {
                Object pc = resp.getBody().get("calculated_postcode");
                return pc != null ? pc.toString().trim() : null;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("[Nominatim] details error: {}", e.getMessage());
        }
        return null;
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

    private Map<String, Object> findBest(String target, List<Map<String, Object>> results) {
        return results.stream()
                .max(Comparator.comparingDouble(r -> computeSimilarity(target, str(r, "name"))))
                .orElse(results.get(0));
    }
}
