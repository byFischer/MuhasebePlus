export const PREFS_STORAGE_KEY = 'muhasebeplus_prefs';

export const HOME_PAGE_OPTIONS = [
  { value: '/dashboard', label: 'Dashboard' },
  { value: '/cari', label: 'Cari Yönetimi' },
  { value: '/fatura', label: 'Fatura Yönetimi' },
  { value: '/banka', label: 'Banka & Kasa' },
];

const DEFAULT_HOME_PAGE = '/dashboard';

export function loadPreferences() {
  try {
    return JSON.parse(localStorage.getItem(PREFS_STORAGE_KEY)) || {};
  } catch {
    return {};
  }
}

export function savePreferences(prefs) {
  localStorage.setItem(PREFS_STORAGE_KEY, JSON.stringify(prefs));
}

export function getHomepagePath() {
  const { homepage } = loadPreferences();
  return HOME_PAGE_OPTIONS.some((option) => option.value === homepage) ? homepage : DEFAULT_HOME_PAGE;
}

