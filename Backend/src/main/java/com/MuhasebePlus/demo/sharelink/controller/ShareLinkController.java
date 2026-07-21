package com.MuhasebePlus.demo.sharelink.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.sharelink.dto.response.ShareLinkResponseDto;
import com.MuhasebePlus.demo.sharelink.service.ShareLinkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/share-links")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ShareLinkResponseDto> getCurrent() {
        return ResponseEntity.ok(shareLinkService.getCurrent());
    }

    @PostMapping("/regenerate")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ShareLinkResponseDto> regenerate() {
        return ResponseEntity.ok(shareLinkService.regenerate());
    }

    @DeleteMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> revoke() {
        shareLinkService.revoke();
        return ResponseEntity.noContent().build();
    }
}
