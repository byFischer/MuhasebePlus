package com.MuhasebePlus.demo.stock.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.stock.dto.request.StockRequestDto;
import com.MuhasebePlus.demo.stock.dto.response.StockResponseDto;
import com.MuhasebePlus.demo.stock.service.StockService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/stocks")
@Validated
public class StockController {

    @Autowired
    private StockService stockService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<StockResponseDto> createStock(@Valid @RequestBody StockRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.createStock(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<StockResponseDto>> getAllActiveStocks() {
        return ResponseEntity.ok(stockService.getAllActiveStocks());
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<StockResponseDto> getByProductId(@PathVariable Integer productId) {
        return ResponseEntity.ok(stockService.getStockByProductId(productId));
    }

    @GetMapping("/low")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<StockResponseDto>> getLowStockItems() {
        return ResponseEntity.ok(stockService.getLowStockItems());
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<StockResponseDto> updateStock(
            @PathVariable Integer productId,
            @Valid @RequestBody StockRequestDto dto) {
        return ResponseEntity.ok(stockService.updateStock(productId, dto));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDelete(@PathVariable Integer productId) {
        stockService.softDeleteStock(productId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{productId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockResponseDto> restore(@PathVariable Integer productId) {
        return ResponseEntity.ok(stockService.restoreStock(productId));
    }
}
