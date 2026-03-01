package com.template.usermanagement.wilayah.validation;

import com.template.usermanagement.config.SystemSettingService;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validation provider backed by Google Custom Search JSON API.
 *
 * <p>Searches Google for the village/kelurahan name and extracts structured
 * information from snippets (zip code, province, kab/kota). Uses Nominatim
 * as supplementary source for coordinates.
 *
 * <p>Requires {@code google.api.key} and {@code google.api.cx} to be configured
 * in system_settings. This is the "paid" provider — only invoked when free
 * providers (Wikipedia, Nominatim) all return {@code found=false}.
 *
 * <p>Activate via: setting {@code validation.mode} to {@code paid} in system settings.
 */
@Slf4j
@Component
public class GoogleSearchValidationProvider extends AbstractValidationProvider {

    static final String PROVIDER_NAME = "google";
    private static final String GOOGLE_API = "https://www.googleapis.com/customsearch/v1";
    private static final Pattern P_ZIP = Pattern.compile("\\b(\\d{5})\\b");
    private static final String USER_AGENT = "ManajemenDistrik/1.0 (educational project)";

    private final SystemSettingService settingService;
    private final RestTemplate restTemplate;

    public GoogleSearchValidationProvider(SystemSettingService settingService,
                                          RestTemplateBuilder builder) {
        this.settingService = settingService;
        this.restTemplate = builder
                .defaultHeader("User-Agent", USER_AGENT)
                .build();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * Returns true if Google API credentials are configured.
     */
    public boolean isConfigured() {
        String apiKey = settingService.getValue("google.api.key");
        String cx = settingService.getValue("google.api.cx");
        return apiKey != null && !apiKey.isBlank() && cx != null && !cx.isBlank();
    }

    @Override
    public ValidationResult validate(String name, String zipCode,
                                     String provinceName, String stateName, String districtName) {
        String apiKey = settingService.getValue("google.api.key");
        String cx = settingService.getValue("google.api.cx");

        if (apiKey == null || apiKey.isBlank() || cx == null || cx.isBlank()) {
            log.warn("[Google] API key or CX not configured — skipping");
            return ValidationResult.builder()
                    .found(false).status("INVALID")
                    .localZipCode(zipCode)
                    .source("Google Custom Search (not configured)")
                    .fieldSources(Map.of())
                    .build();
        }

        String query = buildQuery(name, districtName, stateName, provinceName);
        log.debug("[Google] search query: {}", query);

        List<Map<String, Object>> items = searchGoogle(apiKey, cx, query);
        if (items.isEmpty()) {
            return ValidationResult.builder()
                    .found(false).status("INVALID")
                    .localZipCode(zipCode)
                    .source("Google Custom Search")
                    .fieldSources(Map.of())
                    .build();
        }

        // Extract info from search results
        String bestTitle = null;
        String bestSnippet = null;
        double bestSimilarity = 0;

        for (Map<String, Object> item : items) {
            String title = str(item, "title");
            String snippet = str(item, "snippet");
            String simpleName = extractSimpleName(title);
            double sim = computeSimilarity(name, simpleName);
            if (sim > bestSimilarity) {
                bestSimilarity = sim;
                bestTitle = title;
                bestSnippet = snippet;
            }
        }

        String canonicalName = extractSimpleName(bestTitle);
        int similarityPct = (int) (bestSimilarity * 100);

        // Try to extract zip code from snippets
        String googleZip = extractZipFromResults(items);

        // Try to extract province/county from title or snippets
        String googleProvince = extractProvince(items);
        String googleCounty = extractCounty(items);

        // Nominatim for coordinates
        Map<String, String> fieldSources = new LinkedHashMap<>();
        fieldSources.put("name", "Google");
        if (googleProvince != null) fieldSources.put("province", "Google");
        if (googleCounty != null) fieldSources.put("county", "Google");

        String resolvedZip = googleZip;
        Double lat = null;
        Double lon = null;

        if (resolvedZip != null) {
            fieldSources.put("zipCode", "Google");
        }

        // Use Nominatim for coordinates and supplementary data
        String nomQuery = buildNominatimQuery(name, districtName, stateName, provinceName);
        List<Map<String, Object>> nomResults = searchNominatim(restTemplate, nomQuery, 3);
        if (!nomResults.isEmpty()) {
            Map<String, Object> nomBest = nominatimBestMatch(name, nomResults);
            lat = parseDouble(nomBest.get("lat"));
            lon = parseDouble(nomBest.get("lon"));
            if (lat != null || lon != null) fieldSources.put("coordinates", "Nominatim");

            // Fill in zip from Nominatim if Google didn't find one
            if (resolvedZip == null) {
                String placeId = String.valueOf(nomBest.get("place_id"));
                resolvedZip = getNominatimPostcode(restTemplate, placeId, "Google→Nominatim");
                if (resolvedZip != null) fieldSources.put("zipCode", "Nominatim (fallback)");
            }

            // Fill in province/county from Nominatim if Google didn't find them
            @SuppressWarnings("unchecked")
            Map<String, Object> address = (Map<String, Object>) nomBest.getOrDefault("address", Map.of());
            if (googleProvince == null) {
                googleProvince = str(address, "state");
                if (googleProvince != null) fieldSources.put("province", "Nominatim");
            }
            if (googleCounty == null) {
                googleCounty = address.containsKey("county") ? str(address, "county")
                        : address.containsKey("city") ? str(address, "city") : null;
                if (googleCounty != null) fieldSources.put("county", "Nominatim");
            }
        }

        boolean hasLocalZip = zipCode != null && !zipCode.isBlank();
        boolean zipMatch = hasLocalZip && resolvedZip != null && zipCode.trim().equals(resolvedZip.trim());
        boolean nameValid = bestSimilarity >= SIMILARITY_THRESHOLD;

        String displayName = buildDisplayName(canonicalName, googleCounty, googleProvince);

        return ValidationResult.builder()
                .found(true)
                .status(determineStatus(nameValid, hasLocalZip, zipMatch))
                .nameSimilarity(similarityPct)
                .zipCodeMatch(zipMatch)
                .nominatimName(canonicalName)
                .nominatimDisplayName(displayName)
                .nominatimType(detectTypeFromResults(items))
                .nominatimProvince(googleProvince)
                .nominatimCounty(googleCounty)
                .nominatimZipCode(resolvedZip)
                .localZipCode(zipCode)
                .lat(lat)
                .lon(lon)
                .source("Google Custom Search")
                .fieldSources(fieldSources)
                .build();
    }

    // ─── Google API calls ──────────────────────────────────────────────────────

    private List<Map<String, Object>> searchGoogle(String apiKey, String cx, String query) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(GOOGLE_API)
                    .queryParam("key", "{key}")
                    .queryParam("cx", "{cx}")
                    .queryParam("q", "{q}")
                    .queryParam("num", "5")
                    .queryParam("gl", "id")
                    .queryParam("lr", "lang_id")
                    .buildAndExpand(apiKey, cx, query)
                    .toUri();

            log.debug("[Google] search URI (key hidden): {}",
                    uri.toString().replaceAll("key=[^&]+", "key=***"));

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp.getBody() == null) return List.of();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items =
                    (List<Map<String, Object>>) resp.getBody().get("items");

