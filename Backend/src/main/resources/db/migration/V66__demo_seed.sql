-- ============================================================
-- MuhasebePlus - Demo Seed Data
-- ============================================================

-- ============================================================
-- 1. COMPANY
-- ============================================================
INSERT INTO company (company_name, tax_office, tax_number, address, city, phone, email, is_active, created_at, updated_at)
VALUES ('Teknoloji A.Ş.', 'Kadıköy Vergi Dairesi', '1234567890', 'Bağdat Caddesi No:42', 'İstanbul', '+90 212 555 0100', 'info@teknoloji.com.tr', TRUE, NOW(), NOW());

-- ============================================================
-- 2. USERS
-- ============================================================
-- BCrypt hash for "Sifre1234"
INSERT INTO "user" (email, password, first_name, last_name, phone, birth_date, created_at, updated_at, user_role, is_locked, is_active, failed_login_attempts, company_id)
VALUES
    ('admin@admin.net', '$2a$10$Six9qebCbDdJt9dO9C3YaeN3x9igWAF/5taMnpz1pK.ebDR9kcs4O', 'Admin', 'Kullanıcı',   '+90 505 100 0001', '1985-03-15', NOW(), NOW(), 'ADMIN', FALSE, TRUE, 0, 1),
    ('user@user.net',   '$2a$10$Six9qebCbDdJt9dO9C3YaeN3x9igWAF/5taMnpz1pK.ebDR9kcs4O', 'Emre',  'Şahin',        '+90 505 100 0002', '1992-07-22', NOW(), NOW(), 'USER',  FALSE, TRUE, 0, 1);

-- ============================================================
-- 3. CUSTOMERS
-- ============================================================
INSERT INTO customer (tax_number, name, address, city, phone, type, current_balance, is_deleted, created_at, updated_at, company_id, account_code, opening_balance, opening_balance_date, tax_office, identity_number, iban, currency, credit_limit, customer_role, status, customer_group)
VALUES
    ('1111111111', 'Anadolu Makina Ltd.',     'Ostim OSB No:12',         'Ankara',   '+90 312 111 2233', 'corporate',   15200.00, FALSE, NOW() - INTERVAL '18 months', NOW(), 1, '120.001', 10000.00, NOW() - INTERVAL '18 months', 'Ostim Vergi Dairesi',  NULL,         'TR330006100519786457841326', 'TRY', 50000.00, 'BOTH',  'ACTIVE',   'Sanayi'),
    ('2222222222', 'Başkent İnşaat A.Ş.',     'Çankaya Bulv. No:5',      'Ankara',   '+90 312 222 3344', 'corporate',    8750.00, FALSE, NOW() - INTERVAL '14 months', NOW(), 1, '120.002',  5000.00, NOW() - INTERVAL '14 months', 'Çankaya Vergi Dairesi', NULL,         'TR640017300000102610083520', 'TRY', 30000.00, 'BUYER', 'ACTIVE',   'İnşaat'),
    ('3333333333', 'Delta Lojistik A.Ş.',     'Tuzla Liman Yolu No:88',  'İstanbul', '+90 216 333 4455', 'corporate',   -3200.00, FALSE, NOW() - INTERVAL '10 months', NOW(), 1, '120.003',     0.00, NOW() - INTERVAL '10 months', 'Tuzla Vergi Dairesi',   NULL,         'TR320001200926801038620008', 'TRY', 20000.00, 'BOTH',  'ACTIVE',   'Lojistik'),
    ('4444444444', 'Ege Tekstil San. Ltd.',   'Ege Serbest Bölge A-12',  'İzmir',    '+90 232 444 5566', 'corporate',   22100.00, FALSE, NOW() - INTERVAL '22 months', NOW(), 1, '120.004', 15000.00, NOW() - INTERVAL '22 months', 'Gaziemir Vergi Dairesi',NULL,         'TR180003200000090447508062', 'TRY', 75000.00, 'BUYER', 'ACTIVE',   'Tekstil'),
    ('5555555555', 'Marmara Gıda Ltd.',       'Gıda OSB Blok 3',         'Bursa',    '+90 224 555 6677', 'corporate',    1500.00, FALSE, NOW() - INTERVAL '8 months',  NOW(), 1, '120.005',     0.00, NOW() - INTERVAL '8 months',  'Nilüfer Vergi Dairesi', NULL,         'TR140011100000000043490217', 'TRY', 15000.00, 'SELLER','ACTIVE',   'Gıda'),
    ('6666666666', 'Karadeniz Yapı A.Ş.',    'Trabzon Sanayi Sit. No:7', 'Trabzon',  '+90 462 666 7788', 'corporate',       0.00, FALSE, NOW() - INTERVAL '5 months',  NOW(), 1, '120.006',     0.00, NOW() - INTERVAL '5 months',  'Trabzon Vergi Dairesi', NULL,         'TR720006201000000083558617', 'TRY', 10000.00, 'BUYER', 'PASSIVE',  'İnşaat'),
    ('12345678901','Ahmet Yılmaz',            'Caferağa Mah. No:9/3',    'İstanbul', '+90 555 100 0011', 'individual',   2400.00, FALSE, NOW() - INTERVAL '12 months', NOW(), 1, '120.007',  2000.00, NOW() - INTERVAL '12 months', NULL,                    '12345678901', NULL,                         'TRY',  5000.00, 'BUYER', 'ACTIVE',   'Bireysel'),
    ('98765432109','Zeynep Kaya',             'Bornova Mah. No:22',      'İzmir',    '+90 555 100 0022', 'individual',    800.00, FALSE, NOW() - INTERVAL '6 months',  NOW(), 1, '120.008',   500.00, NOW() - INTERVAL '6 months',  NULL,                    '98765432109', NULL,                         'TRY',  3000.00, 'BUYER', 'ACTIVE',   'Bireysel'),
    ('7777777777', 'Güneş Enerji A.Ş.',      'ODTÜ Teknokent B2-401',    'Ankara',   '+90 312 777 8899', 'corporate',    5600.00, FALSE, NOW() - INTERVAL '3 months',  NOW(), 1, '120.009',     0.00, NOW() - INTERVAL '3 months',  'Çankaya Vergi Dairesi', NULL,         'TR550006100519786457841320', 'TRY', 25000.00, 'BOTH',  'ACTIVE',   'Enerji'),
    ('8888888888', 'Kuzey Yazılım Ltd.',      'Maslak Ax Center No:34',   'İstanbul', '+90 212 888 9900', 'corporate',   -1200.00, FALSE, NOW() - INTERVAL '2 months',  NOW(), 1, '120.010',     0.00, NOW() - INTERVAL '2 months',  'Sarıyer Vergi Dairesi', NULL,         'TR030006100519786457841321', 'TRY', 40000.00, 'SELLER','ACTIVE',   'Yazılım');

