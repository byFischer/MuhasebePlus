import React, { useState } from 'react';
import Icon from '@/components/mp/Icon';
import { TRY } from '@/lib/format';
import { useBudgets, useCreateBudget, useUpdateBudget, useDeleteBudget } from '@/hooks/useBudgets';

const MONTHS = [
  'Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
  'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık',
];

const CURRENT_YEAR = new Date().getFullYear();
const YEARS = [CURRENT_YEAR - 1, CURRENT_YEAR, CURRENT_YEAR + 1];

const BLANK_FORM = {
  year: CURRENT_YEAR,
  month: new Date().getMonth() + 1,
  category: '',
  transactionType: 'INCOME',
  plannedAmount: '',
  notes: '',
};

export default function BudgetPage() {
  const [filterYear, setFilterYear] = useState(CURRENT_YEAR);
  const [form, setForm] = useState(BLANK_FORM);
  const [editId, setEditId] = useState(null);

  const { data: budgets = [], isLoading, isError, refetch } = useBudgets(filterYear);
  const createMut = useCreateBudget();
  const updateMut = useUpdateBudget();
  const deleteMut = useDeleteBudget();

  const isBusy = createMut.isPending || updateMut.isPending;

  const setField = (key, val) => setForm(f => ({ ...f, [key]: val }));

  const resetForm = () => { setForm(BLANK_FORM); setEditId(null); };

  const onEdit = (b) => {
    setEditId(b.budgetId);
    setForm({
      year: b.year,
      month: b.month,
      category: b.category,
      transactionType: b.transactionType,
      plannedAmount: b.plannedAmount,
      notes: b.notes || '',
    });
  };

  const parseAmount = (val) => {
    // "250.000" veya "250,000" → 250000 | "250,50" → 250.50
    const s = String(val).trim();
    // Virgül ondalık ayırıcıysa: "250.000,50" → "250000.50"
    if (s.includes(',')) {
      return parseFloat(s.replace(/\./g, '').replace(',', '.')) || 0;
    }
    // Sadece nokta varsa: "250.000" → 250000 (binlik ayırıcı olarak)
    const parts = s.split('.');
    if (parts.length === 2 && parts[1].length === 3) {
      return parseFloat(s.replace(/\./g, '')) || 0;
    }
    return parseFloat(s) || 0;
  };

  const onSubmit = () => {
    const dto = {
      year: Number(form.year),
      month: Number(form.month),
      category: form.category.trim(),
      transactionType: form.transactionType,
      plannedAmount: parseAmount(form.plannedAmount),
      notes: form.notes.trim() || null,
    };
    if (!dto.category || !dto.plannedAmount) return;
    if (editId) {
      updateMut.mutate({ id: editId, dto }, { onSuccess: resetForm });
    } else {
      createMut.mutate(dto, { onSuccess: resetForm });
    }
  };

  const onDelete = (id) => {
    if (window.confirm('Bu bütçe kalemini silmek istediğinize emin misiniz?')) {
      deleteMut.mutate(id);
    }
  };

  // Group budgets by month for display
  const grouped = {};
  budgets.forEach(b => {
    const key = `${b.year}-${String(b.month).padStart(2, '0')}`;
    if (!grouped[key]) grouped[key] = [];
    grouped[key].push(b);
  });
  const sortedKeys = Object.keys(grouped).sort((a, b) => a.localeCompare(b));

  const totalPlanned = budgets.reduce((s, b) => s + Number(b.plannedAmount || 0), 0);
  const incomeTotal = budgets.filter(b => b.transactionType === 'INCOME')
    .reduce((s, b) => s + Number(b.plannedAmount || 0), 0);
  const expenseTotal = budgets.filter(b => b.transactionType === 'EXPENSE')
    .reduce((s, b) => s + Number(b.plannedAmount || 0), 0);

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Bütçe Yönetimi</h1>
          <p className="page-sub">Aylık gelir ve gider planlarını yönetin</p>
        </div>
        <div className="page-actions">
          <button className="btn" onClick={resetForm}>
            <Icon name="plus" /> Yeni Kalem
          </button>
        </div>
      </div>

      {/* KPI row */}
      <div className="grid-3" style={{ gap: 12, marginBottom: 16 }}>
        <div className="card" style={{ padding: '12px 16px' }}>
          <div className="muted" style={{ fontSize: 11 }}>Toplam Plan ({filterYear})</div>
          <div className="mono tnum" style={{ fontSize: 20, fontWeight: 600 }}>{TRY(totalPlanned)}</div>
        </div>
        <div className="card" style={{ padding: '12px 16px' }}>
          <div className="muted" style={{ fontSize: 11 }}>Planlanan Gelir</div>
          <div className="mono tnum" style={{ fontSize: 20, fontWeight: 600, color: 'var(--pos)' }}>{TRY(incomeTotal)}</div>
        </div>
        <div className="card" style={{ padding: '12px 16px' }}>
          <div className="muted" style={{ fontSize: 11 }}>Planlanan Gider</div>
          <div className="mono tnum" style={{ fontSize: 20, fontWeight: 600, color: 'var(--neg)' }}>{TRY(expenseTotal)}</div>
        </div>
      </div>

      <div className="grid-2" style={{ gap: 16, alignItems: 'start' }}>
        {/* Form */}
        <div className="card">
          <div className="card-h">
            <h3>{editId ? 'Bütçe Düzenle' : 'Yeni Bütçe Kalemi'}</h3>
          </div>
          <div className="card-b col gap-12">
            <div className="grid-2">
              <div className="field">
                <label>Yıl</label>
                <select className="select" value={form.year} onChange={e => setField('year', e.target.value)}>
                  {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
                </select>
              </div>
              <div className="field">
                <label>Ay</label>
                <select className="select" value={form.month} onChange={e => setField('month', e.target.value)}>
                  {MONTHS.map((m, i) => <option key={i + 1} value={i + 1}>{m}</option>)}
                </select>
              </div>
            </div>

            <div className="field">
              <label>Kategori</label>
              <input
                className="input"
                placeholder="Örn: Satış Gelirleri, Kira, Personel"
                value={form.category}
                onChange={e => setField('category', e.target.value)}
              />
            </div>

            <div className="field">
              <label>Tip</label>
              <div className="seg">
                <button
                  type="button"
                  className={form.transactionType === 'INCOME' ? 'on' : ''}
                  onClick={() => setField('transactionType', 'INCOME')}
                >
                  Gelir
                </button>
                <button
                  type="button"
                  className={form.transactionType === 'EXPENSE' ? 'on' : ''}
                  onClick={() => setField('transactionType', 'EXPENSE')}
                >
                  Gider
                </button>
              </div>
            </div>

            <div className="field">
              <label>Planlanan Tutar (TL)</label>
              <input
                className="input mono"
                type="text"
                inputMode="decimal"
                placeholder="Örn: 250.000 veya 250000"
                value={form.plannedAmount}
                onChange={e => setField('plannedAmount', e.target.value)}
              />
            </div>

            <div className="field">
              <label>Notlar (opsiyonel)</label>
              <input
                className="input"
                placeholder="Açıklama..."
                value={form.notes}
                onChange={e => setField('notes', e.target.value)}
              />
            </div>

            <div className="row gap-8">
              <button
                className="btn primary"
                onClick={onSubmit}
                disabled={isBusy || !form.category || !form.plannedAmount}
              >
                <Icon name={editId ? 'check' : 'plus'} />
                {isBusy ? 'Kaydediliyor…' : editId ? 'Güncelle' : 'Ekle'}
              </button>
              {editId && (
                <button className="btn" onClick={resetForm}>İptal</button>
              )}
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="card">
          <div className="card-h">
            <h3>Bütçe Kalemleri</h3>
            <div className="row gap-8">
              <select
                className="select"
                value={filterYear}
                onChange={e => setFilterYear(Number(e.target.value))}
                style={{ width: 90 }}
              >
                {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
              </select>
            </div>
          </div>

          {isLoading && <div className="empty">Yükleniyor…</div>}
          {!isLoading && isError && (
            <div className="empty">
              Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button>
            </div>
          )}
          {!isLoading && !isError && budgets.length === 0 && (
            <div className="empty">Henüz bütçe kalemi yok</div>
          )}
          {!isLoading && !isError && budgets.length > 0 && (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Dönem</th>
                    <th>Kategori</th>
                    <th>Tip</th>
                    <th>Plan</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {sortedKeys.map(key => (
                    grouped[key].map((b, i) => (
                      <tr key={b.budgetId}>
                        {i === 0 && (
                          <td rowSpan={grouped[key].length} style={{ verticalAlign: 'middle' }} className="muted">
                            {MONTHS[b.month - 1]} {b.year}
                          </td>
                        )}
                        <td>{b.category}</td>
                        <td>
                          <span className={`pill ${b.transactionType === 'INCOME' ? 'info' : ''}`}>
                            {b.transactionType === 'INCOME' ? 'Gelir' : 'Gider'}
                          </span>
                        </td>
                        <td className="mono tnum">
                          <span style={{ color: b.transactionType === 'INCOME' ? 'var(--pos)' : 'var(--neg)' }}>
                            {TRY(b.plannedAmount)}
                          </span>
                        </td>
                        <td>
                          <div className="row gap-4">
                            <button className="tb-icon-btn" title="Düzenle" onClick={() => onEdit(b)}>
                              <Icon name="edit" size={14} />
                            </button>
                            <button className="tb-icon-btn" title="Sil" onClick={() => onDelete(b.budgetId)}>
                              <Icon name="trash" size={14} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
