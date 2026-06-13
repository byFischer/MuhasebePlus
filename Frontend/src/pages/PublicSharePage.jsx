import { useState } from 'react';
import { useParams } from 'react-router-dom';
import Icon from '@/components/mp/Icon';
import Pagination from '@/components/mp/Pagination';
import { TRY } from '@/lib/format';
import { toast } from '@/lib/toast';
import { usePublicShareInfo, usePublicShareInvoices, usePublicShareSummary } from '@/hooks/usePublicShare';
import publicShareService from '@/services/publicShareService';

const PAGE_SIZE = 20;

function StatusPill({ status, cancelled }) {
  if (cancelled) return <span className="pill neg"><span className="dot" />İptal</span>;
  const map = {
    pending: { cls: 'info', l: 'Beklemede' },
    partially_paid: { cls: 'warn', l: 'Kısmi Ödeme' },
    paid:    { cls: 'pos',  l: 'Ödendi' },
    overdue: { cls: 'neg',  l: 'Gecikmiş' },
  };
  const s = map[status] || { cls: '', l: status };
  return <span className={`pill ${s.cls}`}><span className="dot" />{s.l}</span>;
}

function SummaryCard({ title, summary }) {
  return (
    <div className="card" style={{ flex: 1, padding: 16 }}>
      <p className="muted" style={{ fontSize: 12, marginBottom: 4 }}>{title}</p>
      <p style={{ fontSize: 20, fontWeight: 700 }} className="mono tnum">{TRY(summary?.totalAmount || 0)}</p>
      <p className="muted" style={{ fontSize: 11 }}>
        {summary?.count || 0} fatura · KDV: <span className="mono tnum">{TRY(summary?.vatAmount || 0)}</span>
      </p>
    </div>
  );
}

