package com.template.usermanagement.config;

import com.template.usermanagement.common.ResourceNotFoundException;
import com.template.usermanagement.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing system settings stored in the database.
 * Secret values (like API keys) are encrypted with AES-GCM before storage.
 */
@Slf4j
@Service
public class SystemSettingService {

    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SystemSettingRepository repository;
    private final SecretKeySpec aesKey;

    public SystemSettingService(SystemSettingRepository repository,
                                @Value("${system.settings.encryption-key:0123456789abcdef0123456789abcdef}") String encryptionKey) {
        this.repository = repository;
        // Key must be 16, 24, or 32 bytes for AES
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 16) {
            byte[] padded = new byte[16];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        } else if (keyBytes.length > 32) {
            byte[] trimmed = new byte[32];
            System.arraycopy(keyBytes, 0, trimmed, 0, 32);
            keyBytes = trimmed;
        } else if (keyBytes.length > 16 && keyBytes.length < 24) {
            byte[] padded = new byte[24];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        } else if (keyBytes.length > 24 && keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Transactional(readOnly = true)
    public String getValue(String key) {
        SystemSetting setting = repository.findById(key).orElse(null);
        if (setting == null || setting.getSettingValue() == null) return null;
        return setting.isSecret() ? decrypt(setting.getSettingValue()) : setting.getSettingValue();
    }

    @Transactional(readOnly = true)
    public Map<String, String> getSettingsForDisplay(List<String> keys) {
        return repository.findAllById(keys).stream()
                .collect(Collectors.toMap(
                        SystemSetting::getSettingKey,
                        s -> {
                            if (s.getSettingValue() == null) return "";
                            if (s.isSecret()) return maskValue(decrypt(s.getSettingValue()));
                            return s.getSettingValue();
                        }
                ));
    }

    @Transactional
    public void setValue(String key, String value, String updatedBy) {
        SystemSetting setting = repository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found: " + key, ErrorCode.NOT_FOUND));
        String storedValue = (value == null || value.isBlank()) ? null
                : setting.isSecret() ? encrypt(value) : value;
        setting.setSettingValue(storedValue);
        setting.setUpdatedBy(updatedBy);
        repository.save(setting);
        log.info("[Settings] '{}' updated by {}", key, updatedBy);
    }

    // ─── Crypto ────────────────────────────────────────────────────────────────

    private String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            // Prepend IV to ciphertext
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt setting value", e);
        }
    }

    private String decrypt(String ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("[Settings] Failed to decrypt value — returning null");
            return null;
        }
    }

    private String maskValue(String value) {
        if (value == null || value.length() <= 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }
}
