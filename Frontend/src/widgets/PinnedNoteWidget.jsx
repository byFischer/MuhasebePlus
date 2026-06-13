
export default function PinnedNoteWidget({ config, mode, variant }) {
  const v = variant || (mode === 'detail' ? 'l' : 'm');
  const text = config?.content || '';

  if (!text.trim()) {
    return <div className="empty" style={{ fontStyle: 'italic' }}>Not alanı boş — ⚙️ ayarlardan içerik ekleyin</div>;
  }

  const lines = text.split('\n');
  const display = v === 's' ? lines.slice(0, 2) : lines;
  const truncated = v === 's' && lines.length > 2;

  return (
    <div className="pinned-note">
      {display.map((line, i) => (
        <p key={i}>{line}</p>
      ))}
      {truncated && (
        <p className="muted" style={{ fontStyle: 'italic', fontSize: 11 }}>
          +{lines.length - 2} satır daha…
        </p>
      )}
    </div>
  );
}
