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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// LinkedHashMap preserves insertion order in fieldSources for consistent JSON output

/**
 * Validation provider backed by the Indonesian Wikipedia (id.wikipedia.org).
 *
 * <p><b>Flow:</b>
 * <ol>
 *   <li>Search id.wikipedia.org for the region name using the MediaWiki
 *       {@code action=query&list=search} API.</li>
 *   <li>Pick the article whose title has the highest Levenshtein similarity
 *       against the input name.</li>
 *   <li>Fetch the article wikitext via {@code action=parse&prop=wikitext}.</li>
 *   <li>Parse the infobox (templates: {@code Desa/Kelurahan}, {@code Kelurahan},
 *       {@code Desa}) to extract: {@code kode pos}, {@code provinsi},
 *       {@code dati ii} / {@code kota}, {@code kecamatan}, {@code nama},
 *       {@code lat} / {@code lon}.</li>
 *   <li>Apply the same 80% name-similarity threshold and exact zip-code
 *       comparison used by all providers.</li>
 * </ol>
 *
 * <p>Wikipedia API is more permissive than Nominatim (no strict 1 req/s), but
 * a short pause is still applied between search and parse calls.
 *
 * <p>Activate via: {@code wilayah.validation.provider: wikipedia}
 */
@Slf4j
@Component
public class WikipediaValidationProvider extends AbstractValidationProvider {

    private static final String PROVIDER_NAME = "wikipedia";
    private static final String WIKI_API      = "https://id.wikipedia.org/w/api.php";
    private static final String USER_AGENT    = "ManajemenDistrik/1.0 (educational project)";

