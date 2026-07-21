// Dashboard 5 sabit layout preset. Her preset 12 sütunlu CSS Grid; slot'lar
// gridColumn / gridRow ile yerleştirilir. size: 's' | 'm' | 'l' — slot içine
// yerleşen widget bu değere göre kompakt/standart/detaylı render edilir.

export const LAYOUT_PRESETS = {
  LAYOUT_1: {
    id: 'LAYOUT_1',
    name: 'Hero + Yan Sütun',
    description: 'Sağda büyük detaylı + solda 4 küçük + altta 2 strip',
    cols: 12,
    rows: 4,
    rowHeight: 'minmax(140px, auto)',
    slots: [
      { id: 1, size: 's', col: '1 / 5',  row: '1 / 2' },
      { id: 2, size: 'm', col: '5 / 13', row: '1 / 3' },
      { id: 3, size: 's', col: '1 / 5',  row: '2 / 3' },
      { id: 4, size: 's', col: '1 / 5',  row: '3 / 4' },
      { id: 5, size: 'l', col: '5 / 13', row: '3 / 5' },
      { id: 6, size: 'm', col: '1 / 5',  row: '4 / 5' },
    ],
  },

  LAYOUT_2: {
    id: 'LAYOUT_2',
    name: '3 Sütun + Stack',
    description: 'Üstte 3 dikey detaylı sütun + altta 3 yatay strip',
    cols: 12,
    rows: 6,
    rowHeight: 'minmax(120px, auto)',
    slots: [
      { id: 1, size: 'l', col: '1 / 5',  row: '1 / 4' },
      { id: 2, size: 'l', col: '5 / 9',  row: '1 / 4' },
      { id: 3, size: 'l', col: '9 / 13', row: '1 / 4' },
      { id: 4, size: 'l', col: '1 / 13', row: '4 / 5' },
      { id: 5, size: 'l', col: '1 / 13', row: '5 / 6' },
      { id: 6, size: 'l', col: '1 / 13', row: '6 / 7' },
    ],
  },

  LAYOUT_3: {
    id: 'LAYOUT_3',
    name: 'Eşit Izgara',
    description: '3 sütun × 5 satır = 15 eşit slot',
    cols: 12,
    rows: 5,
    rowHeight: 'minmax(140px, auto)',
    slots: [
      { id: 1,  size: 'm', col: '1 / 5',  row: '1 / 2' },
      { id: 2,  size: 'm', col: '5 / 9',  row: '1 / 2' },
      { id: 3,  size: 'm', col: '9 / 13', row: '1 / 2' },
      { id: 4,  size: 'm', col: '1 / 5',  row: '2 / 3' },
      { id: 5,  size: 'm', col: '5 / 9',  row: '2 / 3' },
      { id: 6,  size: 'm', col: '9 / 13', row: '2 / 3' },
      { id: 7,  size: 'm', col: '1 / 5',  row: '3 / 4' },
      { id: 8,  size: 'm', col: '5 / 9',  row: '3 / 4' },
      { id: 9,  size: 'm', col: '9 / 13', row: '3 / 4' },
      { id: 10, size: 'm', col: '1 / 5',  row: '4 / 5' },
      { id: 11, size: 'm', col: '5 / 9',  row: '4 / 5' },
      { id: 12, size: 'm', col: '9 / 13', row: '4 / 5' },
      { id: 13, size: 's', col: '1 / 5',  row: '5 / 6' },
      { id: 14, size: 's', col: '5 / 9',  row: '5 / 6' },
      { id: 15, size: 's', col: '9 / 13', row: '5 / 6' },
    ],
  },

  LAYOUT_4: {
    id: 'LAYOUT_4',
    name: 'Karışık Editör',
    description: 'Çeşitli boyutlu strip ve karelerin karışımı',
    cols: 12,
    rows: 5,
    rowHeight: 'minmax(120px, auto)',
    slots: [
      { id: 1, size: 's', col: '1 / 5',  row: '1 / 2' },
      { id: 2, size: 'm', col: '5 / 13', row: '1 / 2' },
      { id: 3, size: 'l', col: '1 / 13', row: '2 / 3' },
      { id: 4, size: 'm', col: '1 / 7',  row: '3 / 4' },
      { id: 5, size: 'm', col: '7 / 13', row: '3 / 4' },
      { id: 6, size: 'l', col: '1 / 13', row: '4 / 5' },
      { id: 7, size: 's', col: '1 / 5',  row: '5 / 6' },
      { id: 8, size: 'm', col: '5 / 13', row: '5 / 6' },
    ],
  },

  LAYOUT_5: {
    id: 'LAYOUT_5',
    name: 'Editorial',
    description: 'Sol büyük dikey + sağ 2x2 + altta strip + altta geniş',
    cols: 12,
    rows: 4,
    rowHeight: 'minmax(140px, auto)',
    slots: [
      { id: 1, size: 'l', col: '1 / 7',   row: '1 / 4' },
      { id: 2, size: 's', col: '7 / 10',  row: '1 / 2' },
      { id: 3, size: 's', col: '10 / 13', row: '1 / 2' },
      { id: 4, size: 's', col: '7 / 10',  row: '2 / 3' },
      { id: 5, size: 's', col: '10 / 13', row: '2 / 3' },
      { id: 6, size: 'm', col: '7 / 13',  row: '3 / 4' },
      { id: 7, size: 'l', col: '1 / 13',  row: '4 / 5' },
    ],
  },
};

export const LAYOUT_KEYS = Object.keys(LAYOUT_PRESETS);
export const DEFAULT_LAYOUT = 'LAYOUT_3';

// slot.size → DashboardCard size string (mevcut variant sistemi ile uyumlu)
export function slotSizeToCardSize(size) {
  if (size === 'l') return 'full';
  if (size === 'm') return 'wide';
  return 'narrow';
}

// slot.size → widget render variant (mevcut komponentler 's'|'m'|'l' bekliyor)
export function slotSizeToVariant(size) {
  return size || 'm';
}
