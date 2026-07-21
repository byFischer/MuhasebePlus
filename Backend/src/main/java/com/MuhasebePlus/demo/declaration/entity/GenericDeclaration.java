package com.MuhasebePlus.demo.declaration.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "generic_declaration",
        uniqueConstraints = @UniqueConstraint(name = "uq_generic_declaration",
                columnNames = {"company_id", "declaration_type", "year", "period"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class GenericDeclaration extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "declaration_id")
    private Long declarationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "declaration_type", nullable = false, length = 30)
    private String declarationType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "period", nullable = false)
    private Integer period;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeclarationStatus status = DeclarationStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> dataSnapshot;

    @Column(name = "xml_content", columnDefinition = "TEXT")
    private String xmlContent;

    @Column(name = "total_payable", precision = 15, scale = 2)
    private BigDecimal totalPayable;
}
