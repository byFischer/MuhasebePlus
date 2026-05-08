export const MOVEMENT_LABELS = {
  PURCHASE: 'Alış',
  SALE: 'Satış',
  RETURN_IN: 'Müşteri İadesi',
  RETURN_OUT: 'Tedarikçi İadesi',
  ADJUSTMENT_IN: 'Manuel Giriş',
  ADJUSTMENT_OUT: 'Manuel Çıkış',
  PRODUCTION_IN: 'Üretim Giriş',
  PRODUCTION_OUT: 'Üretim Çıkış',
  OPENING_BALANCE: 'Açılış',
};

export const MOVEMENT_TYPE_COLORS = {
  PURCHASE: 'pos',
  SALE: 'neg',
  RETURN_IN: 'pos',
  RETURN_OUT: 'neg',
  ADJUSTMENT_IN: 'info',
  ADJUSTMENT_OUT: 'warn',
  PRODUCTION_IN: 'pos',
  PRODUCTION_OUT: 'neg',
  OPENING_BALANCE: 'info',
};

export const MOVEMENT_CATEGORIES = [
  { key: 'ADJUSTMENT', label: 'Manuel Düzeltme' },
  { key: 'RETURN',     label: 'İade' },
  { key: 'PRODUCTION', label: 'Üretim' },
];

export const MOVEMENT_DIRECTION_LABELS = {
  ADJUSTMENT: { IN: 'Giriş',         OUT: 'Çıkış' },
  RETURN:     { IN: 'Müşteriden',    OUT: 'Tedarikçiye' },
  PRODUCTION: { IN: 'Mamul Giriş',   OUT: 'Hammadde Çıkış' },
};

export const REASON_PLACEHOLDERS = {
  ADJUSTMENT_IN:  'buluntu, hediye, sayım fazlası...',
  ADJUSTMENT_OUT: 'fire, kayıp, bozulma, çalıntı...',
  RETURN_IN:      'müşteri iadesi - sebep',
  RETURN_OUT:     'tedarikçiye iade - sebep',
  PRODUCTION_IN:  'üretim mamul girişi',
  PRODUCTION_OUT: 'üretim hammadde tüketimi',
};

export function buildMovementType(category, direction) {
  if (!category || !direction) return null;
  return `${category}_${direction}`;
}
