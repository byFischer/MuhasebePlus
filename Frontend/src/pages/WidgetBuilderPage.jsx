import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import { toast } from '@/lib/toast';
import dashboardService from '@/services/dashboardService';
import DataWidget from '@/widgets/DataWidget';

// ─── Sözlükler — kullanıcı dostu kelimeler ───────────────────────────────────

const METRIC_OPTIONS = [
  { key: 'SUM',   label: 'Toplam',   needsField: true,  hint: 'tüm değerleri topla' },
  { key: 'AVG',   label: 'Ortalama', needsField: true,  hint: 'aritmetik ortalama' },
  { key: 'MIN',   label: 'En Az',    needsField: true,  hint: 'en küçük değer' },
  { key: 'MAX',   label: 'En Çok',   needsField: true,  hint: 'en büyük değer' },
  { key: 'COUNT', label: 'Adet',     needsField: false, hint: 'kayıt sayısı' },
];

const CHART_OPTIONS = [
  { key: 'KPI',   label: 'Tek Değer',     icon: 'sparkle', desc: 'Büyük rakam, tek bakışta' },
  { key: 'BAR',   label: 'Bar Grafik',    icon: 'chart',   desc: 'Karşılaştırmalı sütunlar' },
  { key: 'LINE',  label: 'Çizgi Grafik',  icon: 'chart',   desc: 'Zamanla değişim' },
  { key: 'PIE',   label: 'Pasta Grafik',  icon: 'chart',   desc: 'Yüzdelik dağılım' },
  { key: 'TABLE', label: 'Tablo',         icon: 'log',     desc: 'Detaylı liste' },
];

const COLORS = [
  { key: 'accent', label: 'Turuncu', hex: '#f97316' },
  { key: 'pos',    label: 'Yeşil',   hex: '#22c55e' },
  { key: 'neg',    label: 'Kırmızı', hex: '#ef4444' },
  { key: 'info',   label: 'Mavi',    hex: '#3b82f6' },
  { key: 'purple', label: 'Mor',     hex: '#a855f7' },
  { key: 'teal',   label: 'Turkuaz', hex: '#14b8a6' },
];

const STEPS = [
  { num: 1, label: 'Veri',       desc: 'Neyi sayalım?' },
  { num: 2, label: 'Filtre',     desc: 'Hangi kayıtları dahil edelim?' },
  { num: 3, label: 'Görünüm',    desc: 'Nasıl gösterelim?' },
];

const FILTER_OPS = [
  { key: 'EQ',   label: 'eşit' },
  { key: 'NE',   label: 'eşit değil' },
  { key: 'GT',   label: 'büyük' },
  { key: 'LT',   label: 'küçük' },
  { key: 'GTE',  label: 'büyük/eşit' },
  { key: 'LTE',  label: 'küçük/eşit' },
  { key: 'LIKE', label: 'içerir' },
];

// ─── Yardımcılar ─────────────────────────────────────────────────────────────

function inferWidgetType(chartType) {
  if (chartType === 'KPI')   return 'DATA_KPI';
  if (chartType === 'TABLE') return 'DATA_TABLE';
  return 'DATA_CHART';
}

function emptyForm() {
  return {
    name: '',
    description: '',
    dataSource: '',
    metricFunc: 'SUM',
    metricField: '',
    groupField: '',
    groupTransform: '',
    filters: [],
    chartType: 'KPI',
    color: 'accent',
  };
}

function definitionToForm(def) {
  const q = def.queryConfig || {};
  const v = def.visualConfig || {};
  const agg = q.aggregate || {};
  const grp = (q.groupBy && q.groupBy[0]) || {};
  return {
    name: def.name || '',
    description: def.description || '',
    dataSource: def.dataSource || q.dataSource || '',
    metricFunc: agg.function || 'SUM',
    metricField: agg.field || '',
    groupField: grp.field || '',
    groupTransform: grp.transform || '',
    filters: (q.filters || []).map(f => ({ field: f.field, operator: f.operator, value: f.value })),
    chartType: v.chartType || 'KPI',
    color: v.color || 'accent',
  };
}

