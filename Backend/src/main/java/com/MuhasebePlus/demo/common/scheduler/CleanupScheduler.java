package com.MuhasebePlus.demo.common.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CleanupScheduler {

    private static final int HARD_DELETE_AFTER_DAYS = 30;

    private final List<HardDeletable> hardDeletables;

    @Scheduled(cron = "0 0 2 * * *")
    public void runCleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(HARD_DELETE_AFTER_DAYS);
        log.info("Hard delete cleanup started. Cutoff: {}", cutoff);

        hardDeletables.forEach(service -> {
            int count = service.hardDeleteExpired(cutoff);
            if (count > 0) {
                log.info("Hard deleted {} record(s) from {}", count, service.getClass().getSimpleName());
            }
        });

        log.info("Hard delete cleanup finished.");
    }
}
