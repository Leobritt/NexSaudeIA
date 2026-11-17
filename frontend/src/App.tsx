import type { ReactNode } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { AuthProvider, defaultRouteForPerfil, useAuth } from './contexts/AuthContext';
import { AguardandoLeitoPage } from './pages/AguardandoLeitoPage';
import { AtendimentosPage } from './pages/AtendimentosPage';
import { AuditoriaPage } from './pages/AuditoriaPage';
import { LeitosPage } from './pages/LeitosPage';
import { LoginPage } from './pages/LoginPage';
import { PacientesPage } from './pages/PacientesPage';
import { ProntuarioPage } from './pages/ProntuarioPage';
import { TriagemPage } from './pages/TriagemPage';
import { UsuariosPage } from './pages/UsuariosPage';
import type { Perfil } from './types/api';

function ProtectedRoute({
  perfis,
  children,
}: {
  perfis?: Perfil[];
  children: ReactNode;
}) {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }
  if (perfis && !perfis.includes(user.perfil)) {
    return <Navigate to={defaultRouteForPerfil(user.perfil)} replace />;
  }
  return <Layout>{children}</Layout>;
}

function HomeRedirect() {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated || !user) return <Navigate to="/login" replace />;
  return <Navigate to={defaultRouteForPerfil(user.perfil)} replace />;
}

export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<HomeRedirect />} />

          <Route
            path="/pacientes"
            element={
              <ProtectedRoute perfis={['ENFERMEIRO', 'MEDICO']}>
                <PacientesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/pacientes/:id/prontuario"
            element={
              <ProtectedRoute perfis={['MEDICO', 'ENFERMEIRO', 'GESTOR_LEITOS', 'AUDITOR']}>
                <ProntuarioPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/triagem"
            element={
              <ProtectedRoute perfis={['ENFERMEIRO']}>
                <TriagemPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/atendimentos"
            element={
              <ProtectedRoute perfis={['MEDICO']}>
                <AtendimentosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/leitos"
            element={
              <ProtectedRoute perfis={['GESTOR_LEITOS', 'MEDICO']}>
                <LeitosPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/aguardando-leito"
            element={
              <ProtectedRoute perfis={['GESTOR_LEITOS']}>
                <AguardandoLeitoPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/auditoria"
            element={
              <ProtectedRoute perfis={['AUDITOR']}>
                <AuditoriaPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/usuarios"
            element={
              <ProtectedRoute perfis={['ADMIN']}>
                <UsuariosPage />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<HomeRedirect />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
