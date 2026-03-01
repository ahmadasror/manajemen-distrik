package com.template.usermanagement.config;

import com.template.usermanagement.common.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemSettingService")
class SystemSettingServiceTest {

    @Mock private SystemSettingRepository repository;

    private SystemSettingService service;

    @BeforeEach
    void setUp() {
        // 32-char key for AES-256
        service = new SystemSettingService(repository, "test-key-for-aes-encryption!1234");
    }

    // ─── fixtures ──────────────────────────────────────────────────────────────

    private SystemSetting setting(String key, String value, boolean secret) {
        return SystemSetting.builder()
                .settingKey(key)
                .settingValue(value)
                .isSecret(secret)
                .description("test")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── getValue ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getValue")
    class GetValue {

        @Test
        void returnsNullForMissingSetting() {
            when(repository.findById("missing")).thenReturn(Optional.empty());

            assertThat(service.getValue("missing")).isNull();
        }

        @Test
        void returnsNullForSettingWithNullValue() {
            when(repository.findById("key")).thenReturn(Optional.of(setting("key", null, false)));

            assertThat(service.getValue("key")).isNull();
        }

        @Test
        void returnsPlaintextForNonSecretSetting() {
            when(repository.findById("mode")).thenReturn(Optional.of(setting("mode", "free", false)));

            assertThat(service.getValue("mode")).isEqualTo("free");
        }

        @Test
        void decryptsSecretSettingValue() {
            // First encrypt a value via setValue
            SystemSetting s = setting("google.api.key", null, true);
            when(repository.findById("google.api.key")).thenReturn(Optional.of(s));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.setValue("google.api.key", "my-secret-key", "admin");

            // Capture the encrypted value
            ArgumentCaptor<SystemSetting> captor = ArgumentCaptor.forClass(SystemSetting.class);
            verify(repository).save(captor.capture());
            String encrypted = captor.getValue().getSettingValue();

            // Encrypted value should not equal plaintext
            assertThat(encrypted).isNotEqualTo("my-secret-key");
            assertThat(encrypted).isNotBlank();

            // Now test getValue decrypts it
            s.setSettingValue(encrypted);
            when(repository.findById("google.api.key")).thenReturn(Optional.of(s));

            assertThat(service.getValue("google.api.key")).isEqualTo("my-secret-key");
        }
    }

    // ─── setValue ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setValue")
    class SetValue {

        @Test
        void throwsWhenSettingNotFound() {
            when(repository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setValue("missing", "val", "admin"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void setsNullForBlankValue() {
            SystemSetting s = setting("mode", "free", false);
            when(repository.findById("mode")).thenReturn(Optional.of(s));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.setValue("mode", "", "admin");

            assertThat(s.getSettingValue()).isNull();
            assertThat(s.getUpdatedBy()).isEqualTo("admin");
        }

        @Test
        void storesPlaintextForNonSecret() {
            SystemSetting s = setting("mode", "free", false);
            when(repository.findById("mode")).thenReturn(Optional.of(s));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.setValue("mode", "paid", "admin");

            assertThat(s.getSettingValue()).isEqualTo("paid");
        }

        @Test
        void encryptsSecretValue() {
            SystemSetting s = setting("google.api.key", null, true);
            when(repository.findById("google.api.key")).thenReturn(Optional.of(s));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.setValue("google.api.key", "AIzaSyAbcdef", "admin");

            // Value should be encrypted (Base64), not plaintext
            assertThat(s.getSettingValue()).isNotEqualTo("AIzaSyAbcdef");
            assertThat(s.getSettingValue()).isNotBlank();
        }
    }

    // ─── getSettingsForDisplay ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getSettingsForDisplay")
    class GetSettingsForDisplay {

        @Test
        void masksSecretValues() {
            // Encrypt a value first
            SystemSetting s = setting("google.api.key", null, true);
            when(repository.findById("google.api.key")).thenReturn(Optional.of(s));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            service.setValue("google.api.key", "AIzaSyAbcdef1234", "admin");

            // Now test display
            when(repository.findAllById(anyList())).thenReturn(List.of(s));

            Map<String, String> display = service.getSettingsForDisplay(List.of("google.api.key"));

            String masked = display.get("google.api.key");
            assertThat(masked).startsWith("****");
            assertThat(masked).endsWith("1234");
            assertThat(masked).doesNotContain("AIzaSy");
        }

        @Test
        void showsPlaintextForNonSecret() {
            SystemSetting s = setting("validation.mode", "free", false);
            when(repository.findAllById(anyList())).thenReturn(List.of(s));

            Map<String, String> display = service.getSettingsForDisplay(List.of("validation.mode"));

            assertThat(display.get("validation.mode")).isEqualTo("free");
        }

        @Test
        void showsEmptyStringForNullValue() {
            SystemSetting s = setting("google.api.cx", null, false);
            when(repository.findAllById(anyList())).thenReturn(List.of(s));

            Map<String, String> display = service.getSettingsForDisplay(List.of("google.api.cx"));

            assertThat(display.get("google.api.cx")).isEmpty();
        }
    }
}
