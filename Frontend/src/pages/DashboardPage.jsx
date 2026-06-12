import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import Icon from '@/components/mp/Icon';
import { toast } from '@/lib/toast';
import { useInvoices } from '@/hooks/useInvoices';
import { useBankAccounts } from '@/hooks/useBankAccounts';
import { useLowStock } from '@/hooks/useStock';
import { useTransactions } from '@/hooks/useTransactions';
import { useTemplates } from '@/hooks/useTemplates';
import dashboardService from '@/services/dashboardService';
import { useAuth } from '@/context/AuthContext';

import { WIDGET_REGISTRY, BACKEND_TYPE, FRONTEND_ID, WIDGET_DEF, isCustomWidgetType } from '@/widgets/registry';
import { LAYOUT_PRESETS, DEFAULT_LAYOUT, slotSizeToVariant, slotSizeToCardSize } from '@/lib/dashboardLayouts';
import DashboardCard from '@/widgets/shell/DashboardCard';
import { EmptySlot, FilledSlot } from '@/widgets/shell/SlotCard';
import LayoutSelector from '@/widgets/shell/LayoutSelector';
import KpisWidget from '@/widgets/KpisWidget';
import RevenueChartWidget from '@/widgets/RevenueChartWidget';
import CashPositionWidget from '@/widgets/CashPositionWidget';
import AgingWidget from '@/widgets/AgingWidget';
import LowStockWidget, { getHeaderExtra as lowstockExtra } from '@/widgets/LowStockWidget';
import QuickActionsWidget from '@/widgets/QuickActionsWidget';
import RecentInvoicesWidget from '@/widgets/RecentInvoicesWidget';
import TasksWidget, { getHeaderExtra as tasksExtra } from '@/widgets/TasksWidget';
import DailyBriefWidget from '@/widgets/DailyBriefWidget';
import PinnedNoteWidget from '@/widgets/PinnedNoteWidget';
import ProgressGoalWidget from '@/widgets/ProgressGoalWidget';
import ForecastWidget from '@/widgets/ForecastWidget';
import HeatmapWidget from '@/widgets/HeatmapWidget';
import TemplateQuickWidget from '@/widgets/TemplateQuickWidget';
import DataWidget from '@/widgets/DataWidget';
import WidgetConfigModal from '@/widgets/shell/WidgetConfigModal';
import { useWidgetData } from '@/hooks/useWidgetData';
import AiWidgetChatPanel from '@/components/ai/AiWidgetChatPanel';
import BeyannameReminderWidget from '@/widgets/BeyannameReminderWidget';

const WIDGET_COMPONENTS = {
  kpis:       KpisWidget,
  chart:      RevenueChartWidget,
  cash:       CashPositionWidget,
  aging:      AgingWidget,
  lowstock:   LowStockWidget,
  quick:      QuickActionsWidget,
  recent:     RecentInvoicesWidget,
  tasks:      TasksWidget,
  dailybrief: DailyBriefWidget,
  pinned:     PinnedNoteWidget,
  goal:       ProgressGoalWidget,
  forecast:   ForecastWidget,
  heatmap:    HeatmapWidget,
  templates:  TemplateQuickWidget,
};

