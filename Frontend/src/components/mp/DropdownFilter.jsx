import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import Icon from '@/components/mp/Icon';

export function DropdownFilter({ label, icon = 'filter', options, value, setValue, open, setOpen, placeholderValue = 'hepsi', minWidth = 200 }) {
  const btnRef = useRef(null);
  const [pos, setPos] = useState({ top: 0, left: 0 });
  useEffect(() => {
    if (!open || !btnRef.current) return;
    const r = btnRef.current.getBoundingClientRect();
    setPos({ top: r.bottom + 6, left: r.left });
  }, [open]);
  const selected = options.find(o => o.value === value);
  const isActive = value !== placeholderValue;
  return (
    <div style={{ position: 'relative' }}>
      <button ref={btnRef} className={`filter ${isActive ? 'active' : ''}`} onClick={() => setOpen(o => !o)}>
        <Icon name={icon} size={12} /> {label}: {selected?.label || 'Hepsi'}
        <Icon name="chevDown" size={12} style={{ marginLeft: 4 }} />
      </button>
      {open && createPortal(
        <>
          <div onClick={() => setOpen(false)} style={{ position: 'fixed', inset: 0, zIndex: 200 }} />
          <div style={{ position: 'fixed', top: pos.top, left: pos.left, zIndex: 201, background: 'var(--surface)', border: '1px solid var(--line)', borderRadius: 10, boxShadow: '0 12px 32px rgba(0,0,0,0.16), 0 2px 6px rgba(0,0,0,0.08)', minWidth, maxHeight: 320, overflow: 'auto', padding: 6 }}>
            {options.map(o => (
              <button key={o.value} onClick={() => { setValue(o.value); setOpen(false); }}
                style={{ display: 'block', width: '100%', textAlign: 'left', padding: '8px 12px', border: 'none', background: value === o.value ? 'var(--accent-soft)' : 'transparent', color: value === o.value ? 'var(--accent)' : 'var(--ink)', borderRadius: 6, cursor: 'pointer', font: 'inherit', fontSize: 13, fontWeight: value === o.value ? 500 : 400 }}
                onMouseEnter={e => { if (value !== o.value) e.currentTarget.style.background = 'var(--bg-2)'; }}
                onMouseLeave={e => { if (value !== o.value) e.currentTarget.style.background = 'transparent'; }}
              >{o.label}</button>
            ))}
          </div>
        </>,
        document.body
      )}
    </div>
  );
}

export function CityFilter({ cities, city, setCity, open, setOpen }) {
  return <DropdownFilter label="Şehir" icon="filter" options={cities.map(c => ({ value: c, label: c === 'hepsi' ? 'Hepsi' : c }))} value={city} setValue={setCity} open={open} setOpen={setOpen} placeholderValue="hepsi" />;
}
