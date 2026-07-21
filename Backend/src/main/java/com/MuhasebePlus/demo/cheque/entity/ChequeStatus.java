package com.MuhasebePlus.demo.cheque.entity;

public enum ChequeStatus {
    IN_PORTFOLIO,  // Portföyde — henüz bankaya verilmedi
    DEPOSITED,     // Bankaya tahsile verildi
    COLLECTED,     // Tahsil edildi
    BOUNCED,       // Karşılıksız çıktı
    ENDORSED,      // Ciro edildi (3. tarafa devredildi)
    CANCELLED      // İptal edildi
}
