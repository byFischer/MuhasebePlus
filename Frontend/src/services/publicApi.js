import axios from "axios";

// Public paylaşım sayfası için ayrı instance: Bearer header'ı ve 401 -> /login yönlendirmesi yok
const publicApi = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

export default publicApi;
