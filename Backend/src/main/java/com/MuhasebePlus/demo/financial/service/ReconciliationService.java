package com.MuhasebePlus.demo.financial.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.ManualMatchRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BankStatementLineDto;
import com.MuhasebePlus.demo.financial.dto.response.BankStatementResponseDto;
import com.MuhasebePlus.demo.financial.entity.*;
import com.MuhasebePlus.demo.financial.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service("bankReconciliationService")
@Transactional
@RequiredArgsConstructor
public class ReconciliationService {

    private final BankStatementRepository statementRepository;
    private final BankStatementLineRepository lineRepository;
    private final ReconciliationMatchRepository matchRepository;
    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<BankStatementResponseDto> getAll() {
        Long companyId = companyContext.getCurrentCompanyId();
        return statementRepository.findByCompanyCompanyIdOrderByCreatedAtDesc(companyId)
                .stream().map(s -> toDto(s, false)).toList();
    }

    @Transactional(readOnly = true)
    public BankStatementResponseDto getById(Long id) {
        Long companyId = companyContext.getCurrentCompanyId();
        BankStatement stmt = statementRepository.findByIdAndCompanyCompanyId(id, companyId)
                .orElseThrow(() -> new RuntimeException("Ekstre bulunamadı: " + id));
        return toDto(stmt, true);
    }

    public BankStatementResponseDto importCsv(MultipartFile file, Long bankAccountId, LocalDate statementDate,
                                               BigDecimal openingBalance, BigDecimal closingBalance) {
        Long companyId = companyContext.getCurrentCompanyId();
        BankAccount account = bankAccountRepository.findByAccountIdAndCompanyCompanyId(bankAccountId, companyId)
                .orElseThrow(() -> new RuntimeException("Hesap bulunamadı: " + bankAccountId));

        BankStatement stmt = BankStatement.builder()
                .company(companyRepository.getReferenceById(companyId))
                .bankAccount(account)
                .statementDate(statementDate)
                .openingBalance(openingBalance != null ? openingBalance : BigDecimal.ZERO)
                .closingBalance(closingBalance != null ? closingBalance : BigDecimal.ZERO)
                .build();
        stmt = statementRepository.save(stmt);

        List<BankStatementLine> lines = new ArrayList<>();
        String contentType = file.getContentType();
        try {
            if (contentType != null && contentType.contains("sheet")) {
                lines = parseXlsx(file, stmt);
            } else {
                lines = parseCsv(file, stmt);
            }
        } catch (Exception e) {
            log.error("Import failed", e);
            throw new RuntimeException("Dosya ayrıştırılamadı: " + e.getMessage());
        }
        lineRepository.saveAll(lines);
        stmt.setLines(lines);
        log.info("Imported {} lines for bankAccountId={} companyId={}", lines.size(), bankAccountId, companyId);
        return toDto(stmt, true);
    }

    public BankStatementResponseDto autoMatch(Long statementId) {
        Long companyId = companyContext.getCurrentCompanyId();
        BankStatement stmt = statementRepository.findByIdAndCompanyCompanyId(statementId, companyId)
                .orElseThrow(() -> new RuntimeException("Ekstre bulunamadı: " + statementId));

        List<BankStatementLine> unmatched = lineRepository.findByBankStatementIdAndIsMatchedFalse(statementId);
        List<Transaction> transactions = transactionRepository
                .findByAccountIdAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(stmt.getBankAccount().getAccountId(), companyId);

        int matched = 0;
        for (BankStatementLine line : unmatched) {
            // Find transaction: same amount, date within ±3 days
            Optional<Transaction> match = transactions.stream()
                    .filter(t -> !isAlreadyMatched(t.getTransactionId())
                            && t.getAmount() != null
                            && t.getAmount().compareTo(line.getAmount()) == 0
                            && t.getTransactionDate() != null
                            && !t.getTransactionDate().isBefore(line.getTransactionDate().minusDays(3))
                            && !t.getTransactionDate().isAfter(line.getTransactionDate().plusDays(3)))
                    .findFirst();

            if (match.isPresent()) {
                line.setMatched(true);
                lineRepository.save(line);
                matchRepository.save(ReconciliationMatch.builder()
                        .companyId(companyId)
                        .statementLine(line)
                        .transactionId(match.get().getTransactionId())
                        .matchType("AUTO")
                        .build());
                matched++;
            }
        }

        long totalLines = lineRepository.countByBankStatementId(statementId);
        long matchedLines = lineRepository.countByBankStatementIdAndIsMatchedTrue(statementId);
        if (matchedLines == totalLines && totalLines > 0) {
            stmt.setStatus("COMPLETED");
            statementRepository.save(stmt);
        }

        log.info("Auto-matched {} lines for statementId={} companyId={}", matched, statementId, companyId);
        return toDto(statementRepository.findByIdAndCompanyCompanyId(statementId, companyId).orElseThrow(), true);
    }

