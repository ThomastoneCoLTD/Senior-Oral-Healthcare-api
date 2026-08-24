package com.kaii.dentix.domain.daeguChain.application;

import com.kaii.dentix.domain.daeguChain.config.DaeguChainProperties;
import com.kaii.dentix.global.common.error.exception.BadRequestApiException;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DaeguWalletPrivateKeyCipherTest {

    @Test
    void encryptsWithRandomIvAndDecryptsTheOriginalPrivateKey() {
        DaeguWalletPrivateKeyCipher cipher = cipherWithKey(randomKey());

        String first = cipher.encrypt("wallet-private-key");
        String second = cipher.encrypt("wallet-private-key");

        assertThat(first).startsWith("v1:").doesNotContain("wallet-private-key");
        assertThat(second).isNotEqualTo(first);
        assertThat(cipher.decrypt(first)).isEqualTo("wallet-private-key");
        assertThat(cipher.decrypt(second)).isEqualTo("wallet-private-key");
    }

    @Test
    void rejectsCiphertextWhenTheEncryptionKeyDoesNotMatch() {
        String ciphertext = cipherWithKey(randomKey()).encrypt("wallet-private-key");

        assertThatThrownBy(() -> cipherWithKey(randomKey()).decrypt(ciphertext))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("Unable to decrypt DaeguChain wallet private key");
    }

    @Test
    void requiresA32ByteBase64EncryptionKey() {
        assertThatThrownBy(() -> cipherWithKey(Base64.getEncoder().encodeToString(new byte[16]))
                .encrypt("wallet-private-key"))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessage("DaeguChain wallet encryption key must be 32 bytes");
    }

    private DaeguWalletPrivateKeyCipher cipherWithKey(String encodedKey) {
        DaeguChainProperties properties = new DaeguChainProperties();
        properties.setWalletEncryptionKey(encodedKey);
        return new DaeguWalletPrivateKeyCipher(properties);
    }

    private String randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
