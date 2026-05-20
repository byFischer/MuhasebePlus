package com.MuhasebePlus.demo.common.idempotency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository repository;

    @InjectMocks
    private IdempotencyService service;

    // ── findByKey ─────────────────────────────────────────────────────────────

    @Test
    void findByKey_whenKeyNotFound_returnsEmpty() {
        when(repository.findById("key-1")).thenReturn(Optional.empty());

        assertThat(service.findByKey("key-1")).isEmpty();
    }

    @Test
    void findByKey_whenKeyFoundAndNotExpired_returnsKey() {
        IdempotencyKey key = IdempotencyKey.builder()
                .key("key-1")
                .responseBody("{}")
                .responseStatus(200)
                .createdAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusHours(23))
                .build();
        when(repository.findById("key-1")).thenReturn(Optional.of(key));

        Optional<IdempotencyKey> result = service.findByKey("key-1");

        assertThat(result).isPresent();
        assertThat(result.get().getKey()).isEqualTo("key-1");
        assertThat(result.get().getResponseStatus()).isEqualTo(200);
    }

    @Test
    void findByKey_whenKeyFoundButExpired_returnsEmpty() {
        IdempotencyKey expiredKey = IdempotencyKey.builder()
                .key("key-old")
                .responseBody("{}")
                .responseStatus(201)
                .createdAt(LocalDateTime.now().minusHours(25))
                .expiresAt(LocalDateTime.now().minusHours(1)) // geçmişte
                .build();
        when(repository.findById("key-old")).thenReturn(Optional.of(expiredKey));

        assertThat(service.findByKey("key-old")).isEmpty();
    }

    @Test
    void findByKey_whenExpiresAtIsExactlyNow_treatedAsExpired() {
        // isAfter(now) → eşit olan an "geçmiş" sayılır
        IdempotencyKey key = IdempotencyKey.builder()
                .key("key-now")
                .responseBody("{}")
                .responseStatus(200)
                .createdAt(LocalDateTime.now().minusSeconds(1))
                .expiresAt(LocalDateTime.now().minusNanos(1))
                .build();
        when(repository.findById("key-now")).thenReturn(Optional.of(key));

        assertThat(service.findByKey("key-now")).isEmpty();
    }

    // ── saveResponse ──────────────────────────────────────────────────────────

    @Test
    void saveResponse_whenKeyDoesNotExist_savesRecord() {
        when(repository.existsById("new-key")).thenReturn(false);

        service.saveResponse("new-key", "{\"id\":1}", 201);

        verify(repository).save(argThat(k ->
                "new-key".equals(k.getKey()) &&
                k.getResponseStatus() == 201 &&
                "{\"id\":1}".equals(k.getResponseBody()) &&
                k.getExpiresAt().isAfter(LocalDateTime.now())
        ));
    }

    @Test
    void saveResponse_whenKeyAlreadyExists_skipsDoubleWrite() {
        when(repository.existsById("existing-key")).thenReturn(true);

        service.saveResponse("existing-key", "{}", 200);

        verify(repository, never()).save(any());
    }

    // ── hardDeleteExpired ─────────────────────────────────────────────────────

    @Test
    void hardDeleteExpired_callsRepositoryDeleteExpired() {
        when(repository.deleteExpired(any(LocalDateTime.class))).thenReturn(5);

        int deleted = service.hardDeleteExpired(LocalDateTime.now());

        assertThat(deleted).isEqualTo(5);
        verify(repository).deleteExpired(any(LocalDateTime.class));
    }
}
