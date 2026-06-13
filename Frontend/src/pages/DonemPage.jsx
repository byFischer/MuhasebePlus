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

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Muhasebe Dönemleri</h1>
          <p className="page-sub">Dönemleri kapatarak geçmişe kayıt girilmesini engelleyin</p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <button className="btn ghost" onClick={() => setYear(y => y - 1)}>
            <Icon name="chevLeft" size={14} />
          </button>
          <span style={{ fontWeight: 600, fontSize: '1.1rem', minWidth: 60, textAlign: 'center' }}>{year}</span>
          <button className="btn ghost" onClick={() => setYear(y => y + 1)} disabled={year >= currentYear + 1}>
            <Icon name="chevRight" size={14} />
          </button>
          {isAdmin && allClosed && !yearAlreadyClosed && (
            <button className="btn danger" style={{ marginLeft: 8 }} onClick={() => setYearEndModal(true)}>
              <Icon name="lock" size={13} style={{ marginRight: 4 }} />
              Yıl Sonu Kapat
            </button>
          )}
          {isAdmin && yearAlreadyClosed && (
            <span className="badge neg" style={{ marginLeft: 8, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              <Icon name="lock" size={11} /> {year} Yılı Kapalı
            </span>
          )}
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
                <div className="period-month">{MONTH_NAMES[p.month - 1]}</div>
                <div className="period-year">{p.year}</div>
                <div style={{ marginTop: 4 }}>
                  <span className={`badge${isClosed ? ' neg' : ' pos'}`} style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    <Icon name={isClosed ? 'lock' : 'unlock'} size={11} />
                    {isClosed ? 'Kapalı' : 'Açık'}
                  </span>
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
                      <button className="btn sm danger" style={{ width: '100%' }} onClick={() => handleClose(p.month)}
                        disabled={closePeriod.isPending}>
                        Kapat
                      </button>
                    )}
                    {isClosed && (
                      <button className="btn sm ghost" style={{ width: '100%' }} onClick={() => { setReopenModal({ year: p.year, month: p.month }); setReopenReason(''); }}>
                        Yeniden Aç
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
          <h3>Kapalı Dönem Politikası</h3>
        </div>
        <div className="card-b">
          <ul style={{ margin: 0, padding: '0 0 0 16px', lineHeight: 2, fontSize: 12.5, color: 'var(--ink-2)' }}>
            <li>Kapalı bir döneme geriye dönük fatura, ödeme veya işlem <strong>kaydedilemez</strong>.</li>
            <li>Kapalı dönemdeki kayıtlar <strong>silinemez veya güncellenemez</strong>.</li>
            <li>Dönem yeniden açılırsa bu eylem <strong>sistem loguna</strong> yazılır.</li>
            <li>KDV beyanı verildikten sonra ilgili dönemi kapatmanız önerilir.</li>
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
