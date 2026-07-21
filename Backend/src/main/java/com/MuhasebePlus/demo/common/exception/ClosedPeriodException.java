package com.MuhasebePlus.demo.common.exception;

public class ClosedPeriodException extends BusinessException {

    public ClosedPeriodException(int year, int month) {
        super(String.format("Kapalı dönem: %d/%02d dönemi kapatılmış. Bu döneme işlem kaydedilemez, silinemez veya güncellenemez.", year, month));
    }
}
