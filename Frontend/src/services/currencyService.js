import axios from 'axios';

const GENELPARA = 'https://api.genelpara.com/json/';

const fetchList = async (list, sembol) => {
  const { data } = await axios.get(GENELPARA, {
    params: { list, sembol },
    timeout: 8000,
  });
  if (!data?.success) throw new Error(data?.error || 'Kur verisi alınamadı');
  return data.data;
};

const currencyService = {
  getRates: async () => {
    try {
      const [doviz, altin] = await Promise.all([
        fetchList('doviz', 'USD,EUR'),
        fetchList('altin', 'GA'),
      ]);
      return {
        USD: doviz.USD,
        EUR: doviz.EUR,
        GA: altin.GA,
      };
    } catch {
      return null;
    }
  },
};

export default currencyService;
