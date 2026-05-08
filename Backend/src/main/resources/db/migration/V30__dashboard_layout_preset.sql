-- Dashboard'da artık 5 sabit layout preset kullanılıyor.
-- Mevcut serbest sıralama (positionX/Y/width/height) yerine sabit slot_index sistemi.

ALTER TABLE dashboard_layout
    ADD COLUMN IF NOT EXISTS layout_preset VARCHAR(20) NOT NULL DEFAULT 'LAYOUT_3';

ALTER TABLE dashboard_widget
    ADD COLUMN IF NOT EXISTS slot_index INTEGER;

-- Eski serbest yapıyla uyumsuz oldukları için mevcut tüm widget kayıtları silinir.
-- Kullanıcılar yeni slot tabanlı sistemde widget'larını yeniden ekler.
DELETE FROM dashboard_widget;

-- Aynı layout içinde aynı slot'a iki widget olamaz.
CREATE UNIQUE INDEX IF NOT EXISTS uq_dashboard_widget_slot
    ON dashboard_widget(layout_id, slot_index)
    WHERE slot_index IS NOT NULL;
