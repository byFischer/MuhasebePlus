-- System widget templates (is_system=true, no company/user binding)
-- These show up in every user's widget gallery as starter templates.

INSERT INTO widget_definitions
    (company_id, user_id, name, description, widget_type, data_source, query_config, visual_config, config, is_template, is_system)
VALUES
    -- 1. Bu Ay Toplam Gelir
    (NULL, NULL,
     'Bu Ay Toplam Gelir',
     'Bu ayki tüm gelir işlemlerinin toplamı',
     'DATA_KPI',
     'TRANSACTION',
     '{"dataSource":"TRANSACTION","filters":[{"field":"transactionType","operator":"EQ","value":"INCOME"}],"groupBy":[],"aggregate":{"function":"SUM","field":"amount","alias":"Gelir"}}'::jsonb,
     '{"chartType":"KPI","title":"Bu Ay Toplam Gelir","color":"pos"}'::jsonb,
     '{}'::jsonb,
     TRUE, TRUE),

    -- 2. Bu Ay Toplam Gider
    (NULL, NULL,
     'Bu Ay Toplam Gider',
     'Bu ayki tüm gider işlemlerinin toplamı',
     'DATA_KPI',
     'TRANSACTION',
     '{"dataSource":"TRANSACTION","filters":[{"field":"transactionType","operator":"EQ","value":"EXPENSE"}],"groupBy":[],"aggregate":{"function":"SUM","field":"amount","alias":"Gider"}}'::jsonb,
     '{"chartType":"KPI","title":"Bu Ay Toplam Gider","color":"neg"}'::jsonb,
     '{}'::jsonb,
     TRUE, TRUE),

    -- 3. Aylık Gelir Trendi
    (NULL, NULL,
     'Aylık Gelir Trendi',
     'Son aylarda gelirin aylık değişimi',
     'DATA_CHART',
     'TRANSACTION',
     '{"dataSource":"TRANSACTION","filters":[{"field":"transactionType","operator":"EQ","value":"INCOME"}],"groupBy":[{"field":"transactionDate","transform":"month"}],"aggregate":{"function":"SUM","field":"amount","alias":"Gelir"}}'::jsonb,
     '{"chartType":"LINE","title":"Aylık Gelir Trendi","color":"pos"}'::jsonb,
     '{}'::jsonb,
     TRUE, TRUE),

    -- 4. Vadesi Geçmiş Faturalar Toplamı
    (NULL, NULL,
     'Vadesi Geçmiş Faturalar',
     'Ödenmemiş ve vadesi geçmiş faturaların toplam tutarı',
     'DATA_KPI',
     'INVOICE',
     '{"dataSource":"INVOICE","filters":[{"field":"paymentStatus","operator":"EQ","value":"overdue"}],"groupBy":[],"aggregate":{"function":"SUM","field":"totalAmount","alias":"Borç"}}'::jsonb,
     '{"chartType":"KPI","title":"Vadesi Geçmiş Faturalar","color":"neg"}'::jsonb,
     '{}'::jsonb,
     TRUE, TRUE),

    -- 5. Bu Ay Fatura Sayısı
    (NULL, NULL,
     'Bu Ay Fatura Sayısı',
     'Bu ay kesilen toplam fatura adedi',
     'DATA_KPI',
     'INVOICE',
     '{"dataSource":"INVOICE","filters":[],"groupBy":[],"aggregate":{"function":"COUNT","field":"invoiceId","alias":"Adet"}}'::jsonb,
     '{"chartType":"KPI","title":"Bu Ay Fatura Sayısı","color":"info"}'::jsonb,
     '{}'::jsonb,
     TRUE, TRUE),

    -- 6. Ödeme Durumu Dağılımı
    (NULL, NULL,
     'Ödeme Durumu Dağılımı',
     'Faturaların ödeme durumlarına göre tutarsal dağılımı',
     'DATA_CHART',
     'INVOICE',
     '{"dataSource":"INVOICE","filters":[],"groupBy":[{"field":"paymentStatus"}],"aggregate":{"function":"SUM","field":"totalAmount","alias":"Tutar"}}'::jsonb,
     '{"chartType":"PIE","title":"Ödeme Durumu Dağılımı","color":"accent"}'::jsonb,
     '{}'::jsonb,
     TRUE, TRUE),

    -- 7. Gider Kategori Dağılımı
    (NULL, NULL,
     'Gider Kategori Dağılımı',
     'Hangi kategoride ne kadar harcadığınız',
     'DATA_CHART',
     'TRANSACTION',
     '{"dataSource":"TRANSACTION","filters":[{"field":"transactionType","operator":"EQ","value":"EXPENSE"}],"groupBy":[{"field":"category"}],"aggregate":{"function":"SUM","field":"amount","alias":"Tutar"}}'::jsonb,
     '{"chartType":"PIE","title":"Gider Kategori Dağılımı","color":"warn"}'::jsonb,
     '{}'::jsonb,
     TRUE, TRUE),

    -- 8. Aylık Fatura Cirosu (BAR)
    (NULL, NULL,
     'Aylık Fatura Cirosu',
     'Aylara göre toplam fatura tutarları',
     'DATA_CHART',
     'INVOICE',
     '{"dataSource":"INVOICE","filters":[],"groupBy":[{"field":"createdAt","transform":"month"}],"aggregate":{"function":"SUM","field":"totalAmount","alias":"Ciro"}}'::jsonb,
     '{"chartType":"BAR","title":"Aylık Fatura Cirosu","color":"accent"}'::jsonb,
     '{}'::jsonb,
     TRUE, TRUE);
