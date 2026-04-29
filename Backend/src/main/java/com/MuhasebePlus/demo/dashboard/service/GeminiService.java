package com.MuhasebePlus.demo.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Service
public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}";

    @Value("${app.ai.gemini-api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini-model:gemini-2.5-flash}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Gemini API anahtarı yapılandırılmamış. application.properties dosyasına GEMINI_API_KEY ekleyin.");
        }

        ObjectNode body = objectMapper.createObjectNode();

        ArrayNode contents = body.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        ArrayNode parts = userContent.putArray("parts");
        parts.addObject().put("text", systemPrompt + "\n\n" + userPrompt);

        ObjectNode config = body.putObject("generationConfig");
        config.put("temperature", 0.2);
        config.put("maxOutputTokens", 1024);

        try {
            String responseBody = restClient.post()
                    .uri(GEMINI_URL, model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.toString())
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, resp) -> {
                        String errorBody = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RuntimeException("Gemini API hatası [" + resp.getStatusCode().value() + "]: " + errorBody);
                    })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gemini API çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    public int estimateTokens(String text) {
        return text.length() / 4;
    }
}
