import React, { useState, useMemo } from 'react';
import Icon from '@/components/mp/Icon';

export default function WidgetConfigModal({ def, config, onSave, onClose }) {
  const [draft, setDraft] = useState(() => ({ ...config }));
  const fields = def.configFields || {};

  const canSave = useMemo(() => {
    // basit validasyon: thresholds için artan sıralı olmalı
    if (fields.thresholds && draft.thresholds) {
      const arr = draft.thresholds;
      for (let i = 1; i < arr.length; i++) if (arr[i] <= arr[i - 1]) return false;
    }
    return true;
  }, [draft, fields]);

  const set = (key, val) => setDraft(prev => ({ ...prev, [key]: val }));

  return (
    <div className="dash-detail-scrim" onClick={onClose}>
      <div className="dash-detail" onClick={e => e.stopPropagation()} style={{ maxWidth: 520 }}>
        <div className="dash-detail-h">
          <div>
            <h2>{def.name} — Ayarlar</h2>
            <div className="sub">Widget davranışını kişiselleştir</div>
          </div>
          <button className="tb-icon-btn" onClick={onClose}><Icon name="x" /></button>
        </div>
        <div className="dash-detail-b col gap-12">
          {Object.entries(fields).map(([key, meta]) => (
            <div className="field" key={key}>
              <label>{meta.label}</label>
              {meta.type === 'text' && (
                <textarea
                  className="input"
                  rows={4}
                  value={draft[key] ?? ''}
                  onChange={e => set(key, e.target.value)}
                />
              )}
              {meta.type === 'number' && (
                <input
                  type="number"
                  className="input"
                  value={draft[key] ?? 0}
                  onChange={e => set(key, Number(e.target.value))}
                />
              )}
              {meta.type === 'select' && (
                <select
                  className="select"
                  value={draft[key] ?? meta.options[0]}
                  onChange={e => {
                    const val = e.target.value;
                    set(key, meta.options.includes(Number(val)) ? Number(val) : val);
                  }}
                >
                  {meta.options.map(opt => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                </select>
              )}
              {meta.type === 'multiselect' && (
                <div className="col gap-8" style={{ padding: '8px 0' }}>
                  {(draft[key] || []).map((item, i) => (
                    <label key={i} className="check" style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                      <input
                        type="checkbox"
                        checked={item.checked !== false}
                        onChange={e => {
                          const next = [...draft[key]];
                          next[i] = { ...next[i], checked: e.target.checked };
                          set(key, next);
                        }}
                      />
                      <span>{item.label || item}</span>
                    </label>
                  ))}
                  {(!draft[key] || draft[key].length === 0) && (
                    <span className="muted" style={{ fontSize: 12 }}>Tümü seçili (özelleştirme yakında)</span>
                  )}
                </div>
              )}
              {meta.type === 'thresholds' && (
                <div className="col gap-8">
                  {(draft[key] || []).map((val, i) => (
                    <div className="row gap-8" key={i} style={{ alignItems: 'center' }}>
                      <span className="muted" style={{ width: 90, fontSize: 12 }}>
                        {i === 0 ? '0–' + val : `${(draft[key][i - 1] || 0) + 1}–${val}`} gün
                      </span>
                      <input
                        type="number"
                        className="input"
                        min={1}
                        max={999}
                        value={val}
                        onChange={e => {
                          const next = [...draft[key]];
                          next[i] = Number(e.target.value);
                          set(key, next);
                        }}
                        style={{ width: 100 }}
                      />
                    </div>
                  ))}
                  <div className="row gap-8" style={{ alignItems: 'center' }}>
                    <span className="muted" style={{ width: 90, fontSize: 12 }}>
                      {draft[key]?.length ? `${draft[key][draft[key].length - 1] + 1}+ gün` : '90+ gün'}
                    </span>
                    <span className="muted" style={{ fontSize: 12 }}>son eşik (otomatik)</span>
                  </div>
                </div>
              )}
            </div>
          ))}
          {!canSave && (
            <div className="hint" style={{ background: 'var(--neg-soft)', color: 'var(--neg)' }}>
              <Icon name="alert" size={14} /> Eşik değerleri artan sırada olmalıdır.
            </div>
          )}
        </div>
        <div className="dash-detail-f">
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" onClick={() => onSave(draft)} disabled={!canSave}>
            <Icon name="check" size={14} /> Kaydet
          </button>
        </div>
      </div>
    </div>
  );
}