-- ============================================================
-- 4. CUSTOMER NOTES
-- ============================================================
INSERT INTO customer_note (customer_id, content, created_at, company_id)
VALUES
    (1, 'Müşteri ile yıllık sözleşme yenilendi. 2025 yılı için %5 indirim anlaşması yapıldı.', NOW() - INTERVAL '3 months', 1),
    (1, 'Ödeme düzeni mükemmel, vade uzatma talebi geldi.', NOW() - INTERVAL '1 month', 1),
    (2, 'Yeni proje teklifi bekleniyor - Çankaya konut projesi.', NOW() - INTERVAL '2 months', 1),
    (3, 'Gecikmeli ödeme sorunu yaşandı, 30 gün uzatma verildi.', NOW() - INTERVAL '4 months', 1),
    (4, 'Sezon başı büyük sipariş geldi, öncelikli müşteri statüsüne alındı.', NOW() - INTERVAL '5 months', 1),
    (7, 'Bireysel müşteri - kurumsal faturalama talebi var, vergi numarası bekleniyor.', NOW() - INTERVAL '2 months', 1);

-- ============================================================
-- 5. PRODUCTS
-- ============================================================
INSERT INTO product (barcode, name, description, unit, sale_price, vat_rate, cost_price, company_id)
VALUES
    ('8680001001001', 'Dizüstü Bilgisayar Pro 15"',  '15 inç, i7, 16GB RAM, 512GB SSD', 'Adet', 32999.00, 20.00, 22000.00, 1),
    ('8680001001002', 'Kablosuz Klavye & Mouse Set', 'Ergonomik set, Bluetooth 5.0',    'Adet',  1299.00, 20.00,   650.00, 1),
    ('8680001001003', 'USB-C Hub 7 Port',            '4K HDMI, USB 3.0, SD kart',       'Adet',   899.00, 20.00,   400.00, 1),
    ('8680001001004', 'Monitör 27" 4K IPS',          '3840x2160, 144Hz, HDR400',        'Adet',  8499.00, 20.00,  5500.00, 1),
    ('8680001001005', 'Yazıcı Lazer Renkli A4',      'WiFi, çift taraflı baskı',        'Adet',  3750.00, 20.00,  2200.00, 1),
    ('8680001001006', 'SSD Harici 1TB',              'USB 3.2 Gen2, 1050 MB/s okuma',   'Adet',  1599.00, 20.00,   850.00, 1),
    ('8680001001007', 'Web Kamerası 4K',             'AI Arka Plan, Otomatik Işık',     'Adet',  2299.00, 20.00,  1100.00, 1),
    ('8680001001008', 'UPS 1000VA',                  '600W, AVR, LCD ekran',            'Adet',  2899.00, 20.00,  1600.00, 1),
    ('8680001001009', 'Ağ Anahtarı 24 Port PoE',     'Gigabit, yönetilebilir',          'Adet',  6499.00, 20.00,  3800.00, 1),
    ('8680001001010', 'Telefon IP Masa Üstü',        'PoE, 2 SIP hattı, HD ses',        'Adet',  2100.00, 20.00,  1200.00, 1),
    ('8680001001011', 'Sunucu Rack Kabini 42U',      'Cam kapı, kilitli, tekerlekli',   'Adet', 12500.00, 20.00,  7500.00, 1),
    ('8680001001012', 'Kablo Cat6 305m Makara',      'LSZH, UTP, 305 metre',            'Makara',  850.00, 20.00,   450.00, 1),
    ('8680001001013', 'Yazılım Lisansı - Muhasebe',  'Yıllık abonelik, 5 kullanıcı',   'Lisans', 4500.00, 20.00,     0.00, 1),
    ('8680001001014', 'Güvenlik Kamerası 4MP Dome',  'PoE, IR 30m, IP67',              'Adet',  1850.00, 20.00,   950.00, 1),
    ('8680001001015', 'Akıllı PDU 16A',             '8 çıkış, uzaktan yönetim',        'Adet',  3200.00, 20.00,  1800.00, 1);

