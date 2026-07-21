package com.MuhasebePlus.demo.stock.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.stock.dto.request.StockMovementRequestDto;
import com.MuhasebePlus.demo.stock.dto.response.StockMovementResponseDto;
import com.MuhasebePlus.demo.stock.service.StockMovementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<StockMovementResponseDto> createManualMovement(@Valid @RequestBody StockMovementRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.createManualMovement(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<StockMovementResponseDto>> getMovements(@RequestParam(required = false) Integer productId) {
        if (productId != null) {
            return ResponseEntity.ok(stockMovementService.getMovementsByProduct(productId));
        }
        return ResponseEntity.ok(stockMovementService.getAllMovements());
    }
}
