import React, { useState } from 'react';
import Icon from '@/components/mp/Icon';
import { TRY } from '@/lib/format';
import { useVatPeriods, useCalculateVat, useLockVat, useVatBaBs } from '@/hooks/useVat';

const MONTHS = [
  'Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
  'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık',
];
const CURRENT_YEAR  = new Date().getFullYear();
const CURRENT_MONTH = new Date().getMonth() + 1;
const YEARS = [CURRENT_YEAR - 1, CURRENT_YEAR];

export default function KdvPage() {
  const [year, setYear]   = useState(CURRENT_YEAR);
  const [month, setMonth] = useState(CURRENT_MONTH);
  const [carriedForward, setCarriedForward] = useState('');
  const [selectedId, setSelectedId] = useState(null);
  const [showBaBs, setShowBaBs] = useState(false);

  const { data: periods = [], isLoading } = useVatPeriods();
  const calcMut = useCalculateVat();
  const lockMut = useLockVat();
  const { data: baBsData = [], isFetching: baBsFetching } = useVatBaBs(selectedId, showBaBs);

  const selectedPeriod = periods.find(p => p.id === selectedId);

  const onCalculate = () => {
    calcMut.mutate({
      year: Number(year),
      month: Number(month),
      previousCarriedForward: carriedForward ? parseFloat(String(carriedForward).replace(',', '.')) : null,
    }, {
      onSuccess: (data) => setSelectedId(data.id),
    });
  };

  const onLock = (id) => {
    if (window.confirm('Bu dönemi kilitlemek istediğinize emin misiniz? Kilitli dönemler düzenlenemez.')) {
      lockMut.mutate(id);
    }
  };

  const statusLabel = (s) => s === 'LOCKED' ? 'Kilitli' : s === 'SUBMITTED' ? 'Gönderildi' : 'Taslak';
  const statusClass = (s) => s === 'LOCKED' ? '' : s === 'SUBMITTED' ? 'info' : 'warn';

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">KDV Beyannamesi</h1>
          <p className="page-sub">Aylık KDV hesaplama ve BA/BS form analizi</p>
        </div>
      </div>

      <div className="grid-2" style={{ gap: 16, alignItems: 'start' }}>
        {/* Hesaplama Formu */}
        <div className="card">
          <div className="card-h"><h3>Dönem Hesapla</h3></div>
          <div className="card-b col gap-12">
            <div className="grid-2">
              <div className="field">
                <label>Yıl</label>
                <select className="select" value={year} onChange={e => setYear(e.target.value)}>
                  {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Ay</label>
                <select className="select" value={month} onChange={e => setMonth(e.target.value)}>
                  {MONTHS.map((m, i) => <option key={i + 1} value={i + 1}>{m}</option>)}
                </select>
              </div>
            </div>
            <div className="field">
              <label>Önceki Dönem Devreden KDV (opsiyonel)</label>
              <input
                className="input mono"
                type="text"
                inputMode="decimal"
                placeholder="0.00"
                value={carriedForward}
                onChange={e => setCarriedForward(e.target.value)}
              />
            </div>
            <button
              className="btn primary"
              onClick={onCalculate}
              disabled={calcMut.isPending}
            >
              <Icon name="refresh" />
              {calcMut.isPending ? 'Hesaplanıyor…' : 'Hesapla'}
            </button>
          </div>
        </div>

        {/* Seçili Dönem Özeti */}
        {selectedPeriod && (
          <div className="card">
            <div className="card-h">
              <h3>{MONTHS[selectedPeriod.month - 1]} {selectedPeriod.year} — Özet</h3>
              <span className={`pill ${statusClass(selectedPeriod.status)}`}>
                {statusLabel(selectedPeriod.status)}
              </span>
            </div>
            <div className="card-b col gap-8">
              <KpiRow label="Hesaplanan KDV (Satış)" value={selectedPeriod.totalOutputVat} color="var(--neg)" />
              <KpiRow label="İndirilecek KDV (Alış)" value={selectedPeriod.totalInputVat} color="var(--pos)" />
              <hr style={{ margin: '4px 0', opacity: 0.2 }} />
              <KpiRow
                label={selectedPeriod.netPayableVat > 0 ? 'Ödenecek KDV' : 'Devreden KDV'}
                value={selectedPeriod.netPayableVat > 0 ? selectedPeriod.netPayableVat : selectedPeriod.carriedForwardVat}
                color={selectedPeriod.netPayableVat > 0 ? 'var(--neg)' : 'var(--pos)'}
                bold
              />
            </div>

            {/* KDV Oranı Detayı */}
            {selectedPeriod.lines && selectedPeriod.lines.length > 0 && (
              <div className="table-wrap" style={{ marginTop: 8 }}>
                <table className="table">
                  <thead>
                    <tr>
                      <th>Tür</th>
                      <th>KDV Oranı</th>
                      <th>Matrah</th>
                      <th>KDV Tutarı</th>
                    </tr>
                  </thead>
                  <tbody>
                    {selectedPeriod.lines.map((l, i) => (
                      <tr key={i}>
                        <td>
                          <span className={`pill ${l.lineType === 'OUTPUT' ? '' : 'info'}`}>
                            {l.lineType === 'OUTPUT' ? 'Satış' : 'Alış'}
                          </span>
                        </td>
                        <td className="mono">%{l.vatRate}</td>
                        <td className="mono tnum">{TRY(l.taxBase)}</td>
                        <td className="mono tnum">{TRY(l.vatAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="card-b row gap-8" style={{ paddingTop: 0 }}>
              <button
                className="btn"
                onClick={() => { setShowBaBs(true); }}
                disabled={baBsFetching}
              >
                <Icon name="chart" size={14} />
                BA/BS Listesi
              </button>
              {selectedPeriod.status !== 'LOCKED' && (
                <button
                  className="btn"
                  onClick={() => onLock(selectedPeriod.id)}
                  disabled={lockMut.isPending}
                >
                  <Icon name="lock" size={14} />
                  Dönemi Kilitle
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      {/* BA/BS Tablosu */}
      {showBaBs && selectedId && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-h">
            <h3>BA/BS Formu — {selectedPeriod ? `${MONTHS[selectedPeriod.month - 1]} ${selectedPeriod.year}` : ''}</h3>
            <button className="btn sm" onClick={() => setShowBaBs(false)}>Kapat</button>
          </div>
          {baBsFetching && <div className="empty">Yükleniyor…</div>}
          {!baBsFetching && baBsData.length === 0 && (
            <div className="empty">5.000 TL eşiğini aşan cari bulunamadı</div>
          )}
          {!baBsFetching && baBsData.length > 0 && (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Form</th>
                    <th>Cari Adı</th>
                    <th>Vergi No</th>
                    <th>Toplam Tutar</th>
                  </tr>
                </thead>
                <tbody>
                  {baBsData.map((e, i) => (
                    <tr key={i}>
                      <td><span className={`pill ${e.formType === 'BS' ? '' : 'info'}`}>{e.formType}</span></td>
                      <td>{e.customerName}</td>
                      <td className="mono muted">{e.taxNumber || '—'}</td>
                      <td className="mono tnum">{TRY(e.totalAmount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Dönem Listesi */}
      <div className="card" style={{ marginTop: 16 }}>
        <div className="card-h"><h3>Tüm KDV Dönemleri</h3></div>
        {isLoading && <div className="empty">Yükleniyor…</div>}
        {!isLoading && periods.length === 0 && (
          <div className="empty">Henüz hesaplanmış dönem yok</div>
        )}
        {!isLoading && periods.length > 0 && (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Dönem</th>
                  <th>Hesaplanan KDV</th>
                  <th>İndirilecek KDV</th>
                  <th>Ödenecek / Devreden</th>
                  <th>Durum</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {periods.map(p => (
                  <tr
                    key={p.id}
                    style={{ cursor: 'pointer', background: selectedId === p.id ? 'var(--surface-2)' : '' }}
                    onClick={() => setSelectedId(p.id)}
                  >
                    <td className="mono">{MONTHS[p.month - 1]} {p.year}</td>
                    <td className="mono tnum" style={{ color: 'var(--neg)' }}>{TRY(p.totalOutputVat)}</td>
                    <td className="mono tnum" style={{ color: 'var(--pos)' }}>{TRY(p.totalInputVat)}</td>
                    <td className="mono tnum">
                      {p.netPayableVat > 0
                        ? <span style={{ color: 'var(--neg)' }}>Ödenecek: {TRY(p.netPayableVat)}</span>
                        : <span style={{ color: 'var(--pos)' }}>Devreden: {TRY(p.carriedForwardVat)}</span>
                      }
                    </td>
                    <td><span className={`pill ${statusClass(p.status)}`}>{statusLabel(p.status)}</span></td>
                    <td>
                      <button className="tb-icon-btn" title="Seç ve Detay Gör" onClick={e => { e.stopPropagation(); setSelectedId(p.id); setShowBaBs(false); }}>
                        <Icon name="eye" size={14} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function KpiRow({ label, value, color, bold }) {
  return (
    <div className="row" style={{ justifyContent: 'space-between' }}>
      <span className={bold ? '' : 'muted'} style={{ fontSize: 13 }}>{label}</span>
      <span className="mono tnum" style={{ color, fontWeight: bold ? 700 : 400, fontSize: bold ? 16 : 14 }}>
        {TRY(value)}
      </span>
    </div>
  );
}