-- ============================================================
-- 6. STOCK
-- ============================================================
INSERT INTO stock (product_id, quantity, min_quantity, is_deleted, last_count_date, updated_at, created_at, company_id)
VALUES
    (1,  25,  5, FALSE, NOW() - INTERVAL '7 days',  NOW(), NOW() - INTERVAL '18 months', 1),
    (2,  80, 10, FALSE, NOW() - INTERVAL '7 days',  NOW(), NOW() - INTERVAL '18 months', 1),
    (3, 120, 15, FALSE, NOW() - INTERVAL '14 days', NOW(), NOW() - INTERVAL '18 months', 1),
    (4,  18,  3, FALSE, NOW() - INTERVAL '7 days',  NOW(), NOW() - INTERVAL '18 months', 1),
    (5,  12,  2, FALSE, NOW() - INTERVAL '30 days', NOW(), NOW() - INTERVAL '18 months', 1),
    (6,  95, 20, FALSE, NOW() - INTERVAL '7 days',  NOW(), NOW() - INTERVAL '18 months', 1),
    (7,  35,  5, FALSE, NOW() - INTERVAL '14 days', NOW(), NOW() - INTERVAL '18 months', 1),
    (8,  22,  4, FALSE, NOW() - INTERVAL '7 days',  NOW(), NOW() - INTERVAL '18 months', 1),
    (9,   8,  2, FALSE, NOW() - INTERVAL '30 days', NOW(), NOW() - INTERVAL '18 months', 1),
    (10, 45, 10, FALSE, NOW() - INTERVAL '14 days', NOW(), NOW() - INTERVAL '18 months', 1),
    (11,  4,  1, FALSE, NOW() - INTERVAL '60 days', NOW(), NOW() - INTERVAL '18 months', 1),
    (12, 15,  3, FALSE, NOW() - INTERVAL '30 days', NOW(), NOW() - INTERVAL '18 months', 1),
    (13, 999, 10, FALSE, NOW() - INTERVAL '7 days', NOW(), NOW() - INTERVAL '18 months', 1),
    (14, 62, 10, FALSE, NOW() - INTERVAL '14 days', NOW(), NOW() - INTERVAL '18 months', 1),
    (15, 11,  2, FALSE, NOW() - INTERVAL '30 days', NOW(), NOW() - INTERVAL '18 months', 1);

-- ============================================================
-- 7. BANK ACCOUNTS
-- ============================================================
INSERT INTO bank_account (user_id, bank_name, iban, currency, created_at, updated_at, is_deleted, company_id)
VALUES
    (1, 'Nakit Kasa',       'CASH-1',                             'TRY', NOW() - INTERVAL '18 months', NOW(), FALSE, 1),
    (1, 'Ziraat Bankası',   'TR330006100519786457841326',          'TRY', NOW() - INTERVAL '18 months', NOW(), FALSE, 1),
    (1, 'Garanti BBVA',     'TR640017300000102610083520',          'TRY', NOW() - INTERVAL '18 months', NOW(), FALSE, 1),
    (1, 'İş Bankası',       'TR320001200926801038620008',          'TRY', NOW() - INTERVAL '12 months', NOW(), FALSE, 1),
    (1, 'Garanti BBVA USD', 'TR180003200000090447508062',          'USD', NOW() - INTERVAL '12 months', NOW(), FALSE, 1);

-- ============================================================
-- 8. INVOICE SERIES
-- ============================================================
INSERT INTO invoice_series (company_id, series_code, invoice_type, prefix, year, last_sequence_number, is_active, created_at, updated_at)
VALUES
    (1, 'STF', 'sale',     'STF', 2025, 20, TRUE, NOW() - INTERVAL '12 months', NOW()),
    (1, 'ALF', 'purchase', 'ALF', 2025, 10, TRUE, NOW() - INTERVAL '12 months', NOW());

