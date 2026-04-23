package com.MuhasebePlus.demo.customer.entity;

import com.MuhasebePlus.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.MuhasebePlus.demo.company.entity.Company;

@Entity
@Table(name = "customer_note")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerNote extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

}
