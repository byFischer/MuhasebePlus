package com.MuhasebePlus.demo.declaration.controller;

import com.MuhasebePlus.demo.declaration.dto.GenerateDeclarationRequestDto;
import com.MuhasebePlus.demo.declaration.dto.VatDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.service.VatDeclarationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController("declarationVatDeclarationController")
@RequestMapping("/api/declarations/vat")
@RequiredArgsConstructor
public class VatDeclarationController {

    private final VatDeclarationService service;

    @PostMapping("/generate")
    public ResponseEntity<VatDeclarationResponseDto> generate(@RequestBody GenerateDeclarationRequestDto req) {
        return ResponseEntity.ok(service.generate(req));
    }

    @GetMapping
    public ResponseEntity<List<VatDeclarationResponseDto>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VatDeclarationResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<VatDeclarationResponseDto> finalize(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalize(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<VatDeclarationResponseDto> markSubmitted(@PathVariable Long id) {
        return ResponseEntity.ok(service.markSubmitted(id));
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<byte[]> downloadXml(@PathVariable Long id) {
        String xml = service.getXml(id);
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDispositionFormData("attachment", "kdv1-beyannamesi.xml");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
