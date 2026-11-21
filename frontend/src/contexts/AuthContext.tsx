import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { authApi, getStoredAuth, setStoredAuth } from '../lib/api';
import type { LoginResponse, Perfil } from '../types/api';

interface AuthContextValue {
  user: LoginResponse | null;
  isAuthenticated: boolean;
  login: (email: string, senha: string) => Promise<LoginResponse>;
  logout: () => void;
  hasRole: (...perfis: Perfil[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<LoginResponse | null>(() => getStoredAuth());

  const login = useCallback(async (email: string, senha: string) => {
    const res = await authApi.login({ email, senha });
    setStoredAuth(res);
    setUser(res);
    return res;
  }, []);

  const logout = useCallback(() => {
    setStoredAuth(null);
    setUser(null);
  }, []);

  const hasRole = useCallback(
    (...perfis: Perfil[]) => {
      if (!user) return false;
      return perfis.includes(user.perfil);
    },
    [user],
  );

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider');
  return ctx;
}

export function defaultRouteForPerfil(perfil: Perfil): string {
  switch (perfil) {
    case 'ENFERMEIRO':
      return '/pacientes';
    case 'MEDICO':
      return '/atendimentos';
    case 'GESTOR_LEITOS':
      return '/leitos';
    case 'AUDITOR':
      return '/auditoria';
    case 'ADMIN':
      return '/usuarios';
  }
}
