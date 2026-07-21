-- withholding_tax_code: GIB tevkifat kodları master tablosu
CREATE TABLE withholding_tax_code (
    code_id          SERIAL PRIMARY KEY,
    code             VARCHAR(10)  NOT NULL UNIQUE,
    description      VARCHAR(255) NOT NULL,
    withholding_rate NUMERIC(5,4) NOT NULL,
    denominator      INTEGER      NOT NULL,
    numerator        INTEGER      NOT NULL,
    service_category VARCHAR(100),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE
);

-- vat_exemption_code: GIB KDV istisna kodları master tablosu
CREATE TABLE vat_exemption_code (
    code_id                 SERIAL PRIMARY KEY,
    code                    VARCHAR(10)  NOT NULL UNIQUE,
    description             VARCHAR(255) NOT NULL,
    category                VARCHAR(30)  NOT NULL,
    kdv_beyannamesi_satiri  VARCHAR(10),
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Tevkifat kodları seed (en yaygın GIB kodları)
INSERT INTO withholding_tax_code (code, description, withholding_rate, denominator, numerator, service_category) VALUES
('601', 'Yapım işleri 2/10',                          0.2000, 10, 2, 'Yapım'),
('602', 'Etüt, plan-proje, danışmanlık 9/10',         0.9000, 10, 9, 'Hizmet'),
('603', 'Makine, teçhizat, demirbaş bakım 5/10',      0.5000, 10, 5, 'Bakım'),
('604', 'Yemek servis 5/10',                          0.5000, 10, 5, 'Yemek'),
('605', 'Organizasyon hizmetleri 5/10',               0.5000, 10, 5, 'Organizasyon'),
('606', 'İşgücü temin hizmetleri 9/10',               0.9000, 10, 9, 'İşgücü'),
('607', 'Özel güvenlik 7/10',                         0.7000, 10, 7, 'Güvenlik'),
('608', 'Yapı denetim 9/10',                          0.9000, 10, 9, 'Denetim'),
('609', 'Fason işçilik 7/10',                         0.7000, 10, 7, 'Fason'),
('610', 'Temizlik hizmetleri 7/10',                   0.7000, 10, 7, 'Temizlik'),
('611', 'Çevre ve bahçe bakımı 7/10',                 0.7000, 10, 7, 'Bahçe'),
('612', 'Taşımacılık 2/10',                           0.2000, 10, 2, 'Taşıma'),
('613', 'Her türlü baskı işlemi 7/10',                0.7000, 10, 7, 'Baskı'),
('614', 'Servis taşımacılığı 5/10',                   0.5000, 10, 5, 'Taşıma'),
('615', 'Turistik mağazalara rehber götürme 9/10',    0.9000, 10, 9, 'Turizm'),
('616', 'Spor kulüplerine reklam 9/10',               0.9000, 10, 9, 'Reklam'),
('617', 'Külçe metal/bakır/alüminyum 7/10',           0.7000, 10, 7, 'Metal'),
('618', 'Pamuk, tiftik, yün ve yapağı 9/10',          0.9000, 10, 9, 'Tarım'),
('619', 'Ağaç ve orman ürünleri teslimi 5/10',        0.5000, 10, 5, 'Tarım'),
('620', 'Metal, plastik atık ve hurdası 9/10',        0.9000, 10, 9, 'Hurda'),
('621', 'Demir-çelik ürünleri 4/10',                  0.4000, 10, 4, 'Metal'),
('622', 'Ticari reklam hizmetleri 3/10',              0.3000, 10, 3, 'Reklam'),
('623', 'Fuar, panayır, kongre organizasyonu 5/10',   0.5000, 10, 5, 'Organizasyon');

-- KDV istisna kodları seed
INSERT INTO vat_exemption_code (code, description, category, kdv_beyannamesi_satiri) VALUES
('301', 'İhracat istisnası',                                         'IHRACAT',        '301'),
('302', 'Araçlar, petrol aramaları ve teşvik belgeli yatırımlar',     'TAM_ISTISNA',    '302'),
('303', 'Diplomatik organ teslimleri',                               'DIPLOMATIK',     '303'),
('304', 'Uluslararası kuruluşlar',                                   'DIPLOMATIK',     '304'),
('310', 'Taşımacılık istisnası',                                     'TAM_ISTISNA',    '310'),
('320', 'Altın teslimleri',                                          'TAM_ISTISNA',    '320'),
('321', 'Kıymetli taş teslimleri',                                   'TAM_ISTISNA',    '321'),
('329', 'Diğer tam istisnalar',                                      'TAM_ISTISNA',    '329'),
('351', 'Yatırım teşvik belgeli inşaatlar',                          'KISMI_ISTISNA',  '351'),
('391', 'Kısmi istisna kapsamındaki diğer işlemler',                 'KISMI_ISTISNA',  '391'),
('150', 'KDV''den müstesna tutulan işlemler',                       'KDV_DISI',       NULL),
('220', 'Sosyal güvenlik işlemleri',                                 'KDV_DISI',       NULL),
('340', 'Posta hizmetleri',                                          'KDV_DISI',       NULL);
