package com.template.usermanagement.wilayah;

import com.template.usermanagement.config.SystemSettingService;
import com.template.usermanagement.wilayah.dto.ValidationResult;
import com.template.usermanagement.wilayah.validation.GoogleSearchValidationProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleSearchValidationProvider")
class GoogleSearchValidationProviderTest {

    @Mock private SystemSettingService settingService;

    private GoogleSearchValidationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new GoogleSearchValidationProvider(settingService, new RestTemplateBuilder());
    }

    @Nested
    @DisplayName("isConfigured")
    class IsConfigured {

        @Test
        void returnsFalseWhenApiKeyMissing() {
            when(settingService.getValue("google.api.key")).thenReturn(null);
            when(settingService.getValue("google.api.cx")).thenReturn("cx123");

            assertThat(provider.isConfigured()).isFalse();
        }

        @Test
        void returnsFalseWhenCxMissing() {
            when(settingService.getValue("google.api.key")).thenReturn("key123");
            when(settingService.getValue("google.api.cx")).thenReturn(null);

            assertThat(provider.isConfigured()).isFalse();
        }

        @Test
        void returnsFalseWhenBothBlank() {
            when(settingService.getValue("google.api.key")).thenReturn("");
            when(settingService.getValue("google.api.cx")).thenReturn("  ");

            assertThat(provider.isConfigured()).isFalse();
        }

        @Test
        void returnsTrueWhenBothPresent() {
            when(settingService.getValue("google.api.key")).thenReturn("AIzaSyTest");
            when(settingService.getValue("google.api.cx")).thenReturn("abc123:xyz");

            assertThat(provider.isConfigured()).isTrue();
        }
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        void returnsNotFoundWhenNotConfigured() {
            when(settingService.getValue("google.api.key")).thenReturn(null);
            when(settingService.getValue("google.api.cx")).thenReturn(null);

            ValidationResult result = provider.validate("Test", null, null, null, null);

            assertThat(result.isFound()).isFalse();
            assertThat(result.getSource()).contains("not configured");
        }
    }

    @Test
    void providerNameIsGoogle() {
        assertThat(provider.getProviderName()).isEqualTo("google");
    }
}
