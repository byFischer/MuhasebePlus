package com.MuhasebePlus.demo.customer.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import com.MuhasebePlus.demo.customer.dto.response.CustomerResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerPdfService {

    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;
    private final CustomerService customerService;

    private static final float LANDSCAPE_W = 842;
    private static final float LANDSCAPE_H = 595;
    private static final float MARGIN = 30;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public byte[] exportToPdf() {
        Long companyId = companyContext.getCurrentCompanyId();
        Company company = companyRepository.findById(companyId).orElse(null);
        List<CustomerResponseDto> customers = customerService.getAllCustomers();

        try (PDDocument doc = new PDDocument()) {
            PDFont font = loadFont(doc);

            float[] colX = {MARGIN, MARGIN + 60, MARGIN + 155, MARGIN + 205, MARGIN + 280,
                    MARGIN + 350, MARGIN + 440, MARGIN + 500, MARGIN + 560, MARGIN + 620,
                    MARGIN + 680, MARGIN + 740};
            String[] headers = {"Hesap K.", "Ad", "Tip", "VKN", "Sehir", "Bakiye", "Doviz",
                    "Durum", "Grup", "Rol", "Tel", "E-posta"};
            float[] widths = {60, 95, 50, 75, 70, 90, 60, 50, 60, 50, 60, 90};

            float y = LANDSCAPE_H - MARGIN;
            int pageNum = 1;
            PDPage page = new PDPage(new PDRectangle(LANDSCAPE_W, LANDSCAPE_H));
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            int rowIdx = 0;
            for (CustomerResponseDto c : customers) {
                if (y < MARGIN + 20 || rowIdx == 0) {
                    if (rowIdx > 0) {
                        cs.close();
                    }

                    y = LANDSCAPE_H - MARGIN;
                    if (rowIdx > 0) {
                        page = new PDPage(new PDRectangle(LANDSCAPE_W, LANDSCAPE_H));
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        pageNum++;
                    }

                    cs.beginText();
                    cs.setFont(font, 12);
                    cs.setNonStrokingColor(Color.BLACK);
                    cs.newLineAtOffset(MARGIN, y);
                    String title = "MUSTERI LISTESI";
                    if (company != null) title += " - " + company.getCompanyName();
                    cs.showText(title);
                    cs.endText();
                    y -= 16;

                    cs.beginText();
                    cs.setFont(font, 7);
                    cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText("Tarih: " + LocalDate.now().format(DATE_FMT) + " | Toplam: " + customers.size() + " musteri | Sayfa: " + pageNum);
                    cs.endText();
                    y -= 10;

                    cs.setLineWidth(0.5f);
                    cs.setStrokingColor(new Color(0xCC, 0xCC, 0xCC));
                    cs.moveTo(MARGIN, y);
                    cs.lineTo(LANDSCAPE_W - MARGIN, y);
                    cs.stroke();
                    y -= 4;

                    for (int i = 0; i < headers.length; i++) {
                        cs.beginText();
                        cs.setFont(font, 6);
                        cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
                        cs.newLineAtOffset(colX[i], y);
                        cs.showText(headers[i]);
                        cs.endText();
                    }
                    y -= 12;
                    cs.moveTo(MARGIN, y);
                    cs.lineTo(LANDSCAPE_W - MARGIN, y);
                    cs.stroke();
                    y -= 4;
                }

                String[] values = {
                    c.accountCode(), c.name(), c.type(), c.taxNumber(), c.city(),
                    fmt(c.currentBalance()), c.currency(), c.status(), c.customerGroup(),
                    c.customerRole(), c.phoneNumber(), c.email()
                };

                for (int i = 0; i < values.length; i++) {
                    cs.beginText();
                    cs.setFont(font, 6);
                    cs.setNonStrokingColor(Color.BLACK);
                    cs.newLineAtOffset(colX[i], y);
                    cs.showText(truncate(values[i], i == 1 ? 20 : (i == 11 ? 18 : 15)));
                    cs.endText();
                }
                y -= 13;

                if (y < MARGIN + 20) {
                    cs.setStrokingColor(new Color(0xDD, 0xDD, 0xDD));
                    cs.moveTo(MARGIN, y + 6);
                    cs.lineTo(LANDSCAPE_W - MARGIN, y + 6);
                    cs.stroke();
                }

                rowIdx++;
            }

            cs.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF olusturulamadi: " + e.getMessage(), e);
        }
    }

    private PDFont loadFont(PDDocument doc) throws Exception {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/arial.ttf");
            if (is != null) return PDType0Font.load(doc, is);
        } catch (Exception ignored) {}
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