-- ============================================================
-- 9. INVOICES
-- ============================================================
INSERT INTO invoice (invoice_number, customer_id, invoice_type, invoice_date, due_date, payment_status, subtotal, vat_amount, total_amount, created_at, updated_at, company_id, currency, exchange_rate, description, series_id, sequence_number, cancelled, is_deleted)
VALUES
    -- Satış faturalar (paid)
    ('STF-2025-001', 1, 'sale', '2025-01-10', '2025-02-10', 'paid',    27499.17, 5499.83,  32999.00, '2025-01-10 09:00:00', NOW(), 1, 'TRY', 1.0000, 'Dizüstü bilgisayar satışı', 1,  1, FALSE, FALSE),
    ('STF-2025-002', 2, 'sale', '2025-01-20', '2025-02-20', 'paid',     3415.00,  683.00,   4098.00, '2025-01-20 10:30:00', NOW(), 1, 'TRY', 1.0000, 'Monitör ve klavye seti',    1,  2, FALSE, FALSE),
    ('STF-2025-003', 4, 'sale', '2025-02-05', '2025-03-05', 'paid',    16998.33, 3399.67,  20398.00, '2025-02-05 11:00:00', NOW(), 1, 'TRY', 1.0000, 'Yazıcı ve SSD satışı',      1,  3, FALSE, FALSE),
    ('STF-2025-004', 1, 'sale', '2025-02-15', '2025-03-15', 'paid',    70824.17,14164.83,  84989.00, '2025-02-15 14:00:00', NOW(), 1, 'TRY', 1.0000, '3 adet Dizüstü PC satışı',  1,  4, FALSE, FALSE),
    ('STF-2025-005', 3, 'sale', '2025-03-01', '2025-04-01', 'overdue',  5415.83, 1083.17,   6499.00, '2025-03-01 09:30:00', NOW(), 1, 'TRY', 1.0000, 'Ağ anahtarı satışı',        1,  5, FALSE, FALSE),
    ('STF-2025-006', 7, 'sale', '2025-03-10', '2025-04-10', 'paid',     2665.00,  533.00,   3198.00, '2025-03-10 13:00:00', NOW(), 1, 'TRY', 1.0000, 'Kamera ve UPS satışı',      1,  6, FALSE, FALSE),
    ('STF-2025-007', 9, 'sale', '2025-03-20', '2025-04-20', 'paid',    10415.83, 2083.17,  12499.00, '2025-03-20 10:00:00', NOW(), 1, 'TRY', 1.0000, 'Rack kabini satışı',        1,  7, FALSE, FALSE),
    ('STF-2025-008', 2, 'sale', '2025-04-02', '2025-05-02', 'paid',     5624.17, 1124.83,   6749.00, '2025-04-02 11:00:00', NOW(), 1, 'TRY', 1.0000, 'IP telefon ve kablo',       1,  8, FALSE, FALSE),
    ('STF-2025-009', 5, 'sale', '2025-04-15', '2025-05-15', 'paid',     3748.33,  749.67,   4498.00, '2025-04-15 09:00:00', NOW(), 1, 'TRY', 1.0000, 'Web kamerası x2 satışı',    1,  9, FALSE, FALSE),
    ('STF-2025-010', 4, 'sale', '2025-04-25', '2025-05-25', 'paid',    37499.17, 7499.83,  44999.00, '2025-04-25 14:30:00', NOW(), 1, 'TRY', 1.0000, 'Dizüstü PC x1 + Monitör x3',1, 10, FALSE, FALSE),
    ('STF-2025-011', 1, 'sale', '2025-05-05', '2025-06-05', 'paid',     2665.00,  533.00,   3198.00, '2025-05-05 10:00:00', NOW(), 1, 'TRY', 1.0000, 'PDU ve kamera satışı',      1, 11, FALSE, FALSE),
    ('STF-2025-012', 8, 'sale', '2025-05-15', '2025-06-15', 'pending',  4582.50,  916.50,   5499.00, '2025-05-15 11:30:00', NOW(), 1, 'TRY', 1.0000, 'Lisans + hub satışı',       1, 12, FALSE, FALSE),
    ('STF-2025-013', 3, 'sale', '2025-05-28', '2025-06-28', 'overdue',  8749.17, 1749.83,  10499.00, '2025-05-28 09:00:00', NOW(), 1, 'TRY', 1.0000, 'Ağ ekipmanları paketi',     1, 13, FALSE, FALSE),
    ('STF-2025-014', 6, 'sale', '2025-06-01', '2025-07-01', 'pending',  6665.83, 1333.17,   7999.00, '2025-06-01 13:00:00', NOW(), 1, 'TRY', 1.0000, 'Monitör x1 + UPS x2',      1, 14, FALSE, FALSE),
    ('STF-2025-015', 10,'sale', '2025-06-10', '2025-07-10', 'pending', 27499.17, 5499.83,  32999.00, '2025-06-10 10:00:00', NOW(), 1, 'TRY', 1.0000, 'Dizüstü PC yeni sipariş',   1, 15, FALSE, FALSE),
    -- Alış faturalar
    ('ALF-2025-001', 5, 'purchase','2025-01-08','2025-02-08','paid',    18333.33, 3666.67,  22000.00,'2025-01-08 08:00:00', NOW(), 1, 'TRY', 1.0000, 'Stok alımı - Laptop',       2,  1, FALSE, FALSE),
    ('ALF-2025-002', 5, 'purchase','2025-02-10','2025-03-10','paid',     5416.67, 1083.33,   6500.00,'2025-02-10 09:00:00', NOW(), 1, 'TRY', 1.0000, 'Stok alımı - Yazıcı',       2,  2, FALSE, FALSE),
    ('ALF-2025-003', 5, 'purchase','2025-03-12','2025-04-12','paid',     4583.33,  916.67,   5500.00,'2025-03-12 08:30:00', NOW(), 1, 'TRY', 1.0000, 'Stok alımı - Monitör',      2,  3, FALSE, FALSE),
    ('ALF-2025-004', 5, 'purchase','2025-04-18','2025-05-18','paid',     3166.67,  633.33,   3800.00,'2025-04-18 09:00:00', NOW(), 1, 'TRY', 1.0000, 'Stok alımı - Ağ anahtarı',  2,  4, FALSE, FALSE),
    ('ALF-2025-005', 5, 'purchase','2025-05-22','2025-06-22','pending',  7916.67, 1583.33,   9500.00,'2025-05-22 10:00:00', NOW(), 1, 'TRY', 1.0000, 'Stok alımı - Kamera+PDU',   2,  5, FALSE, FALSE);

-- ============================================================
-- 10. INVOICE LINE ITEMS
-- ============================================================
INSERT INTO invoice_line_item (invoice_id, product_id, quantity, unit_price, vat_rate, line_total, is_deleted, created_at, updated_at, company_id)
VALUES
    -- STF-2025-001: 1x Laptop
    (1,  1,  1, 27499.17, 20.00, 27499.17, FALSE, NOW(), NOW(), 1),
    -- STF-2025-002: 1x Monitör + 2x Klavye
    (2,  4,  1,  7082.50, 20.00,  7082.50, FALSE, NOW(), NOW(), 1),
    (2,  2,  2,  1082.50, 20.00,  2165.00, FALSE, NOW(), NOW(), 1),
    -- STF-2025-003: 2x Yazıcı + 3x SSD
    (3,  5,  2,  3125.00, 20.00,  6250.00, FALSE, NOW(), NOW(), 1),
    (3,  6,  3,  1332.78, 20.00,  3998.33, FALSE, NOW(), NOW(), 1),
    -- STF-2025-004: 3x Laptop
    (4,  1,  3, 23608.06, 20.00, 70824.17, FALSE, NOW(), NOW(), 1),
    -- STF-2025-005: 1x Ağ Anahtarı
    (5,  9,  1,  5415.83, 20.00,  5415.83, FALSE, NOW(), NOW(), 1),
    -- STF-2025-006: 1x Kamera + 1x UPS
    (6, 14,  1,  1541.67, 20.00,  1541.67, FALSE, NOW(), NOW(), 1),
    (6,  8,  1,  1123.33, 20.00,  1123.33, FALSE, NOW(), NOW(), 1),
    -- STF-2025-007: 1x Rack Kabini
    (7, 11,  1, 10415.83, 20.00, 10415.83, FALSE, NOW(), NOW(), 1),
    -- STF-2025-008: 2x IP Telefon + 2x Kablo Makara
    (8, 10,  2,  1750.00, 20.00,  3500.00, FALSE, NOW(), NOW(), 1),
    (8, 12,  2,   708.33, 20.00,  1416.67, FALSE, NOW(), NOW(), 1),
    -- STF-2025-009: 2x Web Kamerası
    (9,  7,  2,  1874.17, 20.00,  3748.33, FALSE, NOW(), NOW(), 1),
    -- STF-2025-010: 1x Laptop + 3x Monitör
    (10, 1,  1, 27499.17, 20.00, 27499.17, FALSE, NOW(), NOW(), 1),
    (10, 4,  3,  3333.33, 20.00, 10000.00, FALSE, NOW(), NOW(), 1),
    -- STF-2025-011: 1x PDU + 1x Kamera
    (11,15,  1,  2666.67, 20.00,  2666.67, FALSE, NOW(), NOW(), 1),
    (11,14,  1,   541.67, 20.00,   541.67, FALSE, NOW(), NOW(), 1),  -- küçük ek ürün
    -- STF-2025-012: 1x Lisans + 2x Hub
    (12,13,  1,  3750.00, 20.00,  3750.00, FALSE, NOW(), NOW(), 1),
    (12, 3,  2,   416.25, 20.00,   832.50, FALSE, NOW(), NOW(), 1),
    -- STF-2025-013: 1x Ağ Anahtarı + 2x PDU
    (13, 9,  1,  5415.83, 20.00,  5415.83, FALSE, NOW(), NOW(), 1),
    (13,15,  2,  1666.67, 20.00,  3333.33, FALSE, NOW(), NOW(), 1),
    -- STF-2025-014: 1x Monitör + 2x UPS
    (14, 4,  1,  7082.50, 20.00,  7082.50, FALSE, NOW(), NOW(), 1),
    (14, 8,  2,   791.67, 20.00,  1583.33, FALSE, NOW(), NOW(), 1),
    -- STF-2025-015: 1x Laptop
    (15, 1,  1, 27499.17, 20.00, 27499.17, FALSE, NOW(), NOW(), 1),
    -- ALF-2025-001 : Laptop stok alımı
    (16, 1,  1, 18333.33, 20.00, 18333.33, FALSE, NOW(), NOW(), 1),
    -- ALF-2025-002 : Yazıcı stok alımı
    (17, 5,  1,  5416.67, 20.00,  5416.67, FALSE, NOW(), NOW(), 1),
    -- ALF-2025-003 : Monitör stok alımı
    (18, 4,  1,  4583.33, 20.00,  4583.33, FALSE, NOW(), NOW(), 1),
    -- ALF-2025-004 : Ağ anahtarı stok alımı
    (19, 9,  1,  3166.67, 20.00,  3166.67, FALSE, NOW(), NOW(), 1),
    -- ALF-2025-005 : Kamera+PDU stok alımı
    (20,14,  3,  1583.33, 20.00,  4750.00, FALSE, NOW(), NOW(), 1),
    (20,15,  1,  3166.67, 20.00,  3166.67, FALSE, NOW(), NOW(), 1);

