export default function SidebarWidget({
  title,
  badge,
  children,
  footer,
  loading,
  error,
  errorMsg = 'Veri alınamadı',
}) {
  return (
    <div className="sb-widget" aria-label={title}>
      <div className="sb-widget-head">
        <span>{title}</span>
        {loading && <span className="sb-widget-dot pulse" />}
        {!loading && !error && badge}
        {!loading && error && <span className="sb-widget-dot err" title={errorMsg} />}
      </div>

      {error && <div className="sb-widget-err">{errorMsg}</div>}

      <div className="sb-widget-body">{children}</div>

      {footer && <div className="sb-widget-foot">{footer}</div>}
    </div>
  );
}
