-- ============================================================
-- MuhasebePlus - Demo Seed: Muhasebe (Yevmiye) + Çek/Senet
-- ============================================================
-- V66 demo verisini tamamlar: company 1 (Teknoloji A.Ş.) için
--   1) Hesap planı (TDHP) — şablon şirketten (company 0) kopyalanır
--   2) Yevmiye fişleri — mevcut faturalardan üretilir
--   3) Çek/senet portföyü + hareket geçmişi
-- Tüm bloklar idempotent (NOT EXISTS / ON CONFLICT) — tekrar çalıştırmak güvenlidir.
-- ============================================================

-- ============================================================
-- 1. HESAP PLANI (TDHP) — şablon company 0 -> company 1
--    ChartOfAccountService.copyTdhpForCompany ile aynı davranış
--    (parent_id kopyalanmaz; kod/ad/tür/leaf/system/description korunur)
-- ============================================================
INSERT INTO chart_of_account
    (company_id, account_code, account_name, account_type, parent_id, is_leaf, is_system, description, is_deleted, created_at, updated_at)
SELECT 1, t.account_code, t.account_name, t.account_type, NULL, t.is_leaf, t.is_system, t.description, FALSE, NOW(), NOW()
FROM chart_of_account t
WHERE t.company_id = 0
  AND NOT EXISTS (
        SELECT 1 FROM chart_of_account c
        WHERE c.company_id = 1 AND c.account_code = t.account_code AND c.is_deleted = FALSE);

-- ============================================================
-- 2. YEVMİYE FİŞLERİ — faturalardan
-- ============================================================
-- 2a. Fiş numarası sıralayıcısı (FIS-2025-0001 .. FIS-2025-NNNN)
INSERT INTO journal_entry_sequence (company_id, year, last_seq)
SELECT 1, 2025, COUNT(*)
FROM invoice
WHERE company_id = 1 AND is_deleted = FALSE AND cancelled = FALSE
ON CONFLICT (company_id, year)
DO UPDATE SET last_seq = GREATEST(journal_entry_sequence.last_seq, EXCLUDED.last_seq);

-- 2b. Fiş başlıkları (her fatura için bir fiş)
WITH inv AS (
    SELECT invoice_id, invoice_number, invoice_type, invoice_date,
           ROW_NUMBER() OVER (ORDER BY invoice_date, invoice_id) AS rn
    FROM invoice
    WHERE company_id = 1 AND is_deleted = FALSE AND cancelled = FALSE
)
INSERT INTO journal_entry
    (company_id, entry_number, entry_date, description, source_type, source_id, is_reversed, is_deleted, created_at, updated_at)
SELECT 1,
       'FIS-2025-' || LPAD(rn::text, 4, '0'),
       invoice_date,
       CASE WHEN invoice_type = 'sale' THEN 'Satış faturası: ' ELSE 'Alış faturası: ' END || invoice_number,
       'INVOICE', invoice_id, FALSE, FALSE, NOW(), NOW()
FROM inv
WHERE NOT EXISTS (
        SELECT 1 FROM journal_entry je
        WHERE je.company_id = 1 AND je.source_type = 'INVOICE' AND je.source_id = inv.invoice_id);

-- 2c. Fiş satırları (çift taraflı, dengeli)
--     Satış: B 120 (müşteri) / A 600 (satış) + A 391 (hes. KDV)
--     Alış : B 153 (mal) + B 191 (ind. KDV) / A 320 (satıcı)
INSERT INTO journal_entry_line
    (company_id, entry_id, account_id, debit_amount, credit_amount, description, line_order)
-- SATIŞ — borç: müşteri (120)
SELECT 1, je.entry_id,
       (SELECT account_id FROM chart_of_account WHERE company_id = 1 AND account_code = '120' AND is_deleted = FALSE),
       i.total_amount, 0, 'Müşteri alacağı', 0
FROM journal_entry je JOIN invoice i ON i.invoice_id = je.source_id
WHERE je.company_id = 1 AND je.source_type = 'INVOICE' AND je.is_deleted = FALSE
  AND i.invoice_type = 'sale'
  AND NOT EXISTS (SELECT 1 FROM journal_entry_line l WHERE l.entry_id = je.entry_id)
UNION ALL
-- SATIŞ — alacak: yurtiçi satışlar (600)
SELECT 1, je.entry_id,
       (SELECT account_id FROM chart_of_account WHERE company_id = 1 AND account_code = '600' AND is_deleted = FALSE),
       0, i.subtotal, 'Satış geliri', 1
FROM journal_entry je JOIN invoice i ON i.invoice_id = je.source_id
WHERE je.company_id = 1 AND je.source_type = 'INVOICE' AND je.is_deleted = FALSE
  AND i.invoice_type = 'sale'
  AND NOT EXISTS (SELECT 1 FROM journal_entry_line l WHERE l.entry_id = je.entry_id)
