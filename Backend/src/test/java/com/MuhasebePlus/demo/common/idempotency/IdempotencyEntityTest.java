package com.MuhasebePlus.demo.common.idempotency;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyEntityTest {

    @Test
    void idempotencyKey_builderAndAllArgsConstructorExposeValues() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 12, 10, 0);
        LocalDateTime expiresAt = createdAt.plusHours(24);

        IdempotencyKey built = IdempotencyKey.builder()
                .key("key-1")
                .responseBody("{\"ok\":true}")
                .responseStatus(201)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
        IdempotencyKey allArgs = new IdempotencyKey("key-2", "{}", 200, createdAt, expiresAt);
        IdempotencyKey empty = new IdempotencyKey();

        assertThat(built.getKey()).isEqualTo("key-1");
        assertThat(built.getResponseBody()).isEqualTo("{\"ok\":true}");
        assertThat(built.getResponseStatus()).isEqualTo(201);
        assertThat(built.getCreatedAt()).isEqualTo(createdAt);
        assertThat(built.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(allArgs.getKey()).isEqualTo("key-2");
        assertThat(empty.getKey()).isNull();
    }
}