-- ============================================================
-- 11. TRANSACTIONS
-- ============================================================
INSERT INTO "transaction" (account_id, invoice_id, transaction_type, amount, description, category, transaction_date, created_at, updated_at, is_recurring, is_deleted, company_id)
VALUES
    -- Tahsilat girişleri (satış fatura ödemeleri)
    (2, 1,  'INCOME',  32999.00, 'STF-2025-001 tahsilatı - Anadolu Makina',  'Satış Tahsilatı',   '2025-02-08', NOW(), NOW(), FALSE, FALSE, 1),
    (2, 2,  'INCOME',   4098.00, 'STF-2025-002 tahsilatı - Başkent İnşaat',  'Satış Tahsilatı',   '2025-02-18', NOW(), NOW(), FALSE, FALSE, 1),
    (3, 3,  'INCOME',  20398.00, 'STF-2025-003 tahsilatı - Ege Tekstil',     'Satış Tahsilatı',   '2025-03-03', NOW(), NOW(), FALSE, FALSE, 1),
    (2, 4,  'INCOME',  84989.00, 'STF-2025-004 tahsilatı - Anadolu Makina',  'Satış Tahsilatı',   '2025-03-14', NOW(), NOW(), FALSE, FALSE, 1),
    (1, 6,  'INCOME',   3198.00, 'STF-2025-006 nakit tahsilatı',             'Satış Tahsilatı',   '2025-04-08', NOW(), NOW(), FALSE, FALSE, 1),
    (3, 7,  'INCOME',  12499.00, 'STF-2025-007 tahsilatı - Güneş Enerji',    'Satış Tahsilatı',   '2025-04-18', NOW(), NOW(), FALSE, FALSE, 1),
    (2, 8,  'INCOME',   6749.00, 'STF-2025-008 tahsilatı - Başkent İnşaat',  'Satış Tahsilatı',   '2025-04-30', NOW(), NOW(), FALSE, FALSE, 1),
    (3, 9,  'INCOME',   4498.00, 'STF-2025-009 tahsilatı - Marmara Gıda',    'Satış Tahsilatı',   '2025-05-13', NOW(), NOW(), FALSE, FALSE, 1),
    (2, 10, 'INCOME',  44999.00, 'STF-2025-010 tahsilatı - Ege Tekstil',     'Satış Tahsilatı',   '2025-05-23', NOW(), NOW(), FALSE, FALSE, 1),
    (4, 11, 'INCOME',   3198.00, 'STF-2025-011 tahsilatı - Anadolu Makina',  'Satış Tahsilatı',   '2025-06-03', NOW(), NOW(), FALSE, FALSE, 1),
    -- Ödeme çıkışları (alış fatura ödemeleri)
    (2, 16, 'EXPENSE', 22000.00, 'ALF-2025-001 ödemesi - Marmara Gıda',      'Stok Alımı',        '2025-02-06', NOW(), NOW(), FALSE, FALSE, 1),
    (3, 17, 'EXPENSE',  6500.00, 'ALF-2025-002 ödemesi - Marmara Gıda',      'Stok Alımı',        '2025-03-09', NOW(), NOW(), FALSE, FALSE, 1),
    (2, 18, 'EXPENSE',  5500.00, 'ALF-2025-003 ödemesi - Marmara Gıda',      'Stok Alımı',        '2025-04-10', NOW(), NOW(), FALSE, FALSE, 1),
    (2, 19, 'EXPENSE',  3800.00, 'ALF-2025-004 ödemesi - Marmara Gıda',      'Stok Alımı',        '2025-05-16', NOW(), NOW(), FALSE, FALSE, 1),
    -- Genel giderler
    (2, NULL, 'EXPENSE',  8500.00, 'Ocak 2025 kira ödemesi',                 'Kira',              '2025-01-03', NOW(), NOW(), TRUE,  FALSE, 1),
    (2, NULL, 'EXPENSE',  8500.00, 'Şubat 2025 kira ödemesi',                'Kira',              '2025-02-03', NOW(), NOW(), TRUE,  FALSE, 1),
    (2, NULL, 'EXPENSE',  8500.00, 'Mart 2025 kira ödemesi',                 'Kira',              '2025-03-03', NOW(), NOW(), TRUE,  FALSE, 1),
    (2, NULL, 'EXPENSE',  8500.00, 'Nisan 2025 kira ödemesi',                'Kira',              '2025-04-03', NOW(), NOW(), TRUE,  FALSE, 1),
    (2, NULL, 'EXPENSE',  8500.00, 'Mayıs 2025 kira ödemesi',                'Kira',              '2025-05-03', NOW(), NOW(), TRUE,  FALSE, 1),
    (2, NULL, 'EXPENSE',  8500.00, 'Haziran 2025 kira ödemesi',              'Kira',              '2025-06-03', NOW(), NOW(), TRUE,  FALSE, 1),
    (3, NULL, 'EXPENSE', 45000.00, 'Ocak 2025 maaş ödemeleri',               'Personel',          '2025-01-31', NOW(), NOW(), TRUE,  FALSE, 1),
    (3, NULL, 'EXPENSE', 45000.00, 'Şubat 2025 maaş ödemeleri',              'Personel',          '2025-02-28', NOW(), NOW(), TRUE,  FALSE, 1),
    (3, NULL, 'EXPENSE', 48000.00, 'Mart 2025 maaş ödemeleri',               'Personel',          '2025-03-31', NOW(), NOW(), TRUE,  FALSE, 1),
    (3, NULL, 'EXPENSE', 48000.00, 'Nisan 2025 maaş ödemeleri',              'Personel',          '2025-04-30', NOW(), NOW(), TRUE,  FALSE, 1),
    (3, NULL, 'EXPENSE', 48000.00, 'Mayıs 2025 maaş ödemeleri',              'Personel',          '2025-05-31', NOW(), NOW(), TRUE,  FALSE, 1),
    (2, NULL, 'EXPENSE',  3200.00, 'Elektrik & Doğalgaz - Q1 2025',          'Faturalar',         '2025-03-15', NOW(), NOW(), FALSE, FALSE, 1),
    (2, NULL, 'EXPENSE',  2800.00, 'Elektrik & Doğalgaz - Q2 2025',          'Faturalar',         '2025-06-15', NOW(), NOW(), FALSE, FALSE, 1),
    (2, NULL, 'EXPENSE',  1500.00, 'İnternet & Telefon - 6 aylık',           'Faturalar',         '2025-01-10', NOW(), NOW(), FALSE, FALSE, 1),
    (5, NULL, 'EXPENSE',  4200.00, 'Fuar katılım ücreti - Teknoloji Fuarı',  'Pazarlama',         '2025-04-20', NOW(), NOW(), FALSE, FALSE, 1),
    (2, NULL, 'INCOME',  15000.00, 'Teşvik ödemesi - KOSGEB',                'Diğer Gelirler',    '2025-02-20', NOW(), NOW(), FALSE, FALSE, 1);

