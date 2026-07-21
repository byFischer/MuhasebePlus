package com.MuhasebePlus.demo.sharelink.exception;

public class ShareLinkNotFoundException extends RuntimeException {
    public ShareLinkNotFoundException() {
        super("Paylaşım bağlantısı bulunamadı veya iptal edilmiş");
    }
}
