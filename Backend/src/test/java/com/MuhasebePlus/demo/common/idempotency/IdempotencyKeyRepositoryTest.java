package com.MuhasebePlus.demo.common.idempotency;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class IdempotencyKeyRepositoryTest {

    @Autowired IdempotencyKeyRepository repository;

    // ── deleteExpired custom JPQL query ───────────────────────────────────────

    @Test
    void deleteExpired_removesExpiredKeys_keepsActive() {
        LocalDateTime now = LocalDateTime.now();

        repository.save(IdempotencyKey.builder()
                .key("expired-1").responseBody("{}").responseStatus(201)
                .createdAt(now.minusHours(25)).expiresAt(now.minusHours(1)).build());

        repository.save(IdempotencyKey.builder()
                .key("expired-2").responseBody("{}").responseStatus(200)
                .createdAt(now.minusHours(26)).expiresAt(now.minusMinutes(30)).build());

        repository.save(IdempotencyKey.builder()
                .key("active-1").responseBody("{}").responseStatus(200)
                .createdAt(now.minusHours(1)).expiresAt(now.plusHours(23)).build());

        int deleted = repository.deleteExpired(now);

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findById("expired-1")).isEmpty();
        assertThat(repository.findById("expired-2")).isEmpty();
        assertThat(repository.findById("active-1")).isPresent();
    }

    @Test
    void deleteExpired_whenNoExpiredKeys_returnsZero() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(IdempotencyKey.builder()
                .key("still-valid").responseBody("{}").responseStatus(200)
                .createdAt(now.minusHours(1)).expiresAt(now.plusHours(23)).build());

        assertThat(repository.deleteExpired(now)).isEqualTo(0);
    }

    @Test
    void deleteExpired_whenRepositoryEmpty_returnsZero() {
        assertThat(repository.deleteExpired(LocalDateTime.now())).isEqualTo(0);
    }
}
