package com.MuhasebePlus.demo.report.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.dto.response.CustomerActivityDto;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.customer.service.CustomerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfStatementService {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;

    private static final float A4_W = 595;
    private static final float A4_H = 842;
    private static final float MARGIN = 40;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public byte[] generatePdf(Long customerId, LocalDate startDate, LocalDate endDate) {
        Long companyId = companyContext.getCurrentCompanyId();
        Company company = companyRepository.findById(companyId).orElse(null);
        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<CustomerActivityDto> activity = customerService.getCustomerActivity(customerId, startDate, endDate);

        BigDecimal opening = customer.getOpeningBalance() != null ? customer.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal closingBalance = opening;
        for (CustomerActivityDto a : activity) {
            totalDebit = totalDebit.add(a.debit());
            totalCredit = totalCredit.add(a.credit());
            closingBalance = a.balance();
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont font = loadFont(doc);
            PDFont fontBold = font;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = A4_H - MARGIN;

                cs.beginText();
                cs.setFont(fontBold, 14);
                cs.setNonStrokingColor(Color.BLACK);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("CARI HESAP EKSTRESI");
                cs.endText();
                y -= 22;

                if (company != null) {
                    cs.beginText();
                    cs.setFont(font, 9);
                    cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText(company.getCompanyName());
                    cs.endText();
                    y -= 14;
                }

                String[][] info = {
                    {"Musdeti:", customer.getName(), "Hesap Kodu:", customer.getAccountCode() != null ? customer.getAccountCode() : "—"},
                    {"VKN/TCKN:", customer.getTaxNumber(), "Vergi Dairesi:", customer.getTaxOffice() != null ? customer.getTaxOffice() : "—"},
                    {"Donem:", startDate.format(DATE_FMT) + " - " + endDate.format(DATE_FMT), "", ""},
                };

                for (String[] row : info) {
                    cs.beginText();
                    cs.setFont(font, 8);
                    cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText(row[0]);
                    cs.endText();
                    cs.beginText();
                    cs.setFont(font, 8);
                    cs.setNonStrokingColor(Color.BLACK);
                    cs.newLineAtOffset(MARGIN + 70, y);
                    cs.showText(row[1] != null ? row[1] : "—");
                    cs.endText();
                    if (row[2] != null && !row[2].isEmpty()) {
                        cs.beginText();
                        cs.setFont(font, 8);
                        cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
                        cs.newLineAtOffset(MARGIN + 220, y);
                        cs.showText(row[2]);
                        cs.endText();
                        cs.beginText();
                        cs.setFont(font, 8);
                        cs.setNonStrokingColor(Color.BLACK);
                        cs.newLineAtOffset(MARGIN + 300, y);
                        cs.showText(row[3] != null ? row[3] : "—");
                        cs.endText();
                    }
                    y -= 13;
                }
                y -= 10;

                float[] cx = {MARGIN, MARGIN + 65, MARGIN + 130, MARGIN + 260, MARGIN + 330, MARGIN + 400, MARGIN + 470};
                String[] headers = {"Tarih", "Tur", "Aciklama", "Belge No", "Borc", "Alacak", "Bakiye"};

                cs.setLineWidth(0.5f);
                cs.setStrokingColor(new Color(0xCC, 0xCC, 0xCC));
                for (int i = 0; i < headers.length; i++) {
                    cs.beginText();
                    cs.setFont(fontBold, 7);
                    cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
                    cs.newLineAtOffset(cx[i], y);
                    cs.showText(headers[i]);
                    cs.endText();
                }
                y -= 12;
                cs.moveTo(MARGIN, y);
                cs.lineTo(A4_W - MARGIN, y);
                cs.stroke();
                y -= 4;

                for (CustomerActivityDto a : activity) {
                    if (y < 100) break;
                    cs.beginText();
                    cs.setFont(font, 7);
                    cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
                    cs.newLineAtOffset(cx[0], y);
                    cs.showText(a.date() != null ? a.date().format(DATE_FMT) : "—");
                    cs.endText();

                    cs.beginText();
                    cs.setFont(font, 7);
                    cs.setNonStrokingColor(Color.BLACK);
                    cs.newLineAtOffset(cx[1], y);
                    cs.showText(a.type() != null ? a.type() : "—");
                    cs.endText();

                    cs.beginText();
                    cs.setFont(font, 7);
                    cs.newLineAtOffset(cx[2], y);
                    cs.showText(truncate(a.description(), 20));
                    cs.endText();

                    cs.beginText();
                    cs.setFont(font, 7);
                    cs.newLineAtOffset(cx[3], y);
                    cs.showText(truncate(a.documentNo(), 15));
                    cs.endText();

                    cs.beginText();
                    cs.setFont(font, 7);
                    cs.setNonStrokingColor(a.debit().compareTo(BigDecimal.ZERO) > 0 ? new Color(0xCC, 0x44, 0x44) : new Color(0x55, 0x55, 0x55));
                    cs.newLineAtOffset(cx[4], y);
                    cs.showText(a.debit().compareTo(BigDecimal.ZERO) > 0 ? fmt(a.debit()) : "");
                    cs.endText();

                    cs.beginText();
                    cs.setFont(font, 7);
                    cs.setNonStrokingColor(a.credit().compareTo(BigDecimal.ZERO) > 0 ? new Color(0x44, 0x88, 0x44) : new Color(0x55, 0x55, 0x55));
                    cs.newLineAtOffset(cx[5], y);
                    cs.showText(a.credit().compareTo(BigDecimal.ZERO) > 0 ? fmt(a.credit()) : "");
                    cs.endText();

                    cs.beginText();
                    cs.setFont(font, 7);
                    cs.setNonStrokingColor(a.balance().compareTo(BigDecimal.ZERO) < 0 ? new Color(0xCC, 0x44, 0x44) : Color.BLACK);
                    cs.newLineAtOffset(cx[6], y);
                    cs.showText(fmt(a.balance()));
                    cs.endText();
                    y -= 14;
                }
                y -= 10;

                cs.setLineWidth(1f);
                cs.setStrokingColor(Color.BLACK);
                cs.moveTo(MARGIN + 300, y);
                cs.lineTo(A4_W - MARGIN, y);
                cs.stroke();
                y -= 16;

                cs.beginText();
                cs.setFont(font, 8);
                cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
                cs.newLineAtOffset(MARGIN + 300, y);
                cs.showText("Toplam Borc: " + fmt(totalDebit) + " TL");
                cs.endText();
                y -= 13;

                cs.beginText();
                cs.setFont(font, 8);
                cs.newLineAtOffset(MARGIN + 300, y);
                cs.showText("Toplam Alacak: " + fmt(totalCredit) + " TL");
                cs.endText();
                y -= 13;

                cs.beginText();
                cs.setFont(fontBold, 10);
                cs.setNonStrokingColor(Color.BLACK);
                cs.newLineAtOffset(MARGIN + 300, y);
                cs.showText("Kapanis Bakiyesi: " + fmt(closingBalance) + " TL");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF ekstre olusturulamadi: " + e.getMessage(), e);
        }
    }

    private PDFont loadFont(PDDocument doc) throws Exception {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/arial.ttf");
            if (is != null) return PDType0Font.load(doc, is);
        } catch (Exception ignored) {
            // Custom font not bundled in classpath — fall back to built-in Helvetica.
        }
        return new org.apache.pdfbox.pdmodel.font.PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }

    private String fmt(BigDecimal val) {
        if (val == null) return "0.00";
        return String.format("%,.2f", val);
    }
}
