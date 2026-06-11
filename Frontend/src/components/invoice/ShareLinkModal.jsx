import React from 'react';
import Icon from '@/components/mp/Icon';
import { toast } from '@/lib/toast';
import { useShareLink, useRegenerateShareLink, useRevokeShareLink } from '@/hooks/useShareLink';

function buildShareUrl(token) {
  const isHash = import.meta.env.VITE_ROUTER === 'hash';
  const base = isHash
    ? `${window.location.origin}${window.location.pathname}#`
    : window.location.origin;
  return `${base}/paylasim/${token}`;
}

export default function ShareLinkModal({ onClose }) {
  const { data: link, isLoading } = useShareLink();
  const regenerateMut = useRegenerateShareLink();
  const revokeMut = useRevokeShareLink();

  const url = link?.exists ? buildShareUrl(link.token) : null;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(url);
      toast.ok('Link kopyalandı');
    } catch {
      toast.err('Link kopyalanamadı');
    }
  };

  const handleWhatsApp = () => {
    const message = `Merhaba, dönem faturalarımızı buradan canlı olarak görüntüleyebilirsiniz: ${url}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(message)}`, '_blank');
  };

  const handleRegenerate = () => {
    if (link?.exists && !window.confirm('Yeni link oluşturulunca eski link çalışmayı durdurur. Devam edilsin mi?')) return;
    regenerateMut.mutate();
  };

  const handleRevoke = () => {
    if (!window.confirm('Link iptal edilecek ve müşaviriniz artık faturalara erişemeyecek. Devam edilsin mi?')) return;
    revokeMut.mutate();
  };

  return (
    <div className="drawer-scrim" onClick={onClose}>
      <div className="drawer" style={{ maxWidth: 480, height: 'auto' }} onClick={e => e.stopPropagation()}>
        <div className="drawer-head">
          <h2>Müşavir Paylaşım Linki</h2>
          <button className="tb-icon-btn" onClick={onClose}><Icon name="x" /></button>
        </div>
        <div className="drawer-body col gap-12">
          <p className="muted" style={{ fontSize: 12 }}>
            Bu linki mali müşavirinizle paylaşın. Müşaviriniz kayıt olmadan faturalarınızı
            canlı olarak görüntüleyebilir ve PDF indirebilir. Taslak faturalar görünmez.
          </p>

          {isLoading ? (
            <div className="card" style={{ height: 60 }} />
          ) : link?.exists ? (
            <>
              <div className="field">
                <label>Paylaşım Linki</label>
                <div className="row gap-4">
                  <input className="input mono" readOnly value={url} style={{ fontSize: 11, flex: 1 }} onFocus={e => e.target.select()} />
                  <button className="tb-icon-btn" title="Kopyala" onClick={handleCopy}><Icon name="copy" size={14} /></button>
                </div>
              </div>
              <p className="muted" style={{ fontSize: 11 }}>
                {link.accessCount > 0
                  ? `${link.accessCount} kez görüntülendi${link.lastAccessedAt ? ' · Son erişim: ' + new Date(link.lastAccessedAt).toLocaleString('tr-TR') : ''}`
                  : 'Henüz görüntülenmedi'}
              </p>
              <div className="row gap-8">
                <button className="btn primary" onClick={handleWhatsApp}>
                  <Icon name="message" size={14} /> WhatsApp ile Gönder
                </button>
                <button className="btn ghost" disabled={regenerateMut.isPending} onClick={handleRegenerate}>
                  <Icon name="refresh" size={14} /> Yeniden Oluştur
                </button>
                <button className="btn ghost" disabled={revokeMut.isPending} onClick={handleRevoke}>
                  <Icon name="x" size={14} /> İptal Et
                </button>
              </div>
            </>
          ) : (
            <button className="btn primary" disabled={regenerateMut.isPending} onClick={handleRegenerate}>
              {regenerateMut.isPending ? 'Oluşturuluyor...' : 'Paylaşım Linki Oluştur'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
