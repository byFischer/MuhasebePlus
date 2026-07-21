package com.MuhasebePlus.demo.edefter.controller;

import com.MuhasebePlus.demo.edefter.dto.EDefterRunResponseDto;
import com.MuhasebePlus.demo.edefter.entity.EDefterRun;
import com.MuhasebePlus.demo.edefter.service.EDefterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/edefter")
@RequiredArgsConstructor
public class EDefterController {

    private final EDefterService service;

    @PostMapping("/generate")
    public ResponseEntity<EDefterRunResponseDto> generate(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(toDto(service.generate(year, month)));
    }

    @GetMapping
    public ResponseEntity<List<EDefterRunResponseDto>> list() {
        return ResponseEntity.ok(service.list().stream().map(this::toDto).toList());
    }

    @GetMapping("/year/{year}")
    public ResponseEntity<List<EDefterRunResponseDto>> listByYear(@PathVariable int year) {
        return ResponseEntity.ok(service.listByYear(year).stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EDefterRunResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.getById(id)));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<EDefterRunResponseDto> regenerate(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.regenerate(id)));
    }

    @GetMapping("/{id}/journal")
    public ResponseEntity<byte[]> downloadJournal(@PathVariable Long id) {
        return xmlDownload(service.getJournalXml(id), "yevmiye.xml");
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<byte[]> downloadLedger(@PathVariable Long id) {
        return xmlDownload(service.getLedgerXml(id), "kebir.xml");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<byte[]> xmlDownload(String xml, String filename) {
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDispositionFormData("attachment", filename);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private EDefterRunResponseDto toDto(EDefterRun run) {
        return new EDefterRunResponseDto(
                run.getRunId(),
                run.getYear(),
                run.getMonth(),
                run.getStatus(),
                run.getJournalEntryCount(),
                run.getJournalLineCount(),
                run.getGeneratedAt()
        );
    }
}
