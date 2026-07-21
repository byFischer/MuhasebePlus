import Icon from '@/components/mp/Icon';

export default function DashboardCard({
  def,
  size,
  headerExtra,
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

  return (
    <div
      className="dash-card"
      data-size={effectiveSize}
      data-widget={def.id}
    >
      <div className="mac-btns">
        {hasConfig && onConfig && (
          <button className="mac-btn grey" title="Ayarlar" onClick={onConfig}>
            <Icon name="settings" size={10} />
          </button>
        )}
        {onRefresh && (
          <button className="mac-btn grey refresh" title="Yenile" onClick={onRefresh}>
            <Icon name="refresh" size={10} />
          </button>
        )}
        {onRemove && (
          <button className="mac-btn red" title="Slot'tan kaldır" onClick={onRemove} />
        )}
        {onDetail && (
          <button className="mac-btn green" title="Detaylı görüntüle" onClick={onDetail} />
        )}
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
          {actionRoute && onNav && (
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
