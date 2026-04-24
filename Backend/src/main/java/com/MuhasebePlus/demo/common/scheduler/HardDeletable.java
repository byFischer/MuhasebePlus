package com.MuhasebePlus.demo.common.scheduler;

import java.time.LocalDateTime;

public interface HardDeletable {
    int hardDeleteExpired(LocalDateTime cutoff);
}
