package com.MuhasebePlus.demo.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private static final String REQUEST_URI = "/api/test-endpoint";

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", REQUEST_URI);
    }

    // ── BusinessException → 400 ───────────────────────────────────────────────

    @Test
    void handleBusiness_whenBusinessException_returns400WithErrorBodyShape() {
        BusinessException ex = new BusinessException("Fis borc/alacak dengesi tutmuyor");

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("Fis borc/alacak dengesi tutmuyor");
        assertThat(body.path()).isEqualTo(REQUEST_URI);
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.details()).isNull();
    }

    // ── ClosedPeriodException → 400 ───────────────────────────────────────────

    @Test
    void handleBusiness_whenClosedPeriodException_returns400WithPeriodInMessage() {
        ClosedPeriodException ex = new ClosedPeriodException(2026, 5);

        ResponseEntity<ErrorResponse> response = handler.handleBusiness(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("2026/05");
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    // ── AccessDenied / Authentication eşlemeleri ──────────────────────────────

    @Test
    void handleAccessDenied_whenAccessDeniedException_returns403WithGenericMessage() {
        AccessDeniedException ex = new AccessDeniedException("internal authorization detail");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Access denied");
        assertThat(response.getBody().message()).doesNotContain("internal authorization detail");
        assertThat(response.getBody().path()).isEqualTo(REQUEST_URI);
    }

    @Test
    void handleAuthentication_whenBadCredentials_returns401WithGenericMessage() {
        BadCredentialsException ex = new BadCredentialsException("password mismatch for user X");

        ResponseEntity<ErrorResponse> response = handler.handleAuthentication(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Authentication failed");
        assertThat(response.getBody().message()).doesNotContain("password mismatch");
    }

    // ── RuntimeException / Exception → 500 ────────────────────────────────────

    @Test
    void handleRuntime_whenRuntimeException_returns500WithoutStackTraceLeak() {
        RuntimeException ex = new RuntimeException("unexpected null value");

        ResponseEntity<ErrorResponse> response = handler.handleRuntime(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.details()).isNull();
        // Yanıt gövdesi yığın izi (stack trace) içermemeli
        assertThat(body.message())
                .doesNotContain("at com.MuhasebePlus")
                .doesNotContain("java.lang.RuntimeException");
    }

    @Test
    void handleGeneric_whenCheckedException_returns500WithSanitizedMessage() {
        Exception ex = new Exception("sensitive internal detail");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().message()).doesNotContain("sensitive internal detail");
    }

    // ── Validation / gövde okuma hataları → 400 ───────────────────────────────

    @Test
    void handleValidation_whenFieldErrors_returns400WithFieldDetails() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "requestDto");
        bindingResult.addError(new FieldError("requestDto", "email", "must not be blank"));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleHandlerMethod", String.class);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().details()).containsExactly("email: must not be blank");
    }

    @Test
    void handleNotReadable_whenMalformedBody_returns400WithGenericMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: unexpected token", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponse> response = handler.handleNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Malformed request body");
        assertThat(response.getBody().message()).doesNotContain("JSON parse error");
    }

    // ── Yardımcılar ───────────────────────────────────────────────────────────

    @SuppressWarnings("unused")
    private void sampleHandlerMethod(String email) {
        // MethodArgumentNotValidException kurulumu için sahte hedef metot
    }
}
