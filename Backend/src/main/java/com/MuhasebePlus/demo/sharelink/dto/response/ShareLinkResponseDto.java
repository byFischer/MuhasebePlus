package com.MuhasebePlus.demo.sharelink.dto.response;

import java.time.LocalDateTime;

public record ShareLinkResponseDto(
        boolean exists,
        String token,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastAccessedAt,
        long accessCount
) {
    public static ShareLinkResponseDto empty() {
        return new ShareLinkResponseDto(false, null, false, null, null, 0);
    }
}
