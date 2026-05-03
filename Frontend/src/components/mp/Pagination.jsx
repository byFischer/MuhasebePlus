import React from 'react';
import Icon from '@/components/mp/Icon';

export default function Pagination({ page, totalPages, setPage, pageStart, pageEnd, total }) {
  if (total === 0) return null;
  const canPrev = page > 1;
  const canNext = page < totalPages;
  return (
    <div className="pagination">
      <div className="pagination-info">
        <b className="mono tnum">{pageStart + 1}–{pageEnd}</b> / <b className="mono tnum">{total}</b> kayıt
      </div>
      <div className="pagination-controls">
        <button className="pg-btn" disabled={!canPrev} onClick={() => setPage(1)} title="İlk sayfa"><Icon name="chevDoubleLeft" size={14} /></button>
        <button className="pg-btn" disabled={!canPrev} onClick={() => setPage(p => Math.max(1, p - 1))} title="Önceki"><Icon name="chevLeft" size={14} /></button>
        <div className="pg-pages">
          {(() => {
            const pages = [];
            const start = Math.max(1, Math.min(page - 2, totalPages - 4));
            const end = Math.min(totalPages, start + 4);
            for (let i = start; i <= end; i++) pages.push(i);
            return pages.map(p => (
              <button key={p} className={`pg-num ${p === page ? 'active' : ''}`} onClick={() => setPage(p)}>{p}</button>
            ));
          })()}
        </div>
        <button className="pg-btn" disabled={!canNext} onClick={() => setPage(p => Math.min(totalPages, p + 1))} title="Sonraki"><Icon name="chevRight" size={14} /></button>
        <button className="pg-btn" disabled={!canNext} onClick={() => setPage(totalPages)} title="Son sayfa"><Icon name="chevDoubleRight" size={14} /></button>
      </div>
    </div>
  );
}
