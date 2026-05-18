-- Tek Düzen Hesap Planı (TDHP) seed for company_id = 0 (template)
-- CompanyService bu kayıtları yeni şirket oluşturulduğunda kopyalar.
-- Bu tabloda company_id=0 şablon satırlarını temsil eder.

-- TDHP şablonu için gerekli olan company_id=0 kayıtı.
-- Gerçek bir şirket değildir; sadece chart_of_account FK kısıtını karşılar.
INSERT INTO company (company_id, company_name, is_active, created_at, updated_at)
VALUES (0, '__TDHP_TEMPLATE__', false, NOW(), NOW())
ON CONFLICT (company_id) DO NOTHING;

-- Benzersizlik kısıtı (company_id=0, account_code)
INSERT INTO chart_of_account (company_id, account_code, account_name, account_type, is_leaf, is_system) VALUES
-- 1xx DÖNEN VARLIKLAR
(0,'100','Kasa',                                          'ASSET', true,  true),
(0,'101','Alınan Çekler',                                 'ASSET', true,  true),
(0,'102','Bankalar',                                      'ASSET', true,  true),
(0,'108','Diğer Hazır Değerler',                          'ASSET', true,  true),
(0,'120','Alıcılar',                                      'ASSET', false, true),
(0,'121','Alacak Senetleri',                              'ASSET', true,  true),
(0,'126','Verilen Depozito ve Teminatlar',                 'ASSET', true,  true),
(0,'128','Şüpheli Ticari Alacaklar',                      'ASSET', true,  true),
(0,'150','İlk Madde ve Malzeme',                          'ASSET', true,  true),
(0,'151','Yarı Mamul Üretim',                             'ASSET', true,  true),
(0,'152','Mamuller',                                      'ASSET', true,  true),
(0,'153','Ticari Mallar',                                 'ASSET', true,  true),
(0,'157','Diğer Stoklar',                                 'ASSET', true,  true),
(0,'159','Verilen Sipariş Avansları',                     'ASSET', true,  true),
(0,'180','Gelecek Aylara Ait Giderler',                   'ASSET', true,  true),
(0,'190','Devreden KDV',                                  'ASSET', true,  true),
(0,'191','İndirilecek KDV',                               'ASSET', true,  true),
(0,'195','İş Avansları',                                  'ASSET', true,  true),
(0,'196','Personel Avansları',                            'ASSET', true,  true),
-- 2xx DURAN VARLIKLAR
(0,'220','Uzun Vadeli Alıcılar',                          'ASSET', true,  true),
(0,'250','Arazi ve Arsalar',                              'ASSET', true,  true),
(0,'251','Yeraltı ve Yerüstü Düzenleri',                  'ASSET', true,  true),
(0,'252','Binalar',                                       'ASSET', true,  true),
(0,'253','Tesis, Makine ve Cihazlar',                     'ASSET', true,  true),
(0,'254','Taşıtlar',                                      'ASSET', true,  true),
(0,'255','Demirbaşlar',                                   'ASSET', true,  true),
(0,'257','Birikmiş Amortismanlar',                        'ASSET', true,  true),
(0,'260','Haklar',                                        'ASSET', true,  true),
(0,'262','Kuruluş ve Örgütlenme Giderleri',               'ASSET', true,  true),
(0,'263','Araştırma ve Geliştirme Giderleri',             'ASSET', true,  true),
(0,'268','Birikmiş İtfa Payları',                         'ASSET', true,  true),
-- 3xx KISA VADELİ YABANCI KAYNAKLAR
(0,'300','Banka Kredileri',                               'LIABILITY', true,  true),
(0,'320','Satıcılar',                                     'LIABILITY', false, true),
(0,'321','Borç Senetleri',                                'LIABILITY', true,  true),
(0,'326','Alınan Depozito ve Teminatlar',                 'LIABILITY', true,  true),
(0,'340','Alınan Sipariş Avansları',                      'LIABILITY', true,  true),
(0,'360','Ödenecek Vergi ve Fonlar',                      'LIABILITY', true,  true),
(0,'361','Ödenecek Sosyal Güvenlik Kesintileri',          'LIABILITY', true,  true),
(0,'370','Dönem Kârı Vergi Karşılıkları',                 'LIABILITY', true,  true),
(0,'380','Gelecek Aylara Ait Gelirler',                   'LIABILITY', true,  true),
(0,'391','Hesaplanan KDV',                                'LIABILITY', true,  true),
(0,'392','Diğer KDV',                                     'LIABILITY', true,  true),
-- 4xx UZUN VADELİ YABANCI KAYNAKLAR
(0,'400','Uzun Vadeli Banka Kredileri',                   'LIABILITY', true,  true),
(0,'420','Uzun Vadeli Satıcılar',                         'LIABILITY', true,  true),
(0,'472','Kıdem Tazminatı Karşılığı',                     'LIABILITY', true,  true),
(0,'480','Gelecek Yıllara Ait Gelirler',                  'LIABILITY', true,  true),
-- 5xx ÖZKAYNAKLAR
(0,'500','Sermaye',                                       'EQUITY', true,  true),
(0,'501','Ödenmemiş Sermaye',                             'EQUITY', true,  true),
(0,'520','Yasal Yedekler',                                'EQUITY', true,  true),
(0,'521','Statü Yedekleri',                               'EQUITY', true,  true),
(0,'522','Olağanüstü Yedekler',                           'EQUITY', true,  true),
(0,'570','Geçmiş Yıllar Kârları',                         'EQUITY', true,  true),
(0,'580','Dönem Net Kârı',                                'EQUITY', true,  true),
(0,'581','Dönem Net Zararı',                              'EQUITY', true,  true),
-- 6xx GELİR TABLOSU
(0,'600','Yurtiçi Satışlar',                              'INCOME',  true,  true),
(0,'601','Yurtdışı Satışlar',                             'INCOME',  true,  true),
(0,'602','Diğer Gelirler',                                'INCOME',  true,  true),
(0,'610','Satıştan İadeler',                              'INCOME',  true,  true),
(0,'611','Satış İskontoları',                             'INCOME',  true,  true),
(0,'620','Satılan Mamuller Maliyeti',                     'COST',    true,  true),
(0,'621','Satılan Ticari Mallar Maliyeti',                'COST',    true,  true),
(0,'622','Satılan Hizmet Maliyeti',                       'COST',    true,  true),
(0,'630','Araştırma ve Geliştirme Giderleri',             'EXPENSE', true,  true),
(0,'631','Pazarlama, Satış ve Dağıtım Giderleri',         'EXPENSE', true,  true),
(0,'632','Genel Yönetim Giderleri',                       'EXPENSE', true,  true),
(0,'640','İştiraklerden Temettü Gelirleri',               'INCOME',  true,  true),
(0,'642','Faiz Gelirleri',                                'INCOME',  true,  true),
(0,'646','Kambiyo Kârları',                               'INCOME',  true,  true),
(0,'649','Diğer Olağan Gelir ve Kârlar',                  'INCOME',  true,  true),
(0,'653','Komisyon Giderleri',                            'EXPENSE', true,  true),
(0,'654','Karşılık Giderleri',                            'EXPENSE', true,  true),
(0,'656','Kambiyo Zararları',                             'EXPENSE', true,  true),
(0,'659','Diğer Olağan Gider ve Zararlar',                'EXPENSE', true,  true),
(0,'671','Önceki Dönem Gelir ve Kârları',                 'INCOME',  true,  true),
(0,'679','Diğer Olağandışı Gelir ve Kârlar',              'INCOME',  true,  true),
(0,'680','Çalışmayan Kısım Gider ve Zararları',           'EXPENSE', true,  true),
(0,'689','Diğer Olağandışı Gider ve Zararlar',            'EXPENSE', true,  true),
(0,'690','Dönem Kârı veya Zararı',                        'INCOME',  true,  true),
(0,'692','Dönem Net Kârı veya Zararı',                    'INCOME',  true,  true),
-- 7xx MALİYET HESAPLARI
(0,'710','Direkt İlk Madde ve Malzeme Giderleri',         'COST',    true,  true),
(0,'720','Direkt İşçilik Giderleri',                      'COST',    true,  true),
(0,'730','Genel Üretim Giderleri',                        'COST',    true,  true),
(0,'740','Hizmet Üretim Maliyeti',                        'COST',    true,  true),
(0,'760','Pazarlama, Satış ve Dağıtım Giderleri',         'COST',    true,  true),
(0,'770','Genel Yönetim Giderleri',                       'COST',    true,  true),
(0,'780','Finansman Giderleri',                           'COST',    true,  true),
-- 9xx NAZIM HESAPLAR
(0,'900','Teminat Mektupları',                            'MEMO',    true,  true),
(0,'901','Teminat Mektupları Karşılığı',                  'MEMO',    true,  true)
ON CONFLICT (company_id, account_code) DO NOTHING;
