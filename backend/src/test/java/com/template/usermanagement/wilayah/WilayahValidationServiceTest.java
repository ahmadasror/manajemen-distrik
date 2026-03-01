package com.template.usermanagement.wilayah;

import com.template.usermanagement.config.SystemSettingService;
import com.template.usermanagement.wilayah.dto.ValidationResult;
import com.template.usermanagement.wilayah.validation.GoogleSearchValidationProvider;
import com.template.usermanagement.wilayah.validation.WilayahValidationProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WilayahValidationService — chain logic")
class WilayahValidationServiceTest {

    @Mock private SystemSettingService settingService;
    @Mock private GoogleSearchValidationProvider googleProvider;

    // Two mock free providers
    @Mock private WilayahValidationProvider wikiProvider;
    @Mock private WilayahValidationProvider nominatimProvider;

    private WilayahValidationService service;

    private static final ValidationResult FOUND = ValidationResult.builder()
            .found(true).status("VALID").source("test").fieldSources(Map.of()).build();
    private static final ValidationResult NOT_FOUND = ValidationResult.builder()
            .found(false).status("INVALID").source("test").fieldSources(Map.of()).build();
    private static final ValidationResult GOOGLE_FOUND = ValidationResult.builder()
            .found(true).status("VALID").source("Google Custom Search").fieldSources(Map.of()).build();

    @BeforeEach
    void setUp() {
        lenient().when(wikiProvider.getProviderName()).thenReturn("wikipedia");
        lenient().when(nominatimProvider.getProviderName()).thenReturn("nominatim");
        lenient().when(googleProvider.getProviderName()).thenReturn("google");

        service = new WilayahValidationService(
                List.of(wikiProvider, nominatimProvider, googleProvider),
                googleProvider,
                settingService);
    }

    // ─── Free mode ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Free mode")
    class FreeMode {

        @BeforeEach
        void setFreeMode() {
            when(settingService.getValue("validation.mode")).thenReturn("free");
        }

        @Test
        void returnsWikipediaResultWhenFound() {
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(FOUND);

            ValidationResult result = service.validate("Test", null, null, null, null);

            assertThat(result.isFound()).isTrue();
            verify(nominatimProvider, never()).validate(any(), any(), any(), any(), any());
            verify(googleProvider, never()).validate(any(), any(), any(), any(), any());
        }

        @Test
        void fallsBackToNominatimWhenWikipediaNotFound() {
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(nominatimProvider.validate(any(), any(), any(), any(), any())).thenReturn(FOUND);

            ValidationResult result = service.validate("Test", null, null, null, null);

            assertThat(result.isFound()).isTrue();
            verify(wikiProvider).validate(any(), any(), any(), any(), any());
            verify(nominatimProvider).validate(any(), any(), any(), any(), any());
        }

        @Test
        void returnsNotFoundWhenBothFreeProvidersFail() {
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(nominatimProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);

            ValidationResult result = service.validate("Test", null, null, null, null);

            assertThat(result.isFound()).isFalse();
        }

        @Test
        void doesNotCallGoogleInFreeMode() {
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(nominatimProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);

            service.validate("Test", null, null, null, null);

            verify(googleProvider, never()).validate(any(), any(), any(), any(), any());
        }
    }

    // ─── Paid mode ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Paid mode")
    class PaidMode {

        @BeforeEach
        void setPaidMode() {
            when(settingService.getValue("validation.mode")).thenReturn("paid");
        }

        @Test
        void returnsWikipediaResultWithoutCallingGoogle() {
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(FOUND);

            ValidationResult result = service.validate("Test", null, null, null, null);

            assertThat(result.isFound()).isTrue();
            verify(googleProvider, never()).validate(any(), any(), any(), any(), any());
        }

        @Test
        void fallsBackToGoogleWhenFreeProvidersExhausted() {
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(nominatimProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(googleProvider.isConfigured()).thenReturn(true);
            when(googleProvider.validate(any(), any(), any(), any(), any())).thenReturn(GOOGLE_FOUND);

            ValidationResult result = service.validate("Test", null, null, null, null);

            assertThat(result.isFound()).isTrue();
            assertThat(result.getSource()).isEqualTo("Google Custom Search");
        }

        @Test
        void skipsGoogleWhenNotConfigured() {
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(nominatimProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(googleProvider.isConfigured()).thenReturn(false);

            ValidationResult result = service.validate("Test", null, null, null, null);

            assertThat(result.isFound()).isFalse();
            verify(googleProvider, never()).validate(any(), any(), any(), any(), any());
        }

        @Test
        void returnsNominatimResultWithoutCallingGoogle() {
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(nominatimProvider.validate(any(), any(), any(), any(), any())).thenReturn(FOUND);

            ValidationResult result = service.validate("Test", null, null, null, null);

            assertThat(result.isFound()).isTrue();
            verify(googleProvider, never()).validate(any(), any(), any(), any(), any());
        }
    }

    // ─── Default mode ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Default / null mode")
    class DefaultMode {

        @Test
        void defaultsToFreeWhenModeIsNull() {
            when(settingService.getValue("validation.mode")).thenReturn(null);
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(nominatimProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);

            service.validate("Test", null, null, null, null);

            // Google should NOT be called (free mode)
            verify(googleProvider, never()).validate(any(), any(), any(), any(), any());
        }

        @Test
        void defaultsToFreeWhenModeIsBlank() {
            when(settingService.getValue("validation.mode")).thenReturn("  ");
            when(wikiProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);
            when(nominatimProvider.validate(any(), any(), any(), any(), any())).thenReturn(NOT_FOUND);

            service.validate("Test", null, null, null, null);

            verify(googleProvider, never()).validate(any(), any(), any(), any(), any());
        }
    }

    // ─── getActiveMode ─────────────────────────────────────────────────────────

    @Test
    void getActiveModeReturnsFreeByDefault() {
        when(settingService.getValue("validation.mode")).thenReturn(null);
        assertThat(service.getActiveMode()).isEqualTo("free");
    }

    @Test
    void getActiveModeReturnsPaidWhenSet() {
        when(settingService.getValue("validation.mode")).thenReturn("paid");
        assertThat(service.getActiveMode()).isEqualTo("paid");
    }
}
