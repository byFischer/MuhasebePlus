import api from "./api";

class AuthService {
  async register(payload) {
    try {
      const response = await api.post("/api/users/register", payload);
      return response.data;
    } catch (error) {
      const message =
        error?.response?.data?.message || error.message || "Registration failed";
      throw new Error(message);
    }
  }

  async login({ email, password }) {
    try {
      const response = await api.post("/api/auth/login", { email, password });
      const data = response.data;

      if (data?.token) {
        sessionStorage.setItem("token", data.token);
      }
      return data;
    } catch (error) {
      const message =
        error?.response?.data?.message || error.message || "Login failed";
      throw new Error(message);
    }
  }

  async getMe() {
    const response = await api.get("/api/users/me");
    return response.data;
  }

  async logout() {
    sessionStorage.removeItem("token");
  }
}

export default new AuthService();