export default function PublicSharePage() {
  const { token } = useParams();
  const [type, setType] = useState('');
  const [status, setStatus] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);

  const invoiceParams = {
    page: page - 1,
    size: PAGE_SIZE,
    ...(type && { invoiceType: type }),
    ...(status && { paymentStatus: status }),
    ...(startDate && { startDate }),
    ...(endDate && { endDate }),
    ...(search && { search }),
  };
  const summaryParams = {
    ...(startDate && { startDate }),
    ...(endDate && { endDate }),
  };

  const { data: info, isLoading: infoLoading, isError: infoError } = usePublicShareInfo(token);
  const { data: invoicePage, isLoading, isFetching, refetch, dataUpdatedAt } = usePublicShareInvoices(token, invoiceParams);
  const { data: summary } = usePublicShareSummary(token, summaryParams);

  const handleDownloadPdf = async (invoiceId) => {
    try {
      const blob = await publicShareService.downloadPdf(token, invoiceId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'fatura-' + invoiceId + '.pdf';
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.err('PDF indirilemedi');
    }
  };

  if (infoError) {
    return (
      <div className="page" style={{ maxWidth: 560, margin: '80px auto', textAlign: 'center' }}>
        <div className="card" style={{ padding: 40 }}>
          <Icon name="alert" size={40} style={{ margin: '0 auto 16px' }} />
          <h1 className="page-title" style={{ marginBottom: 8 }}>Bağlantı geçersiz</h1>
          <p className="muted">
            Bu paylaşım bağlantısı bulunamadı veya iptal edilmiş.
            Lütfen yeni bir bağlantı için iş sahibiyle iletişime geçin.
          </p>
        </div>
      </div>
    );
  }

  if (infoLoading) {
    return <div className="page" style={{ maxWidth: 1100, margin: '24px auto' }}><div className="card" style={{ height: 200 }} /></div>;
  }

  const list = invoicePage?.content || [];
  const totalPages = Math.max(1, invoicePage?.totalPages || 1);
  const totalElements = invoicePage?.totalElements || 0;
  const pageStart = (page - 1) * PAGE_SIZE;
  const pageEnd = pageStart + list.length;
  const saleSummary = summary?.summaries?.find(s => s.invoiceType === 'sale');
  const purchaseSummary = summary?.summaries?.find(s => s.invoiceType === 'purchase');

  const changeFilter = (setter) => (value) => { setter(value); setPage(1); };

  return (
    <div className="page" style={{ maxWidth: 1100, margin: '24px auto', padding: '0 16px' }}>
      <div className="page-head">
        <div>
          <h1 className="page-title">{info?.companyName}</h1>
          <p className="page-sub">Fatura Listesi · Mali Müşavir Görünümü</p>
        </div>
        <div className="page-actions" style={{ alignItems: 'center' }}>
          <span className="muted" style={{ fontSize: 11 }}>
            Son güncelleme: {dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString('tr-TR') : '—'}
          </span>
          <button className="btn ghost" disabled={isFetching} onClick={() => refetch()}>
            <Icon name="refresh" size={14} /> {isFetching ? 'Yenileniyor...' : 'Yenile'}
          </button>
        </div>
      </div>

      <div className="row gap-12" style={{ marginBottom: 16 }}>
        <SummaryCard title="Satış Faturaları" summary={saleSummary} />
        <SummaryCard title="Alış Faturaları" summary={purchaseSummary} />
      </div>

      <div className="card">
        <div className="toolbar" style={{ flexWrap: 'wrap', gap: 8 }}>
          <div className="tb-search" style={{ margin: 0, width: 240 }}>
            <Icon name="search" size={14} />
            <input value={search} onChange={e => changeFilter(setSearch)(e.target.value)} placeholder="Fatura no, açıklama ara..." />
          </div>
          <div className="seg">
            {[['', 'Tümü'], ['sale', 'Satış'], ['purchase', 'Alış']].map(([k, l]) =>
              <button key={k} className={type === k ? 'on' : ''} onClick={() => changeFilter(setType)(k)}>{l}</button>
            )}
          </div>
          <select className="input" style={{ width: 140 }} value={status} onChange={e => changeFilter(setStatus)(e.target.value)}>
            <option value="">Tüm Durumlar</option>
            <option value="pending">Beklemede</option>
            <option value="partially_paid">Kısmi Ödeme</option>
            <option value="paid">Ödendi</option>
            <option value="overdue">Gecikmiş</option>
          </select>
          <input className="input" type="date" style={{ width: 150 }} value={startDate} onChange={e => changeFilter(setStartDate)(e.target.value)} />
          <input className="input" type="date" style={{ width: 150 }} value={endDate} onChange={e => changeFilter(setEndDate)(e.target.value)} />
        </div>

        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>Fatura No</th><th>Cari</th><th>Tür</th><th>Tarih</th><th>Vade</th><th className="num">Matrah</th><th className="num">KDV</th><th className="num">Toplam</th><th>Durum</th><th></th></tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan="10" className="empty">Yükleniyor...</td></tr>
              ) : list.length === 0 ? (
                <tr><td colSpan="10" className="empty">Fatura bulunamadı</td></tr>
              ) : list.map(i => (
                <tr key={i.invoiceId} style={{ opacity: i.cancelled ? 0.5 : 1 }}>
                  <td className="mono">{i.invoiceNumber}</td>
                  <td><b>{i.customerName}</b></td>
                  <td><span className="pill">{i.invoiceType === 'sale' ? 'Satış' : 'Alış'}</span></td>
                  <td className="muted">{i.invoiceDate}</td>
                  <td className="muted">{i.dueDate}</td>
                  <td className="num mono tnum">{TRY(i.subtotal || 0)}</td>
                  <td className="num mono tnum">{TRY(i.vatAmount || 0)}</td>
                  <td className="num mono tnum"><b>{TRY(i.totalAmount || 0)}</b></td>
                  <td><StatusPill status={i.paymentStatus} cancelled={i.cancelled} /></td>
                  <td>
                    <button className="tb-icon-btn" title="PDF İndir" onClick={() => handleDownloadPdf(i.invoiceId)}>
                      <Icon name="download" size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={totalPages} setPage={setPage} pageStart={pageStart} pageEnd={pageEnd} total={totalElements} />
      </div>

      <p className="muted" style={{ fontSize: 11, textAlign: 'center', marginTop: 16 }}>
        Bu sayfa MuhasebePlus ile oluşturulmuştur · Veriler canlı olarak güncellenir
      </p>
    </div>
  );
}
