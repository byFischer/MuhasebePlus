package com.MuhasebePlus.demo.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;

class RateLimitFilterTest {

    private static final int AUTH_LIMIT = 10;
    private static final int PUBLIC_LIMIT = 30;
    private static final int GENERAL_LIMIT = 60;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    // ── Limit altı istekler ───────────────────────────────────────────────────

    @Test
    void doFilter_whenAuthRequestsUnderLimit_passesAllThrough() throws Exception {
        for (int i = 0; i < AUTH_LIMIT; i++) {
            MockHttpServletResponse response = perform("/api/auth/login", "10.0.0.1");
            assertThat(response.getStatus()).as("request #%d", i + 1).isEqualTo(200);
        }
    }

    // ── Limit üstü istekler ───────────────────────────────────────────────────

    @Test
    void doFilter_whenAuthLimitExceeded_returns429WithJsonErrorBody() throws Exception {
        for (int i = 0; i < AUTH_LIMIT; i++) {
            perform("/api/auth/login", "10.0.0.2");
        }

        MockHttpServletResponse response = perform("/api/auth/login", "10.0.0.2");

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString())
                .contains("RATE_LIMIT_EXCEEDED")
                .contains("\"success\":false");
    }

    @Test
    void doFilter_whenLimitExceeded_doesNotInvokeFilterChain() throws Exception {
        for (int i = 0; i < AUTH_LIMIT; i++) {
            perform("/api/auth/login", "10.0.0.3");
        }

        MockHttpServletRequest request = request("/api/auth/login", "10.0.0.3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(chain.getRequest()).as("filter chain must not run when rate limited").isNull();
    }

    // ── Anahtar (istemci IP) bazlı izolasyon ──────────────────────────────────

    @Test
    void doFilter_whenOneClientExceedsLimit_otherClientIsNotAffected() throws Exception {
        for (int i = 0; i < AUTH_LIMIT + 1; i++) {
            perform("/api/auth/login", "10.0.0.4");
        }
        MockHttpServletResponse blocked = perform("/api/auth/login", "10.0.0.4");
        MockHttpServletResponse otherClient = perform("/api/auth/login", "10.0.0.5");

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(otherClient.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_whenXForwardedForHeaderPresent_usesFirstEntryAsClientKey() throws Exception {
        for (int i = 0; i < AUTH_LIMIT; i++) {
            MockHttpServletRequest request = request("/api/auth/login", "192.168.0." + i);
            request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest request = request("/api/auth/login", "192.168.0.99");
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus())
                .as("requests sharing the same X-Forwarded-For client must share one bucket")
                .isEqualTo(429);
    }

    // ── Limit dışı yollar ─────────────────────────────────────────────────────

    @Test
    void doFilter_whenPathIsNotApi_neverRateLimits() throws Exception {
        for (int i = 0; i < GENERAL_LIMIT + 10; i++) {
            MockHttpServletResponse response = perform("/actuator/health", "10.0.0.6");
            assertThat(response.getStatus()).as("request #%d", i + 1).isEqualTo(200);
        }
    }

    // ── Yol kategorisine göre limitler ────────────────────────────────────────

    @Test
    void doFilter_whenGeneralApiPath_allows60ThenRejects() throws Exception {
        for (int i = 0; i < GENERAL_LIMIT; i++) {
            MockHttpServletResponse response = perform("/api/customers", "10.0.0.7");
            assertThat(response.getStatus()).as("request #%d", i + 1).isEqualTo(200);
        }

        MockHttpServletResponse response = perform("/api/customers", "10.0.0.7");
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void doFilter_whenPublicApiPath_allows30ThenRejects() throws Exception {
        for (int i = 0; i < PUBLIC_LIMIT; i++) {
            MockHttpServletResponse response = perform("/api/public/share/abc", "10.0.0.8");
            assertThat(response.getStatus()).as("request #%d", i + 1).isEqualTo(200);
        }

        MockHttpServletResponse response = perform("/api/public/share/abc", "10.0.0.8");
        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────────

    private MockHttpServletRequest request(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private MockHttpServletResponse perform(String uri, String remoteAddr) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(uri, remoteAddr), response, new MockFilterChain());
        return response;
    }
}
