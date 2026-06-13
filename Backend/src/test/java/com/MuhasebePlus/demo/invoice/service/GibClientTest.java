package com.MuhasebePlus.demo.invoice.service;

import com.MuhasebePlus.demo.invoice.entity.EInvoiceProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GibClientTest {

    private RestTemplate restTemplate;
    private GibClient client;
    private EInvoiceProfile profile;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new GibClient("http://gib.test", 100, 100);
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        profile = EInvoiceProfile.builder()
                .gibAlias("alias")
                .gibPassword("secret")
                .build();
    }

    @Test
    void sendInvoice_whenGibReturnsSuccess_parsesSendResponse() {
        String responseXml = """
                <earsivFaturaGonderReturn>
                  <ettn>ETTN-1</ettn>
                  <durumKodu>0</durumKodu>
                  <mesaj>Basarili</mesaj>
                  <belgeNo>FTR-1</belgeNo>
                </earsivFaturaGonderReturn>
                """;
        when(restTemplate.exchange(eq("http://gib.test"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseXml));

        GibClient.GibResponse result = client.sendInvoice(profile, "<Invoice><cbc:UUID>abcdef12-0000</cbc:UUID></Invoice>");

        assertThat(result.success).isTrue();
        assertThat(result.ettin).isEqualTo("ETTN-1");
        assertThat(result.status).isEqualTo("0");
        assertThat(result.documentNo).isEqualTo("FTR-1");
        assertThat(result.rawResponse).contains("Basarili");
    }

    @Test
    void sendInvoice_whenResponseBodyIsNull_returnsErrorResponse() {
        when(restTemplate.exchange(eq("http://gib.test"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(null));

        GibClient.GibResponse result = client.sendInvoice(profile, "<Invoice/>");

        assertThat(result.success).isFalse();
        assertThat(result.message).contains("yan");
    }

    @Test
    void sendInvoice_whenEndpointFails_generatesDemoSuccessResponse() {
        when(restTemplate.exchange(eq("http://gib.test"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("offline"));

        GibClient.GibResponse result = client.sendInvoice(profile, "<Invoice><cbc:UUID>12345678-abcd</cbc:UUID></Invoice>");

        assertThat(result.success).isTrue();
        assertThat(result.ettin).startsWith("DEMO-12345678");
        assertThat(result.message).contains("DEMO");
    }

    @Test
    void checkStatus_whenGibReturnsStatus_parsesStatusResponse() {
        String responseXml = "<root><durum>ONAYLANDI</durum><aciklama>Tamam</aciklama></root>";
        when(restTemplate.exchange(eq("http://gib.test"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseXml));

        GibClient.GibResponse result = client.checkStatus(profile, "ETTN-1");

        assertThat(result.success).isTrue();
        assertThat(result.status).isEqualTo("ONAYLANDI");
        assertThat(result.message).isEqualTo("Tamam");
    }

    @Test
    void checkStatus_whenEndpointFails_returnsErrorResponse() {
        when(restTemplate.exchange(eq("http://gib.test"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("offline"));

        GibClient.GibResponse result = client.checkStatus(profile, "ETTN-1");

        assertThat(result.success).isFalse();
        assertThat(result.message).contains("offline");
    }

    @Test
    void gibResponseErrorFactoryBuildsFailedResponse() {
        GibClient.GibResponse result = GibClient.GibResponse.error("bad");

        assertThat(result.success).isFalse();
        assertThat(result.message).isEqualTo("bad");
    }
}