export default function DashboardPage() {
  const onNav = useNavigate();
  const qc = useQueryClient();
  const { data: invoices = [] } = useInvoices();
  const { data: banks = [] } = useBankAccounts();
  const { data: lowItems = [] } = useLowStock();
  const { data: txns = [] } = useTransactions();
  const { data: templates = [] } = useTemplates();
  const { user } = useAuth();
  const [aiPanelOpen, setAiPanelOpen] = useState(false);

  // Widget configs (id -> object)
  const [configs, setConfigs] = useState(() => {
    try {
      const saved = localStorage.getItem('mp.dashboard.configs');
      if (saved) return JSON.parse(saved);
    } catch { /* ignore */ }
    return Object.fromEntries(WIDGET_REGISTRY.map(w => [w.id, w.defaultConfig || {}]));
  });

  const saveConfigs = useCallback((next) => {
    setConfigs(next);
    try { localStorage.setItem('mp.dashboard.configs', JSON.stringify(next)); } catch { /* ignore */ }
  }, []);

  // Computed dashboard data
  const D = useMemo(() => {
    const now = new Date();
    const months = [], revenue = [], expense = [];
    const chartMonths = configs.chart?.months || 12;

    for (let i = chartMonths - 1; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      months.push(d.toLocaleDateString('tr-TR', { month: 'short' }));
      const mTxns = txns.filter(t => {
        const td = new Date(t.transactionDate || t.createdAt);
        return !isNaN(td.getTime()) && td.getFullYear() === d.getFullYear() && td.getMonth() === d.getMonth();
      });
      revenue.push(mTxns.filter(t => t.transactionType === 'INCOME').reduce((s, t) => s + Number(t.amount || 0), 0));
      expense.push(mTxns.filter(t => t.transactionType === 'EXPENSE').reduce((s, t) => s + Math.abs(Number(t.amount || 0)), 0));
    }

    const thisRev = revenue[revenue.length - 1], thisExp = expense[expense.length - 1];
    const prevRev = revenue[revenue.length - 2], prevExp = expense[expense.length - 2];

    const pct = (curr, prev) => {
      if (!prev && !curr) return '—';
      if (!prev) return curr > 0 ? '+∞' : '—';
      const d = ((curr - prev) / prev * 100).toFixed(1);
      return (Number(d) >= 0 ? '+' : '') + d + '%';
    };

    const agingCfg = configs.aging?.thresholds || [30, 60, 90];
    const aging = new Array(agingCfg.length + 1).fill(0);
    invoices.filter(i => i.paymentStatus !== 'paid' && !i.isDeleted).forEach(i => {
      const due = new Date(i.dueDate || i.createdAt);
      if (isNaN(due.getTime())) return;
      const days = Math.floor((now - due) / 86400000);
      const amt = Number(i.totalAmount || 0);
      let bucket = aging.length - 1;
      for (let b = 0; b < agingCfg.length; b++) {
        if (days <= agingCfg[b]) { bucket = b; break; }
      }
      aging[bucket] += amt;
    });

    const recentLimit = configs.recent?.limit || 6;

    return {
      revenue, expense, months,
      thisRev, thisExp, prevRev, prevExp,
      revDelta: pct(thisRev, prevRev),
      expDelta: pct(thisExp, prevExp),
      netDelta: pct(thisRev - thisExp, prevRev - prevExp),
      revPos: thisRev >= prevRev,
      netPos: (thisRev - thisExp) >= (prevRev - prevExp),
      aging,
      agingTotal: aging.reduce((s, v) => s + v, 0) || 1,
      agingLabels: buildAgingLabels(agingCfg),
      banks: (() => {
        let list = banks.map((b, i) => ({
          id: b.bankAccountId ?? b.accountId ?? i,
          name: b.bankName || b.accountName,
          currency: b.currency,
          balance: Number(b.balance ?? b.currentBalance ?? 0),
          iban: b.iban,
          last: '-',
        }));
        const accCfg = configs.cash?.accounts;
        if (accCfg && accCfg.length > 0 && accCfg.some(a => a.checked === false)) {
          const allowed = new Set(accCfg.filter(a => a.checked !== false).map(a => a.id));
          list = list.filter(b => allowed.has(b.id));
        }
        return list.slice(0, 4);
      })(),
      products: lowItems.map((p, i) => ({
        id: p.productId ?? p.stockId ?? i,
        name: p.name || p.productName,
        stock: p.quantity ?? p.stockQuantity ?? 0,
        min: p.minStockLevel ?? p.minQuantity ?? 0,
        unit: p.unit || 'adet',
      })).slice(0, 3),
      invoices: invoices.filter(i => !i.isDeleted).slice(0, recentLimit).map((i, idx) => ({
        id: i.invoiceNumber ?? String(i.invoiceId ?? idx),
        customer: i.customerName,
        date: i.dueDate
          ? new Date(i.dueDate).toLocaleDateString('tr-TR')
          : i.createdAt ? new Date(i.createdAt).toLocaleDateString('tr-TR') : '-',
        status: i.paymentStatus === 'paid' ? 'onaylı' : i.paymentStatus === 'overdue' ? 'gecikmiş' : i.paymentStatus === 'draft' ? 'taslak' : 'beklemede',
        total: Number(i.totalAmount || 0),
      })),
      openInvoicesTotal: invoices
        .filter(i => (i.paymentStatus === 'pending' || i.paymentStatus === 'overdue') && !i.isDeleted)
        .reduce((s, i) => s + Number(i.totalAmount || 0), 0),
      openInvoicesCount: invoices.filter(i => i.paymentStatus === 'overdue' && !i.isDeleted).length,
      _invoices: invoices,
      _transactions: txns,
      _templates: templates,
    };
  }, [txns, banks, lowItems, invoices, templates, configs]);

  // Layout state
  const [layoutId, setLayoutId] = useState(null);
  const [layoutKey, setLayoutKey] = useState(() => {
    try { return localStorage.getItem('mp.dashboard.layoutKey') || DEFAULT_LAYOUT; } catch { return DEFAULT_LAYOUT; }
  });
  const [widgetsData, setWidgetsData] = useState([]);
  const [detail, setDetail] = useState(null);
  const [pickerSlot, setPickerSlot] = useState(null);
  const [pickerQuery, setPickerQuery] = useState('');
  const [customDefs, setCustomDefs] = useState([]);
  const [layoutSelectorOpen, setLayoutSelectorOpen] = useState(false);
  const [configOpen, setConfigOpen] = useState(null);

  // Backend hydration
  useEffect(() => {
    dashboardService.getDefaultLayout().then(res => {
      setLayoutId(res.layoutId);
      setWidgetsData(res.widgets || []);
      if (res.layoutPreset && LAYOUT_PRESETS[res.layoutPreset]) {
        setLayoutKey(res.layoutPreset);
        try { localStorage.setItem('mp.dashboard.layoutKey', res.layoutPreset); } catch { /* ignore */ }
      }
      if (res.widgets) {
        const backendConfigs = {};
        res.widgets.forEach(w => {
          const fid = FRONTEND_ID[w.widgetType];
          if (fid && w.config) {
            try { backendConfigs[fid] = JSON.parse(w.config); } catch { /* ignore */ }
          }
        });
        if (Object.keys(backendConfigs).length) {
          setConfigs(prev => ({ ...prev, ...backendConfigs }));
        }
      }
    }).catch(console.error);
  }, []);

  // Widget-builder'da oluşturulan özel widget tanımları (picker'da göstermek için)
  useEffect(() => {
    dashboardService.getWidgetDefinitions()
      .then(list => setCustomDefs(Array.isArray(list) ? list.filter(d => !d.isSystem) : []))
      .catch(() => { /* ignore */ });
  }, []);

  // Slot → widget map. Built-in widget aynı tipten birden fazla olamayacağı için
  // backend'deki widget kaydını widget_type üzerinden frontend ID'ye dönüştürürüz.
  // Custom (DATA_*) widget'lar widgetId ile referans verilir.
  const slotAssignments = useMemo(() => {
    const map = {};
    widgetsData.forEach(w => {
      if (w.slotIndex == null) return;
      if (isCustomWidgetType(w.widgetType)) {
        map[w.slotIndex] = { type: 'custom', widget: w };
      } else {
        const fid = FRONTEND_ID[w.widgetType];
        if (fid) map[w.slotIndex] = { type: 'builtin', id: fid, widget: w };
      }
    });
    return map;
  }, [widgetsData]);

  const layout = LAYOUT_PRESETS[layoutKey] || LAYOUT_PRESETS[DEFAULT_LAYOUT];

  // Aksiyonlar
  const removeWidget = async (assignment) => {
    const w = assignment?.widget;
    if (!w || !layoutId) return;
    try {
      await dashboardService.deleteWidget(layoutId, w.widgetId);
      setWidgetsData(prev => prev.filter(x => x.widgetId !== w.widgetId));
      toast.ok('Widget kaldırıldı');
    } catch (e) {
      console.error(e);
      toast.err('Silinemedi');
    }
  };

  const refreshWidget = (assignment) => {
    if (assignment?.type === 'builtin') {
      const def = WIDGET_DEF[assignment.id];
      if (!def?.queryKeys) return;
      def.queryKeys.forEach(qk => qc.invalidateQueries({ queryKey: qk }));
      toast.ok(`${def.name} yenileniyor`);
    } else if (assignment?.type === 'custom' && layoutId && assignment.widget) {
      qc.invalidateQueries({ queryKey: ['widgetData', layoutId, assignment.widget.widgetId] });
      toast.ok('Widget yenileniyor');
    }
  };

  const applyConfig = async (id, nextConfig) => {
    const merged = { ...configs[id], ...nextConfig };
    const newConfigs = { ...configs, [id]: merged };
    saveConfigs(newConfigs);
    try {
      const widget = widgetsData.find(w => w.widgetType === BACKEND_TYPE[id]);
      if (widget && layoutId) {
        await dashboardService.updateWidget(layoutId, widget.widgetId, {
          widgetType: BACKEND_TYPE[id],
          slotIndex: widget.slotIndex,
          config: JSON.stringify(merged),
        });
      }
    } catch (e) { console.error(e); }
  };

  // Layout değişimi — backend tüm widget'ları siler
  const changeLayout = async (newKey) => {
    if (!layoutId) return;
    try {
      const res = await dashboardService.updateLayout(layoutId, {
        name: 'Ana Dashboard',
        isDefault: true,
        layoutPreset: newKey,
      });
      setLayoutKey(newKey);
      setWidgetsData(res.widgets || []);
      try { localStorage.setItem('mp.dashboard.layoutKey', newKey); } catch { /* ignore */ }
      setLayoutSelectorOpen(false);
      toast.ok(`Layout "${LAYOUT_PRESETS[newKey].name}" olarak değiştirildi`);
    } catch (e) {
      console.error(e);
      toast.err('Layout değiştirilemedi');
    }
  };

  // Widget ekleme — pickerSlot dolu olduğunda picker açık demektir
  const assignWidgetToSlot = async (widgetId) => {
    if (!layoutId || pickerSlot == null) return;
    const def = WIDGET_DEF[widgetId];
    if (!def) return;
    const merged = { ...(configs[widgetId] || def.defaultConfig || {}) };
    try {
      const dto = {
        widgetType: BACKEND_TYPE[widgetId],
        slotIndex: pickerSlot,
        positionX: 0,
        positionY: 0,
        width: 4,
        height: 3,
        config: JSON.stringify(merged),
      };
      const w = await dashboardService.addWidget(layoutId, dto);
      setWidgetsData(prev => [...prev, w]);
      saveConfigs({ ...configs, [widgetId]: merged });
      toast.ok(`${def.name} eklendi`);
      closePicker();
    } catch (e) {
      console.error(e);
      toast.err(e?.response?.data?.message || 'Widget eklenemedi');
    }
  };

  // Custom (widget-builder) widget'ı slota ekle
  const assignCustomToSlot = async (def) => {
    if (!layoutId || pickerSlot == null) return;
    const slot = layout.slots.find(s => s.id === pickerSlot);
    try {
      const w = await dashboardService.addWidget(layoutId, {
        widgetType: def.widgetType,
        definitionId: def.definitionId,
        title: def.name,
        slotIndex: pickerSlot,
        positionX: 0,
        positionY: 0,
        width: 4,
        height: 3,
        config: JSON.stringify({ variant: slotSizeToVariant(slot?.size) || 'm' }),
      });
      setWidgetsData(prev => [...prev, w]);
      toast.ok(`${def.name} eklendi`);
      closePicker();
    } catch (e) {
      console.error(e);
      toast.err(e?.response?.data?.message || 'Widget eklenemedi');
    }
  };

  const closePicker = () => { setPickerSlot(null); setPickerQuery(''); };

  // Picker listesi — arama + zaten eklenmiş built-in'leri ele
  const norm = (s) => (s || '').toLocaleLowerCase('tr');
  const pickerBuiltins = useMemo(() => {
    const q = norm(pickerQuery);
    return WIDGET_REGISTRY
      .filter(w => !Object.values(slotAssignments).some(a => a.type === 'builtin' && a.id === w.id))
      .filter(w => !q || norm(w.name).includes(q) || norm(w.desc).includes(q));
  }, [pickerQuery, slotAssignments]);
  const pickerCustoms = useMemo(() => {
    const q = norm(pickerQuery);
    return customDefs.filter(d => !q || norm(d.name).includes(q) || norm(d.description).includes(q));
  }, [pickerQuery, customDefs]);

  // Render helpers
  const renderBuiltin = (assignment, slot) => {
    const def = WIDGET_DEF[assignment.id];
    if (!def) return null;
    const WidgetComp = WIDGET_COMPONENTS[assignment.id];
    if (!WidgetComp) return null;

    const variant = slotSizeToVariant(slot.size);
    const cardSize = slotSizeToCardSize(slot.size);

    const headerExtra =
      assignment.id === 'lowstock' ? lowstockExtra(D) :
      assignment.id === 'tasks' ? tasksExtra() :
      null;

    return (
      <DashboardCard
        def={def}
        size={cardSize}
        headerExtra={headerExtra}
        onRemove={() => removeWidget(assignment)}
        onDetail={() => setDetail(assignment.id)}
        onRefresh={() => refreshWidget(assignment)}
        onConfig={() => setConfigOpen(assignment.id)}
        onNav={onNav}
      >
        <WidgetComp D={D} onNav={onNav} mode="card" variant={variant} config={configs[assignment.id]} />
      </DashboardCard>
    );
  };

  const renderCustom = (assignment, slot) => {
    const w = assignment.widget;
    const def = {
      id: `custom-${w.widgetId}`,
      name: w.title || 'Widget',
      sub: null,
      icon: 'chart',
      backendType: w.widgetType,
    };
    const cardSize = slotSizeToCardSize(slot.size);
    const variant = slotSizeToVariant(slot.size);

    return (
      <DashboardCard
        def={def}
        size={cardSize}
        onRemove={() => removeWidget(assignment)}
        onRefresh={() => refreshWidget(assignment)}
        onNav={onNav}
      >
        <DataWidgetWrapper
          layoutId={layoutId}
          widgetId={w.widgetId}
          visualConfig={w.visualConfig}
          mode="card"
          variant={variant}
        />
      </DashboardCard>
    );
  };

  const detailDef = detail ? WIDGET_DEF[detail] : null;
  const DetailComp = detail ? WIDGET_COMPONENTS[detail] : null;
  const configDef = configOpen ? WIDGET_DEF[configOpen] : null;

  return (
    <div className="page dashboard-page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Hoş geldin{user?.firstName ? `, ${user.firstName}` : ''}</h1>
          <p className="page-sub">
            Bugün <b>{new Date().toLocaleDateString('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' })}</b>
          </p>
        </div>
        <div className="page-actions">
          <button className="btn ghost" onClick={() => setLayoutSelectorOpen(true)}>
            <Icon name="grip" size={14} /> Layout
          </button>
          <button className="btn ghost" onClick={() => onNav('/widgets')}>
            <Icon name="folder" size={14} /> Widgetlarım
          </button>
          <button className="btn ghost" onClick={() => onNav('/widget-builder')}>
            <Icon name="grid" size={14} /> Widget Oluştur
          </button>
          <button className="btn primary" onClick={() => setAiPanelOpen(true)}>
            <Icon name="sparkle" size={14} /> AI ile Oluştur
          </button>
        </div>
      </div>

      {/* Beyanname reminder banner */}
      <BeyannameReminderWidget />

      {/* Slot grid */}
      <div
        className="dashboard-grid"
        style={{
          display: 'grid',
          gap: 12,
          gridTemplateColumns: `repeat(${layout.cols}, 1fr)`,
          gridTemplateRows: `repeat(${layout.rows}, ${layout.rowHeight})`,
        }}
      >
        {layout.slots.map(slot => {
          const assignment = slotAssignments[slot.id];
          if (!assignment) {
            return <EmptySlot key={slot.id} slot={slot} onAdd={setPickerSlot} />;
          }
          return (
            <FilledSlot key={slot.id} slot={slot}>
              {assignment.type === 'builtin' ? renderBuiltin(assignment, slot) : renderCustom(assignment, slot)}
            </FilledSlot>
          );
        })}
      </div>

      {/* Detail modal */}
      {detailDef && DetailComp && (
        <div className="dash-detail-scrim" onClick={() => setDetail(null)}>
          <div className="dash-detail" onClick={e => e.stopPropagation()}>
            <div className="dash-detail-h">
              <div>
                <h2>{detailDef.name}</h2>
                {detailDef.sub && <div className="sub">{detailDef.sub}</div>}
              </div>
              <button className="tb-icon-btn" onClick={() => setDetail(null)}><Icon name="x" /></button>
            </div>
            <div className="dash-detail-b">
              <DetailComp D={D} onNav={onNav} mode="detail" config={configs[detail]} />
            </div>
          </div>
        </div>
      )}

      {/* Widget picker — slot tıklayınca açılır */}
      {pickerSlot != null && (
        <div className="cmdk-scrim" onClick={closePicker}>
          <div className="cmdk widget-picker" onClick={e => e.stopPropagation()}>
            <div className="cmdk-input">
              <Icon name="plus" size={16} style={{ color: 'var(--ink-3)' }} />
              <input
                autoFocus
                placeholder="Widget ara..."
                value={pickerQuery}
                onChange={e => setPickerQuery(e.target.value)}
              />
              <button className="tb-icon-btn" onClick={closePicker} aria-label="Kapat">
                <Icon name="x" size={16} />
              </button>
            </div>
            <div className="cmdk-list">
              <div className="cmdk-section">Slot {pickerSlot} için widget seçin</div>
              <div className="wp-grid">
                {pickerBuiltins.map(w => (
                  <button key={w.id} className="wp-card" onClick={() => assignWidgetToSlot(w.id)}>
                    <div className="wp-card-head">
                      <span className="wp-card-icon"><Icon name={w.icon} size={13} /></span>
                      <span className="wp-card-title">{w.name}</span>
                    </div>
                    <div className="wp-card-body">
                      <BuiltinPreview id={w.id} D={D} config={configs[w.id]} />
                    </div>
                    {w.desc && <div className="wp-card-desc">{w.desc}</div>}
                  </button>
                ))}
                {pickerCustoms.map(def => (
                  <button key={def.definitionId} className="wp-card" onClick={() => assignCustomToSlot(def)}>
                    <div className="wp-card-head">
                      <span className="wp-card-icon"><Icon name="grid" size={13} /></span>
                      <span className="wp-card-title">{def.name}</span>
                      <span className="wp-card-tag">Özel</span>
                    </div>
                    <div className="wp-card-body">
                      <CustomPreview def={def} />
                    </div>
                    {def.description && <div className="wp-card-desc">{def.description}</div>}
                  </button>
                ))}
              </div>
              {pickerBuiltins.length === 0 && pickerCustoms.length === 0 && (
                <div className="empty" style={{ padding: 24 }}>Eşleşen widget yok</div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Layout selector */}
      {layoutSelectorOpen && (
        <LayoutSelector
          currentLayout={layoutKey}
          onSelect={changeLayout}
          onClose={() => setLayoutSelectorOpen(false)}
          hasWidgets={widgetsData.length > 0}
        />
      )}

      {/* Config modal */}
      {configDef && (
        <WidgetConfigModal
          def={configDef}
          config={configs[configOpen]}
          onSave={(next) => { applyConfig(configOpen, next); setConfigOpen(null); }}
          onClose={() => setConfigOpen(null)}
        />
      )}

      {/* AI Widget Chat Panel */}
      {aiPanelOpen && (
        <AiWidgetChatPanel onClose={() => setAiPanelOpen(false)} />
      )}
    </div>
  );
}

// Picker'da built-in widget'ın gerçek kompakt halini canlı render eder
function BuiltinPreview({ id, D, config }) {
  const Comp = WIDGET_COMPONENTS[id];
  if (!Comp) return <div className="wp-skel">—</div>;
  return <Comp D={D} onNav={() => {}} mode="card" variant="s" config={config} />;
}

// Picker'da custom (widget-builder) widget'ın önizlemesini previewQuery ile çeker
function CustomPreview({ def }) {
  const [st, setSt] = useState({ loading: true });
  useEffect(() => {
    let alive = true;
    if (!def?.queryConfig) { setSt({ loading: false, error: true }); return; }
    dashboardService.previewQuery(def.queryConfig)
      .then(res => { if (alive) setSt(res?.success
        ? { loading: false, data: res.data, columns: res.columns }
        : { loading: false, error: true }); })
      .catch(() => { if (alive) setSt({ loading: false, error: true }); });
    return () => { alive = false; };
  }, [def]);

  if (st.loading) return <div className="wp-skel">Önizleme yükleniyor…</div>;
  if (st.error || !st.data || st.data.length === 0) return <div className="wp-skel">Önizleme yok</div>;
  return (
    <DataWidget
      data={{ data: st.data, columns: st.columns }}
      config={{ visualConfig: def.visualConfig || {} }}
      mode="card"
      variant="s"
    />
  );
}

function DataWidgetWrapper({ layoutId, widgetId, visualConfig, mode, variant }) {
  const { data, isLoading } = useWidgetData(layoutId, widgetId, !!layoutId && !!widgetId);
  if (isLoading) return <div className="empty">Yükleniyor...</div>;
  if (!data) return <div className="empty">Veri yok</div>;
  if (data.error) return <div className="empty">Hata: {data.message || 'Sorgu çalıştırılamadı'}</div>;
  const cfg = { visualConfig: data.visualConfig || visualConfig || {} };
  return <DataWidget data={data} config={cfg} mode={mode} variant={variant} />;
}

function buildAgingLabels(thresholds) {
  const labels = [];
  let prev = 0;
  for (const t of thresholds) {
    labels.push(`${prev + 1}–${t} gün`);
    prev = t;
  }
  labels.push(`${prev + 1}+ gün`);
  return labels;
}
