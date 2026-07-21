package com.MuhasebePlus.demo.invoice.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
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
    private static final Locale TR = Locale.forLanguageTag("tr-TR");

    // Kurumsal renk paleti
    private static final Color NAVY = new Color(0x1F, 0x3A, 0x5F);
    private static final Color DARK = new Color(0x1A, 0x1F, 0x29);
    private static final Color GRAY = new Color(0x6B, 0x72, 0x80);
    private static final Color LIGHT = new Color(0xF4, 0xF6, 0xF9);
    private static final Color LINE = new Color(0xD9, 0xDE, 0xE5);
    private static final Color RED = new Color(0xC0, 0x3A, 0x2B);
    private static final Color WHITE = Color.WHITE;

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

        return renderInvoicePdf(invoice, customer, company, lineItems, productMap);
    }

    /**
     * Yüklenmiş verilerden fatura PDF'ini çizer. Veri erişiminden bağımsızdır, bu sayede
     * birim/görsel testlerde sahte entity'lerle doğrudan çağrılabilir.
     */
    byte[] renderInvoicePdf(Invoice invoice, Customer customer, Company company,
                            List<InvoiceLineItem> lineItems,
                            java.util.Map<Integer, Product> productMap) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont font = loadFont(doc);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                drawTopAccent(cs);
                float y = drawHeader(cs, font, company, invoice);
                y = drawCustomerBox(cs, font, customer, y - 22);
                y = drawLineItemsTable(cs, font, lineItems, productMap, invoice, y - 22);
                y = drawTotals(cs, font, invoice, y - 14);
                drawDescription(cs, font, invoice, y - 18);
                drawFooter(cs, font);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /* ----------------------------------------------------------------- */
    /* Bölümler                                                           */
    /* ----------------------------------------------------------------- */

    private void drawTopAccent(PDPageContentStream cs) throws Exception {
        fillRect(cs, 0, A4_H - 6, A4_W, 6, NAVY);
    }

    private float drawHeader(PDPageContentStream cs, PDFont font, Company company, Invoice invoice) throws Exception {
        float topY = A4_H - 34;

        // --- Sol: firma bilgileri ---
        String companyName = company != null && notBlank(company.getCompanyName())
                ? company.getCompanyName() : "Firma";
        text(cs, font, 18, NAVY, MARGIN, topY, companyName, true);

        float ly = topY - 18;
        for (String line : companyInfoLines(company)) {
            text(cs, font, 8.5f, GRAY, MARGIN, ly, truncate(line, 70), false);
            ly -= 12;
        }

        // --- Sağ: fatura başlığı + bilgi paneli ---
        boolean purchase = invoice.getInvoiceType() != null && "purchase".equals(invoice.getInvoiceType().name());
        String title = purchase ? "ALIŞ FATURASI" : "SATIŞ FATURASI";
        textRight(cs, font, 15, NAVY, A4_W - MARGIN, topY, title, true);

        float panelW = 210;
        float panelX = A4_W - MARGIN - panelW;
        float rowH = 16;
        float panelTop = topY - 24;
        float panelH = rowH * 3 + 10;
        float panelBottom = panelTop - panelH;

        fillRect(cs, panelX, panelBottom, panelW, panelH, LIGHT);
        strokeRect(cs, panelX, panelBottom, panelW, panelH, LINE, 0.7f);

        String[][] meta = {
                {"Fatura No", nv(invoice.getInvoiceNumber())},
                {"Tarih", invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FMT) : "—"},
                {"Vade", invoice.getDueDate() != null ? invoice.getDueDate().format(DATE_FMT) : "—"},
        };
        float mry = panelTop - 12;
        for (String[] row : meta) {
            text(cs, font, 8.5f, GRAY, panelX + 10, mry, row[0], false);
            textRight(cs, font, 9, DARK, panelX + panelW - 10, mry, truncate(row[1], 24), true);
            mry -= rowH;
        }

        float bottom = Math.min(ly + 4, panelBottom);
        return bottom;
    }

    private float drawCustomerBox(PDPageContentStream cs, PDFont font, Customer customer, float y) throws Exception {
        text(cs, font, 10, NAVY, MARGIN, y, "MÜŞTERİ BİLGİLERİ", true);
        y -= 8;

        List<String[]> rows = new ArrayList<>();
        if (customer != null) {
            addRow(rows, "Ünvan", customer.getName());
            addRow(rows, "VKN/TCKN", firstNonBlank(customer.getTaxNumber(), customer.getIdentityNumber()));
            addRow(rows, "Vergi Dairesi", customer.getTaxOffice());
            addRow(rows, "Adres", joinNonBlank(" / ", customer.getAddress(), customer.getCity()));
            addRow(rows, "İletişim", joinNonBlank(" • ", customer.getPhoneNumber(), customer.getEmail()));
        }
        if (rows.isEmpty()) {
            addRow(rows, "Ünvan", "—");
        }

        float rowH = 15;
        float boxH = rows.size() * rowH + 12;
        float boxTop = y;
        float boxBottom = boxTop - boxH;

        fillRect(cs, MARGIN, boxBottom, CONTENT_W, boxH, LIGHT);
        strokeRect(cs, MARGIN, boxBottom, CONTENT_W, boxH, LINE, 0.7f);

        float labelX = MARGIN + 12;
        float valueX = MARGIN + 100;
        float ry = boxTop - 14;
        boolean first = true;
        for (String[] row : rows) {
            text(cs, font, 8.5f, GRAY, labelX, ry, row[0], false);
            text(cs, font, 9, DARK, valueX, ry, truncate(row[1], 78), first);
            first = false;
            ry -= rowH;
        }

        return boxBottom;
    }

    private float drawLineItemsTable(PDPageContentStream cs, PDFont font,
                                     List<InvoiceLineItem> lineItems,
                                     java.util.Map<Integer, Product> productMap,
                                     Invoice invoice, float y) throws Exception {

        String cur = currency(invoice);
        String[] headers = {"#", "Ürün / Hizmet", "Miktar", "Birim Fiyat", "İsk. %", "KDV %", "Stopaj %", "Tutar"};
        float[] colW = {24, 151, 46, 72, 44, 44, 50, 84};
        boolean[] rightAlign = {false, false, true, true, true, true, true, true};
        float[] colX = new float[colW.length];
        float acc = MARGIN;
        for (int i = 0; i < colW.length; i++) {
            colX[i] = acc;
            acc += colW[i];
        }

        float tableTop = y;
        float headerH = 20;

        // Başlık bandı
        fillRect(cs, MARGIN, tableTop - headerH, CONTENT_W, headerH, NAVY);
        float hty = tableTop - 13;
        for (int i = 0; i < headers.length; i++) {
            if (i == 0) {
                textCenter(cs, font, 8.5f, WHITE, colX[i] + colW[i] / 2, hty, headers[i], true);
            } else if (rightAlign[i]) {
                textRight(cs, font, 8.5f, WHITE, colX[i] + colW[i] - 6, hty, headers[i], true);
            } else {
                text(cs, font, 8.5f, WHITE, colX[i] + 6, hty, headers[i], true);
            }
        }

        float rowH = 18;
        float cy = tableTop - headerH;
        boolean zebra = false;
        int idx = 1;

        for (InvoiceLineItem li : lineItems) {
            float rowBottom = cy - rowH;
            if (zebra) {
                fillRect(cs, MARGIN, rowBottom, CONTENT_W, rowH, LIGHT);
            }
            zebra = !zebra;

            Product p = productMap.get(li.getProductId());
            String productName = p != null ? p.getName() : ("#" + li.getProductId());
            String unit = p != null && notBlank(p.getUnit()) ? p.getUnit() : "";
            String qty = String.valueOf(li.getQuantity()) + (unit.isEmpty() ? "" : " " + unit);
            String disc = positive(li.getDiscountRate()) ? "%" + plain(li.getDiscountRate()) : "—";
            String vat = "%" + (li.getVatRate() != null ? plain(li.getVatRate()) : "0");
            String wt = positive(li.getWithholdingTaxRate()) ? "%" + plain(li.getWithholdingTaxRate()) : "—";

            float ty = rowBottom + 5.5f;
            textCenter(cs, font, 8.5f, GRAY, colX[0] + colW[0] / 2, ty, String.valueOf(idx), false);
            text(cs, font, 8.5f, DARK, colX[1] + 6, ty, truncate(productName, 32), false);
            textRight(cs, font, 8.5f, DARK, colX[2] + colW[2] - 6, ty, qty, false);
            textRight(cs, font, 8.5f, DARK, colX[3] + colW[3] - 6, ty, fmt(li.getUnitPrice()), false);
            textRight(cs, font, 8.5f, GRAY, colX[4] + colW[4] - 6, ty, disc, false);
            textRight(cs, font, 8.5f, DARK, colX[5] + colW[5] - 6, ty, vat, false);
            textRight(cs, font, 8.5f, GRAY, colX[6] + colW[6] - 6, ty, wt, false);
            textRight(cs, font, 8.5f, DARK, colX[7] + colW[7] - 6, ty, fmt(li.getLineTotal()) + " " + cur, true);

            cy = rowBottom;
            idx++;
        }

        // Dış çerçeve
        strokeRect(cs, MARGIN, cy, CONTENT_W, tableTop - cy, LINE, 0.7f);
        return cy;
    }

    private float drawTotals(PDPageContentStream cs, PDFont font, Invoice invoice, float y) throws Exception {
        String cur = currency(invoice);
        float boxW = 235;
        float boxX = A4_W - MARGIN - boxW;
        float labelX = boxX + 14;
        float valueRight = A4_W - MARGIN - 12;
        float rowH = 16;

        float ty = y;
        ty = totalRow(cs, font, labelX, valueRight, ty, rowH, "Ara Toplam", fmt(invoice.getSubtotal()) + " " + cur, DARK, false);

        if (positive(invoice.getDiscountAmount())) {
            String label = invoice.getDiscountType() != null && "PERCENTAGE".equals(invoice.getDiscountType().name())
                    ? "İskonto (%" + plain(invoice.getDiscountAmount()) + ")" : "İskonto";
            // Yüzde tipi iskontoda tutar zaten ara toplama yansımış olabilir; mevcut alanı negatif olarak gösteriyoruz.
            ty = totalRow(cs, font, labelX, valueRight, ty, rowH, label, "-" + fmt(invoice.getDiscountAmount()) + " " + cur, RED, false);
        }

        ty = totalRow(cs, font, labelX, valueRight, ty, rowH, "KDV", fmt(invoice.getVatAmount()) + " " + cur, DARK, false);

        if (positive(invoice.getWithholdingTaxAmount())) {
            ty = totalRow(cs, font, labelX, valueRight, ty, rowH, "Tevkifat (Stopaj)",
                    "-" + fmt(invoice.getWithholdingTaxAmount()) + " " + cur, RED, false);
        }

        // Genel toplam bandı
        ty -= 4;
        float bandH = 26;
        fillRect(cs, boxX, ty - bandH, boxW, bandH, NAVY);
        float bandBaseline = ty - bandH + 9;
        text(cs, font, 11, WHITE, labelX, bandBaseline, "GENEL TOPLAM", true);
        textRight(cs, font, 12, WHITE, valueRight, bandBaseline, fmt(invoice.getTotalAmount()) + " " + cur, true);
        ty -= bandH;

        // Yazıyla tutar
        String words = amountToWords(invoice.getTotalAmount());
        if (words != null) {
            ty -= 16;
            text(cs, font, 8.5f, GRAY, MARGIN, ty, "Yalnız: " + words, false);
        }

        return ty;
    }

    private float totalRow(PDPageContentStream cs, PDFont font, float labelX, float valueRight,
                           float y, float rowH, String label, String value, Color valueColor, boolean bold) throws Exception {
        text(cs, font, 9, GRAY, labelX, y, label, false);
        textRight(cs, font, 9, valueColor, valueRight, y, value, bold);
        return y - rowH;
    }

    private void drawDescription(PDPageContentStream cs, PDFont font, Invoice invoice, float y) throws Exception {
        if (invoice.getDescription() == null || invoice.getDescription().isBlank()) return;
        text(cs, font, 8.5f, GRAY, MARGIN, y, "Açıklama: " + truncate(invoice.getDescription(), 110), false);
    }

    private void drawFooter(PDPageContentStream cs, PDFont font) throws Exception {
        // İmza alanları
        float signY = 120;
        line(cs, MARGIN, signY, MARGIN + 150, signY, LINE, 0.7f);
        line(cs, A4_W - MARGIN - 150, signY, A4_W - MARGIN, signY, LINE, 0.7f);
        textCenter(cs, font, 9, DARK, MARGIN + 75, signY - 13, "Teslim Eden", false);
        textCenter(cs, font, 9, DARK, A4_W - MARGIN - 75, signY - 13, "Teslim Alan", false);

        // Alt bilgi notu + aksan çizgisi
        line(cs, MARGIN, 60, A4_W - MARGIN, 60, LINE, 0.7f);
        String note = "Bu fatura MuhasebePlus tarafından elektronik ortamda düzenlenmiştir.  •  " + LocalDate.now().format(DATE_FMT);
        textCenter(cs, font, 7.5f, GRAY, A4_W / 2, 46, note, false);

        fillRect(cs, 0, 0, A4_W, 5, NAVY);
    }

    /* ----------------------------------------------------------------- */
    /* Çizim yardımcıları                                                 */
    /* ----------------------------------------------------------------- */

    private void text(PDPageContentStream cs, PDFont font, float size, Color color,
                      float x, float y, String s, boolean bold) throws Exception {
        if (s == null) s = "";
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        if (bold) {
            cs.setRenderingMode(RenderingMode.FILL_STROKE);
            cs.setStrokingColor(color);
            cs.setLineWidth(size * 0.022f);
        }
        cs.newLineAtOffset(x, y);
        cs.showText(s);
        cs.endText();
        if (bold) {
            cs.setRenderingMode(RenderingMode.FILL);
        }
    }

    private void textRight(PDPageContentStream cs, PDFont font, float size, Color color,
                           float rightX, float y, String s, boolean bold) throws Exception {
        text(cs, font, size, color, rightX - textWidth(font, size, s), y, s, bold);
    }

    private void textCenter(PDPageContentStream cs, PDFont font, float size, Color color,
                            float centerX, float y, String s, boolean bold) throws Exception {
        text(cs, font, size, color, centerX - textWidth(font, size, s) / 2, y, s, bold);
    }

    private float textWidth(PDFont font, float size, String s) throws Exception {
        if (s == null || s.isEmpty()) return 0;
        return font.getStringWidth(s) / 1000f * size;
    }

    private void fillRect(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws Exception {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void strokeRect(PDPageContentStream cs, float x, float y, float w, float h, Color c, float lw) throws Exception {
        cs.setLineWidth(lw);
        cs.setStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private void line(PDPageContentStream cs, float x1, float y1, float x2, float y2, Color c, float lw) throws Exception {
        cs.setLineWidth(lw);
        cs.setStrokingColor(c);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    /* ----------------------------------------------------------------- */
    /* Veri / metin yardımcıları                                          */
    /* ----------------------------------------------------------------- */

    private List<String> companyInfoLines(Company company) {
        List<String> lines = new ArrayList<>();
        if (company == null) return lines;

        String taxLine = joinNonBlank("  •  ",
                notBlank(company.getTaxOffice()) ? "Vergi Dairesi: " + company.getTaxOffice() : null,
                notBlank(company.getTaxNumber()) ? "VKN: " + company.getTaxNumber() : null);
        if (notBlank(taxLine)) lines.add(taxLine);

        String addr = joinNonBlank(" ", company.getAddress(), company.getCity());
        if (notBlank(addr)) lines.add(addr);

        String contact = joinNonBlank("  •  ", company.getPhone(), company.getEmail());
        if (notBlank(contact)) lines.add(contact);

        return lines;
    }

    private void addRow(List<String[]> rows, String label, String value) {
        if (notBlank(value)) rows.add(new String[]{label, value});
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank() && !"null".equalsIgnoreCase(s.trim());
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (notBlank(v)) return v;
        }
        return null;
    }

    private String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (notBlank(p)) {
                if (sb.length() > 0) sb.append(sep);
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    private String nv(String s) {
        return notBlank(s) ? s : "—";
    }

    private boolean positive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    private String currency(Invoice invoice) {
        if (invoice != null && invoice.getCurrency() != null) {
            switch (invoice.getCurrency().name()) {
                case "USD": return "$";
                case "EUR": return "€";
                default: return "TL";
            }
        }
        return "TL";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private String fmt(BigDecimal val) {
        if (val == null) return "0,00";
        return String.format(TR, "%,.2f", val.setScale(2, RoundingMode.HALF_UP));
    }

    /** Yüzde gibi değerleri gereksiz sıfır olmadan gösterir (18.00 -> 18, 7.50 -> 7,5). */
    private String plain(BigDecimal val) {
        if (val == null) return "0";
        BigDecimal stripped = val.stripTrailingZeros();
        String s = stripped.scale() <= 0 ? stripped.toBigInteger().toString() : stripped.toPlainString();
        return s.replace('.', ',');
    }

    private String amountToWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return null;

        long tl = amount.longValue();
        long kr = amount.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();

        String tlWords = numberToWordsTurkish(tl);
        if (tlWords.isEmpty()) tlWords = "sıfır";

        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(tlWords.charAt(0))).append(tlWords.substring(1));
        sb.append(" Türk Lirası");

        if (kr > 0) {
            String krWords = numberToWordsTurkish(kr);
            sb.append(" ").append(krWords).append(" Kuruş");
        }

        return sb.toString();
    }

    private String numberToWordsTurkish(long n) {
        if (n == 0) return "";
        if (n < 0) return "eksi " + numberToWordsTurkish(-n);

        String[] ones = {"", "bir", "iki", "üç", "dört", "beş", "altı", "yedi", "sekiz", "dokuz"};
        String[] tens = {"", "on", "yirmi", "otuz", "kırk", "elli", "altmış", "yetmiş", "seksen", "doksan"};
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
            if (yuz == 1) sb.append("yüz ");
            else sb.append(ones[yuz]).append(" yüz ");
        }
        if (on > 0) sb.append(tens[on]).append(" ");
        if (bir > 0) sb.append(ones[bir]).append(" ");

        return sb.toString().trim();
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
}
