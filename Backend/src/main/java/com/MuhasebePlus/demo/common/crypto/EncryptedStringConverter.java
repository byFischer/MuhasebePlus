package com.MuhasebePlus.demo.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    static final String PREFIX = "ENC:";

    // Static reference needed because JPA instantiates converters, not Spring
    private static CryptoService cryptoService;

    @Autowired
    public void setCryptoService(CryptoService service) {
        EncryptedStringConverter.cryptoService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        if (attribute.startsWith(PREFIX)) return attribute;
        return PREFIX + cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if (!dbData.startsWith(PREFIX)) return dbData; // backward compat: plaintext
        return cryptoService.decrypt(dbData.substring(PREFIX.length()));
    }
}
