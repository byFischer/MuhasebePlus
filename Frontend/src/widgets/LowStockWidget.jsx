import React from 'react';

export const getHeaderExtra = (D) => (
  <span className="pill neg">{D.products.length}</span>
);

export default function LowStockWidget({ D, onNav }) {
  if (D.products.length === 0) {
    return <div className="empty">Düşük stok yok 🎉</div>;
  }

  return (
    <div>
      {D.products.map(p => (
        <div
          key={p.id}
          className="row"
          style={{ justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--line)' }}
        >
          <div>
            <div style={{ fontSize: 13 }}>{p.name}</div>
            <div className="muted" style={{ fontSize: 11 }}>{p.id} · min {p.min} {p.unit}</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div
              className="mono tnum"
              style={{ color: p.stock === 0 ? 'var(--neg)' : 'var(--warn)', fontWeight: 600 }}
            >
              {p.stock} {p.unit}
            </div>
            <button className="btn sm" style={{ marginTop: 4 }} onClick={() => onNav('/stok')}>
              Sipariş
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
