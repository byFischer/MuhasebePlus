-- Mevcut faturalar için tek satırlı VatSummary backfill
-- Varsayım: her faturanın tüm satırları aynı KDV oranına sahip (vat_amount / subtotal oranından oran hesaplanır)
-- Gerçek oran bilinmediği için 20 varsayılır; kullanıcı ilerleyen dönemde düzeltebilir.
INSERT INTO invoice_vat_summary (company_id, invoice_id, vat_rate, vat_base_amount, vat_amount, created_at, updated_at)
SELECT
    i.company_id,
    i.invoice_id,
    CASE
        WHEN i.subtotal > 0 AND i.vat_amount > 0
            THEN ROUND((i.vat_amount / i.subtotal) * 100, 0)
        ELSE 20
    END AS vat_rate,
    COALESCE(i.subtotal, 0) AS vat_base_amount,
    COALESCE(i.vat_amount, 0) AS vat_amount,
    NOW(),
    NOW()
FROM invoice i
WHERE i.is_deleted = FALSE
  AND i.company_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM invoice_vat_summary s WHERE s.invoice_id = i.invoice_id
  );
