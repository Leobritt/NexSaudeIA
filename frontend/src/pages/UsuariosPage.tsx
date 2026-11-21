import { UserPlus } from 'lucide-react';
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { AlertBanner, Button, Card, Modal, Spinner } from '../components/ui';
import { ApiError, usuariosApi } from '../lib/api';
import { PERFIL_META } from '../lib/format';
import type { Perfil, UsuarioDto } from '../types/api';

const PERFIS: Perfil[] = ['ENFERMEIRO', 'MEDICO', 'GESTOR_LEITOS', 'AUDITOR', 'ADMIN'];

export function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<UsuarioDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const [modalNovo, setModalNovo] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [form, setForm] = useState({
    nome: '',
    email: '',
    senha: '',
    perfil: 'ENFERMEIRO' as Perfil,
  });

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro(null);
    try {
      const data = await usuariosApi.listar();
      setUsuarios(data);
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao carregar usuários.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function handleCriar(e: FormEvent) {
    e.preventDefault();
    setSalvando(true);
    setErro(null);
    try {
      await usuariosApi.criar({
        nome: form.nome.trim(),
        email: form.email.trim(),
        senha: form.senha,
        perfil: form.perfil,
      });
      setModalNovo(false);
      setForm({ nome: '', email: '', senha: '', perfil: 'ENFERMEIRO' });
      setSucesso('Usuário criado com sucesso.');
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao criar usuário.');
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Usuários do Sistema</h2>
          <p className="text-sm text-slate-500">Gerenciamento de profissionais e perfis de acesso</p>
        </div>
        <Button onClick={() => setModalNovo(true)}>
          <UserPlus className="w-4 h-4" />
          Novo Usuário
        </Button>
      </div>

      {erro && <AlertBanner type="error" message={erro} onClose={() => setErro(null)} />}
      {sucesso && <AlertBanner type="success" message={sucesso} onClose={() => setSucesso(null)} />}

      {loading ? (
        <Spinner />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-xs text-slate-600">
                  <th className="py-3 px-4 font-semibold">Nome</th>
                  <th className="py-3 px-4 font-semibold">E-mail</th>
                  <th className="py-3 px-4 font-semibold">Perfil</th>
                  <th className="py-3 px-4 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {usuarios.map((u) => {
                  const meta = PERFIL_META[u.perfil];
                  return (
                    <tr key={u.id} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4 font-medium text-slate-900">{u.nome}</td>
                      <td className="py-3 px-4 text-slate-600">{u.email}</td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-medium border ${meta.color}`}
                        >
                          {meta.label}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${
                            u.ativo
                              ? 'bg-emerald-50 text-emerald-700'
                              : 'bg-slate-100 text-slate-500'
                          }`}
                        >
                          {u.ativo ? 'Ativo' : 'Inativo'}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal open={modalNovo} onClose={() => setModalNovo(false)} title="Novo Usuário">
        <form onSubmit={handleCriar} className="space-y-3.5">
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">Nome *</label>
            <input
              required
              maxLength={150}
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">E-mail *</label>
            <input
              type="email"
              required
              maxLength={150}
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">
              Senha * (mínimo 8 caracteres)
            </label>
            <input
              type="password"
              required
              minLength={8}
              maxLength={72}
              value={form.senha}
              onChange={(e) => setForm({ ...form, senha: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">Perfil *</label>
            <select
              value={form.perfil}
              onChange={(e) => setForm({ ...form, perfil: e.target.value as Perfil })}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
            >
              {PERFIS.map((p) => (
                <option key={p} value={p}>
                  {PERFIL_META[p].label}
                </option>
              ))}
            </select>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setModalNovo(false)}>
              Cancelar
            </Button>
            <Button type="submit" loading={salvando}>
              Criar Usuário
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
