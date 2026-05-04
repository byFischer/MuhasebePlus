import React, { useState, useMemo, useRef, useEffect, useLayoutEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import Icon from '@/components/mp/Icon';
import { toast } from '@/lib/toast';
import { useInvoices } from '@/hooks/useInvoices';
import { useBankAccounts } from '@/hooks/useBankAccounts';
import { useLowStock } from '@/hooks/useStock';
import { useTransactions } from '@/hooks/useTransactions';
import dashboardService from '@/services/dashboardService';
import { useAuth } from '@/context/AuthContext';

import { WIDGET_REGISTRY, BACKEND_TYPE, FRONTEND_ID, WIDGET_DEF, DEFAULT_ORDER } from '@/widgets/registry';
import DashboardCard from '@/widgets/shell/DashboardCard';
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
import WidgetConfigModal from '@/widgets/shell/WidgetConfigModal';

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
};

export default function DashboardPage() {
  const onNav = useNavigate();
  const qc = useQueryClient();
  const { data: invoices = [] } = useInvoices();
  const { data: banks = [] } = useBankAccounts();
  const { data: lowItems = [] } = useLowStock();
  const { data: txns = [] } = useTransactions();
  const { user } = useAuth();

  // ─── Widget configs (id -> object) ─────────────────────────────────────────
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

  const applyConfig = async (id, nextConfig) => {
    const merged = { ...configs[id], ...nextConfig };
    const newConfigs = { ...configs, [id]: merged };
    saveConfigs(newConfigs);
    try {
      const widget = widgetsData.find(w => w.widgetType === BACKEND_TYPE[id]);
      if (widget && layoutId) {
        await dashboardService.updateWidget(layoutId, widget.widgetId, {
          widgetType: BACKEND_TYPE[id],
          config: JSON.stringify(merged),
        });
      }
    } catch (e) { console.error(e); }
  };

  // ─── Computed dashboard data ───────────────────────────────────────────────
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
    invoices.filter(i => i.paymentStatus !== 'PAID' && !i.isDeleted).forEach(i => {
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
        status: i.paymentStatus === 'PAID' ? 'onaylı' : i.paymentStatus === 'OVERDUE' ? 'gecikmiş' : 'beklemede',
        total: Number(i.totalAmount || 0),
      })),
      openInvoicesTotal: invoices
        .filter(i => (i.paymentStatus === 'UNPAID' || i.paymentStatus === 'OVERDUE') && !i.isDeleted)
        .reduce((s, i) => s + Number(i.totalAmount || 0), 0),
      openInvoicesCount: invoices.filter(i => i.paymentStatus === 'OVERDUE' && !i.isDeleted).length,
      _invoices: invoices,
      _transactions: txns,
    };
  }, [txns, banks, lowItems, invoices, configs]);

  // ─── Layout state ──────────────────────────────────────────────────────────
  const [layoutId, setLayoutId] = useState(null);
  const [widgetsData, setWidgetsData] = useState([]);
  const [order, setOrder] = useState(() => {
    try {
      const saved = localStorage.getItem('mp.dashboard.order');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch { /* ignore */ }
    return DEFAULT_ORDER;
  });
  const [detail, setDetail] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const [preview, setPreview] = useState(null);
  const [dragging, setDragging] = useState(null);
  const [hoverId, setHoverId] = useState(null);
  const [editMode, setEditMode] = useState(() => {
    try { return localStorage.getItem('mp.dashboard.editMode') === 'true'; } catch { return false; }
  });
  const [configOpen, setConfigOpen] = useState(null); // widget id

  const cardRefs = useRef({});
  const flipBefore = useRef({});
  const swapThrottle = useRef(0);
  const widgetsRef = useRef(widgetsData);
  useEffect(() => { widgetsRef.current = widgetsData; }, [widgetsData]);

  useEffect(() => {
    dashboardService.getDefaultLayout().then(res => {
      setLayoutId(res.layoutId);
      setWidgetsData(res.widgets || []);
      // hydrate configs from backend if available
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

  useEffect(() => {
    try { localStorage.setItem('mp.dashboard.order', JSON.stringify(order)); } catch { /* ignore */ }
  }, [order]);

  useEffect(() => {
    try { localStorage.setItem('mp.dashboard.editMode', String(editMode)); } catch { /* ignore */ }
  }, [editMode]);

  // ─── FLIP reorder animation ────────────────────────────────────────────────
  useLayoutEffect(() => {
    const before = flipBefore.current;
    if (!Object.keys(before).length) return;
    flipBefore.current = {};

    Object.entries(cardRefs.current).forEach(([id, el]) => {
      if (!el || dragging?.id === id) return;
      const oldR = before[id];
      if (!oldR) return;
      const newR = el.getBoundingClientRect();
      const dx = oldR.left - newR.left;
      const dy = oldR.top - newR.top;
      if (Math.abs(dx) < 1 && Math.abs(dy) < 1) return;
      el.style.transition = 'none';
      el.style.transform = `translate(${dx}px, ${dy}px)`;
      el.getBoundingClientRect(); // force reflow
      requestAnimationFrame(() => {
        if (!cardRefs.current[id]) return;
        el.style.transition = 'transform 0.35s cubic-bezier(0.16, 1, 0.3, 1)';
        el.style.transform = '';
        el.addEventListener('transitionend', function h() {
          el.style.transition = '';
          el.removeEventListener('transitionend', h);
        });
      });
    });
  }, [order, dragging]);

  // ─── Backend persistence ───────────────────────────────────────────────────
  const saveReorder = useCallback(async (newOrder) => {
    if (!layoutId) return;
    const data = widgetsRef.current;
    const widgetIds = newOrder
      .map(id => data.find(w => w.widgetType === BACKEND_TYPE[id])?.widgetId)
      .filter(Boolean);
    try {
      if (widgetIds.length > 0) await dashboardService.reorderWidgets(layoutId, widgetIds);
    } catch (e) { console.error(e); }
  }, [layoutId]);

  const removeCard = async (id) => {
    setOrder(o => o.filter(x => x !== id));
    toast.ok(`${WIDGET_DEF[id]?.name} dashboardtan kaldırıldı`);
    try {
      const widget = widgetsData.find(w => w.widgetType === BACKEND_TYPE[id]);
      if (widget && layoutId) {
        await dashboardService.deleteWidget(layoutId, widget.widgetId);
        setWidgetsData(prev => prev.filter(w => w.widgetId !== widget.widgetId));
      }
    } catch (e) { console.error(e); }
  };

  const addWidget = async (id) => {
    if (order.includes(id)) { toast.error('Bu widget zaten ekli'); return; }
    const newOrder = [id, ...order];
    setOrder(newOrder);
    setPreview(null);
    setAddOpen(false);
    toast.ok(`${WIDGET_DEF[id]?.name} dashboarda eklendi`);
    try {
      if (layoutId) {
        const dto = { widgetType: BACKEND_TYPE[id], positionX: 0, positionY: 0, width: 1, height: 1 };
        const w = await dashboardService.addWidget(layoutId, dto);
        setWidgetsData(prev => {
          const updated = [...prev, w];
          widgetsRef.current = updated;
          return updated;
        });
      }
    } catch (e) { console.error(e); }
  };

  const refreshWidget = (id) => {
    const def = WIDGET_DEF[id];
    if (!def || !def.queryKeys || def.queryKeys.length === 0) return;
    def.queryKeys.forEach(qk => qc.invalidateQueries({ queryKey: qk }));
    toast.ok(`${def.name} yenileniyor`);
  };

  // ─── Drag-to-reorder ──────────────────────────────────────────────────────
  const onPointerDown = (id) => (e) => {
    if (!editMode) return;
    if (e.button !== 0) return;
    if (e.target.closest('.mac-btn')) return;
    if (!e.target.closest('.drag-handle')) return;
    e.preventDefault();
    const node = cardRefs.current[id];
    if (!node) return;
    const rect = node.getBoundingClientRect();
    const snap = {};
    Object.entries(cardRefs.current).forEach(([cid, el]) => { if (el) snap[cid] = el.getBoundingClientRect(); });
    flipBefore.current = snap;
    setDragging({
      id, x: e.clientX, y: e.clientY,
      offsetX: e.clientX - rect.left, offsetY: e.clientY - rect.top,
      w: rect.width, h: rect.height,
    });
    document.body.style.userSelect = 'none';
  };

  useEffect(() => {
    if (!dragging) return;
    let raf = null;
    const onMove = (e) => {
      const ex = e.clientX, ey = e.clientY;
      setDragging(d => d ? { ...d, x: ex, y: ey } : d);
      if (raf) return;
      raf = requestAnimationFrame(() => {
        raf = null;
        if (Date.now() - swapThrottle.current < 200) return;
        const cardCenterX = ex - dragging.offsetX + dragging.w / 2;
        const cardCenterY = ey - dragging.offsetY + dragging.h / 2;
        let targetId = null, bestDist = Infinity;
        Object.entries(cardRefs.current).forEach(([cid, el]) => {
          if (!el || cid === dragging.id) return;
          const r = el.getBoundingClientRect();
          const cx = r.left + r.width / 2, cy = r.top + r.height / 2;
          const dist = Math.hypot(cardCenterX - cx, cardCenterY - cy);
          const triggerRadius = Math.min(r.width, r.height) * 0.45;
          if (dist < triggerRadius && dist < bestDist) { bestDist = dist; targetId = cid; }
        });
        if (targetId) {
          const snap = {};
          Object.entries(cardRefs.current).forEach(([cid, el]) => { if (el) snap[cid] = el.getBoundingClientRect(); });
          flipBefore.current = snap;
          setOrder(o => {
            if (!o.includes(dragging.id) || !o.includes(targetId)) return o;
            const next = o.filter(x => x !== dragging.id);
            next.splice(next.indexOf(targetId), 0, dragging.id);
            return next;
          });
          swapThrottle.current = Date.now();
        }
      });
    };
    const onUp = () => {
      if (raf) cancelAnimationFrame(raf);
      Object.values(cardRefs.current).forEach(el => {
        if (el) { el.style.transition = ''; el.style.transform = ''; }
      });
      setDragging(null);
      setHoverId(null);
      document.body.style.userSelect = '';
      setOrder(o => { saveReorder(o); return o; });
    };
    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
    return () => {
      if (raf) cancelAnimationFrame(raf);
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };
  }, [dragging?.id, saveReorder]);

  // ─── Card renderer ─────────────────────────────────────────────────────────
  const Card = ({ id }) => {
    const def = WIDGET_DEF[id];
    if (!def) return null;
    const WidgetComp = WIDGET_COMPONENTS[id];
    if (!WidgetComp) return null;

    const headerExtra =
      id === 'lowstock' ? lowstockExtra(D) :
      id === 'tasks'    ? tasksExtra() :
      null;

    return (
      <DashboardCard
        def={def}
        isDragging={dragging?.id === id}
        dragging={dragging}
        isHover={hoverId === id}
        editMode={editMode}
        headerExtra={headerExtra}
        cardRef={el => { if (el) cardRefs.current[id] = el; }}
        onPointerDown={onPointerDown(id)}
        onRemove={() => removeCard(id)}
        onDetail={() => setDetail(id)}
        onRefresh={() => refreshWidget(id)}
        onConfig={() => setConfigOpen(id)}
        onNav={onNav}
      >
        <WidgetComp D={D} onNav={onNav} mode="card" config={configs[id]} />
      </DashboardCard>
    );
  };

  // ─── Render ────────────────────────────────────────────────────────────────
  const detailDef = detail ? WIDGET_DEF[detail] : null;
  const DetailComp = detail ? WIDGET_COMPONENTS[detail] : null;
  const previewDef = preview ? WIDGET_DEF[preview] : null;
  const PreviewComp = preview ? WIDGET_COMPONENTS[preview] : null;
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
          <button
            className={`tb-icon-btn ${editMode ? 'on' : ''}`}
            title={editMode ? 'Düzenlemeyi kapat' : 'Dashboardı düzenle'}
            onClick={() => setEditMode(e => !e)}
            style={{ borderColor: editMode ? 'var(--accent)' : 'transparent' }}
          >
            <Icon name={editMode ? 'unlock' : 'lock'} size={16} />
          </button>
          <button className="btn primary" onClick={() => setAddOpen(true)}>
            <Icon name="plus" /> Widget Ekle
          </button>
        </div>
      </div>

      {/* Flat CSS grid — size handled via data-size on each card */}
      <div className="dash-grid" data-dragging={!!dragging} data-edit-mode={editMode}>
        {order.map(id => <Card key={id} id={id} />)}
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

      {/* Widget picker */}
      {addOpen && !preview && (
        <div className="cmdk-scrim" onClick={() => setAddOpen(false)}>
          <div className="cmdk widget-picker" onClick={e => e.stopPropagation()}>
            <div className="cmdk-input">
              <Icon name="plus" size={16} style={{ color: 'var(--ink-3)' }} />
              <input autoFocus placeholder="Widget ara..." />
              <kbd>esc</kbd>
            </div>
            <div className="cmdk-list">
              <div className="cmdk-section">Eklenebilir Widget'lar</div>
              {WIDGET_REGISTRY.filter(w => !order.includes(w.id)).map(w => (
                <button key={w.id} className="cmdk-item widget-pick-item" onClick={() => setPreview(w.id)}>
                  <div className="widget-item-icon"><Icon name={w.icon} size={14} /></div>
                  <div style={{ flex: 1, textAlign: 'left' }}>
                    <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink)' }}>{w.name}</div>
                    <div style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 2 }}>{w.desc}</div>
                  </div>
                  <Icon name="chevRight" size={12} style={{ color: 'var(--ink-3)' }} />
                </button>
              ))}
              {WIDGET_REGISTRY.filter(w => order.includes(w.id)).length > 0 && (
                <>
                  <div className="cmdk-section" style={{ marginTop: 8 }}>Zaten Ekli</div>
                  {WIDGET_REGISTRY.filter(w => order.includes(w.id)).map(w => (
                    <div key={w.id} className="cmdk-item widget-pick-item" style={{ opacity: 0.5, cursor: 'default' }}>
                      <div className="widget-item-icon"><Icon name={w.icon} size={14} /></div>
                      <div style={{ flex: 1, textAlign: 'left' }}>
                        <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink)' }}>{w.name}</div>
                        <div style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 2 }}>{w.desc}</div>
                      </div>
                      <span className="pill pos"><span className="dot" />Eklendi</span>
                    </div>
                  ))}
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Widget preview before adding */}
      {previewDef && PreviewComp && (
        <div className="dash-detail-scrim" onClick={() => setPreview(null)}>
          <div className="dash-detail" onClick={e => e.stopPropagation()} style={{ maxWidth: 720 }}>
            <div className="dash-detail-h">
              <div>
                <div className="muted" style={{ fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Önizleme</div>
                <h2>{previewDef.name}</h2>
                <div className="sub">{previewDef.desc}</div>
              </div>
              <button className="tb-icon-btn" onClick={() => setPreview(null)}><Icon name="x" /></button>
            </div>
            <div className="dash-detail-b" style={{ background: 'var(--bg)' }}>
              <div className="dash-card preview-card">
                {previewDef.name && (
                  <div className="card-h">
                    <div>
                      <h3>{previewDef.name}</h3>
                      {previewDef.sub && <div className="sub">{previewDef.sub}</div>}
                    </div>
                  </div>
                )}
                <div className="card-b">
                  <PreviewComp D={D} onNav={onNav} mode="card" config={configs[preview]} />
                </div>
              </div>
            </div>
            <div className="dash-detail-f">
              <button className="btn ghost" onClick={() => setPreview(null)}>Vazgeç</button>
              <button className="btn primary" onClick={() => addWidget(preview)}>
                <Icon name="plus" /> Dashboarda Ekle
              </button>
            </div>
          </div>
        </div>
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
    </div>
  );
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
