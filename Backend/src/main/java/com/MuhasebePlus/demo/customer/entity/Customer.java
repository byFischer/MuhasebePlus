package com.MuhasebePlus.demo.customer.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.financial.entity.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer")
@EqualsAndHashCode(callSuper = false)
public class Customer extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    private String taxNumber;
    private String address;
    private String city;

    @Column(name = "phone")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private CustomerType type;

    @Column(name = "account_code", length = 20)
    private String accountCode;

    @Column(name = "opening_balance", precision = 15, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "opening_balance_date")
    private LocalDate openingBalanceDate;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    @Column(name = "identity_number", length = 11)
    private String identityNumber;

    @Column(length = 34)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency currency;

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_role", length = 10)
    private CustomerRole customerRole;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private CustomerStatus status;

    @Column(name = "customer_group", length = 50)
    private String customerGroup;

    @Transient
    private BigDecimal currentBalance;
}
