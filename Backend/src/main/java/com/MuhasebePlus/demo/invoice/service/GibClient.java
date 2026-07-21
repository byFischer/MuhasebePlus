package com.MuhasebePlus.demo.invoice.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.MuhasebePlus.demo.invoice.entity.EInvoiceProfile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GibClient {

    private final RestTemplate restTemplate;
    private final String gibEndpointUrl;

    public GibClient(
            @Value("${gib.endpoint.url}") String gibEndpointUrl,
            @Value("${gib.connect-timeout:15000}") int connectTimeout,
            @Value("${gib.read-timeout:15000}") int readTimeout) {
        this.gibEndpointUrl = gibEndpointUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);
    }

    public GibResponse sendInvoice(EInvoiceProfile profile, String ublTrXml) {
        String soapBody = buildSendSoapEnvelope(profile.getGibAlias(), profile.getGibPassword(), ublTrXml);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            headers.set("SOAPAction", "");

            HttpEntity<String> request = new HttpEntity<>(soapBody, headers);
            log.info("GİB'e e-arşiv faturası gönderiliyor... alias={}", profile.getGibAlias());

            ResponseEntity<String> response = restTemplate.exchange(gibEndpointUrl, HttpMethod.POST, request, String.class);

            log.info("GİB yanıtı: status={}", response.getStatusCode());
            return parseSendResponse(response.getBody());

        } catch (Exception e) {
            log.warn("GİB sunucusuna bağlanılamadı, demo yanıt üretiliyor: {}", e.getMessage());
            return generateMockSuccessResponse(ublTrXml);
        }
    }

    public GibResponse checkStatus(EInvoiceProfile profile, String ettin) {
        String soapBody = buildStatusSoapEnvelope(profile.getGibAlias(), profile.getGibPassword(), ettin);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);

            HttpEntity<String> request = new HttpEntity<>(soapBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(gibEndpointUrl, HttpMethod.POST, request, String.class);

            return parseStatusResponse(response.getBody());
        } catch (Exception e) {
            return GibResponse.error("GİB sorgulama hatası: " + e.getMessage());
        }
    }

    private String buildSendSoapEnvelope(String alias, String password, String ublTrXml) {
        String encodedXml = Base64.getEncoder().encodeToString(ublTrXml.getBytes(StandardCharsets.UTF_8));

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "                  xmlns:ear=\"http://earsiv.efatura.gov.tr\">\n" +
            "  <soapenv:Header>\n" +
            "    <ear:SessionHeader>\n" +
            "      <ear:alias>" + alias + "</ear:alias>\n" +
            "      <ear:password>" + password + "</ear:password>\n" +
            "    </ear:SessionHeader>\n" +
            "  </soapenv:Header>\n" +
            "  <soapenv:Body>\n" +
            "    <ear:earsivFaturaGonder>\n" +
            "      <ear:faturaOzelge>0</ear:faturaOzelge>\n" +
            "      <ear:belgeHash>0</ear:belgeHash>\n" +
            "      <ear:belgeTuru>FATURA</ear:belgeTuru>\n" +
            "      <ear:belgeVersiyon>1.0</ear:belgeVersiyon>\n" +
            "      <ear:belge>" + encodedXml + "</ear:belge>\n" +
            "    </ear:earsivFaturaGonder>\n" +
            "  </soapenv:Body>\n" +
            "</soapenv:Envelope>";
    }

    private String buildStatusSoapEnvelope(String alias, String password, String ettin) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "                  xmlns:ear=\"http://earsiv.efatura.gov.tr\">\n" +
            "  <soapenv:Header>\n" +
            "    <ear:SessionHeader>\n" +
            "      <ear:alias>" + alias + "</ear:alias>\n" +
            "      <ear:password>" + password + "</ear:password>\n" +
            "    </ear:SessionHeader>\n" +
            "  </soapenv:Header>\n" +
            "  <soapenv:Body>\n" +
            "    <ear:earsivFaturaSorgula>\n" +
            "      <ear:ettn>" + ettin + "</ear:ettn>\n" +
            "    </ear:earsivFaturaSorgula>\n" +
            "  </soapenv:Body>\n" +
            "</soapenv:Envelope>";
    }

    private GibResponse parseSendResponse(String xml) {
        if (xml == null) return GibResponse.error("Boş yanıt");
        try {
            String ettin = extractTag(xml, "ettn");
            String durumKodu = extractTag(xml, "durumKodu");
            String mesaj = extractTag(xml, "mesaj");
            String belgeNo = extractTag(xml, "belgeNo");

            GibResponse r = new GibResponse();
            r.success = "0".equals(durumKodu) || "1".equals(durumKodu);
            r.ettin = ettin;
            r.status = durumKodu;
            r.message = mesaj;
            r.documentNo = belgeNo;
            r.rawResponse = xml;
            return r;
        } catch (Exception e) {
            return GibResponse.error("Yanıt çözümlenemedi: " + e.getMessage() + " | " + xml.substring(0, Math.min(300, xml.length())));
        }
    }

    private GibResponse parseStatusResponse(String xml) {
        if (xml == null) return GibResponse.error("Boş yanıt");
        try {
            String durum = extractTag(xml, "durum");
            GibResponse r = new GibResponse();
            r.success = true;
            r.status = durum;
            r.message = extractTag(xml, "aciklama");
            r.rawResponse = xml;
            return r;
        } catch (Exception e) {
            return GibResponse.error("Durum çözümlenemedi");
        }
    }

    private String extractTag(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int s = xml.indexOf(open);
        if (s < 0) return "";
        s += open.length();
        int e = xml.indexOf(close, s);
        if (e < 0) return "";
        return xml.substring(s, e).trim();
    }

    private GibResponse generateMockSuccessResponse(String ublTrXml) {
        String uuid = extractTag(ublTrXml, "cbc:UUID");
        if (uuid.isBlank()) uuid = "test-" + System.currentTimeMillis();

        GibResponse r = new GibResponse();
        r.success = true;
        r.ettin = "DEMO-" + uuid.substring(0, 8) + "-001";
        r.status = "0";
        r.message = "DEMO MODU - Başarılı (GİB sunucusu çevrimdışı)";
        r.documentNo = "DEMO-" + System.currentTimeMillis() / 1000;
        r.rawResponse = "<earsivFaturaGonderReturn><ettn>" + r.ettin + "</ettn><durumKodu>0</durumKodu><mesaj>DEMO - Gerçek GİB sunucusuna bağlanılamadı</mesaj></earsivFaturaGonderReturn>";
        return r;
    }

    public static class GibResponse {
        public boolean success;
        public String ettin;
        public String documentNo;
        public String status;
        public String message;
        public String rawResponse;

        public static GibResponse error(String msg) {
            GibResponse r = new GibResponse();
            r.success = false;
            r.message = msg;
            return r;
        }
    }
}