    // Infobox field patterns — capture full value up to end-of-line so that
    // wiki links like [[Foo|Bar]] are not prematurely truncated at the inner '|'.
    // The clean() method handles stripping of wiki markup afterwards.
    private static final Pattern P_KODE_POS   = Pattern.compile(
            "\\|\\s*kode[_ ]pos\\s*=\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_PROVINSI   = Pattern.compile(
            "\\|\\s*provinsi\\s*=\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    // Kab/Kota — two naming conventions used across Indonesian Wikipedia articles:
    //   1. {{Desa/Kelurahan}}: "| dati ii = ..." or "| dati_ii = ..."
    //   2. {{kelurahan}} / {{Desa}}: "| nama dati2 = ..." (actual name) + "| dati2 = Kota/Kabupaten" (type)
    // P_NAMA_DATI covers convention 2 (more specific — contains the real name).
    // P_DATI covers convention 1 and fallback type fields.
    private static final Pattern P_NAMA_DATI  = Pattern.compile(
            "\\|\\s*nama[_ ]dati\\s*(?:2|ii)\\s*=\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_DATI       = Pattern.compile(
            "\\|\\s*(?:dati[_ ]ii|dati2|kabupaten(?:[_ ]kota)?|kota)\\s*=\\s*([^\\n]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern P_KECAMATAN  = Pattern.compile(
            "\\|\\s*kecamatan\\s*=\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_NAMA       = Pattern.compile(
            "\\|\\s*nama\\s*=\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE);
    // Coordinates: "| latd = -1 | latm = 14..." or "| koordinat = {{coord|...}}"
    private static final Pattern P_LAT        = Pattern.compile(
            "\\|\\s*(?:lat_deg|latd|lat)\\s*=\\s*([\\-\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_LON        = Pattern.compile(
            "\\|\\s*(?:lon_deg|longd|lon|long)\\s*=\\s*([\\-\\d.]+)", Pattern.CASE_INSENSITIVE);
    // Strip wiki link markup: [[Some text|Display]] or [[Display]]
    private static final Pattern P_WIKILINK   = Pattern.compile("\\[\\[(?:[^|\\]]*\\|)?([^\\]]+)\\]\\]");
    // Strip templates like {{PAGENAME}}, {{small|...}}, etc.
    private static final Pattern P_TEMPLATE   = Pattern.compile("\\{\\{[^}]*\\}\\}");

    private final RestTemplate restTemplate;

    public WikipediaValidationProvider(RestTemplateBuilder builder) {
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
        String query = buildSearchQuery(name, districtName, stateName, provinceName);
        log.debug("[Wikipedia] search query: {}", query);

        List<Map<String, Object>> searchHits = search(query);
        if (searchHits.isEmpty()) {
            return ValidationResult.builder()
                    .found(false).status("INVALID")
                    .localZipCode(zipCode)
                    .source("Wikipedia (id.wikipedia.org)")
                    .fieldSources(Map.of())
                    .build();
        }

        // Pick the article with the best title similarity
        Map<String, Object> best   = findBestByTitle(name, searchHits);
        String articleTitle        = str(best, "title");
        double titleSimilarity     = computeSimilarity(name, extractSimpleName(articleTitle));
        log.debug("[Wikipedia] best article: '{}', similarity: {}", articleTitle, titleSimilarity);

        // Fetch wikitext to extract infobox data
        String wikitext = getWikitext(articleTitle);
        Map<String, String> infobox = wikitext != null ? parseInfobox(wikitext) : Map.of();

        // Resolve the canonical name: infobox "nama" field, or article title simple part
        String canonicalName = infobox.containsKey("nama")
                ? infobox.get("nama")
                : extractSimpleName(articleTitle);

        // Use the better of the two similarity scores
        double similarity = computeSimilarity(name, canonicalName);
        if (titleSimilarity > similarity) {
            similarity    = titleSimilarity;
            canonicalName = extractSimpleName(articleTitle);
        }
        int similarityPct = (int) (similarity * 100);

        String wikiProvince  = trimOrNull(infobox.get("provinsi"));
        String wikiCounty    = trimOrNull(infobox.get("dati_ii"));
        String wikiKecamatan = trimOrNull(infobox.get("kecamatan"));
        Double lat           = parseDouble(infobox.get("lat"));
        Double lon           = parseDouble(infobox.get("lon"));

        // ── Zip code resolution: Wikipedia infobox first, Nominatim fallback ──
        Map<String, String> fieldSources = new LinkedHashMap<>();
        fieldSources.put("name", "Wikipedia");
        if (wikiProvince  != null) fieldSources.put("province",    "Wikipedia");
        if (wikiCounty    != null) fieldSources.put("county",      "Wikipedia");
        if (wikiKecamatan != null) fieldSources.put("district",    "Wikipedia");
        if (lat != null || lon != null) fieldSources.put("coordinates", "Wikipedia");

        String resolvedZip   = trimOrNull(infobox.get("kode_pos"));
        String zipSourceLabel;

        if (resolvedZip != null) {
            zipSourceLabel = "Wikipedia";
            log.debug("[Wikipedia] zip from infobox: {}", resolvedZip);
        } else {
            // Fallback: search Nominatim for the same query and get calculated_postcode
            log.debug("[Wikipedia] no zip in infobox — falling back to Nominatim for postcode");
            String nominatimQuery = buildNominatimQuery(name, districtName, stateName, provinceName);
            List<Map<String, Object>> nomResults = searchNominatim(restTemplate, nominatimQuery, 3);
            if (!nomResults.isEmpty()) {
                Map<String, Object> nomBest = nominatimBestMatch(name, nomResults);
                String placeId = String.valueOf(nomBest.get("place_id"));
                resolvedZip = getNominatimPostcode(restTemplate, placeId, "Wikipedia→Nominatim");
                zipSourceLabel = resolvedZip != null ? "Nominatim (fallback)" : null;
                log.debug("[Wikipedia] Nominatim fallback zip: {}", resolvedZip);
            } else {
                zipSourceLabel = null;
            }
        }

        if (zipSourceLabel != null) fieldSources.put("zipCode", zipSourceLabel);

        boolean hasLocalZip = zipCode != null && !zipCode.isBlank();
        boolean zipMatch    = hasLocalZip && resolvedZip != null && zipCode.trim().equals(resolvedZip);
        boolean nameValid   = similarity >= SIMILARITY_THRESHOLD;

        String displayName = buildDisplayName(canonicalName, wikiKecamatan, wikiCounty, wikiProvince, articleTitle);

        return ValidationResult.builder()
                .found(true)
                .status(determineStatus(nameValid, hasLocalZip, zipMatch))
                .nameSimilarity(similarityPct)
                .zipCodeMatch(zipMatch)
                .nominatimName(canonicalName)
                .nominatimDisplayName(displayName)
                .nominatimType(detectType(wikitext))
                .nominatimProvince(wikiProvince)
                .nominatimCounty(wikiCounty)
                .nominatimZipCode(resolvedZip)
                .localZipCode(zipCode)
                .lat(lat)
                .lon(lon)
                .source("Wikipedia (id.wikipedia.org)")
                .fieldSources(fieldSources)
                .build();
    }

    // ─── Wikipedia API calls ──────────────────────────────────────────────────

    private List<Map<String, Object>> search(String query) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(WIKI_API)
                    .queryParam("action", "query")
                    .queryParam("list", "search")
                    .queryParam("srsearch", "{q}")
                    .queryParam("format", "json")
                    .queryParam("srlimit", "5")
                    .queryParam("srprop", "snippet")
                    .buildAndExpand(query)
                    .toUri();

            log.debug("[Wikipedia] search URI: {}", uri);
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp.getBody() == null) return List.of();

            @SuppressWarnings("unchecked")
            Map<String, Object> queryNode = (Map<String, Object>) resp.getBody().get("query");
            if (queryNode == null) return List.of();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> hits = (List<Map<String, Object>>) queryNode.get("search");
            log.debug("[Wikipedia] {} hit(s)", hits != null ? hits.size() : 0);
            return hits != null ? hits : List.of();

        } catch (Exception e) {
            log.warn("[Wikipedia] search error: {}", e.getMessage());
            return List.of();
        }
    }

    /** Returns raw wikitext string for the given article title, or null on error. */
    private String getWikitext(String title) {
        try {
            // Short pause to be respectful to Wikipedia servers
            Thread.sleep(300);

            URI uri = UriComponentsBuilder.fromHttpUrl(WIKI_API)
                    .queryParam("action", "parse")
                    .queryParam("page", "{title}")
                    .queryParam("prop", "wikitext")
                    .queryParam("format", "json")
                    .buildAndExpand(title)
                    .toUri();

            log.debug("[Wikipedia] parse URI: {}", uri);
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp.getBody() == null) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> parseNode = (Map<String, Object>) resp.getBody().get("parse");
            if (parseNode == null) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> wikitextNode = (Map<String, Object>) parseNode.get("wikitext");
            if (wikitextNode == null) return null;

            Object raw = wikitextNode.get("*");
            return raw != null ? raw.toString() : null;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("[Wikipedia] parse error for '{}': {}", title, e.getMessage());
            return null;
        }
    }

    // ─── Infobox parsing ──────────────────────────────────────────────────────

    /**
     * Extracts key fields from Indonesian Wikipedia village/kelurahan infoboxes.
     *
     * <p>Handles templates: {@code {{Desa/Kelurahan}}, {{Kelurahan}}, {{Desa}}}.
     * Returns a map with keys: {@code nama, kode_pos, provinsi, dati_ii,
     * kecamatan, lat, lon}.
     */
    public Map<String, String> parseInfobox(String wikitext) {
        Map<String, String> result = new LinkedHashMap<>();
        if (wikitext == null || wikitext.isBlank()) return result;

        extractField(P_NAMA,      wikitext).ifPresent(v -> result.put("nama",      clean(v)));
        extractField(P_KODE_POS,  wikitext).ifPresent(v -> result.put("kode_pos",  cleanZip(v)));
        extractField(P_PROVINSI,  wikitext).ifPresent(v -> result.put("provinsi",  clean(v)));
        // "nama dati2" (e.g. {{kelurahan}} template) takes priority over generic "dati ii"
        extractField(P_NAMA_DATI, wikitext)
                .or(() -> extractField(P_DATI, wikitext))
                .ifPresent(v -> result.put("dati_ii", clean(v)));
        extractField(P_KECAMATAN, wikitext).ifPresent(v -> result.put("kecamatan", clean(v)));
        extractField(P_LAT,       wikitext).ifPresent(v -> result.put("lat",       v.trim()));
        extractField(P_LON,       wikitext).ifPresent(v -> result.put("lon",       v.trim()));

        log.debug("[Wikipedia] infobox fields: {}", result);
        return result;
    }

    private Optional<String> extractField(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? Optional.ofNullable(m.group(1)) : Optional.empty();
    }

    /** Strips wiki markup (links, templates, ref tags, HTML) and trims. */
    private String clean(String raw) {
        if (raw == null) return null;
        String s = raw;
        // Remove <ref>...</ref> blocks
        s = s.replaceAll("<ref[^>]*>.*?</ref>", "");
        s = s.replaceAll("<ref[^/]*/?>", "");
        // Unwrap [[link|display]] or [[display]]
        s = P_WIKILINK.matcher(s).replaceAll("$1");
        // Remove remaining templates {{...}}
        s = P_TEMPLATE.matcher(s).replaceAll("");
        // Remove HTML tags
        s = s.replaceAll("<[^>]+>", "");
        return s.trim();
    }

    /** Like clean(), but also extracts just the first 5-digit zip code found. */
    private String cleanZip(String raw) {
        String cleaned = clean(raw);
        if (cleaned == null) return null;
        Matcher m = Pattern.compile("\\b(\\d{5})\\b").matcher(cleaned);
        return m.find() ? m.group(1) : cleaned.trim();
    }

    /** Detects the region type from the infobox template name. */
    private String detectType(String wikitext) {
        if (wikitext == null) return null;
        if (wikitext.contains("Desa/Kelurahan")) return "village/kelurahan";
        if (wikitext.contains("Kelurahan"))      return "kelurahan";
        if (wikitext.contains("{{Desa"))         return "desa";
        if (wikitext.contains("Kecamatan"))      return "kecamatan";
        if (wikitext.contains("Kabupaten"))      return "kabupaten";
        if (wikitext.contains("Kota"))           return "kota";
        return null;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String buildSearchQuery(String name, String district, String state, String province) {
        StringBuilder sb = new StringBuilder(name);
        if (district != null && !district.isBlank()) sb.append(" ").append(district);
        if (state    != null && !state.isBlank())    sb.append(" ").append(state);
        if (province != null && !province.isBlank()) sb.append(" ").append(province);
        return sb.toString();
    }

    private String buildNominatimQuery(String name, String district, String state, String province) {
        StringBuilder sb = new StringBuilder(name);
        if (district != null && !district.isBlank()) sb.append(", ").append(district);
        if (state    != null && !state.isBlank())    sb.append(", ").append(state);
        if (province != null && !province.isBlank()) sb.append(", ").append(province);
        sb.append(", Indonesia");
        return sb.toString();
    }

    /**
     * Picks the search hit whose title simple-name (before the first comma)
     * has the highest Levenshtein similarity against the input name.
     */
    private Map<String, Object> findBestByTitle(String target, List<Map<String, Object>> hits) {
        return hits.stream()
                .max(Comparator.comparingDouble(h -> computeSimilarity(target, extractSimpleName(str(h, "title")))))
                .orElse(hits.get(0));
    }

    /**
     * Extracts the simple name from an article title like "Abit, Penajam Paser Utara"
     * → "Abit".
     */
    private String extractSimpleName(String title) {
        if (title == null) return null;
        int comma = title.indexOf(',');
        return comma >= 0 ? title.substring(0, comma).trim() : title.trim();
    }

    private String buildDisplayName(String name, String kecamatan, String county, String province, String fallback) {
        List<String> parts = new ArrayList<>();
        if (name      != null) parts.add(name);
        if (kecamatan != null) parts.add(kecamatan);
        if (county    != null) parts.add(county);
        if (province  != null) parts.add(province);
        parts.add("Indonesia");
        return parts.isEmpty() ? fallback : String.join(", ", parts);
    }
}
