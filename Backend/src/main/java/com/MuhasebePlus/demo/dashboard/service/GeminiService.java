package com.MuhasebePlus.demo.dashboard.service;

import com.MuhasebePlus.demo.dashboard.dto.ChatMessage;
import com.MuhasebePlus.demo.system.AiSettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}";

    private final AiSettingsService aiSettingsService;

    @Value("${app.ai.gemini-model:gemini-2.5-flash}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;

    public GeminiService(AiSettingsService aiSettingsService) {
        this.aiSettingsService = aiSettingsService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        factory.setReadTimeout(120_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public String generate(String systemPrompt, String userPrompt) {
        String apiKey = aiSettingsService.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Gemini API anahtarı yapılandırılmamış. Settings'ten API key giriniz.");
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

    /**
     * Multi-turn structured output call with JSON schema enforcement.
     * Uses Gemini's systemInstruction + contents[] format and responseMimeType=application/json.
     */
    public String generateStructured(String systemInstruction, List<ChatMessage> messages, Map<String, Object> responseSchema) {
        String apiKey = aiSettingsService.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Gemini API anahtarı yapılandırılmamış. Settings'ten API key giriniz.");
        }

        ObjectNode body = objectMapper.createObjectNode();

        // System instruction (separate field, not prepended to user message)
        ObjectNode sysInstr = body.putObject("systemInstruction");
        sysInstr.putArray("parts").addObject().put("text", systemInstruction);

        // Multi-turn conversation contents
        ArrayNode contents = body.putArray("contents");
        for (ChatMessage msg : messages) {
            ObjectNode turn = contents.addObject();
            // Gemini uses "model" for assistant role
            turn.put("role", "assistant".equals(msg.role()) ? "model" : "user");
            turn.putArray("parts").addObject().put("text", msg.content());
        }

        // Generation config with structured output
        ObjectNode config = body.putObject("generationConfig");
        config.put("temperature", 0.2);
        config.put("maxOutputTokens", 8192);
        config.put("responseMimeType", "application/json");
        try {
            config.set("responseSchema", objectMapper.valueToTree(responseSchema));
        } catch (Exception e) {
            throw new RuntimeException("Response schema serileştirilemedi: " + e.getMessage(), e);
        }

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
