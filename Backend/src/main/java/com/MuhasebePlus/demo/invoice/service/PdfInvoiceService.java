package com.MuhasebePlus.demo.invoice.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;

    private static final float A4_W = 595;
    private static final float A4_H = 842;
    private static final float MARGIN = 40;
    private static final float CONTENT_W = A4_W - 2 * MARGIN;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public byte[] generatePdf(Long invoiceId) {
        return generatePdf(invoiceId, companyContext.getCurrentCompanyId());
    }

    public byte[] generatePdf(Long invoiceId, Long companyId) {
        Invoice invoice = invoiceRepository.findByInvoiceIdAndCompanyCompanyId(invoiceId, companyId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyId(invoice.getCustomerId(), companyId)
                .orElse(null);
        Company company = companyRepository.findById(companyId)
                .orElse(null);

        List<InvoiceLineItem> lineItems = lineItemRepository
                .findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoiceId, companyId);

        java.util.Map<Integer, Product> productMap = new java.util.HashMap<>();
        for (InvoiceLineItem li : lineItems) {
            if (!productMap.containsKey(li.getProductId())) {
                productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(li.getProductId(), companyId)
                        .ifPresent(p -> productMap.put(li.getProductId(), p));
            }
        }

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont font = loadFont(doc);
            PDFont fontBold = loadFontBold(doc);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = A4_H - MARGIN;
                y = drawCompanyHeader(cs, font, fontBold, company, y);
                y -= 20;
                y = drawInvoiceInfo(cs, font, fontBold, invoice, y);
                y -= 15;
                y = drawCustomerInfo(cs, font, fontBold, customer, y);
                y -= 15;
                y = drawLineItems(cs, font, fontBold, lineItems, productMap, y);
                y -= 10;
                y = drawTotals(cs, font, fontBold, invoice, y);
                y -= 30;
                drawFooter(cs, font, invoice, y);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF oluşturulamadı: " + e.getMessage(), e);
        }
    }

    private PDFont loadFont(PDDocument doc) throws Exception {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/arial.ttf");
            if (is != null) return PDType0Font.load(doc, is);
        } catch (Exception ignored) {
            // Custom font not bundled in classpath — fall back to next candidate.
        }
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/segoeui.ttf");
            if (is != null) return PDType0Font.load(doc, is);
        } catch (Exception ignored) {
            // Custom font not bundled in classpath — fall back to built-in Helvetica.
        }
        return new org.apache.pdfbox.pdmodel.font.PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private PDFont loadFontBold(PDDocument doc) throws Exception {
        try {
            return PDType0Font.load(doc, getClass().getResourceAsStream("/fonts/arial.ttf"));
        } catch (Exception ignored) {
            // Custom font not bundled in classpath — fall back to loadFont() for regular weight.
        }
        return loadFont(doc);
    }

    private float drawCompanyHeader(PDPageContentStream cs, PDFont font, PDFont fontBold, Company company, float y) throws Exception {
        if (company == null) return y;

        float x = MARGIN;
        cs.beginText();
        cs.setFont(fontBold, 16);
        cs.setNonStrokingColor(Color.BLACK);
        cs.newLineAtOffset(x, y);
        cs.showText(company.getCompanyName() != null ? company.getCompanyName() : "");
        cs.endText();
        y -= 20;

        cs.beginText();
        cs.setFont(font, 9);
        cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
        cs.newLineAtOffset(x, y);

        StringBuilder info = new StringBuilder();
        if (company.getTaxOffice() != null) info.append("Vergi Dairesi: ").append(company.getTaxOffice());
        if (company.getTaxNumber() != null) {
            if (info.length() > 0) info.append(" | ");
            info.append("VKN: ").append(company.getTaxNumber());
        }
        cs.showText(info.toString());
        cs.endText();
        y -= 13;

        if (company.getAddress() != null || company.getCity() != null || company.getPhone() != null) {
            cs.beginText();
            cs.setFont(font, 8);
            cs.newLineAtOffset(x, y);
            StringBuilder addr = new StringBuilder();
            if (company.getAddress() != null) addr.append(company.getAddress());
            if (company.getCity() != null) addr.append(" ").append(company.getCity());
            if (company.getPhone() != null) addr.append(" | ").append(company.getPhone());
            cs.showText(truncate(addr.toString(), 100));
            cs.endText();
            y -= 12;
        }

        drawHr(cs, y);
        return y - 8;
    }

    private float drawInvoiceInfo(PDPageContentStream cs, PDFont font, PDFont fontBold, Invoice invoice, float y) throws Exception {
        float x = MARGIN;

        String typeLabel = invoice.getInvoiceType() != null && invoice.getInvoiceType().name().equals("purchase")
                ? "ALIS FATURASI" : "SATIS FATURASI";

        cs.beginText();
        cs.setFont(fontBold, 14);
        cs.setNonStrokingColor(Color.BLACK);
        cs.newLineAtOffset(x, y);
        cs.showText(typeLabel);
        cs.endText();
        y -= 22;

        String[][] rows = {
                {"Fatura No:", invoice.getInvoiceNumber()},
                {"Tarih:", invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FMT) : "—"},
                {"Vade:", invoice.getDueDate() != null ? invoice.getDueDate().format(DATE_FMT) : "—"},
        };

        for (String[] row : rows) {
            cs.beginText();
            cs.setFont(font, 9);
            cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
            cs.newLineAtOffset(x, y);
            cs.showText(row[0]);
            cs.endText();

            cs.beginText();
            cs.setFont(fontBold, 9);
            cs.setNonStrokingColor(Color.BLACK);
            cs.newLineAtOffset(x + 70, y);
            cs.showText(row[1] != null ? row[1] : "—");
            cs.endText();
            y -= 13;

            x += 200;
        }

        drawHr(cs, y + 13);
        return y - 5;
    }

    private float drawCustomerInfo(PDPageContentStream cs, PDFont font, PDFont fontBold, Customer customer, float y) throws Exception {
        if (customer == null) return y;

        cs.beginText();
        cs.setFont(fontBold, 10);
        cs.setNonStrokingColor(Color.BLACK);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("MUSTERI BILGILERI");
        cs.endText();
        y -= 16;

        String[][] rows = {
                {"Unvan:", customer.getName()},
                {"Vergi Dairesi:", customer.getTaxOffice()},
                {"VKN/TCKN:", customer.getTaxNumber()},
                {"Adres:", customer.getAddress() + (customer.getCity() != null ? " / " + customer.getCity() : "")},
        };

        float labelX = MARGIN;
        float valueX = MARGIN + 80;

        for (String[] row : rows) {
            if (row[1] == null || row[1].isBlank()) continue;

            cs.beginText();
            cs.setFont(font, 9);
            cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
            cs.newLineAtOffset(labelX, y);
            cs.showText(row[0]);
            cs.endText();

            cs.beginText();
            cs.setFont(font, 9);
            cs.setNonStrokingColor(Color.BLACK);
            cs.newLineAtOffset(valueX, y);
            cs.showText(truncate(row[1], 80));
            cs.endText();
            y -= 13;
        }

        drawHr(cs, y + 13);
        return y - 5;
    }

    private float drawLineItems(PDPageContentStream cs, PDFont font, PDFont fontBold,
                                 List<InvoiceLineItem> lineItems,
                                 java.util.Map<Integer, Product> productMap, float y) throws Exception {

        float[] colX = {MARGIN, MARGIN + 22, MARGIN + 170, MARGIN + 240, MARGIN + 310, MARGIN + 370, MARGIN + 430, MARGIN + 500};
        String[] headers = {"#", "Urun Adi", "Miktar", "B.Fiyat", "Isk.%", "KDV%", "Stop.%", "Tutar"};
        float[] colWidths = {22, 148, 60, 60, 50, 50, 50, 90};

        cs.setLineWidth(0.5f);
        cs.setStrokingColor(new Color(0xCC, 0xCC, 0xCC));

        for (int i = 0; i < headers.length; i++) {
            float tx;
            if (i >= headers.length - 1) {
                tx = colX[i] + colWidths[i] - 4 - (fontBold.getStringWidth(headers[i]) / 1000f * 8f);
            } else {
                tx = colX[i];
            }
            cs.beginText();
            cs.setFont(fontBold, 8);
            cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
            cs.newLineAtOffset(tx, y);
            cs.showText(headers[i]);
            cs.endText();
        }
        y -= 14;

        cs.moveTo(MARGIN, y);
        cs.lineTo(A4_W - MARGIN, y);
        cs.stroke();
        y -= 6;

        int idx = 1;
        for (InvoiceLineItem li : lineItems) {
            Product p = productMap.get(li.getProductId());
            String productName = p != null ? p.getName() : ("#" + li.getProductId());

            cs.beginText();
            cs.setFont(font, 8);
            cs.setNonStrokingColor(Color.BLACK);

            cs.newLineAtOffset(colX[0], y);
            cs.showText(String.valueOf(idx));
            cs.endText();

            cs.beginText();
            cs.setFont(font, 8);
            cs.newLineAtOffset(colX[1], y);
            cs.showText(truncate(productName, 28));
            cs.endText();

            cs.beginText();
            cs.setFont(font, 8);
            cs.newLineAtOffset(alignRight(colX[2], colWidths[2], String.valueOf(li.getQuantity()), font, 8), y);
            cs.showText(String.valueOf(li.getQuantity()));
            cs.endText();

            cs.beginText();
            cs.setFont(font, 8);
            cs.newLineAtOffset(alignRight(colX[3], colWidths[3], fmt(li.getUnitPrice()), font, 8), y);
            cs.showText(fmt(li.getUnitPrice()));
            cs.endText();

            cs.beginText();
            cs.setFont(font, 8);
            String disc = li.getDiscountRate() != null && li.getDiscountRate().compareTo(BigDecimal.ZERO) > 0 ? "%" + li.getDiscountRate() : "—";
            cs.newLineAtOffset(alignRight(colX[4], colWidths[4], disc, font, 8), y);
            cs.showText(disc);
            cs.endText();

            cs.beginText();
            cs.setFont(font, 8);
            cs.newLineAtOffset(alignRight(colX[5], colWidths[5], "%" + (li.getVatRate() != null ? li.getVatRate() : "0"), font, 8), y);
            cs.showText("%" + (li.getVatRate() != null ? li.getVatRate().toString() : "0"));
            cs.endText();

            cs.beginText();
            cs.setFont(font, 8);
            String wt = li.getWithholdingTaxRate() != null && li.getWithholdingTaxRate().compareTo(BigDecimal.ZERO) > 0 ? "%" + li.getWithholdingTaxRate() : "—";
            cs.newLineAtOffset(alignRight(colX[6], colWidths[6], wt, font, 8), y);
            cs.showText(wt);
            cs.endText();

            cs.beginText();
            cs.setFont(font, 8);
            cs.newLineAtOffset(alignRight(colX[7], colWidths[7], fmt(li.getLineTotal()), font, 8), y);
            cs.showText(fmt(li.getLineTotal()));
            cs.endText();

            y -= 14;
            idx++;
        }

        drawHr(cs, y + 2);
        return y - 4;
    }

    private float drawTotals(PDPageContentStream cs, PDFont font, PDFont fontBold, Invoice invoice, float y) throws Exception {
        float labelX = A4_W - MARGIN - 200;
        float valueX = A4_W - MARGIN - 4;
        float lineH = 13;

        cs.beginText();
        cs.setFont(font, 9);
        cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
        cs.newLineAtOffset(labelX, y);
        cs.showText("Ara Toplam:");
        cs.endText();

        cs.beginText();
        cs.setFont(font, 9);
        cs.setNonStrokingColor(Color.BLACK);
        cs.newLineAtOffset(alignRightValue(valueX, fmt(invoice.getSubtotal()), font, 9), y);
        cs.showText(fmt(invoice.getSubtotal()) + " TL");
        cs.endText();
        y -= lineH;

        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            String discLabel;
            if (invoice.getDiscountType() != null && "PERCENTAGE".equals(invoice.getDiscountType().name())) {
                discLabel = "Iskonto (%" + invoice.getDiscountAmount() + "):";
            } else {
                discLabel = "Iskonto:";
            }
            cs.beginText();
            cs.setFont(font, 9);
            cs.setNonStrokingColor(new Color(0xCC, 0x44, 0x44));
            cs.newLineAtOffset(labelX, y);
            cs.showText(discLabel);
            cs.endText();

            cs.beginText();
            cs.setFont(font, 9);
            cs.setNonStrokingColor(new Color(0xCC, 0x44, 0x44));
            cs.newLineAtOffset(alignRightValue(valueX, "-" + fmt(invoice.getDiscountAmount()), font, 9), y);
            cs.showText("-" + fmt(invoice.getDiscountAmount()) + " TL");
            cs.endText();
            y -= lineH;
        }

        cs.beginText();
        cs.setFont(font, 9);
        cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
        cs.newLineAtOffset(labelX, y);
        cs.showText("KDV:");
        cs.endText();

        cs.beginText();
        cs.setFont(font, 9);
        cs.setNonStrokingColor(Color.BLACK);
        cs.newLineAtOffset(alignRightValue(valueX, fmt(invoice.getVatAmount()), font, 9), y);
        cs.showText(fmt(invoice.getVatAmount()) + " TL");
        cs.endText();
        y -= lineH;

        if (invoice.getWithholdingTaxAmount() != null && invoice.getWithholdingTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            cs.beginText();
            cs.setFont(font, 9);
            cs.setNonStrokingColor(new Color(0xCC, 0x44, 0x44));
            cs.newLineAtOffset(labelX, y);
            cs.showText("Stopaj:");
            cs.endText();

            cs.beginText();
            cs.setFont(font, 9);
            cs.setNonStrokingColor(new Color(0xCC, 0x44, 0x44));
            cs.newLineAtOffset(alignRightValue(valueX, "-" + fmt(invoice.getWithholdingTaxAmount()), font, 9), y);
            cs.showText("-" + fmt(invoice.getWithholdingTaxAmount()) + " TL");
            cs.endText();
            y -= lineH;
        }

        cs.setLineWidth(1f);
        cs.setStrokingColor(Color.BLACK);
        cs.moveTo(labelX, y + lineH - 2);
        cs.lineTo(valueX + 40, y + lineH - 2);
        cs.stroke();

        cs.beginText();
        cs.setFont(fontBold, 11);
        cs.setNonStrokingColor(Color.BLACK);
        cs.newLineAtOffset(labelX, y - 4);
        cs.showText("GENEL TOPLAM:");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 11);
        cs.newLineAtOffset(alignRightValue(valueX, fmt(invoice.getTotalAmount()), fontBold, 11), y - 4);
        cs.showText(fmt(invoice.getTotalAmount()) + " TL");
        cs.endText();
        y -= 22;

        String inWords = amountToWords(invoice.getTotalAmount());
        if (inWords != null) {
            cs.beginText();
            cs.setFont(font, 8);
            cs.setNonStrokingColor(new Color(0x55, 0x55, 0x55));
            cs.newLineAtOffset(labelX, y);
            cs.showText("Yalniz: " + inWords + " TL");
            cs.endText();
            y -= 14;
        }

        return y;
    }

    private void drawFooter(PDPageContentStream cs, PDFont font, Invoice invoice, float y) throws Exception {
        drawHr(cs, y);
        y -= 20;

        cs.beginText();
        cs.setFont(font, 9);
        cs.setNonStrokingColor(Color.BLACK);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText("Teslim Alan");
        cs.endText();

        cs.beginText();
        cs.setFont(font, 9);
        cs.newLineAtOffset(A4_W - MARGIN - 80, y);
        cs.showText("Teslim Eden");
        cs.endText();
        y -= 18;

        cs.moveTo(MARGIN, y);
        cs.lineTo(MARGIN + 140, y);
        cs.stroke();

        cs.moveTo(A4_W - MARGIN - 140, y);
        cs.lineTo(A4_W - MARGIN, y);
        cs.stroke();

        y -= 30;

        cs.beginText();
        cs.setFont(font, 7);
        cs.setNonStrokingColor(new Color(0x99, 0x99, 0x99));
        String footer = "Bu fatura MuhasebePlus tarafindan elektronik ortamda duzenlenmistir. | " + LocalDate.now().format(DATE_FMT);
        float tw = font.getStringWidth(footer) / 1000f * 7f;
        cs.newLineAtOffset((A4_W - tw) / 2, y);
        cs.showText(footer);
        cs.endText();
    }

    private void drawHr(PDPageContentStream cs, float y) throws Exception {
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(new Color(0xDD, 0xDD, 0xDD));
        cs.moveTo(MARGIN, y);
        cs.lineTo(A4_W - MARGIN, y);
        cs.stroke();
    }

    private float alignRight(float colX, float colW, String text, PDFont font, float fontSize) throws Exception {
        float tw = font.getStringWidth(text) / 1000f * fontSize;
        return colX + colW - tw - 4;
    }

    private float alignRightValue(float rightX, String text, PDFont font, float fontSize) throws Exception {
        float tw = font.getStringWidth(text + " TL") / 1000f * fontSize;
        return rightX - tw;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }

    private String fmt(BigDecimal val) {
        if (val == null) return "0.00";
        return String.format("%,.2f", val.setScale(2, RoundingMode.HALF_UP));
    }

    private String amountToWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return null;

        long tl = amount.longValue();
        long kr = amount.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).longValue();

        String tlWords = numberToWordsTurkish(tl);
        if (tlWords.isEmpty()) tlWords = "sifir";

        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(tlWords.charAt(0))).append(tlWords.substring(1));
        sb.append(" Turk Lirasi");

        if (kr > 0) {
            String krWords = numberToWordsTurkish(kr);
            sb.append(" ").append(krWords).append(" Kurus");
        }

        return sb.toString();
    }

    private String numberToWordsTurkish(long n) {
        if (n == 0) return "";
        if (n < 0) return "eksi " + numberToWordsTurkish(-n);

        String[] ones = {"", "bir", "iki", "uc", "dort", "bes", "alti", "yedi", "sekiz", "dokuz"};
        String[] tens = {"", "on", "yirmi", "otuz", "kirk", "elli", "altmis", "yetmis", "seksen", "doksan"};
        String[] thousands = {"", "bin", "milyon", "milyar", "trilyon"};

        StringBuilder sb = new StringBuilder();
        int group = 0;

        while (n > 0) {
            int part = (int) (n % 1000);
            if (part > 0) {
                String partStr = hundredsToWordsTurkish(part, ones, tens);
                if (group == 1 && part == 1) {
                    sb.insert(0, thousands[group] + " ");
                } else if (group > 0) {
                    sb.insert(0, partStr + " " + thousands[group] + " ");
                } else {
                    sb.insert(0, partStr + " ");
                }
            }
            n /= 1000;
            group++;
        }

        return sb.toString().trim();
    }

    private String hundredsToWordsTurkish(int n, String[] ones, String[] tens) {
        StringBuilder sb = new StringBuilder();
        int yuz = n / 100;
        int on = (n % 100) / 10;
        int bir = n % 10;

        if (yuz > 0) {
            if (yuz == 1) sb.append("yuz ");
            else sb.append(ones[yuz]).append(" yuz ");
        }
        if (on > 0) sb.append(tens[on]).append(" ");
        if (bir > 0) sb.append(ones[bir]).append(" ");

        return sb.toString().trim();
    }
}
