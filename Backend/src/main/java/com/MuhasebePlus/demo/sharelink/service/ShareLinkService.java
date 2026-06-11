package com.MuhasebePlus.demo.sharelink.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.sharelink.dto.response.ShareLinkResponseDto;
import com.MuhasebePlus.demo.sharelink.entity.InvoiceShareLink;
import com.MuhasebePlus.demo.sharelink.repository.InvoiceShareLinkRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ShareLinkService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InvoiceShareLinkRepository shareLinkRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;

    @Transactional(readOnly = true)
    public ShareLinkResponseDto getCurrent() {
        Long companyId = companyContext.getCurrentCompanyId();
        return shareLinkRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId)
                .map(this::toDto)
                .orElse(ShareLinkResponseDto.empty());
    }

    public ShareLinkResponseDto regenerate() {
        Long companyId = companyContext.getCurrentCompanyId();
        revokeActiveLink(companyId);

        InvoiceShareLink link = InvoiceShareLink.builder()
                .company(companyRepository.getReferenceById(companyId))
                .token(generateToken())
                .isActive(true)
                .createdBy(companyContext.getCurrentUserId())
                .accessCount(0)
                .build();

        return toDto(shareLinkRepository.save(link));
    }

    public void revoke() {
        revokeActiveLink(companyContext.getCurrentCompanyId());
    }

    private void revokeActiveLink(Long companyId) {
        shareLinkRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId).ifPresent(link -> {
            link.setActive(false);
            link.setRevokedAt(LocalDateTime.now());
            shareLinkRepository.save(link);
        });
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ShareLinkResponseDto toDto(InvoiceShareLink link) {
        return new ShareLinkResponseDto(
                true,
                link.getToken(),
                link.isActive(),
                link.getCreatedAt(),
                link.getLastAccessedAt(),
                link.getAccessCount()
        );
    }
}
