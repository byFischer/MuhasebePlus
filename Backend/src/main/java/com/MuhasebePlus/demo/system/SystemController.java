package com.MuhasebePlus.demo.system;

import com.MuhasebePlus.demo.user.dto.request.UserRequestDto;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import com.MuhasebePlus.demo.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final AiSettingsService aiSettingsService;
    private final UserRepository userRepository;
    private final UserService userSvc;

    public SystemController(AiSettingsService aiSettingsService,
                            UserRepository userRepository,
                            UserService userSvc) {
        this.aiSettingsService = aiSettingsService;
        this.userRepository = userRepository;
        this.userSvc = userSvc;
    }

    // Her iki modda da calisir; frontend AI butonu + ilk kurulum ekrani icin
    @GetMapping("/features")
    public ResponseEntity<Map<String, Object>> getFeatures() {
        return ResponseEntity.ok(Map.of(
                "aiEnabled", aiSettingsService.isAiEnabled(),
                "needsSetup", userRepository.count() == 0
        ));
    }

    // Sadece User tablosu bos oldugunda calisir — desktop ilk kurulum sihirbazi
    @PostMapping("/initialize")
    public ResponseEntity<Void> initialize(@Valid @RequestBody UserRequestDto dto) {
        if (userRepository.count() > 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userSvc.registerUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
