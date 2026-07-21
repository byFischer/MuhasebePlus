package com.MuhasebePlus.demo.common.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;

class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
        request = new MockHttpServletRequest("GET", "/api/customers");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    // ── İçerik ve çerçeve koruma başlıkları ───────────────────────────────────

    @Test
    void doFilter_whenInvoked_setsContentTypeOptionsAndFrameOptionsHeaders() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }

    // ── Referrer ve XSS koruma başlıkları ─────────────────────────────────────

    @Test
    void doFilter_whenInvoked_setsReferrerPolicyAndXssProtectionHeaders() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader("X-XSS-Protection")).isEqualTo("1; mode=block");
    }

    // ── HSTS başlığı ──────────────────────────────────────────────────────────

    @Test
    void doFilter_whenInvoked_setsStrictTransportSecurityHeader() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("Strict-Transport-Security"))
                .isEqualTo("max-age=31536000; includeSubDomains");
    }

    // ── Zincirin devamı ───────────────────────────────────────────────────────

    @Test
    void doFilter_whenInvoked_continuesFilterChainWithSameRequest() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