UNION ALL
-- SATIŞ — alacak: hesaplanan KDV (391)
SELECT 1, je.entry_id,
       (SELECT account_id FROM chart_of_account WHERE company_id = 1 AND account_code = '391' AND is_deleted = FALSE),
       0, i.vat_amount, 'Hesaplanan KDV', 2
FROM journal_entry je JOIN invoice i ON i.invoice_id = je.source_id
WHERE je.company_id = 1 AND je.source_type = 'INVOICE' AND je.is_deleted = FALSE
  AND i.invoice_type = 'sale' AND i.vat_amount > 0
  AND NOT EXISTS (SELECT 1 FROM journal_entry_line l WHERE l.entry_id = je.entry_id)
UNION ALL
-- ALIŞ — borç: ticari mallar (153)
SELECT 1, je.entry_id,
       (SELECT account_id FROM chart_of_account WHERE company_id = 1 AND account_code = '153' AND is_deleted = FALSE),
       i.subtotal, 0, 'Mal alışı', 0
FROM journal_entry je JOIN invoice i ON i.invoice_id = je.source_id
WHERE je.company_id = 1 AND je.source_type = 'INVOICE' AND je.is_deleted = FALSE
  AND i.invoice_type = 'purchase'
  AND NOT EXISTS (SELECT 1 FROM journal_entry_line l WHERE l.entry_id = je.entry_id)
UNION ALL
-- ALIŞ — borç: indirilecek KDV (191)
SELECT 1, je.entry_id,
       (SELECT account_id FROM chart_of_account WHERE company_id = 1 AND account_code = '191' AND is_deleted = FALSE),
       i.vat_amount, 0, 'İndirilecek KDV', 1
FROM journal_entry je JOIN invoice i ON i.invoice_id = je.source_id
WHERE je.company_id = 1 AND je.source_type = 'INVOICE' AND je.is_deleted = FALSE
  AND i.invoice_type = 'purchase' AND i.vat_amount > 0
  AND NOT EXISTS (SELECT 1 FROM journal_entry_line l WHERE l.entry_id = je.entry_id)
UNION ALL
-- ALIŞ — alacak: satıcılar (320)
SELECT 1, je.entry_id,
       (SELECT account_id FROM chart_of_account WHERE company_id = 1 AND account_code = '320' AND is_deleted = FALSE),
       0, i.total_amount, 'Satıcı borcu', 2
FROM journal_entry je JOIN invoice i ON i.invoice_id = je.source_id
WHERE je.company_id = 1 AND je.source_type = 'INVOICE' AND je.is_deleted = FALSE
  AND i.invoice_type = 'purchase'
  AND NOT EXISTS (SELECT 1 FROM journal_entry_line l WHERE l.entry_id = je.entry_id);

-- ============================================================
-- 3. ÇEK / SENET PORTFÖYÜ
-- ============================================================
INSERT INTO cheque
    (company_id, cheque_number, cheque_type, cheque_kind, customer_id,
     drawer_bank, drawer_branch, drawer_account_number, amount, currency,
     issue_date, due_date, status, bank_account_id, endorsed_to_customer_id, notes,
     is_deleted, created_at, updated_at)
