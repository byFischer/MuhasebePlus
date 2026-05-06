import React from 'react';
import Icon from '@/components/mp/Icon';

export default function DashboardCard({
  def,
  size,
  isDragging,
  dragging,
  isHover,
  editMode,
  headerExtra,
  cardRef,
  onPointerDown,
  onRemove,
  onDetail,
  onRefresh,
  onConfig,
  onNav,
  children,
}) {
  const { name, sub, noPad, actionRoute, actionLabel, configFields } = def;
  const effectiveSize = size || def.size;
  const isFull = effectiveSize === 'full';
  const hasConfig = !!configFields && Object.keys(configFields).length > 0;

  if (isDragging) {
    return (
      <>
        <div
          ref={cardRef}
          className="dash-card drag-placeholder"
          data-size={effectiveSize}
          style={{ height: dragging.h }}
        >
          <div className="drag-placeholder-inner" />
        </div>
        <div
          className="dash-card dragging"
          style={{
            position: 'fixed',
            left: dragging.x - dragging.offsetX,
            top: dragging.y - dragging.offsetY,
            width: dragging.w,
            height: dragging.h,
            zIndex: 1000,
            pointerEvents: 'none',
            transform: 'scale(1.03)',
            boxShadow: '0 0 0 2px var(--accent), 0 24px 48px rgba(0,0,0,0.22)',
          }}
        >
          <div className="drag-handle"><Icon name="drag" size={14} /></div>
          <div className="mac-btns">
            <button className="mac-btn red" aria-label="Kapat" />
            <button className="mac-btn green" aria-label="Detay" />
          </div>
          {!isFull && (
            <div className="card-h">
              <div>
                <h3 style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span>{name}</span>
                  {headerExtra}
                </h3>
                {sub && <div className="sub">{sub}</div>}
              </div>
            </div>
          )}
          <div className={`card-b ${noPad ? 'p0' : ''} ${isFull ? 'kpis-pad' : ''}`}>
            {children}
          </div>
        </div>
      </>
    );
  }

  return (
    <div
      ref={cardRef}
      className={`dash-card ${isHover ? 'hover-target' : ''}`}
      data-size={effectiveSize}
      data-edit-mode={editMode}
      data-widget={def.id}
    >
      {editMode && (
        <div
          className="drag-handle"
          onPointerDown={onPointerDown}
          style={{ cursor: 'grab' }}
        >
          <Icon name="drag" size={14} />
        </div>
      )}
      <div className="mac-btns">
        {editMode && (
          <>
            {hasConfig && (
              <button className="mac-btn grey" title="Ayarlar" onClick={onConfig}>
                <Icon name="settings" size={10} />
              </button>
            )}
            <button className="mac-btn grey refresh" title="Yenile" onClick={onRefresh}>
              <Icon name="refresh" size={10} />
            </button>
            <button className="mac-btn red" title="Sil" onClick={onRemove} />
          </>
        )}
        <button className="mac-btn green" title="Detaylı görüntüle" onClick={onDetail} />
      </div>
      {!isFull && (
        <div className="card-h">
          <div>
            <h3 style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <span>{name}</span>
              {headerExtra}
            </h3>
            {sub && <div className="sub">{sub}</div>}
          </div>
          {actionRoute && (
            <button className="btn ghost sm" onClick={() => onNav(actionRoute)}>
              {actionLabel} <Icon name="chevRight" size={12} />
            </button>
          )}
        </div>
      )}
      <div className={`card-b ${noPad ? 'p0' : ''} ${isFull ? 'kpis-pad' : ''}`}>
        {children}
      </div>
    </div>
  );
}
