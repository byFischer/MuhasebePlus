package com.MuhasebePlus.demo.cheque.repository;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import com.MuhasebePlus.demo.cheque.entity.Cheque;
import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeStatus;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class ChequeRepositoryTest {

    @Autowired ChequeRepository chequeRepository;
    @Autowired CompanyRepository companyRepository;

    private Company company;
    private Company otherCompany;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .companyName("Çek Test A.Ş.").taxNumber("3333333333").build());
        otherCompany = companyRepository.save(Company.builder()
                .companyName("Diğer A.Ş.").taxNumber("4444444444").build());
    }

    // ── existsByChequeNumber ──────────────────────────────────────────────────

    @Test
    void existsByChequeNumber_whenExists_returnsTrue() {
        chequeRepository.save(cheque("CHK-001", ChequeStatus.IN_PORTFOLIO, ChequeType.RECEIVABLE, company, false));

        assertThat(chequeRepository.existsByChequeNumberAndCompanyCompanyId("CHK-001", company.getCompanyId()))
                .isTrue();
    }

    @Test
    void existsByChequeNumber_whenNotExists_returnsFalse() {
        assertThat(chequeRepository.existsByChequeNumberAndCompanyCompanyId("CHK-999", company.getCompanyId()))
                .isFalse();
    }

    @Test
    void existsByChequeNumber_differentCompany_returnsFalse() {
        chequeRepository.save(cheque("CHK-001", ChequeStatus.IN_PORTFOLIO, ChequeType.RECEIVABLE, otherCompany, false));

        assertThat(chequeRepository.existsByChequeNumberAndCompanyCompanyId("CHK-001", company.getCompanyId()))
                .isFalse();
    }

    // ── Status filtering ──────────────────────────────────────────────────────

    @Test
    void findByStatus_returnsOnlyMatchingStatus() {
        chequeRepository.save(cheque("CHK-A1", ChequeStatus.IN_PORTFOLIO, ChequeType.RECEIVABLE, company, false));
        chequeRepository.save(cheque("CHK-A2", ChequeStatus.DEPOSITED, ChequeType.RECEIVABLE, company, false));

        List<Cheque> result = chequeRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                company.getCompanyId(), ChequeStatus.IN_PORTFOLIO);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChequeNumber()).isEqualTo("CHK-A1");
    }

    @Test
    void findByStatus_softDeleted_notReturned() {
        chequeRepository.save(cheque("CHK-B1", ChequeStatus.IN_PORTFOLIO, ChequeType.RECEIVABLE, company, true));

        List<Cheque> result = chequeRepository.findByCompanyCompanyIdAndStatusAndIsDeletedFalse(
                company.getCompanyId(), ChequeStatus.IN_PORTFOLIO);

        assertThat(result).isEmpty();
    }

    // ── ChequeType filtering ──────────────────────────────────────────────────

    @Test
    void findByChequeType_returnsOnlyMatchingType() {
        chequeRepository.save(cheque("CHK-C1", ChequeStatus.IN_PORTFOLIO, ChequeType.RECEIVABLE, company, false));
        chequeRepository.save(cheque("CHK-C2", ChequeStatus.IN_PORTFOLIO, ChequeType.PAYABLE, company, false));

        List<Cheque> result = chequeRepository.findByCompanyCompanyIdAndChequeTypeAndIsDeletedFalse(
                company.getCompanyId(), ChequeType.RECEIVABLE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChequeType()).isEqualTo(ChequeType.RECEIVABLE);
    }

    // ── Purge candidates ──────────────────────────────────────────────────────

    @Test
    void findByIsDeletedTrueAndDeletedAtBefore_returnsSoftDeletedBeforeCutoff() {
        Cheque old = cheque("CHK-D1", ChequeStatus.CANCELLED, ChequeType.RECEIVABLE, company, true);
        old.setDeletedAt(LocalDateTime.now().minusDays(91));
        chequeRepository.save(old);

        Cheque recent = cheque("CHK-D2", ChequeStatus.CANCELLED, ChequeType.RECEIVABLE, company, true);
        recent.setDeletedAt(LocalDateTime.now().minusDays(1));
        chequeRepository.save(recent);

        List<Cheque> result = chequeRepository.findByIsDeletedTrueAndDeletedAtBefore(
                LocalDateTime.now().minusDays(30));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChequeNumber()).isEqualTo("CHK-D1");
    }

    // ── Yardımcı ──────────────────────────────────────────────────────────────

    private Cheque cheque(String number, ChequeStatus status, ChequeType type, Company c, boolean deleted) {
        Cheque ch = Cheque.builder()
                .chequeNumber(number)
                .chequeType(type)
                .chequeKind(ChequeKind.CHEQUE)
                .amount(new BigDecimal("1000.00"))
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(status)
                .company(c)
                .build();
        ch.setDeleted(deleted);
        return ch;
    }
}
