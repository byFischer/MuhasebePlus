import React, { useState, useEffect } from 'react';
import Icon from '@/components/mp/Icon';
import Drawer from '@/components/mp/Drawer';
import AnimatedNumber from '@/components/mp/AnimatedNumber';
import { TRY } from '@/lib/format';
import { useBankAccounts, useCreateBankAccount, useDeleteBankAccount, useBankList } from '@/hooks/useBankAccounts';
import ZiraatLogo        from '@/icons/Ziraat_Bankasi_Logo_JPEG.jpg';
import HalkbankLogo      from '@/icons/halkbank-logo-png_seeklogo-529774.png';
import VakifbankLogo     from '@/icons/Vakifbank-Logo-Small.png';
import GarantiLogo       from '@/icons/png-clipart-ge-garanti-bank-logo-private-banking-bank-leaf-text-thumbnail.png';
import IsBankasiLogo     from '@/icons/turkiye-is-bankasi-vector-logo-download-free-11574033113bz3sqrvhcs.png';
import AkbankLogo        from '@/icons/png-transparent-akbank-hd-logo.png';
import YapiKrediLogo     from '@/icons/Yapı_kredi_logo.png';
import DenizbankLogo     from '@/icons/DenizBank_logo.svg.png';
import QnbLogo           from '@/icons/20161024191547!Qnb-finansbank.png';
import TebLogo           from '@/icons/TEB_LOGO.png';
import IngLogo           from '@/icons/png-transparent-logo-ing-group-bank-advantage-company-text-hand.png';
import HsbcLogo          from '@/icons/HSBC_logo_(2018).svg.png';
import SekerbankLogo     from '@/icons/Şekerbank_logo.svg.png';
import OdeabankLogo      from '@/icons/Odeabank_logo.png';
import AlternatifLogo    from '@/icons/Alternatif_Bank_logo.png';
import BurganLogo        from '@/icons/Burgan_Bank_logo.svg.png';
import FibabankaLogo     from '@/icons/Fibabanka_RGB.png';
import TurkiyeFinansLogo from '@/icons/tf_logo_guide_01.png';
import KuveytTurkLogo    from '@/icons/Kuveyt_Türk_Logo.png';
import AlbarakaLogo      from '@/icons/id_75197.png';
import ZiraatKatilimLogo from '@/icons/Ziraat_Katılım_Bankası_logo.png';
import VakifKatilimLogo  from '@/icons/Vakıf_Katılım_Logo.png';
import EmlakKatilimLogo  from '@/icons/Emlak_Katılım_logo.svg.png';
import PttLogo           from '@/icons/562_ptt_bank.jpg';

const IBAN_RE = /^TR[0-9]{24}$/;
const CURRENCY_SYMBOL = { TRY: '₺', USD: '$', EUR: '€' };
const fmt = (amount, currency) => TRY(amount, { currency: CURRENCY_SYMBOL[currency] || currency });

const BANK_LOGOS = {
  'Ziraat Bankası':     ZiraatLogo,
  'Halkbank':           HalkbankLogo,
  'Vakıfbank':          VakifbankLogo,
  'Garanti BBVA':       GarantiLogo,
  'Türkiye İş Bankası': IsBankasiLogo,
  'Akbank':             AkbankLogo,
  'Yapı Kredi':         YapiKrediLogo,
  'Denizbank':          DenizbankLogo,
  'QNB Finansbank':     QnbLogo,
  'TEB':                TebLogo,
  'İNG Bank':           IngLogo,
  'HSBC':               HsbcLogo,
  'Şekerbank':          SekerbankLogo,
  'Odeabank':           OdeabankLogo,
  'Alternatif Bank':    AlternatifLogo,
  'Burgan Bank':        BurganLogo,
  'Fibabanka':          FibabankaLogo,
  'Türkiye Finans':     TurkiyeFinansLogo,
  'Kuveyt Türk':        KuveytTurkLogo,
  'Albaraka Türk':      AlbarakaLogo,
  'Ziraat Katılım':     ZiraatKatilimLogo,
  'Vakıf Katılım':      VakifKatilimLogo,
  'Emlak Katılım':      EmlakKatilimLogo,
  'PTT':                PttLogo,
};

