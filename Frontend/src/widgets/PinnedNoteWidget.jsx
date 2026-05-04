import React from 'react';

export default function PinnedNoteWidget({ config }) {
  const text = config?.content || '';
  if (!text.trim()) {
    return <div className="empty" style={{ fontStyle: 'italic' }}>Not alanı boş — ⚙️ ayarlardan içerik ekleyin</div>;
  }
  return (
    <div className="pinned-note">
      {text.split('\n').map((line, i) => (
        <p key={i}>{line}</p>
      ))}
    </div>
  );
}
