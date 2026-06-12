package com.MuhasebePlus.demo.common.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock private IdempotencyService idempotencyService;

    @InjectMocks private IdempotencyFilter filter;

    @Test
    void doFilter_whenRequestIsNotPost_bypassesIdempotency() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/invoices");
        request.addHeader("Idempotency-Key", "key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verify(idempotencyService, never()).findByKey(org.mockito.Mockito.anyString());
    }

    @Test
    void doFilter_whenPostHasNoKey_bypassesIdempotency() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/invoices");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verify(idempotencyService, never()).findByKey(org.mockito.Mockito.anyString());
    }

    @Test
    void doFilter_whenCachedResponseExists_writesItAndSkipsChain() throws Exception {
        IdempotencyKey cached = IdempotencyKey.builder()
                .key("key-1")
                .responseStatus(201)
                .responseBody("{\"id\":1}")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(idempotencyService.findByKey("key-1")).thenReturn(Optional.of(cached));
        MockHttpServletRequest request = postWithKey("key-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).isEqualTo("{\"id\":1}");
        assertThat(chain.getRequest()).isNull();
        verify(idempotencyService, never()).saveResponse(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.anyInt());
    }

    @Test
    void doFilter_whenResponseIsSuccessful_cachesBodyAndCopiesResponse() throws Exception {
        when(idempotencyService.findByKey("key-2")).thenReturn(Optional.empty());
        MockHttpServletRequest request = postWithKey("key-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, responseWritingChain(201, "{\"ok\":true}"));

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString()).isEqualTo("{\"ok\":true}");
        verify(idempotencyService).saveResponse("key-2", "{\"ok\":true}", 201);
    }

    @Test
    void doFilter_whenResponseIsNotSuccessful_doesNotCacheBody() throws Exception {
        when(idempotencyService.findByKey("key-3")).thenReturn(Optional.empty());
        MockHttpServletRequest request = postWithKey("key-3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, responseWritingChain(400, "{\"error\":true}"));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).isEqualTo("{\"error\":true}");
        verify(idempotencyService, never()).saveResponse(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.anyInt());
    }

    private MockHttpServletRequest postWithKey(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/invoices");
        request.addHeader("Idempotency-Key", key);
        return request;
    }

    private FilterChain responseWritingChain(int status, String body) {
        return (ServletRequest request, ServletResponse response) -> {
            HttpServletResponse http = (HttpServletResponse) response;
            http.setStatus(status);
            http.setContentType("application/json");
            http.getWriter().write(body);
        };
    }
}
