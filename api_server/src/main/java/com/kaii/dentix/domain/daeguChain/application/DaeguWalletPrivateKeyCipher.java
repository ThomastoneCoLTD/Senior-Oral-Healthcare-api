package com.kaii.dentix.domain.daeguChain.application;

import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class DaeguWalletPrivateKeyCipher {

    private static final String VERSION = "v1";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final DaeguChainProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String privateKey) {
        if (privateKey == null || privateKey.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(privateKey.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":"
                    + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (BadRequestApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestApiException("Unable to encrypt DaeguChain wallet private key");
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new BadRequestApiException("reward wallet signing key is not available");
        }
        try {
            String[] parts = ciphertext.split(":", 3);
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw new BadRequestApiException("Unsupported reward wallet signing key format");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BadRequestApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestApiException("Unable to decrypt DaeguChain wallet private key");
        }
    }

    private SecretKeySpec secretKey() {
        String encodedKey = properties.getWalletEncryptionKey();
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new BadRequestApiException("DaeguChain wallet encryption key is not configured");
        }
        byte[] key = Base64.getDecoder().decode(encodedKey.trim());
        if (key.length != 32) {
            throw new BadRequestApiException("DaeguChain wallet encryption key must be 32 bytes");
        }
        return new SecretKeySpec(key, "AES");
    }
}
