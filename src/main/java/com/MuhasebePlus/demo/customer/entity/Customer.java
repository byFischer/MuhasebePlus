package com.MuhasebePlus.demo.customer.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
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
@EqualsAndHashCode(callSuper = true)
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

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
    private boolean isDeleted;
}