function buildPayload(form) {
  const filters = form.filters
    .filter(f => f.field && (f.value !== '' && f.value !== null && f.value !== undefined))
    .map(f => ({ field: f.field, operator: f.operator, value: f.value }));

  const aggregate = form.metricFunc === 'COUNT'
    ? { function: 'COUNT', field: form.metricField || 'id', alias: 'Adet' }
    : (form.metricField ? {
        function: form.metricFunc,
        field: form.metricField,
        alias: form.metricFunc === 'SUM' ? 'Toplam'
             : form.metricFunc === 'AVG' ? 'Ortalama'
             : form.metricFunc === 'MIN' ? 'En Az'
             : form.metricFunc === 'MAX' ? 'En Çok' : 'Değer'
      } : null);

  const groupBy = form.groupField
    ? [{ field: form.groupField, ...(form.groupTransform ? { transform: form.groupTransform } : {}) }]
    : [];

  return {
    name: form.name,
    description: form.description,
    widgetType: inferWidgetType(form.chartType),
    dataSource: form.dataSource,
    queryConfig: {
      dataSource: form.dataSource,
      filters,
      groupBy,
      aggregate,
    },
    visualConfig: {
      chartType: form.chartType,
      title: form.name || 'Yeni Widget',
      color: form.color,
    },
    config: {},
  };
}

// ─── Bileşen ──────────────────────────────────────────────────────────────────

