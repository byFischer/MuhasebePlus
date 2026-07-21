package com.MuhasebePlus.demo.common.idempotency;

import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService implements HardDeletable {

    private static final int TTL_HOURS = 24;

    private final IdempotencyKeyRepository repository;

    public Optional<IdempotencyKey> findByKey(String key) {
        return repository.findById(key)
                .filter(k -> k.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Transactional
    public void saveResponse(String key, String responseBody, int responseStatus) {
        // Skip saving if this key was already stored (race condition guard)
        if (repository.existsById(key)) {
            return;
        }
        IdempotencyKey record = IdempotencyKey.builder()
                .key(key)
                .responseBody(responseBody)
                .responseStatus(responseStatus)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(TTL_HOURS))
                .build();
        repository.save(record);
    }

    @Override
    @Transactional
    public int hardDeleteExpired(LocalDateTime cutoff) {
        return repository.deleteExpired(LocalDateTime.now());
    }
}
