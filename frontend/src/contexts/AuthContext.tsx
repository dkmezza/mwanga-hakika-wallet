'use client';

import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import type { AuthResponse, UserResponse } from '@/lib/types';

interface AuthState {
  user: UserResponse | null;
  loading: boolean;
  login: (auth: AuthResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState>({
  user: null,
  loading: true,
  login: () => {},
  logout: () => {},
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    try {
      const stored = localStorage.getItem('user');
      const token = localStorage.getItem('accessToken');
      if (stored && token) {
        setUser(JSON.parse(stored) as UserResponse);
      }
    } catch {
      // corrupted localStorage — wipe and force re-auth
    } finally {
      setLoading(false);
    }
  }, []);

  function login(auth: AuthResponse) {
    const userInfo: UserResponse = {
      id: auth.userId,
      email: auth.email,
      fullName: auth.fullName,
      role: auth.role,
      active: true,
      createdAt: new Date().toISOString(),
    };
    localStorage.setItem('accessToken', auth.accessToken);
    localStorage.setItem('refreshToken', auth.refreshToken);
    localStorage.setItem('user', JSON.stringify(userInfo));
    setUser(userInfo);
  }

  function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
