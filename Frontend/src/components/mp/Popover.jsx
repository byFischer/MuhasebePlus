import { useState, useRef, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import Icon from '@/components/mp/Icon';

// Bir butona tutturulmuş, butonun hemen altından açılan dropdown panel.
// (YouTube bildirim panelinde olduğu gibi.) İçerik portal ile body'e taşınır,
// böylece sayfa akışını bozmaz; dışarı tıklayınca veya Esc ile kapanır.
// Panel genişliği içeriğe göre belirlenir (içeriğin gerektirdiği kadar);
// maxWidth yalnızca üst sınırdır. Sağ kenarı butonun sağ kenarına hizalanır,
// böylece içeriğe göre sola doğru büyür.
export default function Popover({ icon, label, title, maxWidth = 480, children }) {
  const [open, setOpen] = useState(false);
  const btnRef = useRef(null);
  const [pos, setPos] = useState({ top: 0, right: 0 });

  const updatePos = useCallback(() => {
    if (!btnRef.current) return;
    const r = btnRef.current.getBoundingClientRect();
    setPos({ top: r.bottom + 8, right: Math.max(8, window.innerWidth - r.right) });
  }, []);

  useEffect(() => {
    if (!open) return;
    updatePos();
    const onMove = () => updatePos();
    const onKey = (e) => { if (e.key === 'Escape') setOpen(false); };
    window.addEventListener('resize', onMove);
    window.addEventListener('scroll', onMove, true);
    window.addEventListener('keydown', onKey);
    return () => {
      window.removeEventListener('resize', onMove);
      window.removeEventListener('scroll', onMove, true);
      window.removeEventListener('keydown', onKey);
    };
  }, [open, updatePos]);

  const close = () => setOpen(false);

  return (
    <div style={{ position: 'relative', display: 'inline-flex' }}>
      <button ref={btnRef} className="btn ghost" onClick={() => setOpen(o => !o)} aria-expanded={open} aria-haspopup="dialog">
        {icon && <Icon name={icon} size={14} />} {label}
        <Icon name="chevDown" size={12} style={{ marginLeft: 2, opacity: 0.6, transform: open ? 'rotate(180deg)' : 'none', transition: 'transform 0.15s' }} />
      </button>
      {open && createPortal(
        <>
          <div onClick={close} style={{ position: 'fixed', inset: 0, zIndex: 200 }} />
          <div className="popover-panel" role="dialog" style={{ position: 'fixed', top: pos.top, right: pos.right, width: 'max-content', maxWidth: `min(${maxWidth}px, calc(100vw - 16px))`, zIndex: 201 }}>
            <div className="popover-head">
              <h3>{title}</h3>
              <button className="tb-icon-btn" onClick={close} aria-label="Kapat"><Icon name="x" size={14} /></button>
            </div>
            <div className="popover-body col gap-12">
              {typeof children === 'function' ? children({ close }) : children}
            </div>
          </div>
        </>,
        document.body
      )}
    </div>
  );
}
