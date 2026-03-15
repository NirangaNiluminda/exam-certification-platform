import React, { createContext, useContext, useState, useCallback } from 'react';
import type { AuthResponse } from '../types';

interface AuthContextValue {
  token: string | null;
  user: Omit<AuthResponse, 'accessToken'> | null;
  login: (response: AuthResponse) => void;
  logout: () => void;
  isAuthenticated: boolean;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'));
  const [user, setUser] = useState<Omit<AuthResponse, 'accessToken'> | null>(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = useCallback((response: AuthResponse) => {
    const { accessToken, ...userInfo } = response;
    localStorage.setItem('token', accessToken);
    localStorage.setItem('user', JSON.stringify(userInfo));
    setToken(accessToken);
    setUser(userInfo);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        login,
        logout,
        isAuthenticated: !!token,
        isAdmin: user?.role === 'ADMIN' || user?.role === 'EXAMINER',
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
