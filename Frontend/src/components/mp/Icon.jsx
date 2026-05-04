// MuhasebePlus — SVG Icon component
import React from 'react';

const PATHS = {
  dashboard: 'M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z',
  users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75',
  invoice: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M9 13h6 M9 17h6',
  box: 'M21 8l-9-5-9 5 9 5 9-5z M3 8v8l9 5 9-5V8 M12 13v8',
  swap: 'M7 10l-4 4 4 4 M3 14h18 M17 4l4 4-4 4 M21 8H3',
  bank: 'M3 21h18 M5 21V10 M19 21V10 M9 21V10 M15 21V10 M2 10l10-7 10 7',
  chart: 'M3 3v18h18 M7 14l4-4 4 4 5-7',
  template: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h18v7H3z',
  log: 'M14 3v6h6 M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z M9 14h6 M9 18h4',
  plus: 'M12 5v14 M5 12h14',
  search: 'M11 4a7 7 0 1 1 0 14 7 7 0 0 1 0-14z M21 21l-4.3-4.3',
  bell: 'M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9 M13.7 21a2 2 0 0 1-3.4 0',
  settings: 'M12 1v6 M12 17v6 M4.2 4.2l4.3 4.3 M15.5 15.5l4.3 4.3 M1 12h6 M17 12h6 M4.2 19.8l4.3-4.3 M15.5 8.5l4.3-4.3 M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8z',
  calendar: 'M3 5h18v16H3z M3 9h18 M8 3v4 M16 3v4',
  download: 'M12 3v12 M7 10l5 5 5-5 M3 21h18',
  upload: 'M12 21V9 M7 14l5-5 5 5 M3 3h18',
  chevDown: 'M6 9l6 6 6-6',
  chevRight: 'M9 6l6 6-6 6',
  chevLeft: 'M15 6l-6 6 6 6',
  chevDoubleLeft: 'M11 17l-5-5 5-5 M18 17l-5-5 5-5',
  chevDoubleRight: 'M13 17l5-5-5-5 M6 17l5-5-5-5',
  grip: 'M9 6h.01 M15 6h.01 M9 12h.01 M15 12h.01 M9 18h.01 M15 18h.01',
  x: 'M6 6l12 12 M18 6L6 18',
  check: 'M5 12l5 5L20 7',
  undo: 'M3 7v6h6 M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6.7 3L3 13',
  edit: 'M11 4H4v16h16v-7 M18.5 2.5a2.1 2.1 0 1 1 3 3L12 15l-4 1 1-4z',
  trash: 'M3 6h18 M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2 M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6 M10 11v6 M14 11v6',
  copy: 'M9 9h13v13H9z M5 15H3a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v2',
  eye: 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8z',
  print: 'M6 9V2h12v7 M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2 M6 14h12v8H6z',
  filter: 'M22 3H2l8 9.5V19l4 2v-8.5z',
  sort: 'M3 6h18 M6 12h12 M10 18h4',
  arrowUp: 'M12 19V5 M5 12l7-7 7 7',
  arrowDown: 'M12 5v14 M19 12l-7 7-7-7',
  barcode: 'M3 5v14 M6 5v14 M9 5v14 M12 5v14 M15 5v14 M18 5v14 M21 5v14',
  moon: 'M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z',
  play: 'M5 3l14 9-14 9z',
  refresh: 'M23 4v6h-6 M1 20v-6h6 M3.5 9a9 9 0 0 1 14.85-3.36L23 10 M1 14l4.65 4.36A9 9 0 0 0 20.5 15',
  lock: 'M5 11V6a4 4 0 0 1 8 0v5 M5 11h14v10H5z M12 16v2',
  unlock: 'M5 11V6a4 4 0 0 1 7.5-1.5 M5 11h14v10H5z M12 16v2',
  folder: 'M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z',
  flash: 'M13 2L3 14h9l-1 8 10-12h-9z',
  drag: 'M5 9h14 M5 15h14',
  info: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M12 16v-4 M12 8h.01',
  alert: 'M12 9v4 M12 17h.01 M10.3 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z',
  clock: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M12 6v6l4 2',
  file: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6',
  minus: 'M5 12h14',
  globe: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M2 12h20 M12 2a15 15 0 0 1 0 20 M12 2a15 15 0 0 0 0 20',
  help: 'M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M9.1 9a3 3 0 1 1 5.8 1c0 2-3 2-3 4 M12 17h.01',
  message: 'M21 11.5a8.4 8.4 0 0 1-1 4 8.5 8.5 0 0 1-7.6 4.5 8.4 8.4 0 0 1-4-1L3 21l2-5a8.4 8.4 0 0 1-1-4 8.5 8.5 0 0 1 4.5-7.6 8.4 8.4 0 0 1 4-1 8.5 8.5 0 0 1 8 8z',
  sparkle: 'M12 3l2 5 5 2-5 2-2 5-2-5-5-2 5-2z M19 14l1 2 2 1-2 1-1 2-1-2-2-1 2-1z',
  crown: 'M2 18l3-12 5 6 4-9 4 9 5-6 3 12z M2 18h20',
  plug: 'M9 2v6 M15 2v6 M5 8h14v4a7 7 0 0 1-14 0z M12 19v3',
  building: 'M3 21h18 M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16 M9 7h2 M13 7h2 M9 11h2 M13 11h2 M9 15h6',
  logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9',
};

export default function Icon({ name, size = 16, ...rest }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" {...rest}>
      <path d={PATHS[name] || ''} />
    </svg>
  );
}
