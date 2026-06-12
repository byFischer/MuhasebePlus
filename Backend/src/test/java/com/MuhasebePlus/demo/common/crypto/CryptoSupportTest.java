package com.MuhasebePlus.demo.common.crypto;

import com.MuhasebePlus.demo.invoice.repository.EInvoiceProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoSupportTest {

    @Mock private CryptoService cryptoService;
    @Mock private EInvoiceProfileRepository profileRepository;

    @Test
    void encryptedStringConverter_encryptsPlainValuesAndKeepsNullOrAlreadyEncryptedValues() {
        EncryptedStringConverter converter = new EncryptedStringConverter();
        converter.setCryptoService(cryptoService);
        when(cryptoService.encrypt("secret")).thenReturn("cipher");

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn("ENC:already")).isEqualTo("ENC:already");
        assertThat(converter.convertToDatabaseColumn("secret")).isEqualTo("ENC:cipher");

        verify(cryptoService).encrypt("secret");
    }

    @Test
    void encryptedStringConverter_decryptsEncryptedValuesAndKeepsPlaintextForBackwardCompatibility() {
        EncryptedStringConverter converter = new EncryptedStringConverter();
        converter.setCryptoService(cryptoService);
        when(cryptoService.decrypt("cipher")).thenReturn("secret");

        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("legacy-plain")).isEqualTo("legacy-plain");
        assertThat(converter.convertToEntityAttribute("ENC:cipher")).isEqualTo("secret");

        verify(cryptoService).decrypt("cipher");
    }

    @Test
    void migrationRunner_encryptsEveryUnencryptedPasswordWithPrefix() {
        when(profileRepository.findUnencryptedPasswords()).thenReturn(List.of(
                new Object[]{1L, "first"},
                new Object[]{2, "second"}));
        when(cryptoService.encrypt("first")).thenReturn("cipher-1");
        when(cryptoService.encrypt("second")).thenReturn("cipher-2");
        EInvoicePasswordMigrationRunner runner = new EInvoicePasswordMigrationRunner(profileRepository, cryptoService);

        runner.run(null);

        verify(profileRepository).updateGibPasswordRaw(1L, "ENC:cipher-1");
        verify(profileRepository).updateGibPasswordRaw(2L, "ENC:cipher-2");
    }

    @Test
    void migrationRunner_whenNoPlainPasswords_doesNotUpdate() {
        when(profileRepository.findUnencryptedPasswords()).thenReturn(List.of());
        EInvoicePasswordMigrationRunner runner = new EInvoicePasswordMigrationRunner(profileRepository, cryptoService);

        runner.run(null);

        verify(profileRepository, never()).updateGibPasswordRaw(org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyString());
    }
}