-- ============================================================
-- 12. INVOICE PAYMENTS
-- ============================================================
INSERT INTO invoice_payment (company_id, invoice_id, amount, payment_date, payment_method, bank_account_id, notes, created_at, updated_at, is_deleted)
VALUES
    (1,  1,  32999.00, '2025-02-08', 'BANK_TRANSFER', 2, 'Tam ödeme alındı',         NOW(), NOW(), FALSE),
    (1,  2,   4098.00, '2025-02-18', 'BANK_TRANSFER', 2, 'Tam ödeme alındı',         NOW(), NOW(), FALSE),
    (1,  3,  20398.00, '2025-03-03', 'BANK_TRANSFER', 3, 'Tam ödeme alındı',         NOW(), NOW(), FALSE),
    (1,  4,  84989.00, '2025-03-14', 'BANK_TRANSFER', 2, 'Tam ödeme alındı',         NOW(), NOW(), FALSE),
    (1,  6,   3198.00, '2025-04-08', 'CASH',          1, 'Nakit ödeme',              NOW(), NOW(), FALSE),
    (1,  7,  12499.00, '2025-04-18', 'BANK_TRANSFER', 3, 'Tam ödeme alındı',         NOW(), NOW(), FALSE),
    (1,  8,   6749.00, '2025-04-30', 'BANK_TRANSFER', 2, 'Tam ödeme alındı',         NOW(), NOW(), FALSE),
    (1,  9,   4498.00, '2025-05-13', 'BANK_TRANSFER', 3, 'Tam ödeme alındı',         NOW(), NOW(), FALSE),
    (1, 10,  44999.00, '2025-05-23', 'BANK_TRANSFER', 2, 'Tam ödeme alındı',         NOW(), NOW(), FALSE),
    (1, 11,   3198.00, '2025-06-03', 'BANK_TRANSFER', 4, 'Tam ödeme alındı',         NOW(), NOW(), FALSE);

