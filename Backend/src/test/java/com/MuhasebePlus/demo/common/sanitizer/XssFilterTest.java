package com.MuhasebePlus.demo.common.sanitizer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class XssFilterTest {

    private final XssFilter filter = new XssFilter();

    @Test
    void doFilter_wrapsRequestAndSanitizesSingleParameterAndHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addParameter("name", "<script>alert(1)</script>Ahmet");
        request.addHeader("X-Note", "javascript:alert(1)");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<HttpServletRequest> wrapped = new AtomicReference<>();

        filter.doFilter(request, response, captureRequest(wrapped));

        assertThat(wrapped.get()).isNotSameAs(request);
        assertThat(wrapped.get().getParameter("name")).isEqualTo("Ahmet");
        assertThat(wrapped.get().getHeader("X-Note")).isEqualTo("alert(1)");
    }

    @Test
    void doFilter_sanitizesParameterValuesAndReturnsNullWhenValuesAreMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addParameter("tags", "<iframe src='x'></iframe>", "safe");
        AtomicReference<HttpServletRequest> wrapped = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), captureRequest(wrapped));

        assertThat(wrapped.get().getParameterValues("tags"))
                .containsExactly(" src='x'>", "safe");
        assertThat(wrapped.get().getParameterValues("missing")).isNull();
    }

    @Test
    void doFilter_sanitizesParameterMapValuesInPlace() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addParameter("comment", "<object data='bad'></object>ok");
        AtomicReference<HttpServletRequest> wrapped = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), captureRequest(wrapped));

        Map<String, String[]> parameterMap = wrapped.get().getParameterMap();
        assertThat(parameterMap.get("comment")).containsExactly(" data='bad'>ok");
    }

    private FilterChain captureRequest(AtomicReference<HttpServletRequest> captured) {
        return (ServletRequest request, ServletResponse response) ->
                captured.set((HttpServletRequest) request);
    }
}
