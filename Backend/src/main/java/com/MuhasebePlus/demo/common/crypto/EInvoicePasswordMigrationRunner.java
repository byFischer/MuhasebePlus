package com.MuhasebePlus.demo.common.crypto;

import com.MuhasebePlus.demo.invoice.repository.EInvoiceProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EInvoicePasswordMigrationRunner implements ApplicationRunner {

    private final EInvoiceProfileRepository profileRepository;
    private final CryptoService cryptoService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Raw DB değerlerini oku — converter bypass edilir, sadece ENC: ile başlamayanlar gelir
        List<Object[]> unencrypted = profileRepository.findUnencryptedPasswords();
        for (Object[] row : unencrypted) {
            Long profileId = ((Number) row[0]).longValue();
            String plainPassword = (String) row[1];
            profileRepository.updateGibPasswordRaw(
                profileId,
                EncryptedStringConverter.PREFIX + cryptoService.encrypt(plainPassword)
            );
        }
        if (!unencrypted.isEmpty()) {
            log.info("EInvoicePasswordMigrationRunner: encrypted {} profile password(s).", unencrypted.size());
        }
    }
}