    public BankStatementResponseDto manualMatch(ManualMatchRequestDto req) {
        Long companyId = companyContext.getCurrentCompanyId();
        BankStatementLine line = lineRepository.findById(req.statementLineId())
                .orElseThrow(() -> new RuntimeException("Satır bulunamadı: " + req.statementLineId()));

        matchRepository.deleteByStatementLineId(line.getId());
        line.setMatched(true);
        lineRepository.save(line);
        matchRepository.save(ReconciliationMatch.builder()
                .companyId(companyId)
                .statementLine(line)
                .transactionId(req.transactionId())
                .matchType("MANUAL")
                .build());

        BankStatement stmt = line.getBankStatement();
        long totalLines = lineRepository.countByBankStatementId(stmt.getId());
        long matchedLines = lineRepository.countByBankStatementIdAndIsMatchedTrue(stmt.getId());
        if (matchedLines == totalLines && totalLines > 0) {
            stmt.setStatus("COMPLETED");
            statementRepository.save(stmt);
        }

        return toDto(statementRepository.findByIdAndCompanyCompanyId(stmt.getId(), companyId).orElseThrow(), true);
    }

    public BankStatementResponseDto unmatch(Long lineId) {
        Long companyId = companyContext.getCurrentCompanyId();
        BankStatementLine line = lineRepository.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Satır bulunamadı: " + lineId));
        matchRepository.deleteByStatementLineId(lineId);
        line.setMatched(false);
        lineRepository.save(line);

        BankStatement stmt = line.getBankStatement();
        stmt.setStatus("IN_PROGRESS");
        statementRepository.save(stmt);
        return toDto(statementRepository.findByIdAndCompanyCompanyId(stmt.getId(), companyId).orElseThrow(), true);
    }

    private boolean isAlreadyMatched(Long transactionId) {
        return matchRepository.findAll().stream()
                .anyMatch(m -> transactionId.equals(m.getTransactionId()));
    }

    private List<BankStatementLine> parseCsv(MultipartFile file, BankStatement stmt) throws Exception {
        List<BankStatementLine> lines = new ArrayList<>();
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        };
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            reader.readLine(); // skip header row
            String row;
            while ((row = reader.readLine()) != null) {
                if (row.isBlank()) continue;
                String[] cols = row.split("[,;\\t]", -1);
                if (cols.length < 3) continue;
                LocalDate date = parseDate(cols[0].trim(), formatters);
                if (date == null) continue;
                BigDecimal amount = parseDecimal(cols[2].trim());
                BigDecimal balance = cols.length > 3 ? parseDecimal(cols[3].trim()) : null;
                lines.add(BankStatementLine.builder()
                        .bankStatement(stmt)
                        .transactionDate(date)
                        .description(cols.length > 1 ? cols[1].trim() : "")
                        .amount(amount)
                        .balance(balance)
                        .build());
            }
        }
        return lines;
    }

    private List<BankStatementLine> parseXlsx(MultipartFile file, BankStatement stmt) throws Exception {
        List<BankStatementLine> lines = new ArrayList<>();
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        };
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            boolean first = true;
            for (Row row : sheet) {
                if (first) { first = false; continue; }
                Cell dateCell = row.getCell(0);
                Cell descCell = row.getCell(1);
                Cell amtCell  = row.getCell(2);
                Cell balCell  = row.getCell(3);
                if (dateCell == null || amtCell == null) continue;
                LocalDate date = null;
                if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                    date = dateCell.getLocalDateTimeCellValue().toLocalDate();
                } else {
                    date = parseDate(dateCell.toString().trim(), formatters);
                }
                if (date == null) continue;
                BigDecimal amount = parseDecimal(amtCell.toString());
                BigDecimal balance = balCell != null ? parseDecimal(balCell.toString()) : null;
                String desc = descCell != null ? descCell.toString().trim() : "";
                lines.add(BankStatementLine.builder()
                        .bankStatement(stmt)
                        .transactionDate(date)
                        .description(desc)
                        .amount(amount)
                        .balance(balance)
                        .build());
            }
        }
        return lines;
    }

    private LocalDate parseDate(String s, DateTimeFormatter[] formatters) {
        for (DateTimeFormatter f : formatters) {
            try { return LocalDate.parse(s, f); } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private BigDecimal parseDecimal(String s) {
        try {
            return new BigDecimal(s.replace(".", "").replace(",", ".").replaceAll("[^\\d.-]", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BankStatementResponseDto toDto(BankStatement s, boolean includeLines) {
        long total = lineRepository.countByBankStatementId(s.getId());
        long matched = lineRepository.countByBankStatementIdAndIsMatchedTrue(s.getId());

        List<BankStatementLineDto> lineDtos = includeLines
                ? lineRepository.findByBankStatementId(s.getId()).stream()
                    .map(l -> {
                        Long txId = matchRepository.findByStatementLineIdAndCompanyId(l.getId(), s.getCompany().getCompanyId())
                                .map(ReconciliationMatch::getTransactionId).orElse(null);
                        return new BankStatementLineDto(l.getId(), l.getTransactionDate(), l.getDescription(),
                                l.getAmount(), l.getBalance(), l.isMatched(), txId);
                    }).toList()
                : List.of();

        return new BankStatementResponseDto(
                s.getId(),
                s.getBankAccount().getAccountId(),
                s.getBankAccount().getBankName(),
                s.getStatementDate(),
                s.getOpeningBalance(),
                s.getClosingBalance(),
                s.getStatus(),
                total, matched,
                lineDtos,
                s.getCreatedAt());
    }
}