-- ============================================================
-- 13. STOCK MOVEMENTS
-- ============================================================
INSERT INTO stock_movement (company_id, product_id, quantity, movement_type, source_type, source_id, unit_cost, reason, created_at, updated_at, is_deleted)
VALUES
    (1,  1, 30, 'IN',              'PURCHASE',  16, 22000.00, 'ALF-2025-001 stok girişi',      '2025-01-08', NOW(), FALSE),
    (1,  1, -1, 'OUT',             'INVOICE',    1,     NULL, 'STF-2025-001 satış çıkışı',     '2025-01-10', NOW(), FALSE),
    (1,  1, -3, 'OUT',             'INVOICE',    4,     NULL, 'STF-2025-004 satış çıkışı',     '2025-02-15', NOW(), FALSE),
    (1,  1, -1, 'OUT',             'INVOICE',   10,     NULL, 'STF-2025-010 satış çıkışı',     '2025-04-25', NOW(), FALSE),
    (1,  1, -1, 'OUT',             'INVOICE',   15,     NULL, 'STF-2025-015 satış çıkışı',     '2025-06-10', NOW(), FALSE),
    (1,  4,  3, 'IN',              'PURCHASE',  18,  5500.00, 'ALF-2025-003 stok girişi',      '2025-03-12', NOW(), FALSE),
    (1,  4, -1, 'OUT',             'INVOICE',    2,     NULL, 'STF-2025-002 satış çıkışı',     '2025-01-20', NOW(), FALSE),
    (1,  4, -3, 'OUT',             'INVOICE',   10,     NULL, 'STF-2025-010 satış çıkışı',     '2025-04-25', NOW(), FALSE),
    (1,  5,  3, 'IN',              'PURCHASE',  17,  2200.00, 'ALF-2025-002 stok girişi',      '2025-02-10', NOW(), FALSE),
    (1,  5, -2, 'OUT',             'INVOICE',    3,     NULL, 'STF-2025-003 satış çıkışı',     '2025-02-05', NOW(), FALSE),
    (1,  9,  2, 'IN',              'PURCHASE',  19,  3800.00, 'ALF-2025-004 stok girişi',      '2025-04-18', NOW(), FALSE),
    (1,  9, -1, 'OUT',             'INVOICE',    5,     NULL, 'STF-2025-005 satış çıkışı',     '2025-03-01', NOW(), FALSE),
    (1,  9, -1, 'OUT',             'INVOICE',   13,     NULL, 'STF-2025-013 satış çıkışı',     '2025-05-28', NOW(), FALSE),
    (1, 14,  5, 'IN',              'PURCHASE',  20,   950.00, 'ALF-2025-005 stok girişi',      '2025-05-22', NOW(), FALSE),
    (1, 15,  2, 'IN',              'PURCHASE',  20,  1800.00, 'ALF-2025-005 stok girişi',      '2025-05-22', NOW(), FALSE),
    (1,  2,  5, 'ADJUSTMENT',      'MANUAL',  NULL,     NULL, 'Sayım fazlası düzeltme',        '2025-04-01', NOW(), FALSE),
    (1,  6, 10, 'ADJUSTMENT',      'MANUAL',  NULL,     NULL, 'Yeni depo devri - stok sayımı', '2025-04-01', NOW(), FALSE);

-- ============================================================
-- 14. BUDGETS
-- ============================================================
INSERT INTO budget (company_id, year, month, category, transaction_type, planned_amount, notes, created_at, updated_at, is_deleted)
VALUES
    (1, 2025, 1, 'Satış Tahsilatı',  'INCOME',  120000.00, 'Q1 satış hedefi',         NOW(), NOW(), FALSE),
    (1, 2025, 2, 'Satış Tahsilatı',  'INCOME',  130000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 3, 'Satış Tahsilatı',  'INCOME',  140000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 4, 'Satış Tahsilatı',  'INCOME',  150000.00, 'Q2 satış hedefi',         NOW(), NOW(), FALSE),
    (1, 2025, 5, 'Satış Tahsilatı',  'INCOME',  160000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 6, 'Satış Tahsilatı',  'INCOME',  170000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 1, 'Kira',             'EXPENSE',   8500.00, 'Yıllık kira bütçesi',     NOW(), NOW(), FALSE),
    (1, 2025, 2, 'Kira',             'EXPENSE',   8500.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 3, 'Kira',             'EXPENSE',   8500.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 4, 'Kira',             'EXPENSE',   8500.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 5, 'Kira',             'EXPENSE',   8500.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 6, 'Kira',             'EXPENSE',   8500.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 1, 'Personel',         'EXPENSE',  45000.00, 'Maaş + SGK giderleri',    NOW(), NOW(), FALSE),
    (1, 2025, 2, 'Personel',         'EXPENSE',  45000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 3, 'Personel',         'EXPENSE',  48000.00, '2 yeni işe alım',         NOW(), NOW(), FALSE),
    (1, 2025, 4, 'Personel',         'EXPENSE',  48000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 5, 'Personel',         'EXPENSE',  48000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 6, 'Personel',         'EXPENSE',  50000.00, 'Yaz dönemi ek personel',  NOW(), NOW(), FALSE),
    (1, 2025, 1, 'Stok Alımı',       'EXPENSE',  40000.00, 'Q1 stok yenileme',        NOW(), NOW(), FALSE),
    (1, 2025, 2, 'Stok Alımı',       'EXPENSE',  35000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 3, 'Stok Alımı',       'EXPENSE',  30000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 4, 'Stok Alımı',       'EXPENSE',  25000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 5, 'Stok Alımı',       'EXPENSE',  35000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 6, 'Stok Alımı',       'EXPENSE',  30000.00, NULL,                       NOW(), NOW(), FALSE),
    (1, 2025, 1, 'Pazarlama',        'EXPENSE',   5000.00, 'Dijital pazarlama',        NOW(), NOW(), FALSE),
    (1, 2025, 4, 'Pazarlama',        'EXPENSE',   8000.00, 'Fuar & etkinlik',          NOW(), NOW(), FALSE),
    (1, 2025, 6, 'Pazarlama',        'EXPENSE',   6000.00, 'Sosyal medya kampanyası',  NOW(), NOW(), FALSE);

