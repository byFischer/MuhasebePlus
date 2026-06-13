package com.MuhasebePlus.demo.system;

import com.MuhasebePlus.demo.user.dto.request.UserRequestDto;
import com.MuhasebePlus.demo.user.repository.UserRepository;
import com.MuhasebePlus.demo.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SystemController için birim testleri. Bağımlılıklar mock'lanır; özellik bayrakları,
 * ilk kurulum koruması (User tablosu doluysa 403) ve AI ayarı güncelleme doğrulanır.
 */
@ExtendWith(MockitoExtension.class)
class SystemControllerUnitTest {

    @Mock private AiSettingsService aiSettingsService;
    @Mock private UserRepository userRepository;
    @Mock private UserService userSvc;

    @InjectMocks
    private SystemController controller;

    // ── getFeatures ───────────────────────────────────────────────────────────

    @Test
    void getFeatures_aiEnabledAndNeedsSetupWhenNoUsers() {
        when(aiSettingsService.isAiEnabled()).thenReturn(true);
        when(userRepository.count()).thenReturn(0L);

        ResponseEntity<Map<String, Object>> response = controller.getFeatures();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("aiEnabled", true);
        assertThat(response.getBody()).containsEntry("needsSetup", true);
    }

    @Test
    void getFeatures_aiDisabledAndSetupDoneWhenUsersExist() {
        when(aiSettingsService.isAiEnabled()).thenReturn(false);
        when(userRepository.count()).thenReturn(5L);

        ResponseEntity<Map<String, Object>> response = controller.getFeatures();

        assertThat(response.getBody()).containsEntry("aiEnabled", false);
        assertThat(response.getBody()).containsEntry("needsSetup", false);
    }

    // ── initialize ────────────────────────────────────────────────────────────

    @Test
    void initialize_registersUserWhenTableEmpty() {
        when(userRepository.count()).thenReturn(0L);
        UserRequestDto dto = userRequest();

        ResponseEntity<Void> response = controller.initialize(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userSvc).registerUser(dto);
    }

    @Test
    void initialize_forbiddenWhenUsersAlreadyExist() {
        when(userRepository.count()).thenReturn(1L);

        ResponseEntity<Void> response = controller.initialize(userRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(userSvc, never()).registerUser(any());
    }

    // ── updateAiSettings ──────────────────────────────────────────────────────

    @Test
    void updateAiSettings_persistsProvidedKey() {
        ResponseEntity<Void> response = controller.updateAiSettings(Map.of("apiKey", "secret-123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(aiSettingsService).setApiKey("secret-123");
    }

    @Test
    void updateAiSettings_defaultsToEmptyWhenKeyMissing() {
        ResponseEntity<Void> response = controller.updateAiSettings(Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(aiSettingsService).setApiKey("");
    }

    private UserRequestDto userRequest() {
        return new UserRequestDto("Ad", "Soyad", "user@example.com", "Password1!",
                "5551112233", java.time.LocalDate.of(1990, 1, 1), "Test A.Ş.", "1234567890");
    }
}
