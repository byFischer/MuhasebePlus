import { useState, useRef, useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useQuery } from '@tanstack/react-query';
import Drawer from '@/components/mp/Drawer';
import Icon from '@/components/mp/Icon';
import { toast } from '@/lib/toast';
import dashboardService from '@/services/dashboardService';
import { LAYOUT_PRESETS } from '@/lib/dashboardLayouts';
import WidgetPreviewCard from './WidgetPreviewCard';

const INTRO_MSG = {
  role: 'assistant',
  content: 'Merhaba! Dashboard\'ına eklemek istediğin widget\'ı Türkçe olarak tarif et. Örnek: "Bu ayın toplam gelirini KPI olarak göster" veya "Son 6 ayın aylık satış trendini çizgi grafik olarak göster."',
};

export default function AiWidgetChatPanel({ onClose }) {
  const qc = useQueryClient();
  const [messages, setMessages] = useState([INTRO_MSG]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [addingToDashboard, setAddingToDashboard] = useState(false);
  const bottomRef = useRef(null);
  const textareaRef = useRef(null);

  const { data: quota } = useQuery({
    queryKey: ['ai-quota'],
    queryFn: dashboardService.getAiQuota,
    staleTime: 30_000,
  });

  // Yeni mesaj gelince aşağı kaydır
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Textarea auto-resize
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 120) + 'px';
    }
  }, [input]);

  const lastAiMsg = [...messages].reverse().find(m => m.role === 'assistant' && m.widgetDefinition);

  const sendMessage = async () => {
    const text = input.trim();
    if (!text || sending) return;

    const userMsg = { role: 'user', content: text };
    const nextMessages = [...messages, userMsg];
    setMessages(nextMessages);
    setInput('');
    setSending(true);

    // Backend'e sadece role + content gönder (preview/widgetDefinition göndermiyoruz)
    const payload = nextMessages.map(m => ({ role: m.role, content: m.content }));

    try {
      const res = await dashboardService.aiWidgetChat({ messages: payload });

      const aiMsg = {
        role: 'assistant',
        content: res.assistantMessage,
        widgetDefinition: res.widgetDefinition || null,
        previewData: res.previewData || null,
        previewColumns: res.previewColumns || null,
        previewSuccess: res.previewSuccess,
        previewError: res.previewError || null,
      };
      setMessages(prev => [...prev, aiMsg]);
      qc.invalidateQueries({ queryKey: ['ai-quota'] });
    } catch (err) {
      const msg = err.response?.data?.detail || err.response?.data?.message || err.message || 'Bağlantı hatası';
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: `Hata oluştu: ${msg}`,
      }]);
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const addToDashboard = async (msg) => {
    if (!msg.widgetDefinition) return;
    setAddingToDashboard(true);
    try {
      // 1. Widget definition kaydet
      const def = await dashboardService.createWidgetDefinition({
        name: msg.widgetDefinition.name,
        description: msg.widgetDefinition.description || null,
        widgetType: msg.widgetDefinition.widgetType,
        dataSource: msg.widgetDefinition.dataSource,
        queryConfig: msg.widgetDefinition.queryConfig || {},
        visualConfig: msg.widgetDefinition.visualConfig || {},
        config: {},
      });

      // 2. Default layout'u al ve ilk boş slotu bul
      const layout = await dashboardService.getDefaultLayout();
      const preset = LAYOUT_PRESETS[layout.layoutPreset];
      if (!preset) {
        toast.err('Dashboard layout bulunamadı');
        return;
      }
      const occupiedSlots = new Set((layout.widgets || []).map(w => w.slotIndex));
      const firstFreeSlot = preset.slots.findIndex((_, i) => !occupiedSlots.has(i));

      if (firstFreeSlot === -1) {
        toast.err('Dashboard\'da boş slot yok. Önce bir widget kaldırın.');
        return;
      }

      // 3. Widget instance ekle
      await dashboardService.addWidget(layout.layoutId, {
        widgetType: def.widgetType,
        definitionId: def.definitionId,
        title: def.name,
        slotIndex: firstFreeSlot,
        positionX: 0,
        positionY: 0,
        width: 2,
        height: 1,
        config: JSON.stringify({ variant: 'm' }),
      });

      toast.ok(`"${def.name}" dashboard'a eklendi`);
      qc.invalidateQueries({ queryKey: ['dashboard'] });
      onClose();
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Eklenemedi';
      toast.err(msg);
    } finally {
      setAddingToDashboard(false);
    }
  };

  const quotaPct = quota ? Math.min(100, (quota.used / quota.budget) * 100) : 0;
  const quotaColor = quotaPct > 85 ? '#ef4444' : quotaPct > 60 ? '#eab308' : 'var(--accent)';

  return (
    <Drawer
      open
      onClose={onClose}
      title="AI Widget Asistanı"
      width="420px"
      footer={
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, width: '100%' }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
            <textarea
              ref={textareaRef}
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Widget isteğini yaz... (Enter'a bas veya Gönder'e tıkla)"
              disabled={sending}
              rows={1}
              className="mp-input"
              style={{
                flex: 1,
                resize: 'none',
                minHeight: 40,
                maxHeight: 120,
                lineHeight: 1.4,
                paddingTop: 10,
                paddingBottom: 10,
              }}
            />
            <button
              className="btn primary"
              onClick={sendMessage}
              disabled={!input.trim() || sending}
              style={{ height: 40, minWidth: 80, flexShrink: 0 }}
            >
              {sending ? <Icon name="loader" size={14} /> : <><Icon name="send" size={14} /> Gönder</>}
            </button>
          </div>
          {quota && (
            <div style={{ fontSize: 11, color: 'var(--ink-3)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3 }}>
                <span>AI Kotası</span>
                <span style={{ color: quotaColor }}>
                  {(quota.used / 1000).toFixed(1)}K / {(quota.budget / 1000).toFixed(0)}K token
                </span>
              </div>
              <div style={{ height: 3, background: 'var(--bg-2)', borderRadius: 2, overflow: 'hidden' }}>
                <div style={{ width: `${quotaPct}%`, height: '100%', background: quotaColor, borderRadius: 2, transition: 'width 0.3s' }} />
              </div>
            </div>
          )}
        </div>
      }
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {messages.map((msg, i) => (
          <MessageBubble
            key={i}
            msg={msg}
            onAddToDashboard={addToDashboard}
            addingToDashboard={addingToDashboard}
            isLastWithWidget={lastAiMsg && msg === lastAiMsg}
          />
        ))}
        {sending && (
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', color: 'var(--ink-3)', fontSize: 13 }}>
            <Icon name="loader" size={14} />
            <span>AI düşünüyor...</span>
          </div>
        )}
        <div ref={bottomRef} />
      </div>
    </Drawer>
  );
}

