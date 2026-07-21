import Icon from '@/components/mp/Icon';

// eslint-disable-next-line react-refresh/only-export-components
export const getHeaderExtra = (D) => (
  <span className="pill neg">{D.products.length}</span>
);

export default function LowStockWidget({ D, onNav, mode, variant }) {
  const v = variant || (mode === 'detail' ? 'l' : 'm');

  if (D.products.length === 0) {
    return <div className="empty">Düşük stok yok 🎉</div>;
  }

  if (v === 's') {
    const critical = D.products.filter(p => p.stock === 0).length;
    return (
      <div className="col gap-8" style={{ textAlign: 'center' }}>
        <Icon name="box" size={20} style={{ opacity: 0.4, margin: '0 auto' }} />
        <div className="mono tnum" style={{ fontSize: 22, fontWeight: 600, color: critical > 0 ? 'var(--neg)' : 'var(--warn)' }}>
          {D.products.length}
        </div>
        <div style={{ fontSize: 12, color: 'var(--ink-2)' }}>düşük stok ürünü</div>
        {critical > 0 && (
          <div style={{ fontSize: 11, color: 'var(--neg)' }}>{critical} adet stokta yok</div>
        )}
      </div>
    );
  }

  if (v === 'l') {
    return (
      <table className="table">
        <thead>
          <tr><th>Ürün</th><th>Min</th><th className="num">Stok</th><th>Birim</th><th></th></tr>
        </thead>
        <tbody>
          {D.products.map(p => (
            <tr key={p.id}>
              <td><b>{p.name}</b><div className="muted" style={{ fontSize: 11 }}>{p.id}</div></td>
              <td className="muted">{p.min}</td>
              <td className="num mono tnum" style={{ color: p.stock === 0 ? 'var(--neg)' : 'var(--warn)', fontWeight: 600 }}>
                {p.stock}
              </td>
              <td className="muted">{p.unit}</td>
              <td><button className="btn sm" onClick={() => onNav('/stok')}>Sipariş</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    );
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
