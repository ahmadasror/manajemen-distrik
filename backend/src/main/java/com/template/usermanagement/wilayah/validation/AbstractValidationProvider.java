package com.template.usermanagement.wilayah.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Shared utilities for validation providers:
 * <ul>
 *   <li>Levenshtein similarity</li>
 *   <li>VALID / PARTIAL_ZIP / PARTIAL_NAME / INVALID status matrix</li>
 *   <li>Nominatim search + postcode lookup (usable as fallback by any provider)</li>
 * </ul>
 */
@Slf4j
abstract class AbstractValidationProvider implements WilayahValidationProvider {

    protected static final double SIMILARITY_THRESHOLD = 0.80;

    private static final String NOMINATIM_SEARCH  = "https://nominatim.openstreetmap.org/search";
    private static final String NOMINATIM_DETAILS = "https://nominatim.openstreetmap.org/details";

    // ─── Status helpers ────────────────────────────────────────────────────────

    protected String determineStatus(boolean nameValid, boolean hasLocalZip, boolean zipMatch) {
        if (nameValid && (!hasLocalZip || zipMatch)) return "VALID";
        if (nameValid)                               return "PARTIAL_ZIP";
        if (zipMatch)                                return "PARTIAL_NAME";
        return "INVALID";
    }

    // ─── Nominatim shared utilities ────────────────────────────────────────────

    /**
     * Calls Nominatim /search with Indonesian country filter.
     * Returns up to {@code limit} results, or an empty list on error.
     */
    protected List<Map<String, Object>> searchNominatim(RestTemplate restTemplate, String query, int limit) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_SEARCH)
                    .queryParam("q", "{q}")
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("limit", String.valueOf(limit))
                    .queryParam("countrycodes", "id")
                    .buildAndExpand(query)
                    .toUri();

            log.debug("[Nominatim] search URI: {}", uri);
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

    /**
     * Picks the Nominatim result whose {@code name} field best matches {@code target}.
     */
    protected Map<String, Object> nominatimBestMatch(String target, List<Map<String, Object>> results) {
        return results.stream()
                .max(Comparator.comparingDouble(r -> computeSimilarity(target, str(r, "name"))))
                .orElse(results.get(0));
    }

    /**
     * Fetches {@code calculated_postcode} from Nominatim /details for the given place_id.
     * Applies a 1100 ms sleep to respect Nominatim fair-use policy (max 1 req/s).
     * Returns null on error or if not found.
     *
     * @param label used in log messages to identify the caller context
     */
    protected String getNominatimPostcode(RestTemplate restTemplate, String placeId, String label) {
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
                String postcode = pc != null ? pc.toString().trim() : null;
                log.debug("[{}] Nominatim postcode for place_id={}: {}", label, placeId, postcode);
                return postcode;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("[{}] Nominatim details error: {}", label, e.getMessage());
        }
        return null;
    }

    // ─── Levenshtein similarity ────────────────────────────────────────────────

    /** Returns similarity in [0.0, 1.0] — 1.0 = identical. */
    public static double computeSimilarity(String a, String b) {
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

    // ─── String helpers ────────────────────────────────────────────────────────

    protected static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    protected static Double parseDouble(Object val) {
        try { return val != null ? Double.parseDouble(val.toString()) : null; }
        catch (NumberFormatException e) { return null; }
    }

    protected static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
