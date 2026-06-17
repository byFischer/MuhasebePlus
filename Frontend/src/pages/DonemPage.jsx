import { useState } from 'react';
import Icon from '@/components/mp/Icon';
import { useAccountingPeriods, useClosePeriod, useReopenPeriod, useCloseYear } from '@/hooks/useAccountingPeriods';
import { useAuth } from '@/context/AuthContext';

const MONTH_NAMES = ['Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
  'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'];

export default function DonemPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const [reopenModal, setReopenModal] = useState(null);
  const [reopenReason, setReopenReason] = useState('');
  const [yearEndModal, setYearEndModal] = useState(false);

  const { data: periods = [], isLoading } = useAccountingPeriods(year);
  const closePeriod  = useClosePeriod();
  const reopenPeriod = useReopenPeriod();
  const closeYear    = useCloseYear();

  const handleClose = (month) => {
    if (!window.confirm(`${MONTH_NAMES[month - 1]} ${year} dönemini kapatmak istediğinizden emin misiniz?`)) return;
    closePeriod.mutate({ year, month });
  };

  const handleReopen = () => {
    if (!reopenReason.trim()) return;
    reopenPeriod.mutate({ year: reopenModal.year, month: reopenModal.month, reason: reopenReason }, {
      onSuccess: () => { setReopenModal(null); setReopenReason(''); },
    });
  };

  const allClosed = periods.length === 12 && periods.every(p => p.status === 'CLOSED');
  const decPeriod  = periods.find(p => p.month === 12);
  const yearAlreadyClosed = decPeriod?.yearEndClosedAt != null;

  const handleCloseYear = () => {
    closeYear.mutate(year, { onSuccess: () => setYearEndModal(false) });
  };

  const grid = Array.from({ length: 12 }, (_, i) => {
    const month = i + 1;
    const found = periods.find(p => p.month === month);
    return found || { year, month, status: 'OPEN' };
  });

  const closedCount = grid.filter(p => p.status === 'CLOSED').length;
  const pct = Math.round((closedCount / 12) * 100);

  return (
    <div className="page">
      <div className="page-head">
        <div className="mod-head">
          <div className="mod-head-ic"><Icon name="calendar" size={22} /></div>
          <div>
            <h1 className="page-title">Muhasebe Dönemleri</h1>
            <p className="page-sub">Dönemleri kapatarak geçmişe kayıt girilmesini engelleyin</p>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          {isAdmin && allClosed && !yearAlreadyClosed && (
            <button className="btn danger" onClick={() => setYearEndModal(true)}>
              <Icon name="lock" size={13} />
              Yıl Sonu Kapat
            </button>
          )}
          {isAdmin && yearAlreadyClosed && (
            <span className="badge neg" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              <Icon name="lock" size={11} /> {year} Yılı Kapalı
            </span>
          )}
        </div>
      </div>

      <div className="ovw">
        <div className="ovw-left">
          <div className="yr-step">
            <button className="yr-btn" onClick={() => setYear(y => y - 1)} aria-label="Önceki yıl">
              <Icon name="chevLeft" size={15} />
            </button>
            <span className="yr-val">{year}</span>
            <button className="yr-btn" onClick={() => setYear(y => y + 1)} disabled={year >= currentYear + 1} aria-label="Sonraki yıl">
              <Icon name="chevRight" size={15} />
            </button>
          </div>
          <div className="ovw-stat">
            <b>{closedCount}<span style={{ color: 'var(--ink-3)', fontWeight: 400 }}> / 12</span></b>
            <span>dönem kapalı</span>
          </div>
        </div>
        <div className="ovw-bar">
          <div className="ovw-bar-label">
            <span>{isLoading ? 'Yükleniyor…' : 'Kapanış durumu'}</span>
            <span>%{pct}</span>
          </div>
          <div className="ovw-track"><div className="ovw-fill" style={{ width: `${pct}%` }} /></div>
        </div>
      </div>

      {isLoading ? (
        <div className="empty">Yükleniyor…</div>
      ) : (
        <div className="period-grid">
          {grid.map(p => {
            const isClosed = p.status === 'CLOSED';
            const today = new Date();
            const isPast = p.year < today.getFullYear() || (p.year === today.getFullYear() && p.month < today.getMonth() + 1);

            return (
              <div key={p.month} className={`period-card${isClosed ? ' closed' : ' open'}`}>
                <div className="period-card-top">
                  <div className={`period-ic ${isClosed ? 'closed' : 'open'}`}>
                    <Icon name={isClosed ? 'lock' : 'unlock'} size={16} />
                  </div>
                  <span className={`chip ${isClosed ? 'lock' : 'ok'}`}>
                    {!isClosed && <span className="chip-dot" />}
                    {isClosed ? 'Kapalı' : 'Açık'}
                  </span>
                </div>
                <div style={{ marginTop: 10 }}>
                  <div className="period-month">{MONTH_NAMES[p.month - 1]}</div>
                  <div className="period-year">{p.year}</div>
                </div>
                {isClosed && p.closedAt && (
                  <div className="period-meta">{new Date(p.closedAt).toLocaleDateString('tr-TR')} tarihinde kapatıldı</div>
                )}
                {isClosed && p.closedBy && (
                  <div className="period-meta">Kapatan: {p.closedBy}</div>
                )}
                {isAdmin && (
                  <div className="period-actions">
                    {!isClosed && isPast && (
                      <button className="btn sm danger" style={{ width: '100%', justifyContent: 'center' }} onClick={() => handleClose(p.month)}
                        disabled={closePeriod.isPending}>
                        <Icon name="lock" size={12} /> Kapat
                      </button>
                    )}
                    {isClosed && (
                      <button className="btn sm ghost" style={{ width: '100%', justifyContent: 'center' }} onClick={() => { setReopenModal({ year: p.year, month: p.month }); setReopenReason(''); }}>
                        <Icon name="unlock" size={12} /> Yeniden Aç
                      </button>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      <div className="card" style={{ marginTop: 24 }}>
        <div className="card-h">
          <h3><Icon name="info" size={14} /> Kapalı Dönem Politikası</h3>
        </div>
        <div className="card-b">
          <ul className="policy-list">
            <li><span className="pic"><Icon name="lock" size={13} /></span><span>Kapalı bir döneme geriye dönük fatura, ödeme veya işlem <strong>kaydedilemez</strong>.</span></li>
            <li><span className="pic"><Icon name="shield" size={13} /></span><span>Kapalı dönemdeki kayıtlar <strong>silinemez veya güncellenemez</strong>.</span></li>
            <li><span className="pic"><Icon name="log" size={13} /></span><span>Dönem yeniden açılırsa bu eylem <strong>sistem loguna</strong> yazılır.</span></li>
            <li><span className="pic"><Icon name="check" size={13} /></span><span>KDV beyanı verildikten sonra ilgili dönemi kapatmanız önerilir.</span></li>
          </ul>
        </div>
      </div>

      {yearEndModal && (
        <div className="scrim" onClick={() => setYearEndModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="drawer-h">
              <h3>Yıl Sonu Kapanışı</h3>
              <button className="tb-icon-btn" onClick={() => setYearEndModal(false)}><Icon name="x" size={14} /></button>
            </div>
            <div className="drawer-b">
              <p style={{ fontSize: 13, marginTop: 0, marginBottom: 12 }}>
                <strong>{year} yılı</strong> sonu kapanışı gerçekleştirilecek. Bu işlem:
              </p>
              <ul style={{ margin: '0 0 16px', padding: '0 0 0 16px', lineHeight: 2, fontSize: 12.5, color: 'var(--ink-2)' }}>
                <li>Gelir (6xx) ve gider (7xx) hesaplarını kapatır</li>
                <li>Net kâr/zararı <strong>590 / 591</strong> hesabına aktarır</li>
                <li>Kapanış yevmiye fişi otomatik oluşturulur</li>
                <li>Bu işlem <strong>geri alınamaz</strong></li>
              </ul>
              <div style={{ padding: '10px 12px', background: 'var(--neg-bg, #fff5f5)', border: '1px solid var(--neg)', borderRadius: 6, fontSize: 12, color: 'var(--neg)' }}>
                Tüm 12 ay kapalı durumda. Devam edebilirsiniz.
              </div>
            </div>
            <div className="drawer-f">
              <button className="btn ghost" onClick={() => setYearEndModal(false)}>İptal</button>
              <button className="btn danger" disabled={closeYear.isPending} onClick={handleCloseYear}>
                {closeYear.isPending ? 'İşleniyor…' : `${year} Yılını Kapat`}
              </button>
            </div>
          </div>
        </div>
      )}

      {reopenModal && (
        <div className="scrim" onClick={() => setReopenModal(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="drawer-h">
              <h3>Dönemi Yeniden Aç</h3>
              <button className="tb-icon-btn" onClick={() => setReopenModal(null)}><Icon name="x" size={14} /></button>
            </div>
            <div className="drawer-b">
              <p style={{ fontSize: 12.5, color: 'var(--ink-3)', marginTop: 0, marginBottom: 16 }}>
                <strong>{MONTH_NAMES[reopenModal.month - 1]} {reopenModal.year}</strong> dönemi yeniden açılacak.
                Bu işlem sistem loguna kaydedilecektir.
              </p>
              <div className="field">
                <label>Sebep <span style={{ color: 'var(--neg)' }}>*</span></label>
                <textarea className="input" value={reopenReason} onChange={e => setReopenReason(e.target.value)}
                  rows={3} placeholder="Beyan düzeltme, ek kayıt vb." />
              </div>
            </div>
            <div className="drawer-f">
              <button className="btn ghost" onClick={() => setReopenModal(null)}>İptal</button>
              <button className="btn danger" disabled={!reopenReason.trim() || reopenPeriod.isPending} onClick={handleReopen}>
                {reopenPeriod.isPending ? 'İşleniyor…' : 'Yeniden Aç'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
