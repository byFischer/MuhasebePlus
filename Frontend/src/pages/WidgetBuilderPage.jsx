import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import { toast } from '@/lib/toast';
import dashboardService from '@/services/dashboardService';
import DataWidget from '@/widgets/DataWidget';

// ─── Sözlükler — kullanıcı dostu kelimeler ───────────────────────────────────
// Hangi tiplerin görüneceğine backend metadata karar verir; bu sözlükler sadece
// key → kullanıcı dostu etiket çevirisi sağlar.

const METRIC_LABELS = {
  SUM:   { label: 'Toplam',   needsField: true,  hint: 'tüm değerleri topla' },
  AVG:   { label: 'Ortalama', needsField: true,  hint: 'aritmetik ortalama' },
  MIN:   { label: 'En Az',    needsField: true,  hint: 'en küçük değer' },
  MAX:   { label: 'En Çok',   needsField: true,  hint: 'en büyük değer' },
  COUNT: { label: 'Adet',     needsField: false, hint: 'kayıt sayısı' },
};

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

const FILTER_OP_LABELS = {
  EQ:     'eşit',
  NE:     'eşit değil',
  GT:     'büyük',
  LT:     'küçük',
  GTE:    'büyük/eşit',
  LTE:    'küçük/eşit',
  LIKE:   'içerir',
  IN:     'şunlardan biri',
  BEFORE: 'öncesi',
  AFTER:  'sonrası',
  BETWEEN:'aralık',
};

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
        alias: METRIC_LABELS[form.metricFunc]?.label || 'Değer',
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

  const [step, setStep] = useState(1);

  const [dataSources, setDataSources] = useState([]);
  const [fields, setFields] = useState([]);

  const [form, setForm] = useState(emptyForm);
  const [previewData, setPreviewData] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // Düzenleme modunda direkt custom tab'a aç ve formu doldur
  useEffect(() => {
    if (isEdit) {
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
  const addFilter   = () => update({ filters: [...form.filters, { field: '', operator: '', value: '' }] });
  const removeFilter= (i) => update({ filters: form.filters.filter((_, idx) => idx !== i) });

  const currentDataSource = useMemo(
    () => dataSources.find(ds => ds.key === form.dataSource),
    [dataSources, form.dataSource]
  );
  const supportedAggKeys = currentDataSource?.supportedAggregations || [];

  const aggregateFields = useMemo(() => fields.filter(f =>
    f.role === 'METRIC' &&
    Array.isArray(f.applicableAggregations) &&
    f.applicableAggregations.includes(form.metricFunc)
  ), [fields, form.metricFunc]);

  const dimensionFields = useMemo(() => fields.filter(f => f.groupable === true), [fields]);

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

  // ─── Render ────────────────────────────────────────────────────────────────

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">{isEdit ? 'Widget Düzenle' : 'Widget Oluştur'}</h1>
          <p className="page-sub">
            {isEdit
              ? 'Var olan widget\'ı güncelle'
              : `${STEPS[step - 1].label} — ${STEPS[step - 1].desc}`}
          </p>
        </div>
        <div className="page-actions">
          <button className="btn ghost" onClick={() => onNav('/widgets')}>
            <Icon name="folder" size={14} /> Widget'larım
          </button>
        </div>
      </div>

      {(isEdit || true) && (
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
                      onClick={() => {
                        const supported = ds.supportedAggregations || [];
                        const nextFunc = supported.includes(form.metricFunc)
                          ? form.metricFunc
                          : (supported[0] || 'COUNT');
                        update({ dataSource: ds.key, metricFunc: nextFunc, metricField: '', groupField: '', groupTransform: '', filters: [] });
                      }}
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
                      {supportedAggKeys.map(k => {
                        const m = METRIC_LABELS[k];
                        if (!m) return null;
                        return (
                          <button
                            key={k}
                            className={`btn sm ${form.metricFunc === k ? 'primary' : 'ghost'}`}
                            onClick={() => update({ metricFunc: k, metricField: '' })}
                            title={m.hint}
                          >
                            {m.label}
                          </button>
                        );
                      })}
                    </div>

                    {form.metricFunc !== 'COUNT' && (
                      <div className="col gap-6">
                        <label style={{ fontSize: 12, fontWeight: 500 }}>Hangi alanın {METRIC_LABELS[form.metricFunc]?.label.toLowerCase()}'ı?</label>
                        <select className="input" value={form.metricField} onChange={e => update({ metricField: e.target.value })}>
                          <option value="">Alan seçin</option>
                          {aggregateFields.map(f => (
                            <option key={f.key} value={f.key}>{f.label}</option>
                          ))}
                        </select>
                        {aggregateFields.length === 0 && (
                          <span style={{ fontSize: 11, color: 'var(--warn)' }}>
                            Bu hesaplama için uygun alan yok. Farklı bir hesaplama veya Adet seçin.
                          </span>
                        )}
                      </div>
                    )}

                    <div style={{ borderTop: '1px solid var(--line)', paddingTop: 14, marginTop: 14 }}>
                      <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 4 }}>3. Gruplama (opsiyonel)</h3>
                      <p style={{ fontSize: 12, color: 'var(--ink-3)', marginBottom: 10 }}>
                        Tek bir değer mi istersin yoksa kategorilere/aylara mı bölünsün?
                      </p>
                      <div className="row gap-8">
                        <select className="input" style={{ flex: 1 }} value={form.groupField} onChange={e => update({ groupField: e.target.value, groupTransform: '' })} disabled={dimensionFields.length === 0}>
                          <option value="">{dimensionFields.length === 0 ? 'Gruplanabilir alan yok' : 'Gruplama yok (tek değer)'}</option>
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
                  const ops = meta?.applicableFilterOps || [];
                  return (
                    <div key={i} className="row gap-8" style={{ alignItems: 'center', background: 'var(--bg-2)', padding: 10, borderRadius: 10, flexWrap: 'wrap' }}>
                      <select className="input" style={{ minWidth: 140, flex: '1 1 140px' }} value={f.field} onChange={e => {
                        const nextField = e.target.value;
                        const nextMeta = fields.find(fld => fld.key === nextField);
                        const nextOps = nextMeta?.applicableFilterOps || [];
                        setFilter(i, { field: nextField, operator: nextOps[0] || 'EQ', value: '' });
                      }}>
                        <option value="">Alan</option>
                        {fields.map(fld => <option key={fld.key} value={fld.key}>{fld.label}</option>)}
                      </select>
                      <select
                        className="input"
                        style={{ minWidth: 110 }}
                        value={f.operator}
                        onChange={e => setFilter(i, { operator: e.target.value })}
                        disabled={!f.field}
                      >
                        {!f.field && <option value="">Önce alan seçin</option>}
                        {ops.map(opKey => (
                          <option key={opKey} value={opKey}>{FILTER_OP_LABELS[opKey] || opKey}</option>
                        ))}
                      </select>
                      {isEnum && meta?.options ? (
                        <select className="input" style={{ flex: 1, minWidth: 140 }} value={f.value} onChange={e => setFilter(i, { value: e.target.value })}>
                          <option value="">Seçin</option>
                          {meta.options.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                        </select>
                      ) : (
                        <input
                          className="input"
                          style={{ flex: 1, minWidth: 140 }}
                          placeholder="Değer"
                          type={meta?.type === 'NUMBER' ? 'number' : meta?.type === 'DATE' ? 'date' : 'text'}
                          value={f.value}
                          onChange={e => setFilter(i, { value: e.target.value })}
                        />
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
