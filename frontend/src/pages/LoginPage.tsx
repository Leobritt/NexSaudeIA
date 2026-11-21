import { Activity, KeyRound, Mail } from 'lucide-react';
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertBanner, Button, Card } from '../components/ui';
import { defaultRouteForPerfil, useAuth } from '../contexts/AuthContext';
import { ApiError } from '../lib/api';

const DEMO_USERS = [
  { label: 'Enfermeiro', email: 'enfermeiro@nexsaude.local', cor: 'bg-teal-50 text-teal-800 border-teal-200' },
  { label: 'Médico', email: 'medico@nexsaude.local', cor: 'bg-blue-50 text-blue-800 border-blue-200' },
  { label: 'Gestor de Leitos', email: 'gestor@nexsaude.local', cor: 'bg-amber-50 text-amber-800 border-amber-200' },
  { label: 'Auditor', email: 'auditor@nexsaude.local', cor: 'bg-slate-100 text-slate-800 border-slate-300' },
  { label: 'Admin', email: 'admin@nexsaude.local', cor: 'bg-purple-50 text-purple-800 border-purple-200' },
];

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setLoading(true);
    try {
      const res = await login(email.trim(), senha);
      navigate(defaultRouteForPerfil(res.perfil), { replace: true });
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Falha ao conectar com a API.');
    } finally {
      setLoading(false);
    }
  }

  function preencherDemo(demoEmail: string) {
    setEmail(demoEmail);
    setSenha('nexsaude123');
    setErro(null);
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-100 via-teal-50/40 to-slate-100 p-4">
      <div className="w-full max-w-md space-y-5">
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-teal-600 text-white shadow-sm mb-3">
            <Activity className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">NexSaúde IA</h1>
          <p className="text-sm text-slate-500 mt-1">
            Triagem, Prontuário Eletrônico e Gestão de Leitos
          </p>
        </div>

        <Card className="p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            {erro && <AlertBanner type="error" message={erro} onClose={() => setErro(null)} />}

            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">E-mail</label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="usuario@nexsaude.local"
                  className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Senha</label>
              <div className="relative">
                <KeyRound className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
                />
              </div>
            </div>

            <Button type="submit" loading={loading} className="w-full">
              Entrar
            </Button>
          </form>

          <div className="mt-6 pt-5 border-t border-slate-200">
            <p className="text-xs font-medium text-slate-500 mb-2.5">
              Acesso rápido (ambiente dev — senha <code className="bg-slate-100 px-1 rounded">nexsaude123</code>):
            </p>
            <div className="flex flex-wrap gap-1.5">
              {DEMO_USERS.map((u) => (
                <button
                  key={u.email}
                  type="button"
                  onClick={() => preencherDemo(u.email)}
                  className={`px-2.5 py-1 rounded-md text-xs font-medium border cursor-pointer hover:opacity-85 transition-opacity ${u.cor}`}
                >
                  {u.label}
                </button>
              ))}
            </div>
          </div>
        </Card>

        <p className="text-center text-xs text-slate-400">
          A IA sugere, o profissional decide — UCSAL
        </p>
      </div>
    </div>
  );
}
