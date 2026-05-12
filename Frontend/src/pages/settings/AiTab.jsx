import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/services/api';
import Icon from '@/components/mp/Icon';

export default function AiTab() {
  const qc = useQueryClient();
  const [apiKey, setApiKey] = useState('');
  const [showKey, setShowKey] = useState(false);
  const [testResult, setTestResult] = useState(null);

  const { data: features } = useQuery({
    queryKey: ['system-features'],
    queryFn: () => fetch(`${import.meta.env.VITE_API_BASE_URL}/api/system/features`).then(r => r.json()),
    staleTime: 30_000,
  });

  const saveMutation = useMutation({
    mutationFn: (key) => api.put('/api/system/ai-settings', { apiKey: key }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['system-features'] });
      setTestResult({ ok: true, msg: 'API key kaydedildi.' });
    },
    onError: () => setTestResult({ ok: false, msg: 'Kaydetme başarısız.' }),
  });

  const testMutation = useMutation({
    mutationFn: () => api.post('/api/ai/widgets/generate', { prompt: 'test' }),
    onSuccess: () => setTestResult({ ok: true, msg: 'Bağlantı başarılı!' }),
    onError: (err) => {
      const msg = err.response?.data?.message || 'Bağlantı hatası';
      setTestResult({ ok: false, msg });
    },
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', maxWidth: 520 }}>
      <div>
        <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 600 }}>Yapay Zeka (Gemini)</h3>
        <p style={{ margin: '6px 0 0', fontSize: '0.85rem', color: 'var(--muted)' }}>
          Widget Builder'ın AI özelliği için Google Gemini API anahtarı gereklidir.
          Anahtarınızı{' '}
          <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noreferrer" style={{ color: 'var(--accent)' }}>
            Google AI Studio
          </a>
          'dan ücretsiz alabilirsiniz.
        </p>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '10px 14px', borderRadius: 8, background: features?.aiEnabled ? 'rgba(34,197,94,0.08)' : 'rgba(239,68,68,0.08)', border: `1px solid ${features?.aiEnabled ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}` }}>
        <Icon name={features?.aiEnabled ? 'check' : 'x'} size={14} style={{ color: features?.aiEnabled ? '#22c55e' : '#ef4444' }} />
        <span style={{ fontSize: '0.85rem', color: features?.aiEnabled ? '#22c55e' : '#ef4444' }}>
          {features?.aiEnabled ? 'AI aktif — API key tanımlı' : 'AI devre dışı — API key girilmemiş'}
        </span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <label style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--muted)' }}>Gemini API Key</label>
        <div style={{ display: 'flex', gap: 8 }}>
          <input
            type={showKey ? 'text' : 'password'}
            value={apiKey}
            onChange={e => { setApiKey(e.target.value); setTestResult(null); }}
            placeholder="AIza..."
            className="mp-input"
            style={{ flex: 1 }}
          />
          <button className="tb-icon-btn" type="button" onClick={() => setShowKey(v => !v)} title={showKey ? 'Gizle' : 'Göster'}>
            <Icon name={showKey ? 'eye-off' : 'eye'} size={16} />
          </button>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            className="btn primary"
            type="button"
            disabled={!apiKey.trim() || saveMutation.isPending}
            onClick={() => saveMutation.mutate(apiKey.trim())}
          >
            <Icon name="save" size={14} /> Kaydet
          </button>
          <button
            className="btn ghost"
            type="button"
            disabled={testMutation.isPending}
            onClick={() => testMutation.mutate()}
            title="Mevcut kayıtlı key ile test et"
          >
            <Icon name="zap" size={14} /> Test Et
          </button>
        </div>
      </div>

      {testResult && (
        <div style={{ fontSize: '0.85rem', color: testResult.ok ? '#22c55e' : '#ef4444', padding: '8px 12px', borderRadius: 6, background: testResult.ok ? 'rgba(34,197,94,0.08)' : 'rgba(239,68,68,0.08)' }}>
          {testResult.msg}
        </div>
      )}
    </div>
  );
}
