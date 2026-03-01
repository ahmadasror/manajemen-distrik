package com.template.usermanagement.wilayah;

import com.template.usermanagement.wilayah.dto.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WilayahValidationService {

    private static final String NOMINATIM_SEARCH  = "https://nominatim.openstreetmap.org/search";
    private static final String NOMINATIM_DETAILS = "https://nominatim.openstreetmap.org/details";
    private static final String USER_AGENT        = "ManajemenDistrik/1.0 (educational project)";
    private static final double SIMILARITY_THRESHOLD = 0.80;

    private final RestTemplate restTemplate;

    public WilayahValidationService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    public ValidationResult validate(String name, String zipCode,
                                     String provinceName, String stateName, String districtName) {

        String query = buildQuery(name, districtName, stateName, provinceName);
        log.debug("[Validate] Nominatim query: {}", query);

        List<Map<String, Object>> searchResults = searchNominatim(query);
        if (searchResults.isEmpty()) {
            return ValidationResult.builder()
                    .found(false)
                    .status("INVALID")
                    .localZipCode(zipCode)
                    .source("OpenStreetMap Nominatim")
                    .build();
        }

        // Pick best name match from top results
        Map<String, Object> best = findBestMatch(name, searchResults);
        String nominatimName = str(best, "name");
        double similarity = computeSimilarity(name, nominatimName);
        int similarityPct = (int) (similarity * 100);

        // Get postcode via /details (needs separate call)
        String placeId = String.valueOf(best.get("place_id"));
        String nominatimZip = getPostcode(placeId);

        // Zip comparison
        boolean hasLocalZip = zipCode != null && !zipCode.isBlank();
        String normNomZip = nominatimZip != null ? nominatimZip.trim() : "";
        boolean zipMatch = hasLocalZip && zipCode.trim().equals(normNomZip);

        // Status determination
        boolean nameValid = similarity >= SIMILARITY_THRESHOLD;
        String status;
        if (nameValid && (!hasLocalZip || zipMatch))     status = "VALID";
        else if (nameValid)                              status = "PARTIAL_ZIP";
        else if (zipMatch)                               status = "PARTIAL_NAME";
        else                                             status = "INVALID";

        // Address details from search result
        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) best.getOrDefault("address", Map.of());
        String nominatimProvince = str(address, "state");
        String nominatimCounty  = address.containsKey("county") ? str(address, "county")
                                : address.containsKey("city")   ? str(address, "city") : null;

        return ValidationResult.builder()
                .found(true)
                .status(status)
                .nameSimilarity(similarityPct)
                .zipCodeMatch(zipMatch)
                .nominatimName(nominatimName)
                .nominatimDisplayName(str(best, "display_name"))
                .nominatimType(str(best, "type"))
                .nominatimProvince(nominatimProvince)
                .nominatimCounty(nominatimCounty)
                .nominatimZipCode(normNomZip.isEmpty() ? null : normNomZip)
                .localZipCode(zipCode)
                .lat(parseDouble(best.get("lat")))
                .lon(parseDouble(best.get("lon")))
                .source("OpenStreetMap Nominatim")
                .build();
    }

    // ─── Nominatim search ─────────────────────────────────────────────────────

    private List<Map<String, Object>> searchNominatim(String query) {
        try {
            // Use template variable to avoid double-encoding by RestTemplate
            URI uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_SEARCH)
                    .queryParam("q", "{q}")
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("limit", "3")
                    .queryParam("countrycodes", "id")
                    .buildAndExpand(query)
                    .toUri();

            log.debug("[Validate] Nominatim URI: {}", uri);
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            List<Map<String, Object>> body = resp.getBody();
            log.debug("[Validate] Nominatim returned {} result(s)", body != null ? body.size() : 0);
            return body != null ? body : List.of();
        } catch (Exception e) {
            log.warn("[Validate] Nominatim search error: {}", e.getMessage());
            return List.of();
        }
    }

    private String getPostcode(String placeId) {
        try {
            // Nominatim fair-use: max 1 req/sec
            Thread.sleep(1100);

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
            log.warn("[Validate] Nominatim details error: {}", e.getMessage());
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

    private Map<String, Object> findBestMatch(String target, List<Map<String, Object>> results) {
        return results.stream()
                .max(Comparator.comparingDouble(r -> computeSimilarity(target, str(r, "name"))))
                .orElse(results.get(0));
    }

    /** Levenshtein-based similarity in [0.0, 1.0] */
    static double computeSimilarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        String s1 = a.toLowerCase().trim();
        String s2 = b.toLowerCase().trim();
        if (s1.equals(s2)) return 1.0;
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0;
        return 1.0 - (double) levenshtein(s1, s2) / maxLen;
    }

    private static int levenshtein(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1];
                else dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[m][n];
    }

    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private static Double parseDouble(Object val) {
        try { return val != null ? Double.parseDouble(val.toString()) : null; }
        catch (NumberFormatException e) { return null; }
    }
}
