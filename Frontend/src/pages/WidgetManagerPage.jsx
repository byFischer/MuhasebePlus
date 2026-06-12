import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import { toast } from '@/lib/toast';
import dashboardService from '@/services/dashboardService';
import DataWidget from '@/widgets/DataWidget';

const CHART_TYPE_LABEL = {
  KPI: 'KPI',
  BAR: 'Bar Grafik',
  LINE: 'Çizgi Grafik',
  PIE: 'Pasta Grafik',
  TABLE: 'Tablo',
  PROGRESS: 'İlerleme',
};

const SOURCE_LABEL = {
  INVOICE: 'Faturalar',
  TRANSACTION: 'İşlemler',
};

export default function WidgetManagerPage() {
  const onNav = useNavigate();
  const [definitions, setDefinitions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const list = await dashboardService.getWidgetDefinitions();
      setDefinitions(list);
    } catch (e) {
      console.error(e);
      toast.err('Widget\'lar yüklenemedi: ' + (e?.response?.data?.message || e.message));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const addToDashboard = async (def) => {
    setBusyId(def.definitionId);
    try {
      const layout = await dashboardService.getDefaultLayout();
      await dashboardService.addWidget(layout.layoutId, {
        widgetType: def.widgetType,
        definitionId: def.definitionId,
        title: def.name,
        positionX: 0,
        positionY: 0,
        width: 2,
        height: 1,
        config: JSON.stringify({ variant: 'm' }),
      });
      toast.ok(`"${def.name}" dashboard'a eklendi`);
      onNav('/dashboard');
    } catch (e) {
      console.error(e);
      toast.err('Eklenemedi: ' + (e?.response?.data?.message || e.message));
    } finally {
      setBusyId(null);
    }
  };

  const cloneDef = async (def) => {
    setBusyId(def.definitionId);
    try {
      const created = await dashboardService.cloneWidgetDefinition(def.definitionId);
      toast.ok(`"${created.name}" kopyalandı`);
      await load();
    } catch (e) {
      console.error(e);
      toast.err('Kopyalanamadı: ' + (e?.response?.data?.message || e.message));
    } finally {
      setBusyId(null);
    }
  };

  const removeDef = async (def) => {
    if (!window.confirm(`"${def.name}" silinsin mi? Bu işlem geri alınamaz.`)) return;
    setBusyId(def.definitionId);
    try {
      await dashboardService.deleteWidgetDefinition(def.definitionId);
      toast.ok(`"${def.name}" silindi`);
      await load();
    } catch (e) {
      console.error(e);
      toast.err('Silinemedi: ' + (e?.response?.data?.message || e.message));
    } finally {
      setBusyId(null);
    }
  };

  const editDef = (def) => {
    onNav(`/widget-builder?edit=${def.definitionId}`);
  };

  const systemDefs = definitions.filter(d => d.isSystem);
  const userDefs = definitions.filter(d => !d.isSystem);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Widget'larım</h1>
          <p className="page-sub">Oluşturduğun özel widget'ları yönet</p>
        </div>
        <div className="page-actions">
          <button className="btn ghost" onClick={() => onNav('/dashboard')}>
            <Icon name="chevLeft" size={14} /> Dashboard'a Dön
          </button>
          <button className="btn primary" onClick={() => onNav('/widget-builder')}>
            <Icon name="plus" size={14} /> Yeni Widget Oluştur
          </button>
        </div>
      </div>

      {loading && (
        <div className="empty" style={{ padding: 48 }}>Yükleniyor...</div>
      )}

      {!loading && (
        <>
          {/* Kullanıcının kendi widget'ları */}
          <section style={{ marginBottom: 32 }}>
            <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12 }}>
              Benim Widget'larım
              <span className="muted" style={{ fontSize: 12, fontWeight: 400, marginLeft: 8 }}>
                ({userDefs.length})
              </span>
            </h2>
            {userDefs.length === 0 && (
              <div className="empty" style={{ padding: 32 }}>
                Henüz kendi widget'ın yok. <button className="btn ghost sm" onClick={() => onNav('/widget-builder')} style={{ marginLeft: 8 }}>Hemen oluştur</button>
              </div>
            )}
            <div className="grid-2" style={{ gap: 12 }}>
              {userDefs.map(def => (
                <DefinitionCard
                  key={def.definitionId}
                  def={def}
                  busy={busyId === def.definitionId}
                  onAdd={() => addToDashboard(def)}
                  onEdit={() => editDef(def)}
                  onDelete={() => removeDef(def)}
                />
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function WidgetDefPreview({ def }) {
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

  if (st.loading) return <div className="wm-preview-skel">Önizleme yükleniyor…</div>;
  if (st.error || !st.data || st.data.length === 0) return <div className="wm-preview-skel">Önizleme yok</div>;
  return (
    <DataWidget
      data={{ data: st.data, columns: st.columns }}
      config={{ visualConfig: def.visualConfig || {} }}
      mode="card"
      variant="s"
    />
  );
}

function DefinitionCard({ def, busy, isSystem, onAdd, onEdit, onDelete, onClone }) {
  const chartType = def.visualConfig?.chartType || 'TABLE';
  const dataSource = def.dataSource;

  return (
    <div className="panel" style={{ padding: 16, opacity: busy ? 0.6 : 1 }}>
      <div className="wm-preview"><WidgetDefPreview def={def} /></div>

      <div className="row gap-8" style={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="row gap-8" style={{ alignItems: 'center', marginBottom: 4 }}>
            <h3 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>{def.name}</h3>
            {isSystem && <span className="pill info" style={{ fontSize: 10 }}><span className="dot" />Hazır</span>}
          </div>
          {def.description && (
            <p style={{ fontSize: 12, color: 'var(--ink-3)', margin: '0 0 8px 0' }}>{def.description}</p>
          )}
          <div className="row gap-8" style={{ flexWrap: 'wrap' }}>
            <span className="pill" style={{ fontSize: 10 }}>{CHART_TYPE_LABEL[chartType] || chartType}</span>
            <span className="pill" style={{ fontSize: 10 }}>{SOURCE_LABEL[dataSource] || dataSource}</span>
          </div>
        </div>
      </div>

      <div className="row gap-8" style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--line)' }}>
        <button className="btn primary sm" onClick={onAdd} disabled={busy}>
          <Icon name="plus" size={12} /> Dashboard'a Ekle
        </button>
        {isSystem ? (
          <button className="btn ghost sm" onClick={onClone} disabled={busy} title="Kopyala ve düzenle">
            <Icon name="copy" size={12} /> Kopyala
          </button>
        ) : (
          <>
            <button className="btn ghost sm" onClick={onEdit} disabled={busy}>
              <Icon name="settings" size={12} /> Düzenle
            </button>
            <button className="btn ghost sm" onClick={onDelete} disabled={busy} title="Sil" style={{ marginLeft: 'auto', color: 'var(--neg)' }}>
              <Icon name="trash" size={12} />
            </button>
          </>
        )}
      </div>
    </div>
  );
}
