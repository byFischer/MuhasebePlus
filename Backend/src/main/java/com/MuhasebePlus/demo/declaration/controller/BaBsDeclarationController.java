package com.MuhasebePlus.demo.declaration.controller;

import com.MuhasebePlus.demo.declaration.dto.BaBsDeclarationLineDto;
import com.MuhasebePlus.demo.declaration.dto.BaBsDeclarationResponseDto;
import com.MuhasebePlus.demo.declaration.entity.BaBsDeclaration;
import com.MuhasebePlus.demo.declaration.entity.BaBsDeclarationLine;
import com.MuhasebePlus.demo.declaration.service.BaBsDeclarationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/declarations/babs")
@RequiredArgsConstructor
public class BaBsDeclarationController {

    private final BaBsDeclarationService service;

    @PostMapping("/generate")
    public ResponseEntity<BaBsDeclarationResponseDto> generate(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam String type) {
        return ResponseEntity.ok(toDto(service.generate(year, month, type)));
    }

    @GetMapping
    public ResponseEntity<List<BaBsDeclarationResponseDto>> list() {
        return ResponseEntity.ok(service.list().stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaBsDeclarationResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.getById(id)));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<BaBsDeclarationResponseDto> finalize(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(service.finalize(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private BaBsDeclarationResponseDto toDto(BaBsDeclaration d) {
        List<BaBsDeclarationLineDto> lines = d.getLines().stream()
                .map(this::toLineDto)
                .toList();
        return new BaBsDeclarationResponseDto(
                d.getDeclarationId(),
                d.getBabsType(),
                d.getYear(),
                d.getMonth(),
                d.getStatus(),
                d.getRecordCount(),
                d.getTotalAmount(),
                lines
        );
    }

    private BaBsDeclarationLineDto toLineDto(BaBsDeclarationLine l) {
        return new BaBsDeclarationLineDto(
                l.getLineId(),
                l.getTaxNumber(),
                l.getCustomerName(),
                l.getTotalAmount(),
                l.getDocumentCount()
        );
    }
}
