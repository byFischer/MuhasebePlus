package com.MuhasebePlus.demo.sharelink.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import com.MuhasebePlus.demo.company.entity.Company;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_share_link")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceShareLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "token", length = 64, nullable = false, unique = true)
    private String token;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "access_count", nullable = false)
    private long accessCount;
}
