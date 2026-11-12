import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('ledgerline_user');
    return savedUser ? JSON.parse(savedUser) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('ledgerline_token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      refreshProfile();
    } else {
      setLoading(false);
    }
  }, [token]);

  const refreshProfile = async () => {
    try {
      const res = await api.get('/auth/me');
      if (res.data.success && res.data.data) {
        const profile = res.data.data;
        const primaryAccount = profile.accounts && profile.accounts.length > 0 ? profile.accounts[0] : null;

        const updatedUser = {
          userId: profile.id,
          username: profile.username,
          email: profile.email,
          fullName: profile.fullName,
          role: profile.role,
          accountNumber: primaryAccount ? primaryAccount.accountNumber : null,
          accountBalance: primaryAccount ? primaryAccount.balance : 0,
          accountStatus: primaryAccount ? primaryAccount.status : 'ACTIVE',
          accounts: profile.accounts || [],
        };

        setUser(updatedUser);
        localStorage.setItem('ledgerline_user', JSON.stringify(updatedUser));
      }
    } catch (err) {
      console.error('Failed to refresh user profile:', err);
    } finally {
      setLoading(false);
    }
  };

  const login = async (usernameOrEmail, password) => {
    const res = await api.post('/auth/login', { usernameOrEmail, password });
    if (res.data.success) {
      const data = res.data.data;
      setToken(data.token);
      localStorage.setItem('ledgerline_token', data.token);

      const authUser = {
        userId: data.userId,
        username: data.username,
        email: data.email,
        fullName: data.fullName,
        role: data.role,
        accountNumber: data.accountNumber,
        accountBalance: data.accountBalance,
      };

      setUser(authUser);
      localStorage.setItem('ledgerline_user', JSON.stringify(authUser));
      return authUser;
    }
    throw new Error(res.data.message || 'Login failed');
  };

  const register = async (formData) => {
    const res = await api.post('/auth/register', formData);
    if (res.data.success) {
      const data = res.data.data;
      setToken(data.token);
      localStorage.setItem('ledgerline_token', data.token);

      const authUser = {
        userId: data.userId,
        username: data.username,
        email: data.email,
        fullName: data.fullName,
        role: data.role,
        accountNumber: data.accountNumber,
        accountBalance: data.accountBalance,
      };

      setUser(authUser);
      localStorage.setItem('ledgerline_user', JSON.stringify(authUser));
      return authUser;
    }
    throw new Error(res.data.message || 'Registration failed');
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('ledgerline_token');
    localStorage.removeItem('ledgerline_user');
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, logout, refreshProfile }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
