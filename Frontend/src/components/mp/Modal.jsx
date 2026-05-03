import React from 'react';
import Icon from '@/components/mp/Icon';

export default function Modal({ open, onClose, title, children, footer }) {
  if (!open) return null;
  return (
    <div className="scrim" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="drawer-h">
          <h3>{title}</h3>
          <button className="tb-icon-btn" onClick={onClose}><Icon name="x" /></button>
        </div>
        <div className="drawer-b" style={{ padding: '18px 20px' }}>{children}</div>
        {footer && <div className="drawer-f">{footer}</div>}
      </div>
    </div>
  );
}