function BankLogo({ bankName }) {
  const logo = BANK_LOGOS[bankName];
  if (logo) {
    return (
      <img
        src={logo}
        alt={bankName}
        style={{ width: 48, height: 48, borderRadius: 8, objectFit: 'contain', background: '#fff' }}
      />
    );
  }
  return (
    <div style={{ width: 48, height: 48, borderRadius: 8, background: 'var(--accent-soft)', color: 'var(--accent-ink)', display: 'grid', placeItems: 'center' }}>
      <Icon name="bank" size={20} />
    </div>
  );
}

export default function BankaPage() {
  const { data: list = [], isLoading, isError, refetch } = useBankAccounts();
  const deleteMut = useDeleteBankAccount();
  const [drawer, setDrawer] = useState(false);

  if (isLoading) return <div className="page"><div className="card" style={{ height: 200 }} /></div>;
  if (isError) return <div className="page"><div className="card empty">Veri alınamadı <button className="btn sm" onClick={() => refetch()}>Tekrar Dene</button></div></div>;

  return (
    <div className="page">
      <div className="page-head">
        <div><h1 className="page-title">Banka &amp; Kasa Hesapları</h1><p className="page-sub">{list.length} hesap</p></div>
        <div className="page-actions">
          <button className="btn primary" onClick={() => setDrawer(true)}><Icon name="plus" /> Yeni Hesap</button>
        </div>
      </div>
      <div className="grid-3">
        {list.map(b => (
          <div key={b.bankAccountId || b.accountId} className="card">
            <div className="card-h">
              <div className="row gap-8">
                <BankLogo bankName={b.accountName || b.bankName} />
                <div>
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{b.accountName || b.bankName}</div>
                  <div className="muted" style={{ fontSize: 11 }}>
                    {b.accountType === 'CASH' ? 'Kasa' : b.accountType === 'POS' ? 'POS' : 'Banka'} · {b.currency || 'TRY'}{b.accountCode ? ` · ${b.accountCode}` : ''}
                  </div>
                </div>
              </div>
              <button className="tb-icon-btn" onClick={() => deleteMut.mutate(b.bankAccountId || b.accountId)}><Icon name="trash" size={14} /></button>
            </div>
            <div className="card-b">
              <div className="mono tnum" style={{ fontSize: 22, fontWeight: 600 }}>
                <AnimatedNumber
                  value={b.balance || b.currentBalance || 0}
                  formatter={(v) => fmt(v, b.currency)}
                  duration={800}
                />
              </div>
              {b.iban && (
                <CopyIban iban={b.iban} />
              )}
            </div>
          </div>
        ))}
        {list.length === 0 && <div className="card empty">Henüz hesap eklenmemiş</div>}
      </div>
      <BankAccountDrawer open={drawer} onClose={() => setDrawer(false)} />
    </div>
  );
}

function CopyIban({ iban }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = (e) => {
    e.stopPropagation();
    navigator.clipboard.writeText(iban);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4 }}>
      <div className="muted mono" style={{ fontSize: 11 }}>{iban}</div>
      <button
        className="tb-icon-btn"
        style={{
          width: 22, height: 22,
          background: copied ? 'var(--pos)' : 'transparent',
          color: copied ? '#fff' : 'var(--ink-2)',
          boxShadow: copied ? '0 0 8px var(--pos)' : 'none',
          transition: 'all 0.2s ease',
        }}
        title="IBAN'ı kopyala"
        onClick={handleCopy}
      >
        <Icon name={copied ? 'check' : 'copy'} size={12} />
      </button>
    </div>
  );
}

