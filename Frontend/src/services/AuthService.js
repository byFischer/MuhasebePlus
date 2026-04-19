import api from "./api";

class AuthService {
    async Register(payload) {
        try{
            const response = await api.post('/api/users/register', payload);
            return response.data;
        } catch (error) {
            const message = error?.response?.data?.message || error.message || "Registration failed";
            throw new Error(message);
        }
           
        }

    async Login({email, password}) {
        try{
            const response = await api.post('/api/auth/Login', {email, password});
            const data = response.data;

            if(data?.token){
                LocalStorage.setItem("token", data.token);
            }
            return data;
        }
        catch (error) {
            const message = error?.response?.data?.message || error.message || "Login failed";
            throw new Error(message);
        }
        }
 
    async Logout() {
        localstorage.removeItem("token");
    }

}
export default new AuthService();