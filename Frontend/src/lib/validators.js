// MuhasebePlus — client-side validators
export function validateIBAN(iban) {
  const s = (iban || '').replace(/\s+/g, '').toUpperCase();
  if (!/^TR\d{24}$/.test(s)) return { ok: false, msg: 'TR + 24 hane gerekli' };
  const rearr = s.slice(4) + s.slice(0, 4);
  const num = rearr.replace(/[A-Z]/g, c => (c.charCodeAt(0) - 55).toString());
  let r = 0;
  for (const ch of num) r = (r * 10 + Number(ch)) % 97;
  return r === 1 ? { ok: true } : { ok: false, msg: 'IBAN doğrulaması başarısız' };
}
export function validateTaxNumberByType(type, taxNumber) {
  const normalizedType = (type || '').toUpperCase();

  if (!taxNumber || !taxNumber.trim()) {
    return { ok: false, msg: 'VKN/TCKN zorunlu' };
  }

  if (normalizedType === 'INDIVIDUAL') {
    return validateTCKN(taxNumber);
  }

  if (normalizedType === 'CORPORATE') {
    return validateVKN(taxNumber);
  }

  return { ok: false, msg: 'Müşteri tipi seçilmeli' };
}

export function validateTCKN(t) {
  const s = (t || '').replace(/\s/g, '');
  if (!/^\d{11}$/.test(s)) return { ok: false, msg: '11 hane gerekli' };
  if (s[0] === '0') return { ok: false, msg: 'İlk hane 0 olamaz' };
  const d = s.split('').map(Number);
  const odd = d[0] + d[2] + d[4] + d[6] + d[8];
  const even = d[1] + d[3] + d[5] + d[7];
  // JS'te % negatif sonuç verebilir; (7*odd < even) olan geçerli TCKN'ler
  // yanlışlıkla reddedilmesin diye pozitif modülo kullanılır.
  const c10 = (((odd * 7 - even) % 10) + 10) % 10;
  const c11 = (d.slice(0, 10).reduce((a, b) => a + b, 0)) % 10;
  if (c10 !== d[9] || c11 !== d[10]) return { ok: false, msg: 'TCKN algoritma hatası' };
  return { ok: true };
}

export function validateVKN(v) {
  const s = (v || '').replace(/\s/g, '');
  if (!/^\d{10}$/.test(s)) return { ok: false, msg: '10 hane gerekli' };
  return { ok: true };
}

export function validateEmail(e) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e || '') ? { ok: true } : { ok: false, msg: 'Geçersiz e-posta formatı' };
}
