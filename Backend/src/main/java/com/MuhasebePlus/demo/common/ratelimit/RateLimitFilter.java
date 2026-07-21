package com.MuhasebePlus.demo.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AUTH_LIMIT = 10;
    private static final int AUTH_WINDOW_MS = 60_000;
    private static final int GENERAL_LIMIT = 60;
    private static final int GENERAL_WINDOW_MS = 60_000;
    private static final int PUBLIC_LIMIT = 30;
    private static final int PUBLIC_WINDOW_MS = 60_000;

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> publicBuckets = new ConcurrentHashMap<>();
    private long lastCleanup = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        boolean isAuthPath = path.startsWith("/api/auth/") || path.equals("/api/users/register");
        boolean isPublicPath = path.startsWith("/api/public/");

        if (!isAuthPath && !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = isAuthPath ? AUTH_LIMIT : isPublicPath ? PUBLIC_LIMIT : GENERAL_LIMIT;
        int windowMs = isAuthPath ? AUTH_WINDOW_MS : isPublicPath ? PUBLIC_WINDOW_MS : GENERAL_WINDOW_MS;
        Map<String, Bucket> buckets = isAuthPath ? authBuckets : isPublicPath ? publicBuckets : generalBuckets;

        String key = isAuthPath ? "auth:" + clientIp : isPublicPath ? "public:" + clientIp : "general:" + clientIp;
        long now = System.currentTimeMillis();

        cleanupIfNeeded(now);

        Bucket bucket = buckets.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > windowMs) {
                return new Bucket(now, 1);
            }
            v.count++;
            return v;
        });

        if (bucket.count > limit) {
            log.warn("Rate limit exceeded for {} on path {} (count={})", clientIp, path, bucket.count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Cok fazla istek gonderdiniz. Lutfen bekleyip tekrar deneyin.\"}}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupIfNeeded(long now) {
        if (now - lastCleanup < 60_000) return;
        lastCleanup = now;
        long cutoff = now - Math.max(AUTH_WINDOW_MS, Math.max(GENERAL_WINDOW_MS, PUBLIC_WINDOW_MS));
        authBuckets.entrySet().removeIf(e -> e.getValue().windowStart < cutoff);
        generalBuckets.entrySet().removeIf(e -> e.getValue().windowStart < cutoff);
        publicBuckets.entrySet().removeIf(e -> e.getValue().windowStart < cutoff);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private static class Bucket {
        final long windowStart;
        int count;

        Bucket(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
