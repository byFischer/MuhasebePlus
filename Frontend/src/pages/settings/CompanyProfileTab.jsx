import { useEffect, useState } from 'react';
import { useCurrentCompany, useUpdateCurrentCompany } from '@/hooks/useCompany';

export default function CompanyProfileTab() {
  const { data: company, isLoading } = useCurrentCompany();
  const updateMut = useUpdateCurrentCompany();

  const [f, setF] = useState({
    companyName: '',
    taxNumber: '',
    taxOffice: '',
    taxOfficeCode: '',
    address: '',
    city: '',
    phone: '',
    email: '',
    tradeRegistryNo: '',
    mersisNo: '',
    nace: '',
    declarationPeriodType: 'MONTHLY',
    corporateTaxType: 'GELIR_VERGISI',
  });

  useEffect(() => {
    if (company) {
      setF({
        companyName: company.companyName || '',
        taxNumber: company.taxNumber || '',
        taxOffice: company.taxOffice || '',
        taxOfficeCode: company.taxOfficeCode || '',
        address: company.address || '',
        city: company.city || '',
        phone: company.phone || '',
        email: company.email || '',
        tradeRegistryNo: company.tradeRegistryNo || '',
        mersisNo: company.mersisNo || '',
        nace: company.nace || '',
        declarationPeriodType: company.declarationPeriodType || 'MONTHLY',
        corporateTaxType: company.corporateTaxType || 'GELIR_VERGISI',
      });
    }
  }, [company]);

  const profileIncomplete = !company?.mersisNo || !company?.taxOffice;

  const save = (e) => {
    e.preventDefault();
    if (!f.companyName.trim()) return;
    updateMut.mutate(f);
  };

  if (isLoading) return <div className="muted" style={{ padding: 24 }}>Yükleniyor...</div>;

  return (
    <div className="col gap-16">
      {profileIncomplete && (
        <div style={{
          padding: '10px 14px',
          background: 'var(--red-soft, #fff0f0)',
          border: '1px solid var(--red, #e53e3e)',
          borderRadius: 6,
          fontSize: 13,
          color: 'var(--red, #c53030)',
        }}>
          Beyanname üretebilmek için <strong>Vergi Dairesi</strong> ve <strong>MERSİS No</strong> bilgilerini tamamlayın.
        </div>
      )}

      <form onSubmit={save} className="col gap-12">
        <div className="grid-2">
          <div className="field">
            <label>Şirket Adı *</label>
            <input className="input" value={f.companyName} onChange={e => setF({ ...f, companyName: e.target.value })} required />
          </div>
          <div className="field">
            <label>Vergi Numarası</label>
            <input className="input mono" value={f.taxNumber} onChange={e => setF({ ...f, taxNumber: e.target.value })} maxLength={20} />
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Vergi Dairesi</label>
            <input className="input" value={f.taxOffice} onChange={e => setF({ ...f, taxOffice: e.target.value })} placeholder="Kadıköy" maxLength={255} />
          </div>
          <div className="field">
            <label>Vergi Dairesi Kodu</label>
            <input className="input mono" value={f.taxOfficeCode} onChange={e => setF({ ...f, taxOfficeCode: e.target.value })} placeholder="34110" maxLength={10} />
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>MERSİS No</label>
            <input className="input mono" value={f.mersisNo} onChange={e => setF({ ...f, mersisNo: e.target.value })} placeholder="16 haneli" maxLength={20} />
          </div>
          <div className="field">
            <label>Ticaret Sicil No</label>
            <input className="input mono" value={f.tradeRegistryNo} onChange={e => setF({ ...f, tradeRegistryNo: e.target.value })} maxLength={30} />
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>NACE Kodu</label>
            <input className="input mono" value={f.nace} onChange={e => setF({ ...f, nace: e.target.value })} placeholder="Örn: 6201" maxLength={10} />
          </div>
          <div className="field">
            <label>Mükellefiyet Türü</label>
            <select className="input" value={f.corporateTaxType} onChange={e => setF({ ...f, corporateTaxType: e.target.value })}>
              <option value="GELIR_VERGISI">Gelir Vergisi</option>
              <option value="KURUMLAR_VERGISI">Kurumlar Vergisi</option>
            </select>
          </div>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Beyanname Periyodu</label>
            <select className="input" value={f.declarationPeriodType} onChange={e => setF({ ...f, declarationPeriodType: e.target.value })}>
              <option value="MONTHLY">Aylık</option>
              <option value="QUARTERLY">Üç Aylık</option>
            </select>
          </div>
          <div className="field">
            <label>Şehir</label>
            <input className="input" value={f.city} onChange={e => setF({ ...f, city: e.target.value })} maxLength={100} />
          </div>
        </div>

        <div className="field">
          <label>Adres</label>
          <input className="input" value={f.address} onChange={e => setF({ ...f, address: e.target.value })} maxLength={255} />
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Telefon</label>
            <input className="input mono" value={f.phone} onChange={e => setF({ ...f, phone: e.target.value })} maxLength={20} />
          </div>
          <div className="field">
            <label>E-posta</label>
            <input className="input" type="email" value={f.email} onChange={e => setF({ ...f, email: e.target.value })} maxLength={255} />
          </div>
        </div>

        <div className="row" style={{ justifyContent: 'flex-end', marginTop: 8 }}>
          <button className="btn primary" type="submit" disabled={updateMut.isPending}>
            {updateMut.isPending ? 'Kaydediliyor...' : 'Kaydet'}
          </button>
        </div>
      </form>
    </div>
  );
}
