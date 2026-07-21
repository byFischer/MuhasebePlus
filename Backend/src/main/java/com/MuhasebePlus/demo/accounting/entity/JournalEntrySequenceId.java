package com.MuhasebePlus.demo.accounting.entity;

import java.io.Serializable;
import java.util.Objects;

public class JournalEntrySequenceId implements Serializable {

    private Long companyId;
    private int year;

    public JournalEntrySequenceId() {}

    public JournalEntrySequenceId(Long companyId, int year) {
        this.companyId = companyId;
        this.year = year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JournalEntrySequenceId that)) return false;
        return year == that.year && Objects.equals(companyId, that.companyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, year);
    }
}
