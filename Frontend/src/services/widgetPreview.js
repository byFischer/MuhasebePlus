import dashboardService from './dashboardService';

// Widget önizleme sorgularını tek bir yerden yönetir. Dashboard'daki widget
// picker'ı her custom widget kartı için ayrı bir preview isteği atar. Bunları
// sınırlamazsak çok sayıda widget'ı olan bir kullanıcıda onlarca istek aynı
// anda gidip rate limit'i (60 istek/dk) doldurur ve ardından gelen istekler
// 429 alır.
//
// İki koruma uygular:
//  1) Tanım bazında önbellek — aynı widget (definitionId + updatedAt) için
//     sorgu yalnızca bir kez çalışır; React StrictMode'un çift mount'u veya
//     sayfaya tekrar girişler aynı sonucu paylaşır.
//  2) Eşzamanlılık sınırı — aynı anda en fazla MAX_CONCURRENT istek gider,
//     gerisi kuyrukta bekler. Böylece istekler ani bir patlama yerine
//     zamana yayılır.

const MAX_CONCURRENT = 4;

const previewCache = new Map();
const queue = [];
let active = 0;

function pump() {
  while (active < MAX_CONCURRENT && queue.length > 0) {
    const { queryConfig, resolve, reject } = queue.shift();
    active++;
    dashboardService.previewQuery(queryConfig)
      .then(resolve, reject)
      .finally(() => { active--; pump(); });
  }
}

function enqueue(queryConfig) {
  return new Promise((resolve, reject) => {
    queue.push({ queryConfig, resolve, reject });
    pump();
  });
}

export function fetchWidgetPreview(def) {
  const key = `${def.definitionId}:${def.updatedAt || ''}`;
  if (!previewCache.has(key)) {
    const promise = enqueue(def.queryConfig)
      .catch(err => { previewCache.delete(key); throw err; });
    previewCache.set(key, promise);
  }
  return previewCache.get(key);
}

export default fetchWidgetPreview;
