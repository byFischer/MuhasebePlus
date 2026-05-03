import React, { useState as dS, useMemo as dM, useRef as dR, useEffect as dE, useCallback as dC } from 'react';
import { useNavigate } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import Donut from '@/components/mp/charts/Donut';
import BarLineChart from '@/components/mp/charts/BarLineChart';
import Spark from '@/components/mp/charts/Spark';
import { TRY } from '@/lib/format';
import { toast } from '@/lib/toast';
import { useInvoices } from '@/hooks/useInvoices';
import { useBankAccounts } from '@/hooks/useBankAccounts';
import { useLowStock } from '@/hooks/useStock';
import { useTransactions } from '@/hooks/useTransactions';
import dashboardService from '@/services/dashboardService';
import { useAuth } from '@/context/AuthContext';

const WIDGET_CATALOG = [
  { id: 'kpis',     name: 'KPI Kartları',         desc: 'Gelir, gider, net kar, açık alacak — tek bakışta',     size: 'full', icon: 'chart' },
  { id: 'chart',    name: 'Gelir & Gider Trendi', desc: '12 aylık karşılaştırmalı bar/çizgi grafik',            size: 'wide', icon: 'chart' },
  { id: 'cash',     name: 'Nakit Pozisyonu',      desc: 'Tüm banka hesaplarının donut dağılımı',                size: 'narrow', icon: 'bank' },
  { id: 'aging',    name: 'Cari Yaşlandırma',     desc: 'Açık alacakların 0-30, 31-60, 61-90, 90+ kırılımı',    size: 'third', icon: 'users' },
  { id: 'lowstock', name: 'Düşük Stok Uyarıları', desc: 'Min eşiğin altındaki ürünler ve sipariş aksiyonları',  size: 'third', icon: 'box' },
  { id: 'quick',    name: 'Hızlı İşlemler',       desc: 'Sık kullanılan işlemler için kısayollar',              size: 'third', icon: 'flash' },
  { id: 'recent',   name: 'Son Faturalar',        desc: 'En son kesilen faturalar listesi',                     size: 'wide', icon: 'invoice' },
  { id: 'tasks',    name: 'Yaklaşan Görevler',    desc: 'Planlanmış işlemler ve hatırlatmalar',                 size: 'narrow', icon: 'clock' },
];

const WIDGET_MAP = {
  kpis: 'KPI', chart: 'TIME_SERIES', cash: 'PIE', aging: 'TOP_N',
  lowstock: 'ANOMALY', quick: 'QUICK_ACTIONS', recent: 'ACTIVITY_FEED', tasks: 'DAILY_BRIEF'
};
const REV_MAP = Object.fromEntries(Object.entries(WIDGET_MAP).map(([k,v])=>[v,k]));

function Kpi({ label, value, delta, data, pos, warn, color }) {
  return (
    <div className="kpi widget">
      <div className="kpi-label"><span>{label}</span></div>
      <div className="kpi-val"><span className="cur">₺</span>{(value||0).toLocaleString('tr-TR')}</div>
      <div className={`kpi-delta ${pos ? 'pos' : ''}`} style={warn ? { color: 'var(--warn)' } : {}}>
        {pos && <Icon name="arrowUp" size={12}/>}
        {delta}
      </div>
      {data && <div className="kpi-spark"><Spark data={data} color={color || 'var(--accent)'}/></div>}
    </div>
  );
}

function InvoicePill({ status }) {
  const map = {
    'onaylı': { cls: 'pos', l: 'Onaylı' },
    'beklemede': { cls: 'info', l: 'Beklemede' },
    'taslak': { cls: 'warn', l: 'Taslak' },
    'gecikmiş': { cls: 'neg', l: 'Gecikmiş' },
  };
  const s = map[status] || { cls: '', l: status };
  return <span className={`pill ${s.cls}`}><span className="dot"></span>{s.l}</span>;
}

