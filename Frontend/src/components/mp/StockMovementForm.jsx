import { useMemo, useState } from 'react';
import { useCreateStockMovement } from '@/hooks/useStock';
import {
  MOVEMENT_CATEGORIES,
  MOVEMENT_DIRECTION_LABELS,
  REASON_PLACEHOLDERS,
  buildMovementType,
} from '@/lib/movementLabels';

export default function StockMovementForm({ productId, currentStock = 0, onSuccess, onCancel }) {
  const createMut = useCreateStockMovement();
  const [category, setCategory] = useState('ADJUSTMENT');
  const [direction, setDirection] = useState('IN');
  const [quantity, setQuantity] = useState('1');
  const [reason, setReason] = useState('');
  const [unitCost, setUnitCost] = useState('');

  const movementType = useMemo(() => buildMovementType(category, direction), [category, direction]);
  const isOut = direction === 'OUT';
  const qtyNum = Number(quantity) || 0;
  const projected = isOut ? currentStock - qtyNum : currentStock + qtyNum;
  const exceeds = isOut && qtyNum > currentStock;

  const reasonRequired = !reason.trim();
  const qtyInvalid = qtyNum < 1;
  const valid = !reasonRequired && !qtyInvalid && !exceeds && movementType;

  const dirLabels = MOVEMENT_DIRECTION_LABELS[category] || { IN: 'Giriş', OUT: 'Çıkış' };

  const submit = () => {
    if (!valid) return;
    const payload = {
      productId,
      quantity: qtyNum,
      movementType,
      reason: reason.trim(),
    };
    if (!isOut && unitCost) {
      const uc = Number(unitCost);
      if (uc > 0) payload.unitCost = uc;
    }
    createMut.mutate(payload, { onSuccess: () => onSuccess?.() });
  };

  return (
    <div className="col gap-12">
      <div className="field">
        <label>Hareket Kategorisi *</label>
        <div className="seg">
          {MOVEMENT_CATEGORIES.map(c => (
            <button
              key={c.key}
              type="button"
              className={category === c.key ? 'on' : ''}
              onClick={() => setCategory(c.key)}
            >
              {c.label}
            </button>
          ))}
        </div>
      </div>

      <div className="field">
        <label>Hareket Yönü *</label>
        <div className="seg">
          <button type="button" className={direction === 'IN' ? 'on' : ''} onClick={() => setDirection('IN')}>
            {dirLabels.IN}
          </button>
          <button type="button" className={direction === 'OUT' ? 'on' : ''} onClick={() => setDirection('OUT')}>
            {dirLabels.OUT}
          </button>
        </div>
      </div>

      <div className="grid-2">
        <div className="field">
          <label>Miktar *</label>
          <input
            className="input mono"
            type="number"
            min="1"
            value={quantity}
            onChange={e => setQuantity(e.target.value)}
          />
        </div>
        {!isOut && (
          <div className="field">
            <label>Birim Maliyet (₺)</label>
            <input
              className="input mono"
              type="number"
              min="0"
              step="0.01"
              value={unitCost}
              onChange={e => setUnitCost(e.target.value)}
              placeholder="opsiyonel"
            />
          </div>
        )}
      </div>

      <div className="field">
        <label>Sebep *</label>
        <textarea
          className="input"
          rows={2}
          value={reason}
          onChange={e => setReason(e.target.value)}
          placeholder={REASON_PLACEHOLDERS[movementType] || 'sebep girin'}
          maxLength={255}
        />
        {reasonRequired && (
          <span style={{ fontSize: 11, color: 'var(--warn)', marginTop: 4, display: 'block' }}>
            Hareket sebebi zorunludur
          </span>
        )}
      </div>

      <div className="card" style={{ padding: 10, background: 'var(--bg-2)' }}>
        <div className="row" style={{ justifyContent: 'space-between', fontSize: 12 }}>
          <span className="muted">Mevcut Stok:</span>
          <span className="mono tnum">{currentStock}</span>
        </div>
        <div className="row" style={{ justifyContent: 'space-between', fontSize: 12, marginTop: 4 }}>
          <span className="muted">Hareket:</span>
          <span
            className="mono tnum"
            style={{ color: isOut ? 'var(--neg)' : 'var(--pos)', fontWeight: 600 }}
          >
            {isOut ? '−' : '+'}{qtyNum || 0}
          </span>
        </div>
        <div className="row" style={{ justifyContent: 'space-between', fontSize: 13, marginTop: 4, paddingTop: 6, borderTop: '1px solid var(--line)' }}>
          <span><b>Sonra:</b></span>
          <span
            className="mono tnum"
            style={{ color: exceeds ? 'var(--neg)' : projected === 0 ? 'var(--warn)' : 'var(--ink-1)', fontWeight: 700 }}
          >
            {projected}
          </span>
        </div>
        {exceeds && (
          <div style={{ fontSize: 11, color: 'var(--neg)', marginTop: 6 }}>
            Yetersiz stok — çıkış miktarı mevcut stoktan büyük olamaz.
          </div>
        )}
      </div>

      <div className="row gap-8" style={{ justifyContent: 'flex-end', marginTop: 4 }}>
        <button type="button" className="btn ghost" onClick={onCancel} disabled={createMut.isPending}>
          Vazgeç
        </button>
        <button
          type="button"
          className="btn primary"
          onClick={submit}
          disabled={!valid || createMut.isPending}
        >
          Kaydet
        </button>
      </div>
    </div>
  );
}
