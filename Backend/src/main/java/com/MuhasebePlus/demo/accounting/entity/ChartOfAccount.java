package com.MuhasebePlus.demo.accounting.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chart_of_account")
@EqualsAndHashCode(callSuper = false)
public class ChartOfAccount extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private ChartOfAccount parent;

    @Column(name = "is_leaf", nullable = false)
    private boolean isLeaf = true;

    // System (TDHP standard) accounts cannot be deleted
    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @Column(name = "description", length = 500)
    private String description;
}
