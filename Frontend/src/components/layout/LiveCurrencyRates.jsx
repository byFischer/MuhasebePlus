import React from 'react';
import { useCurrencyRates } from '@/hooks/useCurrencyRates';
import SidebarWidget from '@/widgets/shell/SidebarWidget';

const LABELS = {
  USD: { name: 'USD', suffix: '₺' },
  EUR: { name: 'EUR', suffix: '₺' },
  GA: { name: 'Gram Altın', suffix: '₺' },
};

const fmtPrice = (s) =>
  Number(s).toLocaleString('tr-TR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

const fmtPct = (degisim) => {
  const n = Number(degisim);
  if (!Number.isFinite(n)) return '0,00%';
  const sign = n > 0 ? '+' : '';
  return `${sign}${n.toLocaleString('tr-TR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}%`;
};

const yonClass = (yon) =>
  yon === 'moneyUp' ? 'up' : yon === 'moneyDown' ? 'down' : 'flat';

export default function LiveCurrencyRates() {
  const { data, isLoading, isError, dataUpdatedAt } = useCurrencyRates();

  const footer = dataUpdatedAt > 0 && (
    <>
      Son güncelleme:{' '}
      {new Date(dataUpdatedAt).toLocaleTimeString('tr-TR', {
        hour: '2-digit',
        minute: '2-digit',
      })}
    </>
  );

  return (
    <SidebarWidget
      title="DÖVİZ & ALTIN"
      badge={!isLoading && !isError ? <span className="sb-widget-dot live" /> : null}
      loading={isLoading}
      error={isError}
      errorMsg="Kur verisi alınamadı"
      footer={footer}
    >
      {data &&
        Object.entries(LABELS).map(([key, meta]) => {
          const r = data[key];
          if (!r) return null;
          return (
            <div key={key} className="currency-row">
              <span className="currency-name">{meta.name}</span>
              <span className="currency-price">
                {fmtPrice(r.satis)} <small>{meta.suffix}</small>
              </span>
              <span className={`currency-pct ${yonClass(r.yon)}`}>
                {fmtPct(r.degisim)}
              </span>
            </div>
          );
        })}
    </SidebarWidget>
  );
}
