package com.MuhasebePlus.demo.common.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(5)
public class IdempotencyFilter extends OncePerRequestFilter {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyService idempotencyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!HttpMethod.POST.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (key == null || key.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyKey> cached = idempotencyService.findByKey(key);

        if (cached.isPresent()) {
            log.debug("Idempotency cache hit for key: {}", key);
            IdempotencyKey hit = cached.get();
            response.setStatus(hit.getResponseStatus());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(hit.getResponseBody());
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        // Only cache successful responses (2xx)
        if (responseWrapper.getStatus() >= 200 && responseWrapper.getStatus() < 300) {
            idempotencyService.saveResponse(key, responseBody, responseWrapper.getStatus());
        }

        responseWrapper.copyBodyToResponse();
    }
}