export default function WidgetBuilderPage() {
  const onNav = useNavigate();
  const [search] = useSearchParams();
  const editId = search.get('edit');
  const isEdit = !!editId;

  const [tab, setTab] = useState('templates'); // 'templates' | 'custom'
  const [step, setStep] = useState(1);

  const [dataSources, setDataSources] = useState([]);
  const [fields, setFields] = useState([]);
  const [definitions, setDefinitions] = useState([]);

  const [form, setForm] = useState(emptyForm);
  const [previewData, setPreviewData] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // Düzenleme modunda direkt custom tab'a aç ve formu doldur
  useEffect(() => {
    if (isEdit) {
      setTab('custom');
      dashboardService.getWidgetDefinition(editId)
        .then(def => setForm(definitionToForm(def)))
        .catch(e => {
          console.error(e);
          toast.err('Widget yüklenemedi');
          onNav('/widgets');
        });
    }
  }, [editId, isEdit, onNav]);

  useEffect(() => {
    dashboardService.getDataSources().then(setDataSources).catch(console.error);
    dashboardService.getWidgetDefinitions().then(setDefinitions).catch(console.error);
  }, []);

  useEffect(() => {
    if (form.dataSource) {
      dashboardService.getDataSourceFields(form.dataSource)
        .then(setFields).catch(console.error);
    } else {
      setFields([]);
    }
  }, [form.dataSource]);

  const update = (patch) => {
    setForm(prev => ({ ...prev, ...patch }));
    setPreviewData(null);
  };

  const setFilter = (i, patch) => {
    const next = [...form.filters];
    next[i] = { ...next[i], ...patch };
    update({ filters: next });
  };
  const addFilter   = () => update({ filters: [...form.filters, { field: '', operator: 'EQ', value: '' }] });
  const removeFilter= (i) => update({ filters: form.filters.filter((_, idx) => idx !== i) });

  const aggregateFields = useMemo(() => fields.filter(f => f.aggregateable), [fields]);
  const dimensionFields = useMemo(() => fields.filter(f => !f.aggregateable && f.type !== 'REFERENCE'), [fields]);

  const canPreview = form.dataSource && (form.metricFunc === 'COUNT' || form.metricField);

  const runPreview = async () => {
    if (!canPreview) return;
    setPreviewLoading(true);
    try {
      const payload = buildPayload(form);
      const res = await dashboardService.previewQuery(payload.queryConfig);
      if (res.success) {
        setPreviewData({
          data: res.data,
          columns: res.columns,
          aggregateMeta: res.aggregateMeta,
          totalCount: res.totalCount,
        });
      } else {
        toast.err('Önizleme: ' + (res.message || 'bilinmeyen hata'));
      }
    } catch (e) {
      console.error(e);
      toast.err('Önizleme başarısız: ' + (e?.response?.data?.message || e.message));
    } finally {
      setPreviewLoading(false);
    }
  };

  const save = async () => {
    if (!form.name.trim())   { toast.err('Lütfen widget için bir isim girin'); setStep(3); return; }
    if (!form.dataSource)    { toast.err('Lütfen bir veri kaynağı seçin');     setStep(1); return; }
    if (form.metricFunc !== 'COUNT' && !form.metricField) {
      toast.err('Lütfen hesaplanacak alanı seçin'); setStep(1); return;
    }

    const payload = buildPayload(form);
    setSaving(true);
    try {
      let def;
      if (isEdit) {
        def = await dashboardService.updateWidgetDefinition(editId, payload);
        toast.ok(`"${def.name}" güncellendi`);
        onNav('/widgets');
      } else {
        def = await dashboardService.createWidgetDefinition(payload);
        const layout = await dashboardService.getDefaultLayout();
        await dashboardService.addWidget(layout.layoutId, {
          widgetType: def.widgetType,
          definitionId: def.definitionId,
          title: def.name,
          positionX: 0, positionY: 0, width: 2, height: 1,
          config: JSON.stringify({ variant: 'm' }),
        });
        toast.ok(`"${def.name}" oluşturuldu ve dashboard'a eklendi`);
        onNav('/dashboard');
      }
    } catch (e) {
      console.error(e);
      toast.err('Kaydedilemedi: ' + (e?.response?.data?.message || e.message));
    } finally {
      setSaving(false);
    }
  };

  // ─── Şablondan dashboard'a ekle ───────────────────────────────────────────
  const addTemplateToDashboard = async (def) => {
    setSaving(true);
    try {
      // Sistem şablonu ise önce kullanıcı kopyası oluştur
      const target = def.isSystem
        ? await dashboardService.cloneWidgetDefinition(def.definitionId)
        : def;
      const layout = await dashboardService.getDefaultLayout();
      await dashboardService.addWidget(layout.layoutId, {
        widgetType: target.widgetType,
        definitionId: target.definitionId,
        title: target.name,
        positionX: 0, positionY: 0, width: 2, height: 1,
        config: JSON.stringify({ variant: 'm' }),
      });
      toast.ok(`"${target.name}" dashboard'a eklendi`);
      onNav('/dashboard');
    } catch (e) {
      console.error(e);
      toast.err('Eklenemedi: ' + (e?.response?.data?.message || e.message));
    } finally {
      setSaving(false);
    }
  };

  // ─── Render ────────────────────────────────────────────────────────────────

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">{isEdit ? 'Widget Düzenle' : 'Widget Oluştur'}</h1>
          <p className="page-sub">
            {isEdit
              ? 'Var olan widget\'ı güncelle'
              : tab === 'templates' ? 'Hazır şablon seç veya sıfırdan oluştur'
              : `${STEPS[step - 1].label} — ${STEPS[step - 1].desc}`}
          </p>
        </div>
        <div className="page-actions">
          <button className="btn ghost" onClick={() => onNav('/widgets')}>
            <Icon name="folder" size={14} /> Widget'larım
          </button>
        </div>
      </div>

      {/* Sekme barı (edit modunda gizli) */}
      {!isEdit && (
        <div className="row gap-8" style={{ marginBottom: 20, borderBottom: '1px solid var(--line)' }}>
          <button
            className={`btn ${tab === 'templates' ? 'primary' : 'ghost'}`}
            style={{ borderRadius: '8px 8px 0 0', borderBottom: 'none' }}
            onClick={() => setTab('templates')}
          >
            <Icon name="template" size={14} /> Hazır Şablonlar
          </button>
          <button
            className={`btn ${tab === 'custom' ? 'primary' : 'ghost'}`}
            style={{ borderRadius: '8px 8px 0 0', borderBottom: 'none' }}
            onClick={() => setTab('custom')}
          >
            <Icon name="plus" size={14} /> Sıfırdan Oluştur
          </button>
        </div>
      )}

      {/* ŞABLONLAR SEKMESİ */}
      {!isEdit && tab === 'templates' && (
        <TemplateGallery
          definitions={definitions}
          saving={saving}
          onAdd={addTemplateToDashboard}
          onCustom={() => setTab('custom')}
        />
      )}

      {/* CUSTOM SEKMESİ */}
      {(isEdit || tab === 'custom') && (
        <div className="row gap-16" style={{ alignItems: 'flex-start' }}>
          {/* Sol: form */}
          <div className="panel" style={{ flex: 1, maxWidth: 640 }}>
            {/* Stepper */}
            <div className="row gap-8" style={{ alignItems: 'center', marginBottom: 24 }}>
              {STEPS.map((s, i) => (
                <React.Fragment key={s.num}>
                  <button
                    onClick={() => setStep(s.num)}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 8, padding: '8px 14px',
                      borderRadius: 20, border: 'none', cursor: 'pointer',
                      background: step >= s.num ? 'var(--accent)' : 'var(--bg-2)',
                      color: step >= s.num ? '#fff' : 'var(--ink-3)',
                      fontSize: 13, fontWeight: 500
                    }}
                  >
                    <span style={{
                      width: 22, height: 22, borderRadius: '50%', display: 'grid', placeItems: 'center',
                      background: step >= s.num ? 'rgba(255,255,255,0.25)' : 'var(--surface)',
                      fontSize: 11, fontWeight: 700
                    }}>{s.num}</span>
                    {s.label}
                  </button>
                  {i < STEPS.length - 1 && (
                    <div style={{ flex: 1, height: 2, background: step > s.num ? 'var(--accent)' : 'var(--bg-2)', borderRadius: 1 }} />
                  )}
                </React.Fragment>
              ))}
            </div>

            {/* ADIM 1 — Veri */}
            {step === 1 && (
              <div className="col gap-16">
                <div>
                  <h3 style={{ fontSize: 15, fontWeight: 600 }}>1. Hangi veriden?</h3>
                  <p style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>
                    Faturalar mı, gelir-gider işlemleri mi?
                  </p>
                </div>
                <div className="grid-2" style={{ gap: 10 }}>
                  {dataSources.map(ds => (
                    <button
                      key={ds.key}
                      className={`btn ${form.dataSource === ds.key ? 'primary' : 'ghost'}`}
                      onClick={() => update({ dataSource: ds.key, metricField: '', groupField: '', filters: [] })}
                      style={{ justifyContent: 'flex-start', textAlign: 'left', height: 'auto', padding: '14px 16px', borderWidth: 2 }}
                    >
                      <div>
                        <div style={{ fontWeight: 600, fontSize: 14 }}>{ds.label}</div>
                        <div style={{ fontSize: 11, opacity: 0.75, marginTop: 2 }}>{ds.description}</div>
                      </div>
                    </button>
                  ))}
                </div>

                {form.dataSource && (
                  <div style={{ borderTop: '1px solid var(--line)', paddingTop: 16, marginTop: 8 }}>
                    <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 4 }}>2. Neyi hesaplayalım?</h3>
                    <p style={{ fontSize: 12, color: 'var(--ink-3)', marginBottom: 10 }}>
                      Toplam mı, ortalama mı, adet mi?
                    </p>
                    <div className="row gap-8" style={{ flexWrap: 'wrap', marginBottom: 12 }}>
                      {METRIC_OPTIONS.map(m => (
                        <button
                          key={m.key}
                          className={`btn sm ${form.metricFunc === m.key ? 'primary' : 'ghost'}`}
                          onClick={() => update({ metricFunc: m.key })}
                          title={m.hint}
                        >
                          {m.label}
                        </button>
                      ))}
                    </div>

                    {form.metricFunc !== 'COUNT' && (
                      <div className="col gap-6">
                        <label style={{ fontSize: 12, fontWeight: 500 }}>Hangi alanın {METRIC_OPTIONS.find(m=>m.key===form.metricFunc)?.label.toLowerCase()}'ı?</label>
                        <select className="input" value={form.metricField} onChange={e => update({ metricField: e.target.value })}>
                          <option value="">Alan seçin</option>
                          {aggregateFields.map(f => (
                            <option key={f.key} value={f.key}>{f.label}</option>
                          ))}
                        </select>
                      </div>
                    )}

                    <div style={{ borderTop: '1px solid var(--line)', paddingTop: 14, marginTop: 14 }}>
                      <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 4 }}>3. Gruplama (opsiyonel)</h3>
                      <p style={{ fontSize: 12, color: 'var(--ink-3)', marginBottom: 10 }}>
                        Tek bir değer mi istersin yoksa kategorilere/aylara mı bölünsün?
                      </p>
                      <div className="row gap-8">
                        <select className="input" style={{ flex: 1 }} value={form.groupField} onChange={e => update({ groupField: e.target.value })}>
                          <option value="">Gruplama yok (tek değer)</option>
                          {dimensionFields.map(f => (
                            <option key={f.key} value={f.key}>{f.label}</option>
                          ))}
                        </select>
                        {form.groupField && fields.find(f => f.key === form.groupField)?.type === 'DATE' && (
                          <select className="input" value={form.groupTransform} onChange={e => update({ groupTransform: e.target.value })}>
                            <option value="">Günlük</option>
                            <option value="month">Ay bazında</option>
                            <option value="year">Yıl bazında</option>
                          </select>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* ADIM 2 — Filtre */}
            {step === 2 && (
              <div className="col gap-16">
                <div>
                  <h3 style={{ fontSize: 15, fontWeight: 600 }}>Filtre (opsiyonel)</h3>
                  <p style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>
                    Yalnızca belirli kayıtları dahil etmek istiyorsan filtre ekle. Boş bırakırsan tüm kayıtlar dahil edilir.
                  </p>
                </div>

                {form.filters.length === 0 && (
                  <div className="empty" style={{ padding: 24 }}>Henüz filtre yok. Tüm kayıtlar dahil edilecek.</div>
                )}

                {form.filters.map((f, i) => {
                  const meta = fields.find(fld => fld.key === f.field);
                  const isEnum = meta?.type === 'ENUM';
                  return (
                    <div key={i} className="row gap-8" style={{ alignItems: 'center', background: 'var(--bg-2)', padding: 10, borderRadius: 10, flexWrap: 'wrap' }}>
                      <select className="input" style={{ minWidth: 140, flex: '1 1 140px' }} value={f.field} onChange={e => setFilter(i, { field: e.target.value, value: '' })}>
                        <option value="">Alan</option>
                        {fields.map(fld => <option key={fld.key} value={fld.key}>{fld.label}</option>)}
                      </select>
                      <select className="input" style={{ minWidth: 110 }} value={f.operator} onChange={e => setFilter(i, { operator: e.target.value })}>
                        {FILTER_OPS.map(op => <option key={op.key} value={op.key}>{op.label}</option>)}
                      </select>
                      {isEnum && meta?.options ? (
                        <select className="input" style={{ flex: 1, minWidth: 140 }} value={f.value} onChange={e => setFilter(i, { value: e.target.value })}>
                          <option value="">Seçin</option>
                          {meta.options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                        </select>
                      ) : (
                        <input className="input" style={{ flex: 1, minWidth: 140 }} placeholder="Değer" value={f.value} onChange={e => setFilter(i, { value: e.target.value })} />
                      )}
                      <button className="tb-icon-btn" onClick={() => removeFilter(i)}><Icon name="x" size={14} /></button>
                    </div>
                  );
                })}
                <button className="btn ghost sm" onClick={addFilter} style={{ alignSelf: 'flex-start' }}>
                  <Icon name="plus" size={12} /> Filtre Ekle
                </button>
              </div>
            )}

            {/* ADIM 3 — Görünüm */}
            {step === 3 && (
              <div className="col gap-16">
                <div>
                  <h3 style={{ fontSize: 15, fontWeight: 600 }}>Görünüm</h3>
                  <p style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 2 }}>
                    Widget'ın nasıl görüneceğini seç.
                  </p>
                </div>
                <div className="grid-3" style={{ gap: 10 }}>
                  {CHART_OPTIONS.map(c => (
                    <button
                      key={c.key}
                      className={`btn ${form.chartType === c.key ? 'primary' : 'ghost'}`}
                      onClick={() => update({ chartType: c.key })}
                      style={{ flexDirection: 'column', gap: 6, padding: '14px 10px', height: 'auto', borderWidth: 2 }}
                    >
                      <Icon name={c.icon} size={20} />
                      <span style={{ fontSize: 12, fontWeight: 600 }}>{c.label}</span>
                      <span style={{ fontSize: 10, opacity: 0.7, lineHeight: 1.2 }}>{c.desc}</span>
                    </button>
                  ))}
                </div>

                <div style={{ borderTop: '1px solid var(--line)', paddingTop: 14, marginTop: 6 }}>
                  <div className="col gap-8" style={{ marginBottom: 12 }}>
                    <label style={{ fontSize: 12, fontWeight: 500 }}>İsim <span style={{ color: 'var(--neg)' }}>*</span></label>
                    <input className="input" placeholder="Örn: Vadesi Geçen Faturalar" value={form.name} onChange={e => update({ name: e.target.value })} />
                  </div>
                  <div className="col gap-8" style={{ marginBottom: 12 }}>
                    <label style={{ fontSize: 12, fontWeight: 500 }}>Açıklama (opsiyonel)</label>
                    <input className="input" placeholder="Kısa açıklama" value={form.description} onChange={e => update({ description: e.target.value })} />
                  </div>
                  <div className="col gap-8">
                    <label style={{ fontSize: 12, fontWeight: 500 }}>Renk</label>
                    <div className="row gap-10">
                      {COLORS.map(c => (
                        <button
                          key={c.key}
                          onClick={() => update({ color: c.key })}
                          title={c.label}
                          style={{
                            width: 32, height: 32, borderRadius: '50%',
                            background: c.hex,
                            border: form.color === c.key ? '3px solid var(--ink)' : '3px solid transparent',
                            cursor: 'pointer',
                          }}
                        />
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Navigation */}
            <div className="row gap-8" style={{ justifyContent: 'space-between', marginTop: 24, borderTop: '1px solid var(--line)', paddingTop: 14 }}>
              <button className="btn ghost" onClick={() => setStep(s => Math.max(1, s - 1))} disabled={step === 1}>
                <Icon name="chevLeft" size={14} /> Geri
              </button>
              {step < 3 ? (
                <button className="btn primary" onClick={() => setStep(s => s + 1)}>
                  İleri <Icon name="chevRight" size={14} />
                </button>
              ) : (
                <button className="btn primary" onClick={save} disabled={saving}>
                  <Icon name="check" size={14} /> {saving ? 'Kaydediliyor...' : (isEdit ? 'Kaydet' : 'Oluştur ve Dashboard\'a Ekle')}
                </button>
              )}
            </div>
          </div>

          {/* Sağ: önizleme */}
          <div className="panel" style={{ width: 420, position: 'sticky', top: 20 }}>
            <div className="row" style={{ alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <h3 style={{ fontSize: 14, fontWeight: 600, margin: 0 }}>
                <Icon name="eye" size={14} /> Canlı Önizleme
              </h3>
              <button className="btn primary sm" onClick={runPreview} disabled={!canPreview || previewLoading}>
                {previewLoading ? <Icon name="refresh" size={12} /> : <Icon name="play" size={12} />}
                {previewLoading ? ' Yükleniyor' : ' Çalıştır'}
              </button>
            </div>
            {!form.dataSource && <div className="empty" style={{ padding: 24 }}>Önce bir veri kaynağı seç.</div>}
            {form.dataSource && !previewData && !previewLoading && (
              <div className="empty" style={{ padding: 24, fontSize: 12, lineHeight: 1.5 }}>
                Ayarlarını yaptıktan sonra <b>Çalıştır</b>'a basarak gerçek verilerle önizle.
              </div>
            )}
            {previewData && (
              <DataWidget
                data={previewData}
                config={{ visualConfig: { chartType: form.chartType, title: form.name || 'Önizleme', color: form.color } }}
                mode="card"
                variant="m"
              />
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Şablon galerisi alt bileşeni ────────────────────────────────────────────

function TemplateGallery({ definitions, saving, onAdd, onCustom }) {
  const systemDefs = definitions.filter(d => d.isSystem);
  const userDefs   = definitions.filter(d => !d.isSystem);

  return (
    <div className="col gap-24">
      <section>
        <div className="row" style={{ alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <h2 style={{ fontSize: 16, fontWeight: 600, margin: 0 }}>
            <Icon name="sparkle" size={14} /> Hazır Şablonlar
            <span className="muted" style={{ fontSize: 12, fontWeight: 400, marginLeft: 8 }}>({systemDefs.length})</span>
          </h2>
          <button className="btn ghost sm" onClick={onCustom}>
            <Icon name="plus" size={12} /> Sıfırdan Oluştur
          </button>
        </div>
        <p style={{ fontSize: 12, color: 'var(--ink-3)', marginBottom: 12 }}>
          Tek tıkla dashboard'a ekle. İhtiyaçlarına göre sonradan düzenleyebilirsin.
        </p>
        <div className="grid-3" style={{ gap: 12 }}>
          {systemDefs.map(def => (
            <TemplateCard key={def.definitionId} def={def} onAdd={onAdd} disabled={saving} />
          ))}
          {systemDefs.length === 0 && (
            <div className="empty" style={{ padding: 32, gridColumn: '1 / -1' }}>Şablon bulunamadı.</div>
          )}
        </div>
      </section>

      {userDefs.length > 0 && (
        <section>
          <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12 }}>
            <Icon name="folder" size={14} /> Daha Önce Oluşturduklarım
            <span className="muted" style={{ fontSize: 12, fontWeight: 400, marginLeft: 8 }}>({userDefs.length})</span>
          </h2>
          <div className="grid-3" style={{ gap: 12 }}>
            {userDefs.map(def => (
              <TemplateCard key={def.definitionId} def={def} onAdd={onAdd} disabled={saving} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

function TemplateCard({ def, onAdd, disabled }) {
  const chart = def.visualConfig?.chartType || 'TABLE';
  const chartLabel = CHART_OPTIONS.find(c => c.key === chart)?.label || chart;
  const sourceLabel = def.dataSource === 'INVOICE' ? 'Faturalar' : def.dataSource === 'TRANSACTION' ? 'İşlemler' : def.dataSource;

  return (
    <div className="panel" style={{ padding: 14, opacity: disabled ? 0.6 : 1 }}>
      <div className="row gap-6" style={{ alignItems: 'center', marginBottom: 6 }}>
        <Icon name={CHART_OPTIONS.find(c => c.key === chart)?.icon || 'chart'} size={14} style={{ color: 'var(--accent)' }} />
        <h3 style={{ fontSize: 14, fontWeight: 600, margin: 0, flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{def.name}</h3>
      </div>
      {def.description && (
        <p style={{ fontSize: 11, color: 'var(--ink-3)', margin: '0 0 10px 0', lineHeight: 1.4, minHeight: 30 }}>{def.description}</p>
      )}
      <div className="row gap-6" style={{ marginBottom: 12, flexWrap: 'wrap' }}>
        <span className="pill" style={{ fontSize: 10 }}>{chartLabel}</span>
        <span className="pill" style={{ fontSize: 10 }}>{sourceLabel}</span>
      </div>
      <button className="btn primary sm" onClick={() => onAdd(def)} disabled={disabled} style={{ width: '100%' }}>
        <Icon name="plus" size={12} /> Dashboard'a Ekle
      </button>
    </div>
  );
}
