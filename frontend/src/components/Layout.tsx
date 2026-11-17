import {
  Activity,
  BedDouble,
  ClipboardList,
  Clock,
  HeartPulse,
  LogOut,
  ShieldCheck,
  Stethoscope,
  UserCog,
  Users,
} from 'lucide-react';
import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { PERFIL_META } from '../lib/format';
import type { Perfil } from '../types/api';

interface NavItem {
  to: string;
  label: string;
  icon: ReactNode;
  perfis: Perfil[];
}

const NAV_ITEMS: NavItem[] = [
  {
    to: '/pacientes',
    label: 'Pacientes',
    icon: <Users className="w-4 h-4" />,
    perfis: ['ENFERMEIRO', 'MEDICO'],
  },
  {
    to: '/triagem',
    label: 'Triagem & Fila',
    icon: <HeartPulse className="w-4 h-4" />,
    perfis: ['ENFERMEIRO'],
  },
  {
    to: '/atendimentos',
    label: 'Fila Médica',
    icon: <Stethoscope className="w-4 h-4" />,
    perfis: ['MEDICO'],
  },
  {
    to: '/leitos',
    label: 'Mapa de Leitos',
    icon: <BedDouble className="w-4 h-4" />,
    perfis: ['GESTOR_LEITOS', 'MEDICO'],
  },
  {
    to: '/aguardando-leito',
    label: 'Aguardando Leito',
    icon: <Clock className="w-4 h-4" />,
    perfis: ['GESTOR_LEITOS'],
  },
  {
    to: '/auditoria',
    label: 'Auditoria',
    icon: <ShieldCheck className="w-4 h-4" />,
    perfis: ['AUDITOR'],
  },
  {
    to: '/usuarios',
    label: 'Usuários',
    icon: <UserCog className="w-4 h-4" />,
    perfis: ['ADMIN'],
  },
];

export function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;
  const perfilMeta = PERFIL_META[user.perfil];
  const visibleNav = NAV_ITEMS.filter((item) => item.perfis.includes(user.perfil));

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="min-h-screen flex flex-col md:flex-row bg-slate-50">
      <aside className="w-full md:w-64 bg-white border-b md:border-b-0 md:border-r border-slate-200 flex flex-col shrink-0">
        <div className="px-5 py-4 border-b border-slate-200 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-teal-600 text-white flex items-center justify-center shrink-0">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-slate-900 leading-tight">NexSaúde IA</h1>
            <p className="text-[11px] text-slate-500">Gestão Hospitalar</p>
          </div>
        </div>

        <nav className="flex-1 p-3 flex md:flex-col gap-1 overflow-x-auto">
          {visibleNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-colors ${
                  isActive
                    ? 'bg-teal-50 text-teal-700'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                }`
              }
            >
              {item.icon}
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-slate-200 hidden md:block">
          <div className="mb-3">
            <p className="text-sm font-medium text-slate-900 truncate">{user.nome}</p>
            <span
              className={`inline-block mt-1 px-2 py-0.5 rounded-full text-[11px] font-medium border ${perfilMeta.color}`}
            >
              {perfilMeta.label}
            </span>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900 border border-slate-200 cursor-pointer"
          >
            <LogOut className="w-3.5 h-3.5" />
            Sair
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <header className="md:hidden bg-white border-b border-slate-200 px-4 py-2.5 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ClipboardList className="w-4 h-4 text-teal-600" />
            <span className="text-sm font-medium text-slate-900">{user.nome}</span>
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-medium border ${perfilMeta.color}`}>
              {perfilMeta.label}
            </span>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="text-xs text-slate-600 hover:text-slate-900 flex items-center gap-1 cursor-pointer"
          >
            <LogOut className="w-3.5 h-3.5" />
            Sair
          </button>
        </header>

        <main className="flex-1 p-4 md:p-6 max-w-7xl w-full mx-auto">{children}</main>
      </div>
    </div>
  );
}
