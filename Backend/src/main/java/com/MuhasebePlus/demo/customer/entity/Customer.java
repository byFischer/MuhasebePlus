package com.MuhasebePlus.demo.customer.entity;

import com.MuhasebePlus.demo.common.entity.SoftDeletableEntity;
import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    private String taxNumber;
    private String address;
    private String city;

    @Column(name = "phone")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private CustomerType type;

    private BigDecimal currentBalance;
}
