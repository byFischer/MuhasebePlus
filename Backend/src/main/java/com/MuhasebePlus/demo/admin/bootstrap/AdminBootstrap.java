package com.MuhasebePlus.demo.admin.bootstrap;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.user.entity.User;
import com.MuhasebePlus.demo.user.entity.UserRole;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * İlk yönetici hesabını oluşturur.
 *
 * ADMIN_EMAIL ve ADMIN_PASSWORD ortam değişkenleri tanımlıysa ve sistemde hiç
 * ADMIN rolünde kullanıcı yoksa, açılışta bu bilgilerle bir yönetici hesabı
 * oluşturulur. Değişkenler tanımlı değilse hiçbir şey yapmaz; sonraki
 * açılışlarda ADMIN zaten var olduğu için tekrar çalışmaz.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.debug("Admin bootstrap atlandı: ADMIN_EMAIL/ADMIN_PASSWORD tanımlı değil");
            return;
        }
        if (userRepository.countByRole(UserRole.ADMIN) > 0) {
            log.debug("Admin bootstrap atlandı: sistemde zaten ADMIN kullanıcı var");
            return;
        }
        if (userRepository.existsByEmail(adminEmail.trim())) {
            log.warn("Admin bootstrap atlandı: {} e-postası mevcut bir kullanıcıya ait. "
                    + "Güvenlik gereği mevcut hesap otomatik yükseltilmez; farklı bir ADMIN_EMAIL kullanın.",
                    adminEmail.trim());
            return;
        }
        if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Admin bootstrap atlandı: ADMIN_PASSWORD en az {} karakter olmalıdır", MIN_PASSWORD_LENGTH);
            return;
        }

        Company company = Company.builder()
                .companyName("Sistem Yönetimi")
                .isActive(true)
                .build();
        company = companyRepository.save(company);

        User admin = new User();
        admin.setCompany(company);
        admin.setFirstName("Sistem");
        admin.setLastName("Yöneticisi");
        admin.setEmail(adminEmail.trim());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin.setLocked(false);
        admin.setFailedLoginAttempts(0);
        userRepository.save(admin);

        log.info("İlk yönetici hesabı oluşturuldu email={}", admin.getEmail());
    }
}
