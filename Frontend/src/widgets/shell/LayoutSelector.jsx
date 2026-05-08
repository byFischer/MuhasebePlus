import React, { useState } from 'react';
import Icon from '@/components/mp/Icon';
import { LAYOUT_PRESETS, LAYOUT_KEYS } from '@/lib/dashboardLayouts';

// Layout preview — her preset'in slot dağılımını mini SVG grid olarak çizer.
function LayoutPreview({ preset, isSelected }) {
  // SVG viewport: 12 sütun × N satır, oran korunur
  const W = 120, H = 80;
  const cellW = W / preset.cols;
  const cellH = H / preset.rows;

  return (
    <svg
      width={W}
      height={H}
      viewBox={`0 0 ${W} ${H}`}
      style={{ display: 'block' }}
    >
      {preset.slots.map(slot => {
        // gridColumn '1 / 5' → start=1, end=5, colSpan=4
        const [colStart, colEnd] = slot.col.split('/').map(s => parseInt(s.trim(), 10));
        const [rowStart, rowEnd] = slot.row.split('/').map(s => parseInt(s.trim(), 10));
        const x = (colStart - 1) * cellW;
        const y = (rowStart - 1) * cellH;
        const w = (colEnd - colStart) * cellW;
        const h = (rowEnd - rowStart) * cellH;
        return (
          <rect
            key={slot.id}
            x={x + 1}
            y={y + 1}
            width={w - 2}
            height={h - 2}
            rx={2}
            fill={isSelected ? 'var(--accent)' : 'var(--surface)'}
            opacity={isSelected ? 0.35 : 1}
            stroke={isSelected ? 'var(--accent)' : 'var(--line)'}
            strokeWidth={1}
          />
        );
      })}
    </svg>
  );
}

export default function LayoutSelector({ currentLayout, onSelect, onClose, hasWidgets }) {
  const [pendingLayout, setPendingLayout] = useState(null);

  const handlePick = (key) => {
    if (key === currentLayout) return;
    if (hasWidgets) {
      setPendingLayout(key);
    } else {
      onSelect(key);
    }
  };

  const confirmChange = () => {
    if (pendingLayout) {
      onSelect(pendingLayout);
      setPendingLayout(null);
    }
  };

  return (
    <div className="dash-detail-scrim" onClick={onClose}>
      <div className="dash-detail" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 720 }}>
        <div className="dash-detail-h">
          <div>
            <h2>Dashboard Layout</h2>
            <div className="sub">5 farklı şablondan birini seçin. Slotlara widget ekleyebilirsiniz.</div>
          </div>
          <button className="tb-icon-btn" onClick={onClose}><Icon name="x" /></button>
        </div>
        <div className="dash-detail-b">
          <div className="layout-grid">
            {LAYOUT_KEYS.map((key) => {
              const preset = LAYOUT_PRESETS[key];
              const isSelected = key === currentLayout;
              return (
                <button
                  key={key}
                  type="button"
                  className={`layout-card ${isSelected ? 'selected' : ''}`}
                  onClick={() => handlePick(key)}
                >
                  <div className="layout-card-preview">
                    <LayoutPreview preset={preset} isSelected={isSelected} />
                  </div>
                  <div className="layout-card-meta">
                    <div className="layout-card-name">{preset.name}</div>
                    <div className="layout-card-desc">{preset.description}</div>
                    <div className="layout-card-slot-count">{preset.slots.length} slot</div>
                  </div>
                  {isSelected && (
                    <div className="layout-card-badge">
                      <Icon name="check" size={12} /> Aktif
                    </div>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Widget silme onay modal'ı */}
      {pendingLayout && (
        <div className="dash-detail-scrim" onClick={() => setPendingLayout(null)} style={{ zIndex: 1100 }}>
          <div className="dash-detail" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 460 }}>
            <div className="dash-detail-h">
              <div>
                <h2>Layout Değişikliği</h2>
                <div className="sub">{LAYOUT_PRESETS[pendingLayout].name} seçilecek</div>
              </div>
            </div>
            <div className="dash-detail-b">
              <p style={{ margin: 0, lineHeight: 1.6 }}>
                Bu layouta geçmek için <b>mevcut tüm widgetlar silinecek</b>. Yeni layoutta
                widgetlarınızı sıfırdan eklemeniz gerekir.
              </p>
              <p style={{ marginTop: 12, fontSize: 13, color: 'var(--ink-3)' }}>
                Devam etmek istiyor musunuz?
              </p>
            </div>
            <div className="dash-detail-f">
              <button className="btn ghost" onClick={() => setPendingLayout(null)}>Vazgeç</button>
              <button className="btn primary" onClick={confirmChange}>
                <Icon name="check" size={14} /> Devam Et
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
