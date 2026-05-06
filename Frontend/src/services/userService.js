import api from "./api";

class UserService {
  async updateProfile(payload) {
    const response = await api.put("/api/users/me", payload);
    return response.data;
  }

  async changePassword(payload) {
    const response = await api.put("/api/users/me/password", payload);
    return response.data;
  }

  async changeEmail(newEmail) {
    const response = await api.put(`/api/users/me/email?newEmail=${encodeURIComponent(newEmail)}`);
    return response.data;
  }
}

export default new UserService();