SELECT v.*, FALSE, NOW(), NOW() FROM (VALUES
    -- Alınan çekler
    (1, 'CEK-2025-1001', 'RECEIVABLE', 'CHEQUE',          1::bigint, 'Ziraat Bankası', 'Kadıköy Şubesi',  '6457841326', 15000.00::numeric, 'TRY', DATE '2025-11-15', DATE '2026-07-15', 'IN_PORTFOLIO', NULL::bigint, NULL::bigint, 'Anadolu Makina vadeli tahsilat çeki'),
    (1, 'CEK-2025-1002', 'RECEIVABLE', 'CHEQUE',          4::bigint, 'Garanti BBVA',   'Gaziemir Şubesi', '7508062001', 22100.00::numeric, 'TRY', DATE '2025-12-01', DATE '2026-06-30', 'DEPOSITED',    2::bigint,    NULL::bigint, 'Tahsile verildi - Ziraat'),
    (1, 'CEK-2025-1003', 'RECEIVABLE', 'CHEQUE',          2::bigint, 'İş Bankası',     'Çankaya Şubesi',  '1038620008', 8750.00::numeric,  'TRY', DATE '2025-09-10', DATE '2026-03-10', 'COLLECTED',    3::bigint,    NULL::bigint, 'Tahsil edildi'),
    (1, 'SEN-2025-2001', 'RECEIVABLE', 'PROMISSORY_NOTE', 9::bigint, NULL,             NULL,              NULL,         5600.00::numeric,  'TRY', DATE '2026-01-20', DATE '2026-08-20', 'IN_PORTFOLIO', NULL::bigint, NULL::bigint, 'Güneş Enerji borç senedi'),
    (1, 'CEK-2025-1004', 'RECEIVABLE', 'CHEQUE',          3::bigint, 'Ziraat Bankası', 'Tuzla Şubesi',    '1038620099', 6499.00::numeric,  'TRY', DATE '2025-10-05', DATE '2026-04-05', 'BOUNCED',      2::bigint,    NULL::bigint, 'Karşılıksız çıktı - hukuki takip'),
    (1, 'CEK-2025-1005', 'RECEIVABLE', 'CHEQUE',          1::bigint, 'Garanti BBVA',   'Ankara Şubesi',   '6457841399', 12000.00::numeric, 'TRY', DATE '2025-08-01', DATE '2026-02-01', 'ENDORSED',     NULL::bigint, 5::bigint,    'Marmara Gıda''ya ciro edildi'),
    -- Verilen çekler / senetler (tedarikçilere)
    (1, 'CEK-2026-3001', 'PAYABLE',    'CHEQUE',          5::bigint, 'Garanti BBVA',   'Maslak Şubesi',   '0102610083', 9500.00::numeric,  'TRY', DATE '2026-05-22', DATE '2026-09-22', 'IN_PORTFOLIO', 3::bigint,    NULL::bigint, 'Marmara Gıda alış ödemesi çeki'),
    (1, 'SEN-2026-4001', 'PAYABLE',    'PROMISSORY_NOTE', 10::bigint,NULL,             NULL,              NULL,         7000.00::numeric,  'TRY', DATE '2026-04-10', DATE '2026-10-10', 'IN_PORTFOLIO', NULL::bigint, NULL::bigint, 'Kuzey Yazılım''a verilen senet')
) AS v(company_id, cheque_number, cheque_type, cheque_kind, customer_id,
       drawer_bank, drawer_branch, drawer_account_number, amount, currency,
       issue_date, due_date, status, bank_account_id, endorsed_to_customer_id, notes)
WHERE NOT EXISTS (
        SELECT 1 FROM cheque c WHERE c.company_id = 1 AND c.cheque_number = v.cheque_number);

-- ------------------------------------------------------------
-- 3b. Çek hareket geçmişi (status değişim audit trail)
-- ------------------------------------------------------------
INSERT INTO cheque_movement
    (company_id, cheque_id, movement_type, source_type, movement_date, bank_account_id, reason, is_deleted, created_at, updated_at)
SELECT 1, c.cheque_id, m.movement_type, 'MANUAL', m.movement_date, m.bank_account_id, m.reason, FALSE, NOW(), NOW()
FROM (VALUES
    ('CEK-2025-1001', 'REGISTERED', DATE '2025-11-15', NULL::bigint, 'Portföye giriş'),
    ('CEK-2025-1002', 'REGISTERED', DATE '2025-12-01', NULL::bigint, 'Portföye giriş'),
    ('CEK-2025-1002', 'DEPOSITED',  DATE '2026-06-10', 2::bigint,    'Ziraat hesabına tahsile verildi'),
    ('CEK-2025-1003', 'REGISTERED', DATE '2025-09-10', NULL::bigint, 'Portföye giriş'),
    ('CEK-2025-1003', 'DEPOSITED',  DATE '2026-03-01', 3::bigint,    'Garanti hesabına tahsile verildi'),
    ('CEK-2025-1003', 'COLLECTED',  DATE '2026-03-10', 3::bigint,    'Tahsil edildi'),
    ('SEN-2025-2001', 'REGISTERED', DATE '2026-01-20', NULL::bigint, 'Portföye giriş'),
    ('CEK-2025-1004', 'REGISTERED', DATE '2025-10-05', NULL::bigint, 'Portföye giriş'),
    ('CEK-2025-1004', 'DEPOSITED',  DATE '2026-03-20', 2::bigint,    'Tahsile verildi'),
    ('CEK-2025-1004', 'BOUNCED',    DATE '2026-04-05', NULL::bigint, 'Karşılıksız çıktı'),
    ('CEK-2025-1005', 'REGISTERED', DATE '2025-08-01', NULL::bigint, 'Portföye giriş'),
    ('CEK-2025-1005', 'ENDORSED',   DATE '2026-01-15', NULL::bigint, 'Marmara Gıda Ltd. firmasına ciro edildi'),
    ('CEK-2026-3001', 'REGISTERED', DATE '2026-05-22', NULL::bigint, 'Verilen çek kaydı'),
    ('SEN-2026-4001', 'REGISTERED', DATE '2026-04-10', NULL::bigint, 'Verilen senet kaydı')
) AS m(cheque_number, movement_type, movement_date, bank_account_id, reason)
JOIN cheque c ON c.company_id = 1 AND c.cheque_number = m.cheque_number
WHERE NOT EXISTS (
        SELECT 1 FROM cheque_movement cm
        WHERE cm.cheque_id = c.cheque_id
          AND cm.movement_type = m.movement_type
          AND cm.movement_date = m.movement_date);
