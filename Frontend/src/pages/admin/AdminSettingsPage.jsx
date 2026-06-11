import Icon from '@/components/mp/Icon';
import AiTab from '@/pages/settings/AiTab';

export default function AdminSettingsPage() {
  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Sistem Ayarları</h1>
          <p className="page-sub">Platform geneli yapılandırma</p>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-h"><h3>Yapay Zeka</h3></div>
          <div className="card-b">
            <AiTab />
          </div>
        </div>

        <div className="card">
          <div className="card-h"><h3>Bilgi</h3></div>
          <div className="card-b col gap-12" style={{ fontSize: 13 }}>
            <p style={{ margin: 0 }}>
              <Icon name="info" size={13} /> AI API anahtarı tüm şirketler için ortak kullanılır;
              her şirketin aylık token kotası ayrı takip edilir.
            </p>
            <p style={{ margin: 0 }}>
              <Icon name="shield" size={13} /> İlk yönetici hesabı <code className="mono">ADMIN_EMAIL</code> ve{' '}
              <code className="mono">ADMIN_PASSWORD</code> ortam değişkenleriyle, sistemde hiç yönetici
              yokken açılışta bir kez oluşturulur.
            </p>
            <p style={{ margin: 0 }}>
              <Icon name="log" size={13} /> Yönetici işlemleri (rol değiştirme, kilitleme, silme vb.)
              denetim kaydına yazılır — <b>Loglar &amp; Denetim → Denetim Kaydı</b> sekmesinden izlenebilir.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
