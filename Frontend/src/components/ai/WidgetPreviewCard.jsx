import React from 'react';
import DataWidget from '@/widgets/DataWidget';

export default function WidgetPreviewCard({ widgetDefinition, previewData, previewColumns, previewError }) {
  if (previewError) {
    return (
      <div style={{
        padding: '12px 14px',
        borderRadius: 8,
        background: 'rgba(239,68,68,0.08)',
        border: '1px solid rgba(239,68,68,0.2)',
        fontSize: 13,
        color: '#ef4444',
      }}>
        {previewError}
      </div>
    );
  }

  if (!previewData || previewData.length === 0) {
    return (
      <div style={{
        padding: '12px 14px',
        borderRadius: 8,
        background: 'var(--bg-2)',
        fontSize: 13,
        color: 'var(--ink-3)',
        textAlign: 'center',
      }}>
        Önizleme verisi yok
      </div>
    );
  }

  // DataWidget'ın beklediği data formatı: { data: rows, columns }
  const dataForWidget = {
    data: previewData,
    columns: previewColumns || [],
  };

  const config = {
    visualConfig: widgetDefinition?.visualConfig || {},
  };

  return (
    <div style={{
      border: '1px solid var(--line)',
      borderRadius: 8,
      overflow: 'hidden',
      background: 'var(--bg)',
    }}>
      <div style={{
        padding: '8px 12px',
        borderBottom: '1px solid var(--line)',
        fontSize: 12,
        fontWeight: 600,
        color: 'var(--ink-2)',
        display: 'flex',
        alignItems: 'center',
        gap: 6,
      }}>
        <span style={{ color: 'var(--accent)' }}>●</span>
        {widgetDefinition?.name || 'Önizleme'}
      </div>
      <div style={{ padding: 12, minHeight: 120 }}>
        <DataWidget data={dataForWidget} config={config} mode="card" variant="m" />
      </div>
    </div>
  );
}
