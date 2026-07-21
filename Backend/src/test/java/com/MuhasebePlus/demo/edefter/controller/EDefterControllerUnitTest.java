package com.MuhasebePlus.demo.edefter.controller;

import com.MuhasebePlus.demo.edefter.dto.EDefterRunResponseDto;
import com.MuhasebePlus.demo.edefter.entity.EDefterRun;
import com.MuhasebePlus.demo.edefter.entity.EDefterStatus;
import com.MuhasebePlus.demo.edefter.service.EDefterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EDefterController için birim testleri. Servis mock'lanır; controller'ın DTO eşleme,
 * HTTP durum kodu ve XML indirme başlıklarını doğru ürettiği doğrulanır.
 */
@ExtendWith(MockitoExtension.class)
class EDefterControllerUnitTest {

    @Mock private EDefterService service;

    @InjectMocks
    private EDefterController controller;

    private EDefterRun run(Long id, int year, int month) {
        return EDefterRun.builder()
                .runId(id)
                .year(year)
                .month(month)
                .status(EDefterStatus.GENERATED)
                .journalEntryCount(3)
                .journalLineCount(6)
                .generatedAt(LocalDateTime.of(2026, 4, 1, 12, 0))
                .build();
    }

    @Test
    void generate_mapsRunToDtoWithOkStatus() {
        when(service.generate(2026, 4)).thenReturn(run(10L, 2026, 4));

        ResponseEntity<EDefterRunResponseDto> response = controller.generate(2026, 4);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        EDefterRunResponseDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.runId()).isEqualTo(10L);
        assertThat(body.year()).isEqualTo(2026);
        assertThat(body.month()).isEqualTo(4);
        assertThat(body.status()).isEqualTo(EDefterStatus.GENERATED);
        assertThat(body.journalEntryCount()).isEqualTo(3);
        assertThat(body.journalLineCount()).isEqualTo(6);
        assertThat(body.generatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 1, 12, 0));
    }

    @Test
    void list_mapsAllRunsToDtos() {
        when(service.list()).thenReturn(List.of(run(1L, 2026, 1), run(2L, 2026, 2)));

        ResponseEntity<List<EDefterRunResponseDto>> response = controller.list();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).extracting(EDefterRunResponseDto::runId)
                .containsExactly(1L, 2L);
    }

    @Test
    void listByYear_mapsRunsToDtos() {
        when(service.listByYear(2026)).thenReturn(List.of(run(1L, 2026, 1)));

        ResponseEntity<List<EDefterRunResponseDto>> response = controller.listByYear(2026);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).month()).isEqualTo(1);
    }

    @Test
    void getById_mapsRunToDto() {
        when(service.getById(10L)).thenReturn(run(10L, 2026, 4));

        ResponseEntity<EDefterRunResponseDto> response = controller.getById(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().runId()).isEqualTo(10L);
    }

    @Test
    void regenerate_mapsRunToDto() {
        when(service.regenerate(10L)).thenReturn(run(11L, 2026, 4));

        ResponseEntity<EDefterRunResponseDto> response = controller.regenerate(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().runId()).isEqualTo(11L);
    }

    @Test
    void downloadJournal_returnsXmlBytesWithAttachmentHeaders() {
        String xml = "<?xml version=\"1.0\"?><journal/>";
        when(service.getJournalXml(10L)).thenReturn(xml);

        ResponseEntity<byte[]> response = controller.downloadJournal(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(xml.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = response.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_XML);
        // setContentDispositionFormData("attachment", filename) → tip "form-data", ad "attachment"
        assertThat(headers.getContentDisposition().getType()).isEqualTo("form-data");
        assertThat(headers.getContentDisposition().getName()).isEqualTo("attachment");
        assertThat(headers.getContentDisposition().getFilename()).isEqualTo("yevmiye.xml");
    }

    @Test
    void downloadLedger_returnsXmlBytesWithKebirFilename() {
        String xml = "<?xml version=\"1.0\"?><ledger/>";
        when(service.getLedgerXml(10L)).thenReturn(xml);

        ResponseEntity<byte[]> response = controller.downloadLedger(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(xml.getBytes(StandardCharsets.UTF_8));
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_XML);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("kebir.xml");
    }

    @Test
    void delete_returnsNoContentAndDelegatesToService() {
        ResponseEntity<Void> response = controller.delete(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(service).delete(10L);
    }
}
