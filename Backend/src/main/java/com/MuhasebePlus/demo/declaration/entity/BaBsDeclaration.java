package com.MuhasebePlus.demo.declaration.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "babs_declaration",
        uniqueConstraints = @UniqueConstraint(name = "uq_babs_declaration",
                columnNames = {"company_id", "babs_type", "year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class BaBsDeclaration extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "declaration_id")
    private Long declarationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "babs_type", nullable = false, length = 5)
    private String babsType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeclarationStatus status = DeclarationStatus.DRAFT;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "declaration", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BaBsDeclarationLine> lines = new ArrayList<>();
}