-- ============================================================
-- 15. NOTIFICATIONS
-- ============================================================
INSERT INTO notification (user_id, notification_type, subject, message, sent_at, company_id)
VALUES
    (1, 'warning', 'Vadesi Geçen Fatura',      'STF-2025-005 numaralı fatura (Delta Lojistik) vadesi 31 gün geçti. Tutar: 6.499 TL', NOW() - INTERVAL '31 days', 1),
    (1, 'warning', 'Vadesi Geçen Fatura',      'STF-2025-013 numaralı fatura (Delta Lojistik) vadesi 5 gün geçti. Tutar: 10.499 TL', NOW() - INTERVAL '5 days',  1),
    (1, 'info',    'Ödeme Alındı',             'STF-2025-010 faturası için 44.999 TL tahsilat gerçekleşti.',                          NOW() - INTERVAL '24 days', 1),
    (1, 'info',    'Yeni Sipariş',             'Kuzey Yazılım Ltd. yeni sipariş oluşturdu. Fatura STF-2025-015 kesildi.',             NOW() - INTERVAL '7 days',  1),
    (1, 'warning', 'Düşük Stok Uyarısı',      'Sunucu Rack Kabini (42U) stok miktarı minimum seviyede: 4 adet kaldı.',               NOW() - INTERVAL '3 days',  1),
    (1, 'warning', 'Düşük Stok Uyarısı',      'Ağ Anahtarı 24 Port PoE stok miktarı minimum seviyede: 8 adet kaldı.',               NOW() - INTERVAL '2 days',  1),
    (1, 'info',    'Bütçe Raporu Hazır',       'Ocak-Haziran 2025 dönemi bütçe gerçekleşme raporu oluşturuldu.',                     NOW() - INTERVAL '1 day',   1),
    (2, 'info',    'Hoş Geldiniz',             'MuhasebePlus sistemine hoş geldiniz. Yardım için destek bölümünü ziyaret ediniz.',    NOW() - INTERVAL '6 months',1),
    (1, 'error',   'Giriş Başarısız Uyarısı', '3 başarısız giriş denemesi tespit edildi. IP: 192.168.1.105',                         NOW() - INTERVAL '15 days', 1),
    (1, 'info',    'Sistem Güncellemesi',      'MuhasebePlus v2.5.0 başarıyla güncellendi. Yeni özellikler için sürüm notlarına bakın.', NOW() - INTERVAL '10 days',1);

-- ============================================================
-- 16. SYSTEM LOGS
-- ============================================================
INSERT INTO system_log (user_id, log_level, details, timestamp, ip_address, company_id)
VALUES
    (1, 'info',    'Kullanıcı giriş yaptı: admin@admin.net',                              NOW() - INTERVAL '1 day',    '192.168.1.1',   1),
    (2, 'info',    'Kullanıcı giriş yaptı: user@user.net',                                NOW() - INTERVAL '2 days',   '192.168.1.2',   1),
    (1, 'info',    'Yeni fatura oluşturuldu: STF-2025-015',                               NOW() - INTERVAL '7 days',   '192.168.1.1',   1),
    (1, 'info',    'Fatura ödeme kaydedildi: STF-2025-011 - 3.198 TL',                   NOW() - INTERVAL '14 days',  '192.168.1.1',   1),
    (1, 'info',    'Yeni müşteri eklendi: Kuzey Yazılım Ltd.',                            NOW() - INTERVAL '2 months', '192.168.1.1',   1),
    (1, 'info',    'Stok güncellendi: Dizüstü Bilgisayar Pro 15" - 30 adet giriş',       NOW() - INTERVAL '5 months', '192.168.1.1',   1),
    (1, 'warning', 'Vadesi geçen fatura tespit edildi: STF-2025-005 (Delta Lojistik)',    NOW() - INTERVAL '31 days',  '192.168.1.1',   1),
    (1, 'warning', 'Vadesi geçen fatura tespit edildi: STF-2025-013 (Delta Lojistik)',    NOW() - INTERVAL '5 days',   '192.168.1.1',   1),
    (1, 'error',   'Başarısız giriş denemesi: sahtekullanici@hacker.com - IP: 192.168.1.105', NOW() - INTERVAL '15 days', '192.168.1.105', 1),
    (1, 'info',    'Rapor oluşturuldu: Kar/Zarar raporu - Ocak-Haziran 2025',            NOW() - INTERVAL '1 day',    '192.168.1.1',   1),
    (1, 'info',    'Bütçe güncellendi: Haziran 2025 Personel bütçesi 50.000 TL olarak düzenlendi', NOW() - INTERVAL '20 days', '192.168.1.1', 1),
    (2, 'info',    'Fatura görüntülendi: STF-2025-012',                                  NOW() - INTERVAL '3 days',   '192.168.1.2',   1),
    (1, 'info',    'Banka hesabı eklendi: İş Bankası TRY hesabı',                        NOW() - INTERVAL '6 months', '192.168.1.1',   1),
    (1, 'info',    'Fatura serisi oluşturuldu: STF (Satış) ve ALF (Alış)',               NOW() - INTERVAL '12 months','192.168.1.1',   1),
    (1, 'warning', 'Minimum stok uyarısı: Sunucu Rack Kabini 42U - 4 adet',              NOW() - INTERVAL '3 days',   '192.168.1.1',   1);

-- ============================================================
-- 17. TRANSACTION TEMPLATES
-- ============================================================
INSERT INTO transaction_template (user_id, template_name, default_amount, company_id, transaction_type, category, description, created_at, updated_at, is_deleted)
VALUES
    (1, 'Aylık Kira',       8500.00, 1, 'EXPENSE', 'Kira',      'Aylık kira ödemesi',            NOW(), NOW(), FALSE),
    (1, 'Maaş Ödemesi',    48000.00, 1, 'EXPENSE', 'Personel',  'Aylık maaş ve SGK ödemeleri',   NOW(), NOW(), FALSE),
    (1, 'Elektrik Faturası', 1200.00, 1, 'EXPENSE', 'Faturalar', 'Aylık elektrik ödemesi',        NOW(), NOW(), FALSE),
    (1, 'İnternet Faturası',  500.00, 1, 'EXPENSE', 'Faturalar', 'Aylık internet ödemesi',        NOW(), NOW(), FALSE),
    (1, 'Tahsilat Girişi',      0.00, 1, 'INCOME',  'Satış Tahsilatı', 'Genel tahsilat şablonu',  NOW(), NOW(), FALSE);
