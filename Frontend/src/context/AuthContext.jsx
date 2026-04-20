// Frontend/src/context/AuthContext.jsx
import { createContext, useContext, useMemo, useState } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('token'));

  const loginUser = async (credentials) => {
    const data = await authService.login(credentials);

    if (data?.token) {
      setToken(data.token);
      localStorage.setItem('token', data.token);
    }

    return data;
  };

  const registerUser = async (formData) => {
    await authService.register(formData);

    const loginData = await authService.login({
      email: formData.email,
      password: formData.password,
    });

    if (loginData?.token) {
      setToken(loginData.token);
      localStorage.setItem('token', loginData.token);
    }

    return loginData;
  };

  const logoutUser = () => {
    setToken(null);
    localStorage.removeItem('token');
    authService.logout();
  };

  const isAuthenticated = Boolean(token);

  const value = useMemo(
    () => ({
      token,
      isAuthenticated,
      loginUser,
      registerUser,
      logoutUser,
    }),
    [token, isAuthenticated]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  return context;
};