            log.debug("[Google] {} result(s)", items != null ? items.size() : 0);
            return items != null ? items : List.of();

        } catch (Exception e) {
            log.warn("[Google] search error: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── Extraction helpers ────────────────────────────────────────────────────

    private String extractSimpleName(String title) {
        if (title == null) return null;
        // Google titles often: "Samonanggal, Pangaribuan, Tapanuli Utara - Wikipedia"
        String cleaned = title.replaceAll("\\s*[-–—]\\s*Wikipedia.*", "").trim();
        int comma = cleaned.indexOf(',');
        return comma >= 0 ? cleaned.substring(0, comma).trim() : cleaned.trim();
    }

    private String extractZipFromResults(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            String snippet = str(item, "snippet");
            if (snippet != null) {
                Matcher m = P_ZIP.matcher(snippet);
                while (m.find()) {
                    String candidate = m.group(1);
                    // Filter out years and obviously non-postal codes
                    if (!candidate.startsWith("19") && !candidate.startsWith("20")
                            && !candidate.equals("00000")) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private String extractProvince(List<Map<String, Object>> items) {
        // Common Indonesian province names that might appear in titles/snippets
        for (Map<String, Object> item : items) {
            String title = str(item, "title");
            String snippet = str(item, "snippet");
            String combined = (title != null ? title : "") + " " + (snippet != null ? snippet : "");

            // Look for "Provinsi X" pattern
            Matcher m = Pattern.compile("Provinsi\\s+([A-Z][a-zA-Z\\s]+?)(?:[,.]|\\s+(?:adalah|merupakan|terletak))")
                    .matcher(combined);
            if (m.find()) return m.group(1).trim();
        }
        return null;
    }

    private String extractCounty(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            String title = str(item, "title");
            if (title == null) continue;
            // Titles like "Samonanggal, Pangaribuan, Tapanuli Utara"
            String[] parts = title.replaceAll("\\s*[-–—]\\s*Wikipedia.*", "").split(",");
            if (parts.length >= 3) {
                return parts[2].trim().replaceAll("\\s*[-–].*", "").trim();
            }
        }
        return null;
    }

    private String detectTypeFromResults(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            String snippet = str(item, "snippet");
            if (snippet == null) continue;
            String lower = snippet.toLowerCase();
            if (lower.contains("kelurahan")) return "kelurahan";
            if (lower.contains("desa")) return "desa";
            if (lower.contains("kecamatan")) return "kecamatan";
        }
        return null;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private String buildQuery(String name, String district, String state, String province) {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" kelurahan desa");
        if (district != null && !district.isBlank()) sb.append(" ").append(district);
        if (state != null && !state.isBlank()) sb.append(" ").append(state);
        if (province != null && !province.isBlank()) sb.append(" ").append(province);
        sb.append(" Indonesia kode pos");
        return sb.toString();
    }

    private String buildNominatimQuery(String name, String district, String state, String province) {
        StringBuilder sb = new StringBuilder(name);
        if (district != null && !district.isBlank()) sb.append(", ").append(district);
        if (state != null && !state.isBlank()) sb.append(", ").append(state);
        if (province != null && !province.isBlank()) sb.append(", ").append(province);
        sb.append(", Indonesia");
        return sb.toString();
    }

    private String buildDisplayName(String name, String county, String province) {
        List<String> parts = new ArrayList<>();
        if (name != null) parts.add(name);
        if (county != null) parts.add(county);
        if (province != null) parts.add(province);
        parts.add("Indonesia");
        return String.join(", ", parts);
    }
}
