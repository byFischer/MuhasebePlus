package com.MuhasebePlus.demo.invoice.service;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.stock.entity.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class UblTrBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String build(Invoice invoice, Company company, Customer customer,
                                List<InvoiceLineItem> lineItems, Map<Integer, Product> productMap) {

        String uuid = UUID.randomUUID().toString();
        boolean isSale = invoice.getInvoiceType() == InvoiceType.sale;
        String invoiceTypeCode = isSale ? "SATIS" : "ALIS";
        String currency = invoice.getCurrency() != null ? invoice.getCurrency().name() : "TRY";

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        xml.append("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"\n");
        xml.append("         xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n");
        xml.append("         xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n");

        xml.append("  <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n");
        xml.append("  <cbc:CustomizationID>TR1.2</cbc:CustomizationID>\n");
        xml.append("  <cbc:ProfileID>EARSIVFATURA</cbc:ProfileID>\n");
        xml.append("  <cbc:ID>").append(esc(invoice.getInvoiceNumber())).append("</cbc:ID>\n");
        xml.append("  <cbc:UUID>").append(uuid).append("</cbc:UUID>\n");
        if (invoice.getInvoiceDate() != null)
            xml.append("  <cbc:IssueDate>").append(invoice.getInvoiceDate().format(DATE_FMT)).append("</cbc:IssueDate>\n");
        if (invoice.getDueDate() != null)
            xml.append("  <cbc:DueDate>").append(invoice.getDueDate().format(DATE_FMT)).append("</cbc:DueDate>\n");
        xml.append("  <cbc:InvoiceTypeCode>").append(invoiceTypeCode).append("</cbc:InvoiceTypeCode>\n");
        if (invoice.getDescription() != null)
            xml.append("  <cbc:Note>").append(esc(invoice.getDescription())).append("</cbc:Note>\n");
        xml.append("  <cbc:DocumentCurrencyCode>").append(currency).append("</cbc:DocumentCurrencyCode>\n");

        if (invoice.getExchangeRate() != null && invoice.getExchangeRate().compareTo(BigDecimal.ONE) != 0) {
            xml.append("  <cac:TaxExchangeRate>\n");
            xml.append("    <cbc:CalculationRate>").append(invoice.getExchangeRate()).append("</cbc:CalculationRate>\n");
            xml.append("  </cac:TaxExchangeRate>\n");
        }

        appendParty(xml, "AccountingSupplierParty", company);
        if (customer != null) {
            appendParty(xml, "AccountingCustomerParty", customer);
        }

        // Group line items by VAT rate for multi-rate TaxSubtotal (UBL-TR requirement)
        Map<BigDecimal, BigDecimal> vatBaseByRate = new TreeMap<>();
        Map<BigDecimal, BigDecimal> vatAmtByRate  = new TreeMap<>();
        BigDecimal taxExclusive = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        for (InvoiceLineItem li : lineItems) {
            BigDecimal net = calcNet(li);
            BigDecimal vat = calcVat(li);
            BigDecimal rate = li.getVatRate() != null ? li.getVatRate().stripTrailingZeros() : BigDecimal.ZERO;
            taxExclusive = taxExclusive.add(net);
            totalVat = totalVat.add(vat);
            vatBaseByRate.merge(rate, net, BigDecimal::add);
            vatAmtByRate.merge(rate, vat, BigDecimal::add);
        }

        String taxExclusiveStr = fmt(taxExclusive);
        String totalVatStr = fmt(totalVat);
        String taxInclusiveStr = fmt(taxExclusive.add(totalVat));
        String payableStr = fmt(invoice.getTotalAmount() != null ? invoice.getTotalAmount() : taxExclusive.add(totalVat));

        xml.append("  <cac:LegalMonetaryTotal>\n");
        xml.append("    <cbc:LineExtensionAmount currencyID=\"").append(currency).append("\">").append(taxExclusiveStr).append("</cbc:LineExtensionAmount>\n");
        xml.append("    <cbc:TaxExclusiveAmount currencyID=\"").append(currency).append("\">").append(taxExclusiveStr).append("</cbc:TaxExclusiveAmount>\n");
        xml.append("    <cbc:TaxInclusiveAmount currencyID=\"").append(currency).append("\">").append(taxInclusiveStr).append("</cbc:TaxInclusiveAmount>\n");
        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            xml.append("    <cbc:AllowanceTotalAmount currencyID=\"").append(currency).append("\">").append(fmt(invoice.getDiscountAmount())).append("</cbc:AllowanceTotalAmount>\n");
        }
        xml.append("    <cbc:PayableAmount currencyID=\"").append(currency).append("\">").append(payableStr).append("</cbc:PayableAmount>\n");
        xml.append("  </cac:LegalMonetaryTotal>\n");

        // TaxTotal with one TaxSubtotal per VAT rate (GIB UBL-TR requirement)
        xml.append("  <cac:TaxTotal>\n");
        xml.append("    <cbc:TaxAmount currencyID=\"").append(currency).append("\">").append(totalVatStr).append("</cbc:TaxAmount>\n");
        for (Map.Entry<BigDecimal, BigDecimal> entry : vatBaseByRate.entrySet()) {
            BigDecimal rate = entry.getKey();
            BigDecimal base = entry.getValue();
            BigDecimal vatAmt = vatAmtByRate.getOrDefault(rate, BigDecimal.ZERO);
            xml.append("    <cac:TaxSubtotal>\n");
            xml.append("      <cbc:TaxableAmount currencyID=\"").append(currency).append("\">").append(fmt(base)).append("</cbc:TaxableAmount>\n");
            xml.append("      <cbc:TaxAmount currencyID=\"").append(currency).append("\">").append(fmt(vatAmt)).append("</cbc:TaxAmount>\n");
            xml.append("      <cac:TaxCategory>\n");
            xml.append("        <cbc:Percent>").append(rate.toPlainString()).append("</cbc:Percent>\n");
            xml.append("        <cac:TaxScheme>\n");
            xml.append("          <cbc:Name>KDV</cbc:Name>\n");
            xml.append("          <cbc:TaxTypeCode>0015</cbc:TaxTypeCode>\n");
            xml.append("        </cac:TaxScheme>\n");
            xml.append("      </cac:TaxCategory>\n");
            xml.append("    </cac:TaxSubtotal>\n");
        }
        xml.append("  </cac:TaxTotal>\n");

        if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            xml.append("  <cac:AllowanceCharge>\n");
            xml.append("    <cbc:ChargeIndicator>false</cbc:ChargeIndicator>\n");
            xml.append("    <cbc:Amount currencyID=\"").append(currency).append("\">").append(fmt(invoice.getDiscountAmount())).append("</cbc:Amount>\n");
            xml.append("  </cac:AllowanceCharge>\n");
        }

        int lineNum = 1;
        for (InvoiceLineItem li : lineItems) {
            Product p = productMap.get(li.getProductId());
            appendLineItem(xml, li, p, lineNum++, currency);
        }

        xml.append("</Invoice>");
        return xml.toString();
    }

    private static void appendParty(StringBuilder xml, String tag, Company company) {
        String idScheme = "VKN";

        xml.append("  <cac:").append(tag).append(">\n");
        xml.append("    <cac:Party>\n");
        xml.append("      <cbc:WebsiteURI>").append(esc(company.getCompanyName())).append("</cbc:WebsiteURI>\n");
        xml.append("      <cac:PartyIdentification>\n");
        xml.append("        <cbc:ID schemeID=\"").append(idScheme).append("\">").append(esc(company.getTaxNumber())).append("</cbc:ID>\n");
        xml.append("      </cac:PartyIdentification>\n");
        xml.append("      <cac:PartyName>\n");
        xml.append("        <cbc:Name>").append(esc(company.getCompanyName())).append("</cbc:Name>\n");
        xml.append("      </cac:PartyName>\n");
        xml.append("      <cac:PostalAddress>\n");
        if (company.getCity() != null)
            xml.append("        <cbc:CityName>").append(esc(company.getCity())).append("</cbc:CityName>\n");
        if (company.getAddress() != null)
            xml.append("        <cbc:StreetName>").append(esc(company.getAddress())).append("</cbc:StreetName>\n");
        xml.append("        <cac:Country>\n");
        xml.append("          <cbc:Name>TÜRKİYE</cbc:Name>\n");
        xml.append("        </cac:Country>\n");
        xml.append("      </cac:PostalAddress>\n");
        if (tag.equals("AccountingSupplierParty")) {
            xml.append("      <cac:Contact>\n");
            if (company.getPhone() != null)
                xml.append("        <cbc:Telephone>").append(esc(company.getPhone())).append("</cbc:Telephone>\n");
            if (company.getEmail() != null)
                xml.append("        <cbc:ElectronicMail>").append(esc(company.getEmail())).append("</cbc:ElectronicMail>\n");
            xml.append("      </cac:Contact>\n");
        }
        xml.append("    </cac:Party>\n");
        xml.append("  </cac:").append(tag).append(">\n");
    }

    private static void appendParty(StringBuilder xml, String tag, Customer customer) {
        xml.append("  <cac:").append(tag).append(">\n");
        xml.append("    <cac:Party>\n");
        xml.append("      <cbc:WebsiteURI>").append(esc(customer.getName())).append("</cbc:WebsiteURI>\n");
        xml.append("      <cac:PartyIdentification>\n");
        String scheme = customer.getType() != null && customer.getType().name().equals("INDIVIDUAL") ? "TCKN" : "VKN";
        xml.append("        <cbc:ID schemeID=\"").append(scheme).append("\">").append(esc(customer.getTaxNumber())).append("</cbc:ID>\n");
        xml.append("      </cac:PartyIdentification>\n");
        xml.append("      <cac:PartyName>\n");
        xml.append("        <cbc:Name>").append(esc(customer.getName())).append("</cbc:Name>\n");
        xml.append("      </cac:PartyName>\n");
        xml.append("      <cac:PostalAddress>\n");
        if (customer.getCity() != null)
            xml.append("        <cbc:CityName>").append(esc(customer.getCity())).append("</cbc:CityName>\n");
        if (customer.getAddress() != null)
            xml.append("        <cbc:StreetName>").append(esc(customer.getAddress())).append("</cbc:StreetName>\n");
        xml.append("        <cac:Country>\n");
        xml.append("          <cbc:Name>TÜRKİYE</cbc:Name>\n");
        xml.append("        </cac:Country>\n");
        xml.append("      </cac:PostalAddress>\n");
        xml.append("    </cac:Party>\n");
        xml.append("  </cac:").append(tag).append(">\n");
    }

    private static void appendLineItem(StringBuilder xml, InvoiceLineItem li, Product product, int lineNum, String currency) {
        String name = product != null ? product.getName() : ("#" + li.getProductId());
        BigDecimal net = calcNet(li);
        BigDecimal vat = calcVat(li);

        xml.append("  <cac:InvoiceLine>\n");
        xml.append("    <cbc:ID>").append(lineNum).append("</cbc:ID>\n");
        xml.append("    <cbc:InvoicedQuantity unitCode=\"C62\">").append(li.getQuantity()).append("</cbc:InvoicedQuantity>\n");
        xml.append("    <cbc:LineExtensionAmount currencyID=\"").append(currency).append("\">").append(fmt(net)).append("</cbc:LineExtensionAmount>\n");
        xml.append("    <cac:Item>\n");
        xml.append("      <cbc:Name>").append(esc(name)).append("</cbc:Name>\n");
        xml.append("    </cac:Item>\n");
        xml.append("    <cac:Price>\n");
        xml.append("      <cbc:PriceAmount currencyID=\"").append(currency).append("\">").append(fmt(li.getUnitPrice())).append("</cbc:PriceAmount>\n");
        xml.append("    </cac:Price>\n");
        if (li.getVatRate() != null && li.getVatRate().compareTo(BigDecimal.ZERO) > 0) {
            xml.append("    <cac:TaxTotal>\n");
            xml.append("      <cbc:TaxAmount currencyID=\"").append(currency).append("\">").append(fmt(vat)).append("</cbc:TaxAmount>\n");
            xml.append("      <cac:TaxSubtotal>\n");
            xml.append("        <cbc:TaxableAmount currencyID=\"").append(currency).append("\">").append(fmt(net)).append("</cbc:TaxableAmount>\n");
            xml.append("        <cbc:TaxAmount currencyID=\"").append(currency).append("\">").append(fmt(vat)).append("</cbc:TaxAmount>\n");
            xml.append("        <cbc:Percent>").append(li.getVatRate()).append("</cbc:Percent>\n");
            xml.append("        <cac:TaxCategory>\n");
            xml.append("          <cac:TaxScheme>\n");
            xml.append("            <cbc:Name>KDV</cbc:Name>\n");
            xml.append("            <cbc:TaxTypeCode>0015</cbc:TaxTypeCode>\n");
            xml.append("          </cac:TaxScheme>\n");
            xml.append("        </cac:TaxCategory>\n");
            xml.append("      </cac:TaxSubtotal>\n");
            xml.append("    </cac:TaxTotal>\n");
        }
        xml.append("  </cac:InvoiceLine>\n");
    }

    private static BigDecimal calcNet(InvoiceLineItem li) {
        BigDecimal qty = BigDecimal.valueOf(li.getQuantity());
        BigDecimal net = qty.multiply(li.getUnitPrice() != null ? li.getUnitPrice() : BigDecimal.ZERO);
        if (li.getDiscountRate() != null && li.getDiscountRate().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal disc = net.multiply(li.getDiscountRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            net = net.subtract(disc);
        }
        return net.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal calcVat(InvoiceLineItem li) {
        BigDecimal net = calcNet(li);
        BigDecimal rate = li.getVatRate() != null ? li.getVatRate() : BigDecimal.ZERO;
        return net.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static String fmt(BigDecimal val) {
        if (val == null) return "0.00";
        return val.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
