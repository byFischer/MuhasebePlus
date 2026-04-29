package com.MuhasebePlus.demo.dashboard.controller;

import com.MuhasebePlus.demo.dashboard.service.AiWidgetGeneratorService;
import com.MuhasebePlus.demo.dashboard.service.AiQuotaGuardService;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiWidgetController {

    @Autowired private AiWidgetGeneratorService generatorService;
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
