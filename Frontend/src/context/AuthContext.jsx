// Frontend/src/context/AuthContext.jsx
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(sessionStorage.getItem('token'));
  const [user, setUser] = useState(null);

  const loginUser = async (credentials) => {
    const data = await authService.login(credentials);

    if (data?.token) {
      setToken(data.token);
      sessionStorage.setItem('token', data.token);
      const profile = await authService.getMe();
      setUser(profile);
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
      sessionStorage.setItem('token', loginData.token);
      const profile = await authService.getMe();
      setUser(profile);
    }

    return loginData;
  };

  const logoutUser = () => {
    setToken(null);
    setUser(null);
    sessionStorage.removeItem('token');
    authService.logout();
  };

  useEffect(() => {
    if (token && !user) {
      authService.getMe()
        .then(profile => setUser(profile))
        .catch(() => {
          setToken(null);
          sessionStorage.removeItem('token');
        });
    }
  }, [token, user]);

  const isAuthenticated = Boolean(token);

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated,
      loginUser,
      registerUser,
      logoutUser,
    }),
    [token, user, isAuthenticated]
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