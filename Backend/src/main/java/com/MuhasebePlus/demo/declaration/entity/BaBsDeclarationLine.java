package com.MuhasebePlus.demo.declaration.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "babs_declaration_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaBsDeclarationLine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declaration_id", nullable = false)
    private BaBsDeclaration declaration;

    @Column(name = "tax_number", length = 20)
    private String taxNumber;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "document_count")
    private Integer documentCount;
}
