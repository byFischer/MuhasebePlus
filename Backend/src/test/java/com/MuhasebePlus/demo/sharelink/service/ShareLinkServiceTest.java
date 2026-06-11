package com.MuhasebePlus.demo.sharelink.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.sharelink.dto.response.ShareLinkResponseDto;
import com.MuhasebePlus.demo.sharelink.entity.InvoiceShareLink;
import com.MuhasebePlus.demo.sharelink.repository.InvoiceShareLinkRepository;

class ShareLinkServiceTest {

    private InvoiceShareLinkRepository shareLinkRepository;
    private CompanyContext companyContext;
    private ShareLinkService shareLinkService;

    @BeforeEach
    void setUp() {
        shareLinkRepository = mock(InvoiceShareLinkRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        companyContext = mock(CompanyContext.class);
        shareLinkService = new ShareLinkService(shareLinkRepository, companyRepository, companyContext);

        when(companyContext.getCurrentCompanyId()).thenReturn(1L);
        when(companyContext.getCurrentUserId()).thenReturn(10L);
        when(companyRepository.getReferenceById(1L)).thenReturn(new Company());
        when(shareLinkRepository.save(any(InvoiceShareLink.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getCurrentReturnsEmptyStateWhenNoActiveLink() {
        when(shareLinkRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

        ShareLinkResponseDto dto = shareLinkService.getCurrent();

        assertFalse(dto.exists());
    }

    @Test
    void regenerateCreatesUrlSafeTokenAndActiveLink() {
        when(shareLinkRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

        ShareLinkResponseDto dto = shareLinkService.regenerate();

        assertTrue(dto.exists());
        assertTrue(dto.active());
        assertNotNull(dto.token());
        assertEquals(43, dto.token().length());
        assertTrue(dto.token().matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void regenerateRevokesExistingActiveLink() {
        InvoiceShareLink existing = InvoiceShareLink.builder().id(5L).token("old").isActive(true).build();
        when(shareLinkRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.of(existing));

        shareLinkService.regenerate();

        assertFalse(existing.isActive());
        assertNotNull(existing.getRevokedAt());
        ArgumentCaptor<InvoiceShareLink> captor = ArgumentCaptor.forClass(InvoiceShareLink.class);
        verify(shareLinkRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        InvoiceShareLink created = captor.getAllValues().get(1);
        assertTrue(created.isActive());
        assertEquals(10L, created.getCreatedBy());
    }

    @Test
    void revokeDeactivatesActiveLink() {
        InvoiceShareLink existing = InvoiceShareLink.builder().id(5L).token("old").isActive(true).build();
        when(shareLinkRepository.findByCompanyCompanyIdAndIsActiveTrue(1L)).thenReturn(Optional.of(existing));

        shareLinkService.revoke();

        assertFalse(existing.isActive());
        assertNotNull(existing.getRevokedAt());
    }
}
