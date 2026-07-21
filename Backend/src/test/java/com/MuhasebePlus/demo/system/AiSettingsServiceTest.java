package com.MuhasebePlus.demo.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AiSettingsServiceTest {

    private AiSettingsService service;

    @BeforeEach
    void setUp() {
        service = new AiSettingsService();
    }

    @Test
    void getApiKey_returnsApplicationPropertyValue() {
        ReflectionTestUtils.setField(service, "defaultApiKey", "PROP_DEFAULT_KEY");

        assertThat(service.getApiKey()).isEqualTo("PROP_DEFAULT_KEY");
    }

    @Test
    void getApiKey_nullProperty_returnsEmptyString() {
        ReflectionTestUtils.setField(service, "defaultApiKey", null);

        assertThat(service.getApiKey()).isEmpty();
    }

    @Test
    void isAiEnabled_trueWhenPropertyKeyPresent() {
        ReflectionTestUtils.setField(service, "defaultApiKey", "real-key");

        assertThat(service.isAiEnabled()).isTrue();
    }

    @Test
    void isAiEnabled_falseWhenPropertyKeyBlank() {
        ReflectionTestUtils.setField(service, "defaultApiKey", "   ");

        assertThat(service.isAiEnabled()).isFalse();
    }
}

