package com.MuhasebePlus.demo.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class CryptoServiceTest {

    // 32 sıfır-byte'ın Base64 kodlaması (test için geçerli 256-bit anahtar)
    private static final String VALID_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService();
        ReflectionTestUtils.setField(cryptoService, "encodedKey", VALID_KEY);
        ReflectionTestUtils.invokeMethod(cryptoService, "init");
    }

    // ── init ──────────────────────────────────────────────────────────────────

    @Test
    void init_withInvalidKeyLength_throwsIllegalStateException() {
        CryptoService badService = new CryptoService();
        // 16 byte (128-bit) key → geçersiz, 32 byte gerekiyor
        ReflectionTestUtils.setField(badService, "encodedKey", "AAAAAAAAAAAAAAAAAAAAAA==");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(badService, "init"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32-byte");
    }

    // ── encrypt / decrypt roundtrip ───────────────────────────────────────────

    @Test
    void encryptThenDecrypt_returnsOriginalPlaintext() {
        String plaintext = "Merhaba, Dünya!";

        String encrypted = cryptoService.encrypt(plaintext);
        String decrypted = cryptoService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptThenDecrypt_worksWithEmptyString() {
        String plaintext = "";

        String encrypted = cryptoService.encrypt(plaintext);
        String decrypted = cryptoService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptThenDecrypt_worksWithSpecialCharacters() {
        String plaintext = "Test@123!#$%^&*()_+-=[]{}|;':\",./<>?";

        String encrypted = cryptoService.encrypt(plaintext);
        String decrypted = cryptoService.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptTwice_producesDifferentCiphertexts() {
        // Random IV nedeniyle aynı plaintext farklı ciphertext üretmeli
        String plaintext = "aynı metin";

        String enc1 = cryptoService.encrypt(plaintext);
        String enc2 = cryptoService.encrypt(plaintext);

        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    void decrypt_withTamperedCiphertext_throwsIllegalStateException() {
        String encrypted = cryptoService.encrypt("orijinal metin");
        // Ciphertext'i boz
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "XXXX";

        assertThatThrownBy(() -> cryptoService.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Decryption failed");
    }

    @Test
    void decrypt_withRandomBase64String_throwsIllegalStateException() {
        assertThatThrownBy(() -> cryptoService.decrypt("cmFuZG9tLWdhcmJhZ2U="))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void encrypt_returnsBase64EncodedString() {
        String encrypted = cryptoService.encrypt("test");

        // Sonuç geçerli Base64 olmalı (decoding hata vermemeli)
        assertThatNoException().isThrownBy(() ->
                java.util.Base64.getDecoder().decode(encrypted));
    }
}
