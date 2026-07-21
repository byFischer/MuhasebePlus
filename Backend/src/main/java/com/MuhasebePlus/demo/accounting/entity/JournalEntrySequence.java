package com.MuhasebePlus.demo.accounting.entity;

import com.MuhasebePlus.demo.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "journal_entry_sequence")
@IdClass(JournalEntrySequenceId.class)
public class JournalEntrySequence {

    @Id
    @Column(name = "company_id")
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;

    @Id
    @Column(name = "year")
    private int year;

    @Column(name = "last_seq", nullable = false)
    private int lastSeq = 0;
}
