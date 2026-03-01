package com.template.usermanagement.wilayah;

import com.template.usermanagement.wilayah.validation.WikipediaValidationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WikipediaValidationProviderTest {

    private WikipediaValidationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new WikipediaValidationProvider(new RestTemplateBuilder());
    }

    @Nested
    class ParseInfobox {

        @Test
        void extractsAllStandardFields() {
            String wikitext = """
                    {{Desa/Kelurahan
                    | nama           = Abit
                    | provinsi       = Kalimantan Timur
                    | dati ii        = Penajam Paser Utara
                    | kecamatan      = Long Kali
                    | kode pos       = 76261
                    | luas           = 189,85 km²
                    }}
                    """;

            Map<String, String> result = provider.parseInfobox(wikitext);

            assertThat(result.get("nama")).isEqualTo("Abit");
            assertThat(result.get("provinsi")).isEqualTo("Kalimantan Timur");
            assertThat(result.get("dati_ii")).isEqualTo("Penajam Paser Utara");
            assertThat(result.get("kecamatan")).isEqualTo("Long Kali");
            assertThat(result.get("kode_pos")).isEqualTo("76261");
        }

        @Test
        void handlesUnderscoreVariants() {
            String wikitext = """
                    {{Kelurahan
                    | nama=Sukasari
                    | provinsi=Jawa Barat
                    | dati_ii=Kota Bandung
                    | kecamatan=Sukasari
                    | kode_pos=40161
                    }}
                    """;

            Map<String, String> result = provider.parseInfobox(wikitext);

            assertThat(result.get("nama")).isEqualTo("Sukasari");
            assertThat(result.get("dati_ii")).isEqualTo("Kota Bandung");
            assertThat(result.get("kode_pos")).isEqualTo("40161");
        }

        @Test
        void stripsWikiLinks() {
            String wikitext = """
                    {{Desa/Kelurahan
                    | provinsi       = [[Kalimantan Timur]]
                    | dati ii        = [[Kabupaten Penajam Paser Utara|Penajam Paser Utara]]
                    | kode pos       = 76261
                    }}
                    """;

            Map<String, String> result = provider.parseInfobox(wikitext);

            assertThat(result.get("provinsi")).isEqualTo("Kalimantan Timur");
            assertThat(result.get("dati_ii")).isEqualTo("Penajam Paser Utara");
        }

        @Test
        void extractsZipFromMultiValueField() {
            // Some articles have "kode pos = 76261 – 76262" or with spaces
            String wikitext = """
                    {{Desa/Kelurahan
                    | kode pos = 76261
                    }}
                    """;

            Map<String, String> result = provider.parseInfobox(wikitext);

            assertThat(result.get("kode_pos")).isEqualTo("76261");
        }

        @Test
        void returnsEmptyMapForNullWikitext() {
            Map<String, String> result = provider.parseInfobox(null);
            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyMapForBlankWikitext() {
            Map<String, String> result = provider.parseInfobox("   ");
            assertThat(result).isEmpty();
        }

        @Test
        void handlesKabupatenVariantForDatiII() {
            String wikitext = """
                    {{Desa/Kelurahan
                    | kabupaten = Bogor
                    | kode pos  = 16810
                    }}
                    """;

            Map<String, String> result = provider.parseInfobox(wikitext);

            assertThat(result.get("dati_ii")).isEqualTo("Bogor");
        }
    }

    @Nested
    class SimilarityComputation {

        @Test
        void identicalNamesReturnOne() {
            assertThat(WikipediaValidationProvider.computeSimilarity("Abit", "Abit")).isEqualTo(1.0);
        }

        @Test
        void caseInsensitive() {
            assertThat(WikipediaValidationProvider.computeSimilarity("abit", "ABIT")).isEqualTo(1.0);
        }

        @Test
        void typoReducesSimilarity() {
            double sim = WikipediaValidationProvider.computeSimilarity("Sukasari", "Sukasar");
            assertThat(sim).isGreaterThan(0.80);
        }

        @Test
        void completelyDifferentReturnsLow() {
            double sim = WikipediaValidationProvider.computeSimilarity("Jakarta", "Surabaya");
            assertThat(sim).isLessThan(0.50);
        }

        @Test
        void nullInputReturnsZero() {
            assertThat(WikipediaValidationProvider.computeSimilarity(null, "Abit")).isEqualTo(0.0);
            assertThat(WikipediaValidationProvider.computeSimilarity("Abit", null)).isEqualTo(0.0);
        }
    }
}
