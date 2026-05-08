import React from 'react';
import Icon from '@/components/mp/Icon';

export default function Drawer({ open, onClose, title, children, footer, width, closeOnBackdrop = true }) {
  if (!open) return null;
  return (
    <div className="scrim" onClick={closeOnBackdrop ? onClose : undefined}>
      <aside className="drawer" style={width ? { width } : undefined} onClick={e => e.stopPropagation()}>
        <div className="drawer-h">
          <h3>{title}</h3>
          <button className="tb-icon-btn" onClick={onClose}><Icon name="x" /></button>
        </div>
        <div className="drawer-b">{children}</div>
        {footer && <div className="drawer-f">{footer}</div>}
      </aside>
    </div>
  );
}
