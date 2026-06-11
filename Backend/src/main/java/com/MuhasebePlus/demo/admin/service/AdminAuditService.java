package com.MuhasebePlus.demo.admin.service;

import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Admin panelinden yapılan kritik işlemleri (rol değiştirme, silme, kilitleme vb.)
 * denetlenebilir olması için SystemLog'a yazar. Kayıtlar {@link #AUDIT_PREFIX}
 * ile işaretlenir; admin log ekranındaki "Denetim Kaydı" filtresi bu öneke dayanır.
 */
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    public static final String AUDIT_PREFIX = "[ADMIN]";

    private final SystemLogService systemLogService;

    public void logAction(String action) {
        systemLogService.log(LogLevel.INFO, AUDIT_PREFIX + " " + action);
    }
}