export default function DashboardPage() {
  const onNav = useNavigate();
  const { data: invoices = [] } = useInvoices();
  const { data: banks = [] } = useBankAccounts();
  const { data: lowItems = [] } = useLowStock();
  const { data: txns = [] } = useTransactions();
  const { user } = useAuth();

  const D = dM(() => {
    const now = new Date();
    const months = [];
    const revenue = [];
    const expense = [];

    // Son 12 ayı gerçek transaction verisiyle hesapla
    for (let i = 11; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      months.push(d.toLocaleDateString('tr-TR', { month: 'short' }));
      const mTxns = txns.filter(t => {
        const td = new Date(t.transactionDate || t.createdAt);
        return !isNaN(td.getTime()) && td.getFullYear() === d.getFullYear() && td.getMonth() === d.getMonth();
      });
      revenue.push(mTxns.filter(t => t.transactionType === 'INCOME').reduce((s, t) => s + Number(t.amount || 0), 0));
      expense.push(mTxns.filter(t => t.transactionType === 'EXPENSE').reduce((s, t) => s + Math.abs(Number(t.amount || 0)), 0));
    }

    const thisRev = revenue[11];
    const thisExp = expense[11];
    const prevRev = revenue[10];
    const prevExp = expense[10];

    const pct = (curr, prev) => {
      if (!prev && !curr) return '—';
      if (!prev) return curr > 0 ? '+∞' : '—';
      const d = ((curr - prev) / prev * 100).toFixed(1);
      return (Number(d) >= 0 ? '+' : '') + d + '%';
    };

    // Cari yaşlandırma: ödenmemiş faturaları vade tarihine göre grupla
    const aging = [0, 0, 0, 0];
    invoices.filter(i => i.paymentStatus !== 'PAID' && !i.isDeleted).forEach(i => {
      const due = new Date(i.dueDate || i.createdAt);
      if (isNaN(due.getTime())) return;
      const days = Math.floor((now - due) / 86400000);
      const amt = Number(i.totalAmount || 0);
      if (days <= 30) aging[0] += amt;
      else if (days <= 60) aging[1] += amt;
      else if (days <= 90) aging[2] += amt;
      else aging[3] += amt;
    });
    const agingTotal = aging.reduce((s, v) => s + v, 0) || 1;

    return {
      revenue, expense, months,
      thisRev, thisExp, prevRev, prevExp,
      revDelta: pct(thisRev, prevRev),
      expDelta: pct(thisExp, prevExp),
      netDelta: pct(thisRev - thisExp, prevRev - prevExp),
      revPos: thisRev >= prevRev,
      netPos: (thisRev - thisExp) >= (prevRev - prevExp),
      aging, agingTotal,
      banks: banks.map((b, i) => ({ id: b.bankAccountId ?? b.accountId ?? i, name: b.bankName || b.accountName, currency: b.currency, balance: Number(b.balance ?? b.currentBalance ?? 0), iban: b.iban, last: '-' })).slice(0, 4),
      products: lowItems.map((p, i) => ({ id: p.productId ?? p.stockId ?? i, name: p.name || p.productName, stock: p.quantity ?? p.stockQuantity ?? 0, min: p.minStockLevel ?? p.minQuantity ?? 0, unit: p.unit || 'adet' })).slice(0, 3),
      invoices: invoices.filter(i => !i.isDeleted).slice(0, 6).map((i, idx) => ({ id: i.invoiceNumber ?? String(i.invoiceId ?? idx), customer: i.customerName, date: i.dueDate ? new Date(i.dueDate).toLocaleDateString('tr-TR') : i.createdAt ? new Date(i.createdAt).toLocaleDateString('tr-TR') : '-', status: i.paymentStatus === 'PAID' ? 'onaylı' : i.paymentStatus === 'OVERDUE' ? 'gecikmiş' : 'beklemede', total: Number(i.totalAmount || 0) })),
      openInvoicesTotal: invoices.filter(i => (i.paymentStatus === 'UNPAID' || i.paymentStatus === 'OVERDUE') && !i.isDeleted).reduce((s, i) => s + Number(i.totalAmount || 0), 0),
      openInvoicesCount: invoices.filter(i => i.paymentStatus === 'OVERDUE' && !i.isDeleted).length,
    };
  }, [txns, banks, lowItems, invoices]);

  const [layoutId, setLayoutId] = dS(null);
  const [widgetsData, setWidgetsData] = dS([]);
  const [order, setOrder] = dS(() => {
    try {
      const saved = localStorage.getItem('mp.dashboard.order');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed;
      }
    } catch(e) { /* ignore */ }
    return ['kpis', 'chart', 'cash', 'aging', 'lowstock', 'quick', 'recent', 'tasks'];
  });
  const [detail, setDetail] = dS(null);
  const [addOpen, setAddOpen] = dS(false);
  const [preview, setPreview] = dS(null);
  const [dragging, setDragging] = dS(null);
  const [hoverId, setHoverId] = dS(null);
  const cardRefs = dR({});
  const flipBefore = dR({});
  const swapThrottle = dR(0);

  dE(() => {
    dashboardService.getDefaultLayout().then(res => {
      setLayoutId(res.layoutId);
      setWidgetsData(res.widgets || []);
      // Intentionally NOT overriding order — localStorage takes precedence over backend order
    }).catch(console.error);
  }, []);

  dE(() => {
    try { localStorage.setItem('mp.dashboard.order', JSON.stringify(order)); } catch(e) {}
  }, [order]);

  const saveReorder = async (newOrder, widgetsOverride) => {
    if (!layoutId) return;
    const data = widgetsOverride || widgetsData;
    const widgetIds = newOrder.map(id => data.find(w => w.widgetType === WIDGET_MAP[id])?.widgetId).filter(Boolean);
    try {
      if (widgetIds.length > 0) await dashboardService.reorderWidgets(layoutId, widgetIds);
    } catch(e) { console.error(e); }
  };

  React.useLayoutEffect(() => {
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
      el.getBoundingClientRect();
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

  const removeCard = async (id) => {
    setOrder(o => o.filter(x => x !== id));
    toast.ok(`${WIDGET_CATALOG.find(w => w.id === id)?.name} dashboardtan kaldırıldı`);
    try {
      const widget = widgetsData.find(w => w.widgetType === WIDGET_MAP[id]);
      if (widget && layoutId) {
        await dashboardService.deleteWidget(layoutId, widget.widgetId);
        setWidgetsData(prev => prev.filter(w => w.widgetId !== widget.widgetId));
      }
    } catch(e) { console.error(e); }
  };

  const addWidget = async (id) => {
    if (order.includes(id)) { toast.error('Bu widget zaten ekli'); return; }
    const newOrder = [id, ...order];
    setOrder(newOrder);
    setPreview(null);
    setAddOpen(false);
    toast.ok(`${WIDGET_CATALOG.find(w => w.id === id)?.name} dashboarda eklendi`);
    try {
      if (layoutId) {
        const dto = { widgetType: WIDGET_MAP[id], positionX: 0, positionY: 0, width: 1, height: 1 };
        const w = await dashboardService.addWidget(layoutId, dto);
        const updatedWidgets = [...widgetsData, w];
        setWidgetsData(updatedWidgets);
        saveReorder(newOrder, updatedWidgets);
      }
    } catch(e) { console.error(e); }
  };

  const onPointerDown = (id) => (e) => {
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

  dE(() => {
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
        let targetId = null;
        let bestDist = Infinity;
        Object.entries(cardRefs.current).forEach(([cid, el]) => {
          if (!el || cid === dragging.id) return;
          const r = el.getBoundingClientRect();
          const cx = r.left + r.width / 2;
          const cy = r.top + r.height / 2;
          const dist = Math.hypot(cardCenterX - cx, cardCenterY - cy);
          const triggerRadius = Math.min(r.width, r.height) * 0.45;
          if (dist < triggerRadius && dist < bestDist) {
            bestDist = dist; targetId = cid;
          }
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
      Object.values(cardRefs.current).forEach(el => { if (el) { el.style.transition = ''; el.style.transform = ''; } });
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
  }, [dragging?.id]);

  const widgets = {
    kpis: {
      title: 'KPI Kartları',
      render: (mode) => (
        <div className={mode === 'detail' ? 'col gap-12' : 'kpis'}>
          <Kpi label="Toplam Gelir (Bu Ay)" value={D.thisRev} delta={D.revDelta} pos={D.revPos} data={D.revenue} />
          <Kpi label="Toplam Gider (Bu Ay)" value={D.thisExp} delta={D.expDelta} data={D.expense} color="var(--ink-3)" />
          <Kpi label="Net Kar" value={D.thisRev - D.thisExp} delta={D.netDelta} pos={D.netPos} data={D.revenue.map((v, i) => v - D.expense[i])} />
          <Kpi label="Açık Alacak" value={D.openInvoicesTotal} delta={`${D.openInvoicesCount} fatura gecikmiş`} warn />
        </div>
      ),
    },
    chart: {
      title: 'Gelir & Gider Trendi',
      sub: 'Son 12 ay · TRY',
      render: () => (
        <>
          <div className="row gap-8" style={{marginBottom: 8}}>
            <span className="pill info"><span className="dot"></span>Gelir</span>
            <span className="pill"><span className="dot"></span>Gider</span>
          </div>
          <BarLineChart months={D.months} revenue={D.revenue} expense={D.expense} />
        </>
      ),
      detail: () => (
        <div className="col gap-12">
          <BarLineChart months={D.months} revenue={D.revenue} expense={D.expense} />
          <div className="grid-3">
            <Kpi label="12 Ay Gelir" value={D.revenue.reduce((s,x)=>s+x,0)} pos data={D.revenue}/>
            <Kpi label="12 Ay Gider" value={D.expense.reduce((s,x)=>s+x,0)} data={D.expense} color="var(--neg)"/>
            <Kpi label="12 Ay Net" value={D.revenue.reduce((s,x)=>s+x,0) - D.expense.reduce((s,x)=>s+x,0)} pos data={D.revenue.map((v,i)=>v-D.expense[i])}/>
          </div>
          <table className="table">
            <thead><tr><th>Ay</th><th className="num">Gelir</th><th className="num">Gider</th><th className="num">Net</th></tr></thead>
            <tbody>
              {D.months.map((m,i)=>(
                <tr key={m}>
                  <td>{m}</td>
                  <td className="num mono tnum" style={{color:'var(--pos)'}}>{TRY(D.revenue[i])}</td>
                  <td className="num mono tnum" style={{color:'var(--neg)'}}>{TRY(D.expense[i])}</td>
                  <td className="num mono tnum"><b>{TRY(D.revenue[i]-D.expense[i])}</b></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ),
    },
    cash: {
      title: 'Nakit Pozisyonu',
      render: () => (
        <div className="col gap-8">
          <div className="row" style={{justifyContent:'center', marginBottom: 4}}>
            {D.banks.length > 0 && <Donut size={200} segments={D.banks.map((b,i)=>({ color: `oklch(${0.55+i*0.1} 0.06 ${240-i*20})`, value: b.balance||0, label: b.name }))} />}
          </div>
          {D.banks.map(b => (
            <div key={b.id} className="row" style={{justifyContent:'space-between', padding: '3px 0', fontSize: 12}}>
              <span style={{color:'var(--ink-2)'}}>{b.name} <span className="muted">· {b.currency}</span></span>
              <span className="mono tnum">{TRY(b.balance||0, { currency: b.currency==='TRY'?'₺':b.currency==='USD'?'$':'€' })}</span>
            </div>
          ))}
        </div>
      ),
      detail: () => (
        <div className="col gap-16">
          <div className="row" style={{justifyContent:'center'}}>
            <Donut size={280} segments={D.banks.map((b,i)=>({color:`oklch(${0.50+i*0.05} 0.10 ${165+i*20})`, value: b.balance||0, label: b.name}))}/>
          </div>
          <table className="table">
            <thead><tr><th>Hesap</th><th>IBAN</th><th>Para</th><th className="num">Bakiye</th><th>Son Hareket</th></tr></thead>
            <tbody>
              {D.banks.map(b=>(
                <tr key={b.id}>
                  <td><b>{b.name}</b></td>
                  <td className="mono muted" style={{fontSize:11}}>{b.iban}</td>
                  <td>{b.currency}</td>
                  <td className="num mono tnum"><b>{TRY(b.balance||0, { currency: b.currency==='TRY'?'₺':b.currency==='USD'?'$':'€' })}</b></td>
                  <td className="muted">{b.last}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ),
    },
    aging: {
      title: 'Cari Yaşlandırma',
      action: () => onNav('/cari'),
      actionLabel: 'Tümü',
      render: () => (
        <div>
          {D.aging.every(v => v === 0) ? (
            <div className="empty">Açık alacak yok 🎉</div>
          ) : (
            [
              { label: '0–30 gün', val: D.aging[0], color: 'var(--accent)' },
              { label: '31–60 gün', val: D.aging[1], color: 'var(--warn)' },
              { label: '61–90 gün', val: D.aging[2], color: 'oklch(0.60 0.13 50)' },
              { label: '90+ gün', val: D.aging[3], color: 'var(--neg)' },
            ].map(r => (
              <div key={r.label} style={{margin: '8px 0'}}>
                <div className="row" style={{justifyContent:'space-between', fontSize:12, marginBottom:4}}>
                  <span>{r.label}</span><span className="mono tnum">{TRY(r.val)}</span>
                </div>
                <div className="bar"><span style={{width:`${r.val/D.agingTotal*100}%`, background: r.color}}/></div>
              </div>
            ))
          )}
        </div>
      ),
    },
    lowstock: {
      title: 'Düşük Stok Uyarıları',
      action: () => onNav('/stok'),
      actionLabel: 'Stoklar',
      headerExtra: <span className="pill neg">{D.products.length}</span>,
      render: () => (
        <div>
          {D.products.length === 0 && <div className="empty">Düşük stok yok 🎉</div>}
          {D.products.map(p => (
            <div key={p.id} className="row" style={{justifyContent:'space-between', padding:'8px 0', borderBottom:'1px solid var(--line)'}}>
              <div>
                <div style={{fontSize:13}}>{p.name}</div>
                <div className="muted" style={{fontSize:11}}>{p.id} · min {p.min} {p.unit}</div>
              </div>
              <div style={{textAlign:'right'}}>
                <div className="mono tnum" style={{color: p.stock===0 ? 'var(--neg)' : 'var(--warn)', fontWeight:600}}>{p.stock} {p.unit}</div>
                <button className="btn sm" style={{marginTop:4}} onClick={()=>onNav('/stok')}>Sipariş</button>
              </div>
            </div>
          ))}
        </div>
      ),
    },
    quick: {
      title: 'Hızlı İşlemler',
      render: () => (
        <div className="qa-grid">
          {[
            { ic:'invoice', l:'Fatura Kes', s:'⌘ N', a:()=>onNav('/fatura') },
            { ic:'users', l:'Müşteri Ekle', s:'⌘ M', a:()=>onNav('/cari') },
            { ic:'swap', l:'Gelir/Gider', s:'⌘ G', a:()=>onNav('/gelir-gider') },
            { ic:'box', l:'Stok Kaydı', s:'⌘ S', a:()=>onNav('/stok') },
            { ic:'template', l:'Şablon Uygula', s:'⌘ T', a:()=>onNav('/sablon') },
            { ic:'chart', l:'Kar-Zarar', s:'⌘ R', a:()=>onNav('/rapor') },
            { ic:'bank', l:'Banka Hareketi', s:'⌘ B', a:()=>onNav('/banka') },
            { ic:'log', l:'Sistem Logları', s:'⌘ L', a:()=>onNav('/log') },
          ].map(q => (
            <button key={q.l} className="qa" onClick={q.a}>
              <div className="qa-icon"><Icon name={q.ic} size={14}/></div>
              <div className="qa-label">{q.l}</div>
              <div className="qa-sub mono">{q.s}</div>
            </button>
          ))}
        </div>
      ),
    },
    recent: {
      title: 'Son Faturalar',
      action: () => onNav('/fatura'),
      actionLabel: 'Tümü',
      noPad: true,
      render: () => (
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Fatura</th><th>Müşteri</th><th>Tarih</th><th>Durum</th><th className="num">Tutar</th></tr></thead>
            <tbody>
              {D.invoices.map(i => (
                <tr key={i.id}>
                  <td className="mono">{i.id}</td>
                  <td>{i.customer}</td>
                  <td className="muted">{i.date}</td>
                  <td><InvoicePill status={i.status}/></td>
                  <td className="num mono tnum">{TRY(i.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ),
    },
    tasks: {
      title: 'Yaklaşan Görevler',
      headerExtra: <span className="pill info">Scheduler</span>,
      render: () => (
        <div className="empty" style={{ paddingTop: 32, textAlign: 'center' }}>
          <Icon name="clock" size={24} style={{ opacity: 0.25, marginBottom: 8 }} />
          <div style={{ color: 'var(--ink-3)', fontSize: 13 }}>Görev modülü yakında aktif olacak</div>
        </div>
      ),
    },
  };

  const Card = ({ id }) => {
    const w = widgets[id]; if (!w) return null;
    const isFull = id === 'kpis';
    const isDragging = dragging?.id === id;
    const isHover = hoverId === id;

    if (isDragging) {
      return (
        <>
          <div ref={el => { if (el) cardRefs.current[id] = el; }} className={`dash-card ${isFull?'is-kpis':''} drag-placeholder`} style={{ width: dragging.w, height: dragging.h }}>
            <div className="drag-placeholder-inner"></div>
          </div>
          <div className={`dash-card ${isFull?'is-kpis':''} dragging`} style={{ position: 'fixed', left: dragging.x - dragging.offsetX, top: dragging.y - dragging.offsetY, width: dragging.w, height: dragging.h, zIndex: 1000, pointerEvents: 'none', transform: 'scale(1.03)', boxShadow: '0 0 0 2px var(--accent), 0 24px 48px rgba(0,0,0,0.22)' }}>
            <div className="drag-handle"><Icon name="drag" size={14}/></div>
            <div className="mac-btns">
              <button className="mac-btn red" aria-label="Kapat"></button>
              <button className="mac-btn green" aria-label="Detay"></button>
            </div>
            {!isFull && (
              <div className="card-h">
                <div>
                  <h3 style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span>{w.title}</span>{w.headerExtra || null}</h3>
                  {w.sub && <div className="sub">{w.sub}</div>}
                </div>
              </div>
            )}
            <div className={`card-b ${w.noPad ? 'p0':''} ${isFull ? 'kpis-pad':''}`}>{w.render('card')}</div>
          </div>
        </>
      );
    }

    return (
      <div ref={el => { if (el) cardRefs.current[id] = el; }} className={`dash-card ${isFull?'is-kpis':''} ${isHover?'hover-target':''}`}>
        <div className="drag-handle" onPointerDown={onPointerDown(id)}><Icon name="drag" size={14}/></div>
        <div className="mac-btns">
          <button className="mac-btn red" title="Sil" onClick={() => removeCard(id)}></button>
          <button className="mac-btn green" title="Detaylı görüntüle" onClick={() => setDetail(id)}></button>
        </div>
        {!isFull && (
          <div className="card-h">
            <div>
              <h3 style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span>{w.title}</span>{w.headerExtra || null}</h3>
              {w.sub && <div className="sub">{w.sub}</div>}
            </div>
            {w.action && (
              <button className="btn ghost sm" onClick={w.action}>{w.actionLabel} <Icon name="chevRight" size={12}/></button>
            )}
          </div>
        )}
        <div className={`card-b ${w.noPad ? 'p0':''} ${isFull ? 'kpis-pad':''}`}>{w.render('card')}</div>
      </div>
    );
  };

  const renderRows = () => {
    const rows = [];
    let buf = [];
    const flushBuf = () => { if (buf.length) { rows.push(<div key={'row-'+rows.length} className="dash-row">{buf}</div>); buf = []; } };
    order.forEach(id => {
      if (id === 'kpis') { flushBuf(); rows.push(<div key={id} className="dash-row full">{<Card id={id} key={id}/>}</div>); }
      else { buf.push(<Card id={id} key={id}/>); if (buf.length === 3) flushBuf(); }
    });
    flushBuf();
    return rows;
  };

  return (
    <div className="page dashboard-page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Hoş geldin{user?.firstName ? `, ${user.firstName}` : ''}</h1>
          <p className="page-sub">Bugün <b>{new Date().toLocaleDateString('tr-TR', {day:'numeric',month:'long',year:'numeric'})}</b></p>
        </div>
        <div className="page-actions">
          <button className="btn primary" onClick={() => setAddOpen(true)}>
            <Icon name="plus"/> Widget Ekle
          </button>
        </div>
      </div>

      <div className="dash-grid" data-dragging={!!dragging}>
        {renderRows()}
      </div>

      {dragging && <div className="dash-ghost-slot" style={{ width: dragging.w, height: dragging.h }}/>}

      {detail && (
        <div className="dash-detail-scrim" onClick={() => setDetail(null)}>
          <div className="dash-detail" onClick={e => e.stopPropagation()}>
            <div className="dash-detail-h">
              <div>
                <h2>{widgets[detail].title}</h2>
                {widgets[detail].sub && <div className="sub">{widgets[detail].sub}</div>}
              </div>
              <button className="tb-icon-btn" onClick={() => setDetail(null)}><Icon name="x"/></button>
            </div>
            <div className="dash-detail-b">
              {widgets[detail].detail ? widgets[detail].detail() : widgets[detail].render('detail')}
            </div>
          </div>
        </div>
      )}

      {addOpen && !preview && (
        <div className="cmdk-scrim" onClick={() => setAddOpen(false)}>
          <div className="cmdk widget-picker" onClick={e => e.stopPropagation()}>
            <div className="cmdk-input">
              <Icon name="plus" size={16} style={{color:'var(--ink-3)'}}/>
              <input autoFocus placeholder="Widget ara..." />
              <kbd>esc</kbd>
            </div>
            <div className="cmdk-list">
              <div className="cmdk-section">Eklenebilir Widget'lar</div>
              {WIDGET_CATALOG.filter(w => !order.includes(w.id)).map(w => (
                <button key={w.id} className="cmdk-item widget-pick-item" onClick={() => setPreview(w.id)}>
                  <div className="widget-item-icon"><Icon name={w.icon} size={14}/></div>
                  <div style={{flex:1, textAlign:'left'}}>
                    <div style={{fontSize:13, fontWeight:600, color:'var(--ink)'}}>{w.name}</div>
                    <div style={{fontSize:11, color:'var(--ink-3)', marginTop:2}}>{w.desc}</div>
                  </div>
                  <Icon name="chevRight" size={12} style={{color:'var(--ink-3)'}}/>
                </button>
              ))}
              {WIDGET_CATALOG.filter(w => order.includes(w.id)).length > 0 && (
                <>
                  <div className="cmdk-section" style={{marginTop:8}}>Zaten Ekli</div>
                  {WIDGET_CATALOG.filter(w => order.includes(w.id)).map(w => (
                    <div key={w.id} className="cmdk-item widget-pick-item" style={{opacity:0.5, cursor:'default'}}>
                      <div className="widget-item-icon"><Icon name={w.icon} size={14}/></div>
                      <div style={{flex:1, textAlign:'left'}}>
                        <div style={{fontSize:13, fontWeight:600, color:'var(--ink)'}}>{w.name}</div>
                        <div style={{fontSize:11, color:'var(--ink-3)', marginTop:2}}>{w.desc}</div>
                      </div>
                      <span className="pill pos"><span className="dot"/>Eklendi</span>
                    </div>
                  ))}
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {preview && (
        <div className="dash-detail-scrim" onClick={() => setPreview(null)}>
          <div className="dash-detail" onClick={e => e.stopPropagation()} style={{maxWidth: 720}}>
            <div className="dash-detail-h">
              <div>
                <div className="muted" style={{fontSize:11, textTransform:'uppercase', letterSpacing:'0.06em'}}>Önizleme</div>
                <h2>{widgets[preview].title}</h2>
                <div className="sub">{WIDGET_CATALOG.find(w=>w.id===preview)?.desc}</div>
              </div>
              <button className="tb-icon-btn" onClick={() => setPreview(null)}><Icon name="x"/></button>
            </div>
            <div className="dash-detail-b" style={{background:'var(--bg)'}}>
              <div className="dash-card preview-card">
                {widgets[preview].title && (
                  <div className="card-h">
                    <div>
                      <h3>{widgets[preview].title}</h3>
                      {widgets[preview].sub && <div className="sub">{widgets[preview].sub}</div>}
                    </div>
                  </div>
                )}
                <div className="card-b">{widgets[preview].render('card')}</div>
              </div>
            </div>
            <div className="dash-detail-f">
              <button className="btn ghost" onClick={() => setPreview(null)}>Vazgeç</button>
              <button className="btn primary" onClick={() => addWidget(preview)}>
                <Icon name="plus"/> Dashboarda Ekle
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
