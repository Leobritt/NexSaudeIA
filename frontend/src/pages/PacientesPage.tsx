import { FileText, Plus, Search, ShieldAlert, ShieldCheck, Stethoscope, UserPlus } from 'lucide-react';
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertBanner, Button, Card, EmptyState, Modal, Spinner } from '../components/ui';
import { useAuth } from '../contexts/AuthContext';
import { ApiError, atendimentosApi, pacientesApi } from '../lib/api';
import { calcIdade, formatCpf, formatDate } from '../lib/format';
import type { PacienteDto } from '../types/api';

export function PacientesPage() {
  const { hasRole } = useAuth();
  const navigate = useNavigate();

  const [pacientes, setPacientes] = useState<PacienteDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [busca, setBusca] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const [modalNovo, setModalNovo] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [form, setForm] = useState({
    nome: '',
    cpf: '',
    cns: '',
    dataNascimento: '',
    sexo: 'F' as 'F' | 'M' | 'O',
    medicamentos: '',
    alergias: '',
  });

  const carregar = useCallback(async (termo?: string) => {
    setLoading(true);
    setErro(null);
    try {
      const limpo = termo?.trim() ?? '';
      const ehCpf = limpo.length > 0 && /^[\d.\-]+$/.test(limpo);
      const data = await pacientesApi.pesquisar(
        limpo ? (ehCpf ? { cpf: limpo } : { nome: limpo }) : undefined,
      );
      setPacientes(data);
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao carregar pacientes.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  function handleSearch(e: FormEvent) {
    e.preventDefault();
    void carregar(busca);
  }

  async function handleCriar(e: FormEvent) {
    e.preventDefault();
    setSalvando(true);
    setErro(null);
    try {
      const splitList = (s: string) =>
        s
          .split(',')
          .map((x) => x.trim())
          .filter(Boolean);

      await pacientesApi.criar({
        nome: form.nome.trim(),
        cpf: form.cpf.trim(),
        cns: form.cns.trim() || undefined,
        dataNascimento: form.dataNascimento,
        sexo: form.sexo,
        medicamentosUsoContinuo: splitList(form.medicamentos),
        alergias: splitList(form.alergias),
      });
      setModalNovo(false);
      setForm({
        nome: '',
        cpf: '',
        cns: '',
        dataNascimento: '',
        sexo: 'F',
        medicamentos: '',
        alergias: '',
      });
      setSucesso('Paciente cadastrado com sucesso.');
      await carregar(busca);
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao cadastrar paciente.');
    } finally {
      setSalvando(false);
    }
  }

  async function toggleConsentimento(p: PacienteDto) {
    setErro(null);
    try {
      const atualizado = await pacientesApi.definirConsentimento(p.id, !p.consentimentoIa);
      setPacientes((prev) => prev.map((item) => (item.id === p.id ? atualizado : item)));
      setSucesso(
        `Consentimento de IA ${atualizado.consentimentoIa ? 'concedido' : 'revogado'} para ${p.nome}.`,
      );
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao atualizar consentimento.');
    }
  }

  async function abrirAtendimento(p: PacienteDto) {
    setErro(null);
    try {
      const atendimento = await atendimentosApi.abrir(p.id);
      navigate(`/triagem?atendimentoId=${atendimento.id}&pacienteId=${p.id}`);
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Não foi possível abrir atendimento.');
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Pacientes</h2>
          <p className="text-sm text-slate-500">
            Cadastro, consentimento para IA (LGPD) e acesso ao prontuário eletrônico
          </p>
        </div>
        <Button onClick={() => setModalNovo(true)}>
          <UserPlus className="w-4 h-4" />
          Novo Paciente
        </Button>
      </div>

      {erro && <AlertBanner type="error" message={erro} onClose={() => setErro(null)} />}
      {sucesso && <AlertBanner type="success" message={sucesso} onClose={() => setSucesso(null)} />}

      <Card className="p-4">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-2">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={busca}
              onChange={(e) => setBusca(e.target.value)}
              placeholder="Buscar por nome ou CPF (com ou sem pontuação)..."
              className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
            />
          </div>
          <Button type="submit" variant="secondary">
            Buscar
          </Button>
          {busca && (
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setBusca('');
                void carregar('');
              }}
            >
              Limpar
            </Button>
          )}
        </form>
      </Card>

      {loading ? (
        <Spinner />
      ) : pacientes.length === 0 ? (
        <EmptyState
          title="Nenhum paciente encontrado"
          description="Tente ajustar os termos da busca ou cadastre um novo paciente."
        />
      ) : (
        <div className="grid grid-cols-1 gap-3">
          {pacientes.map((p) => (
            <Card key={p.id} className="p-4 flex flex-col lg:flex-row lg:items-center justify-between gap-4">
              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="font-semibold text-slate-900">{p.nome}</h3>
                  <span className="text-xs text-slate-500">
                    {calcIdade(p.dataNascimento)} anos • Sexo {p.sexo}
                  </span>
                  <button
                    type="button"
                    onClick={() => toggleConsentimento(p)}
                    title="Clique para alterar o consentimento de uso de IA (RN-002)"
                    className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium border cursor-pointer transition-colors ${
                      p.consentimentoIa
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'
                        : 'bg-amber-50 text-amber-800 border-amber-200 hover:bg-amber-100'
                    }`}
                  >
                    {p.consentimentoIa ? (
                      <>
                        <ShieldCheck className="w-3.5 h-3.5" /> IA Autorizada
                      </>
                    ) : (
                      <>
                        <ShieldAlert className="w-3.5 h-3.5" /> Sem Consentimento IA
                      </>
                    )}
                  </button>
                </div>

                <div className="text-xs text-slate-600 flex flex-wrap gap-x-4 gap-y-1">
                  <span>CPF: {formatCpf(p.cpf)}</span>
                  <span>Nasc.: {formatDate(p.dataNascimento)}</span>
                  {p.cns && <span>CNS: {p.cns}</span>}
                </div>

                <div className="flex flex-wrap gap-2 pt-1">
                  {p.alergias.length > 0 && (
                    <span className="text-xs bg-red-50 text-red-700 border border-red-200 px-2 py-0.5 rounded-md">
                      Alergias: {p.alergias.join(', ')}
                    </span>
                  )}
                  {p.medicamentosUsoContinuo.length > 0 && (
                    <span className="text-xs bg-slate-100 text-slate-700 border border-slate-200 px-2 py-0.5 rounded-md">
                      Uso contínuo: {p.medicamentosUsoContinuo.join(', ')}
                    </span>
                  )}
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-2 shrink-0">
                <Link
                  to={`/pacientes/${p.id}/prontuario`}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-slate-300 text-slate-700 bg-white hover:bg-slate-50"
                >
                  <FileText className="w-3.5 h-3.5" />
                  Prontuário
                </Link>
                {hasRole('ENFERMEIRO') && (
                  <Button size="sm" onClick={() => abrirAtendimento(p)}>
                    <Stethoscope className="w-3.5 h-3.5" />
                    Abrir Atendimento
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={modalNovo} onClose={() => setModalNovo(false)} title="Cadastrar Paciente">
        <form onSubmit={handleCriar} className="space-y-3.5">
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">Nome completo *</label>
            <input
              required
              maxLength={150}
              value={form.nome}
              onChange={(e) => setForm({ ...form, nome: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">CPF *</label>
              <input
                required
                maxLength={20}
                placeholder="000.000.000-00"
                value={form.cpf}
                onChange={(e) => setForm({ ...form, cpf: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">CNS (opcional)</label>
              <input
                maxLength={15}
                value={form.cns}
                onChange={(e) => setForm({ ...form, cns: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
              />
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Data de nascimento *
              </label>
              <input
                type="date"
                required
                value={form.dataNascimento}
                onChange={(e) => setForm({ ...form, dataNascimento: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Sexo *</label>
              <select
                value={form.sexo}
                onChange={(e) => setForm({ ...form, sexo: e.target.value as 'F' | 'M' | 'O' })}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
              >
                <option value="F">Feminino (F)</option>
                <option value="M">Masculino (M)</option>
                <option value="O">Outro (O)</option>
              </select>
            </div>
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">
              Medicamentos de uso contínuo (separados por vírgula)
            </label>
            <input
              placeholder="ex.: varfarina, losartana"
              value={form.medicamentos}
              onChange={(e) => setForm({ ...form, medicamentos: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">
              Alergias (separadas por vírgula)
            </label>
            <input
              placeholder="ex.: dipirona, penicilina"
              value={form.alergias}
              onChange={(e) => setForm({ ...form, alergias: e.target.value })}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
            />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={() => setModalNovo(false)}>
              Cancelar
            </Button>
            <Button type="submit" loading={salvando}>
              <Plus className="w-4 h-4" />
              Cadastrar
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
