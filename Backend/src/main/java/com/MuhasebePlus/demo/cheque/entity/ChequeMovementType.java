package com.MuhasebePlus.demo.cheque.entity;

public enum ChequeMovementType {
    REGISTERED,   // Portföye giriş
    DEPOSITED,    // Bankaya tahsile verildi
    COLLECTED,    // Tahsil edildi
    BOUNCED,      // Karşılıksız
    ENDORSED,     // Ciro edildi
    CANCELLED     // İptal
}
