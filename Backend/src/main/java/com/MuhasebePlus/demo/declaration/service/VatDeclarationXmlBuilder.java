package com.MuhasebePlus.demo.declaration.service;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.declaration.entity.VatDeclaration;
import com.MuhasebePlus.demo.declaration.entity.VatDeclarationLine;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class VatDeclarationXmlBuilder {

    public static String build(VatDeclaration decl, Company company) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<KDVBeyannamesi>\n");

        // Mükellef bilgileri
        xml.append("  <Mukellef>\n");
        xml.append("    <VergiKimlikNo>").append(esc(company.getTaxNumber())).append("</VergiKimlikNo>\n");
        xml.append("    <VergiDairesi>").append(esc(company.getTaxOffice())).append("</VergiDairesi>\n");
        xml.append("    <Unvan>").append(esc(company.getCompanyName())).append("</Unvan>\n");
        xml.append("  </Mukellef>\n");

        // Dönem
        xml.append("  <Donem>\n");
        xml.append("    <Yil>").append(decl.getYear()).append("</Yil>\n");
        xml.append("    <Ay>").append(String.format("%02d", decl.getMonth())).append("</Ay>\n");
        xml.append("  </Donem>\n");

        // Tablo I — Matrah ve Vergi
        xml.append("  <TableI>\n");
        for (VatDeclarationLine line : decl.getLines()) {
            if (!line.getLineCode().startsWith("T2_") && !line.getLineCode().startsWith("T3_")) {
                xml.append("    <Satir kod=\"").append(esc(line.getLineCode())).append("\">\n");
                xml.append("      <Aciklama>").append(esc(line.getDescription())).append("</Aciklama>\n");
                xml.append("      <Matrah>").append(fmt(line.getBaseAmount())).append("</Matrah>\n");
                xml.append("      <Vergi>").append(fmt(line.getVatAmount())).append("</Vergi>\n");
                xml.append("    </Satir>\n");
            }
        }
        xml.append("  </TableI>\n");

        // Tablo II — İndirimler
        xml.append("  <TableII>\n");
        for (VatDeclarationLine line : decl.getLines()) {
            if (line.getLineCode().startsWith("T2_")) {
                xml.append("    <Satir kod=\"").append(esc(line.getLineCode().substring(3))).append("\">\n");
                xml.append("      <Aciklama>").append(esc(line.getDescription())).append("</Aciklama>\n");
                xml.append("      <Tutar>").append(fmt(line.getVatAmount())).append("</Tutar>\n");
                xml.append("    </Satir>\n");
            }
        }
        xml.append("  </TableII>\n");

        // Özet
        xml.append("  <Ozet>\n");
        xml.append("    <ToplamSatisMatrahi>").append(fmt(decl.getTotalSalesBase())).append("</ToplamSatisMatrahi>\n");
        xml.append("    <HesaplananKDV>").append(fmt(decl.getTotalSalesVat())).append("</HesaplananKDV>\n");
        xml.append("    <IndirilecekKDV>").append(fmt(decl.getTotalPurchasesVat())).append("</IndirilecekKDV>\n");
        xml.append("    <DevredenKDV>").append(fmt(decl.getPreviousPeriodCarryover())).append("</DevredenKDV>\n");
        xml.append("    <OdenecekKDV>").append(fmt(decl.getPayableVat())).append("</OdenecekKDV>\n");
        xml.append("    <SonrakiDonemeDevreden>").append(fmt(decl.getNextPeriodCarryover())).append("</SonrakiDonemeDevreden>\n");
        xml.append("  </Ozet>\n");

        xml.append("</KDVBeyannamesi>");
        return xml.toString();
    }

    private static String fmt(BigDecimal val) {
        if (val == null) return "0.00";
        return val.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
