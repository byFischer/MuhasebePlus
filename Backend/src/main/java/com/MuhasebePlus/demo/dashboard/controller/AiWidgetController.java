package com.MuhasebePlus.demo.dashboard.controller;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.dashboard.dto.request.AiWidgetChatRequestDto;
import com.MuhasebePlus.demo.dashboard.dto.response.AiWidgetChatResponseDto;
import com.MuhasebePlus.demo.dashboard.service.AiQuotaGuardService;
import com.MuhasebePlus.demo.dashboard.service.AiWidgetChatService;
import com.MuhasebePlus.demo.dashboard.service.AiWidgetGeneratorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiWidgetController {

    @Autowired private AiWidgetGeneratorService generatorService;
    @Autowired private AiWidgetChatService chatService;
    @Autowired private AiQuotaGuardService quotaGuard;
    @Autowired private CompanyContext companyContext;

    @PostMapping("/widgets/generate")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> generateWidget(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt boş olamaz"));
        }
        try {
            String configJson = generatorService.generateWidgetConfig(prompt);
            return ResponseEntity.ok(Map.of("config", configJson, "prompt", prompt));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/widgets/chat")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<AiWidgetChatResponseDto> chat(@Valid @RequestBody AiWidgetChatRequestDto request) {
        try {
            AiWidgetChatResponseDto response = chatService.chat(request.messages());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "AI servisi hatası";
            if (msg.contains("kota") || msg.contains("quota") || msg.contains("limit")) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, msg);
            }
            if (msg.contains("API anahtarı") || msg.contains("yapılandırılmamış")) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, msg);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, msg);
        }
    }

    @GetMapping("/quota")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> getQuota() {
        Long companyId = companyContext.getCurrentCompanyId();
        var quota = quotaGuard.getQuota(companyId);
        return ResponseEntity.ok(Map.of(
                "used", quota.getTokensUsedThisMonth(),
                "budget", quota.getMonthlyTokenBudget(),
                "resetAt", quota.getResetAt() != null ? quota.getResetAt().toString() : null,
                "remaining", quota.getMonthlyTokenBudget() - quota.getTokensUsedThisMonth()
        ));
    }
}
