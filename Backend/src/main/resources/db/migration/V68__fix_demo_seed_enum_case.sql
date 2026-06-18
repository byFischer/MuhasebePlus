-- ============================================================
-- V66 demo seed verilerindeki enum değerlerini Java enum sabitleriyle
-- hizalar. İlgili kolonlar VARCHAR olduğundan @Enumerated(STRING)
-- eşleşmesi büyük/küçük harf ve sabit adına birebir uymak zorunda.
-- İdempotent: yalnızca hatalı değerleri günceller, tekrar çalıştırılabilir.
-- ============================================================

-- CustomerType: INDIVIDUAL / CORPORATE
UPDATE customer SET type = 'CORPORATE'  WHERE type = 'corporate';
UPDATE customer SET type = 'INDIVIDUAL' WHERE type = 'individual';

-- LogLevel: INFO / WARNING / ERROR
UPDATE system_log SET log_level = UPPER(log_level) WHERE log_level IN ('info', 'warning', 'error');

-- MovementType: PURCHASE / SALE / ADJUSTMENT_IN (stok girişi pozitif ayarlama)
UPDATE stock_movement SET movement_type = 'PURCHASE'      WHERE movement_type = 'IN';
UPDATE stock_movement SET movement_type = 'SALE'          WHERE movement_type = 'OUT';
UPDATE stock_movement SET movement_type = 'ADJUSTMENT_IN' WHERE movement_type = 'ADJUSTMENT';

-- PaymentMethod: cash / bank_transfer
UPDATE invoice_payment SET payment_method = 'bank_transfer' WHERE payment_method = 'BANK_TRANSFER';
UPDATE invoice_payment SET payment_method = 'cash'          WHERE payment_method = 'CASH';