function BankAccountDrawer({ open, onClose }) {
  const createMut = useCreateBankAccount();
  const { data: bankList = [] } = useBankList();
  const EMPTY = { accountType: 'BANK', bankName: '', iban: '', currency: 'TRY' };
  const [f, setF] = useState(EMPTY);
  const ibanValid = IBAN_RE.test('TR' + f.iban);
  const valid = f.accountType === 'BANK' ? Boolean(f.bankName && ibanValid) : Boolean(f.bankName);

  useEffect(() => { if (!open) setF(EMPTY); }, [open]);

  const save = () => {
    if (!valid) return;
    createMut.mutate(
      {
        accountType: f.accountType,
        bankName: f.bankName,
        iban: f.accountType === 'BANK' ? 'TR' + f.iban.trim() : null,
        currency: f.currency
      },
      { onSuccess: onClose }
    );
  };

  return (
    <Drawer open={open} onClose={onClose} closeOnBackdrop={false} title="Yeni Banka Hesabı"
      footer={
        <>
          <button className="btn ghost" onClick={onClose}>Vazgeç</button>
          <button className="btn primary" disabled={!valid || createMut.isPending} onClick={save}>Kaydet</button>
        </>
      }>
      <div className="col gap-12">
        <div className="field">
          <label>Hesap Tipi *</label>
          <select
            className="input"
            value={f.accountType}
            onChange={e => setF({ ...f, accountType: e.target.value, bankName: '', iban: '' })}
          >
            <option value="BANK">Banka</option>
            <option value="CASH">Kasa</option>
            <option value="POS">POS</option>
          </select>
        </div>
        <div className="field">
          <label>{f.accountType === 'CASH' ? 'Kasa Adi *' : f.accountType === 'POS' ? 'POS Hesabi *' : 'Banka *'}</label>
          {f.accountType !== 'BANK' ? (
            <input
              className="input"
              value={f.bankName}
              onChange={e => setF({ ...f, bankName: e.target.value })}
              placeholder={f.accountType === 'POS' ? 'POS Hesabi' : 'Merkez Kasa'}
            />
          ) : (
          <select
            className="input"
            value={f.bankName}
            onChange={e => setF({ ...f, bankName: e.target.value })}
            style={{ cursor: 'pointer' }}
          >
            <option value="">Banka seçin...</option>
            {bankList.map(b => <option key={b} value={b}>{b}</option>)}
          </select>
          )}
        </div>
        {f.accountType === 'BANK' && <div className="field">
          <label>IBAN * <span className="muted" style={{ fontSize: 11 }}>24 rakam</span></label>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <span className="input mono" style={{
              width: 36, flexShrink: 0, borderRight: 'none', borderRadius: '6px 0 0 6px',
              background: 'var(--bg-2)', color: 'var(--text-2)', userSelect: 'none',
              display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600,
            }}>TR</span>
            <input
              className={`input mono ${f.iban && !ibanValid ? 'is-error' : ''}`}
              style={{ borderRadius: '0 6px 6px 0', flex: 1 }}
              value={f.iban}
              onChange={e => setF({ ...f, iban: e.target.value.replace(/\D/g, '').slice(0, 24) })}
              placeholder="000000000000000000000000"
              maxLength={24}
            />
          </div>
          {f.iban && !ibanValid && (
            <span style={{ fontSize: 11, color: 'var(--neg)', marginTop: 4, display: 'block' }}>
              24 haneli rakam girin
            </span>
          )}
        </div>}
        <div className="field">
          <label>Para Birimi *</label>
          <select className="input" value={f.currency} onChange={e => setF({ ...f, currency: e.target.value })}>
            <option value="TRY">₺ TRY — Türk Lirası</option>
            <option value="USD">$ USD — Amerikan Doları</option>
            <option value="EUR">€ EUR — Euro</option>
          </select>
        </div>
      </div>
    </Drawer>
  );
}