function MessageBubble({ msg, onAddToDashboard, addingToDashboard, isLastWithWidget }) {
  const isUser = msg.role === 'user';

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: isUser ? 'flex-end' : 'flex-start',
      gap: 8,
    }}>
      <div style={{
        maxWidth: '85%',
        padding: '10px 14px',
        borderRadius: isUser ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
        background: isUser ? 'var(--accent)' : 'var(--bg-2)',
        color: isUser ? '#fff' : 'var(--ink)',
        fontSize: 13,
        lineHeight: 1.5,
        whiteSpace: 'pre-wrap',
        wordBreak: 'break-word',
      }}>
        {msg.content}
      </div>

      {!isUser && msg.widgetDefinition && (
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 8 }}>
          <WidgetPreviewCard
            widgetDefinition={msg.widgetDefinition}
            previewData={msg.previewData}
            previewColumns={msg.previewColumns}
            previewError={msg.previewSuccess ? null : msg.previewError}
          />
          {isLastWithWidget && (
            <button
              className="btn primary"
              onClick={() => onAddToDashboard(msg)}
              disabled={addingToDashboard}
              style={{ alignSelf: 'flex-start' }}
            >
              <Icon name="plus" size={14} />
              {addingToDashboard ? 'Ekleniyor...' : 'Dashboard\'a Ekle'}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
