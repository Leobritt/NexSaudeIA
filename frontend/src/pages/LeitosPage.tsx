import { BedDouble, FileText, RefreshCw, User } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertBanner, Button, Card, Spinner } from '../components/ui';
import { useAuth } from '../contexts/AuthContext';
import { ApiError, leitosApi } from '../lib/api';
import { formatCpf, STATUS_LEITO_META, TRANSICOES_LEITO_MANUAIS } from '../lib/format';
import type { LeitoDto, SetorDto, StatusLeito } from '../types/api';

export function LeitosPage() {
  const { hasRole } = useAuth();
  const ehGestor = hasRole('GESTOR_LEITOS');

  const [setores, setSetores] = useState<SetorDto[]>([]);
  const [leitos, setLeitos] = useState<LeitoDto[]>([]);
  const [setorFiltro, setSetorFiltro] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [atualizandoId, setAtualizandoId] = useState<number | null>(null);

  const carregar = useCallback(async (silencioso = false) => {
    if (!silencioso) setLoading(true);
    try {
      const [s, l] = await Promise.all([leitosApi.setores(), leitosApi.leitos()]);
      setSetores(s);
      setLeitos(l);
      setErro(null);
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao carregar mapa de leitos.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
    const timer = setInterval(() => {
      void carregar(true);
    }, 10000);
    return () => clearInterval(timer);
  }, [carregar]);

  async function alterarStatus(leito: LeitoDto, novoStatus: StatusLeito) {
    setErro(null);
    setAtualizandoId(leito.id);
    try {
      const atualizado = await leitosApi.alterarStatus(leito.id, novoStatus);
      setLeitos((prev) => prev.map((l) => (l.id === leito.id ? atualizado : l)));
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao alterar status do leito.');
    } finally {
      setAtualizandoId(null);
    }
  }

  const leitosVisiveis = setorFiltro ? leitos.filter((l) => l.setorId === setorFiltro) : leitos;

  const contagem = {
    total: leitosVisiveis.length,
    LIVRE: leitosVisiveis.filter((l) => l.status === 'LIVRE').length,
    OCUPADO: leitosVisiveis.filter((l) => l.status === 'OCUPADO').length,
    HIGIENIZACAO: leitosVisiveis.filter((l) => l.status === 'HIGIENIZACAO').length,
    RESERVADO: leitosVisiveis.filter((l) => l.status === 'RESERVADO').length,
    INTERDITADO: leitosVisiveis.filter((l) => l.status === 'INTERDITADO').length,
  };

  const setoresVisiveis = setorFiltro ? setores.filter((s) => s.id === setorFiltro) : setores;

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Mapa de Leitos</h2>
          <p className="text-sm text-slate-500">
            Visão em tempo real por setor (atualização automática a cada 10s)
          </p>
        </div>
        <Button variant="secondary" onClick={() => void carregar()}>
          <RefreshCw className="w-4 h-4" />
          Atualizar
        </Button>
      </div>

      {erro && <AlertBanner type="error" message={erro} onClose={() => setErro(null)} />}

      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <Card className="p-3.5">
          <p className="text-xs text-slate-500">Total</p>
          <p className="text-2xl font-bold text-slate-900">{contagem.total}</p>
        </Card>
        <Card className="p-3.5 border-emerald-200">
          <p className="text-xs text-emerald-700 font-medium">Livres</p>
          <p className="text-2xl font-bold text-emerald-700">{contagem.LIVRE}</p>
        </Card>
        <Card className="p-3.5 border-rose-200">
          <p className="text-xs text-rose-700 font-medium">Ocupados</p>
          <p className="text-2xl font-bold text-rose-700">{contagem.OCUPADO}</p>
        </Card>
        <Card className="p-3.5 border-amber-200">
          <p className="text-xs text-amber-700 font-medium">Higienização</p>
          <p className="text-2xl font-bold text-amber-700">{contagem.HIGIENIZACAO}</p>
        </Card>
        <Card className="p-3.5 border-sky-200">
          <p className="text-xs text-sky-700 font-medium">Reservados</p>
          <p className="text-2xl font-bold text-sky-700">{contagem.RESERVADO}</p>
        </Card>
        <Card className="p-3.5 border-slate-300">
          <p className="text-xs text-slate-600 font-medium">Interditados</p>
          <p className="text-2xl font-bold text-slate-700">{contagem.INTERDITADO}</p>
        </Card>
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => setSetorFiltro(null)}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium border cursor-pointer ${
            setorFiltro === null
              ? 'bg-teal-600 text-white border-teal-600'
              : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'
          }`}
        >
          Todos os Setores
        </button>
        {setores.map((s) => (
          <button
            key={s.id}
            type="button"
            onClick={() => setSetorFiltro(s.id)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium border cursor-pointer ${
              setorFiltro === s.id
                ? 'bg-teal-600 text-white border-teal-600'
                : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'
            }`}
          >
            {s.nome}
          </button>
        ))}
      </div>

      {loading ? (
        <Spinner />
      ) : (
        <div className="space-y-6">
          {setoresVisiveis.map((setor) => {
            const leitosDoSetor = leitosVisiveis.filter((l) => l.setorId === setor.id);
            return (
              <div key={setor.id} className="space-y-3">
                <div className="flex items-center gap-2">
                  <BedDouble className="w-4 h-4 text-teal-600" />
                  <h3 className="font-semibold text-slate-900">{setor.nome}</h3>
                  <span className="text-xs text-slate-500">({setor.tipo})</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                  {leitosDoSetor.map((leito) => {
                    const meta = STATUS_LEITO_META[leito.status];
                    const transicoes = TRANSICOES_LEITO_MANUAIS[leito.status];
                    return (
                      <div
                        key={leito.id}
                        className={`rounded-xl border p-4 flex flex-col justify-between gap-3 transition-colors ${meta.cardBg}`}
                      >
                        <div className="space-y-2">
                          <div className="flex items-center justify-between">
                            <span className="font-bold text-slate-900">{leito.codigo}</span>
                            <span
                              className={`px-2 py-0.5 rounded-full text-[11px] font-semibold ${meta.badge}`}
                            >
                              {meta.label}
                            </span>
                          </div>

                          {leito.pacienteAtual ? (
                            <div className="bg-white/80 rounded-lg p-2.5 border border-rose-200/70 space-y-1">
                              <p className="text-xs font-semibold text-slate-900 flex items-center gap-1.5">
                                <User className="w-3.5 h-3.5 text-rose-600 shrink-0" />
                                <span className="truncate">{leito.pacienteAtual.nome}</span>
                              </p>
                              <p className="text-[11px] text-slate-500">
                                CPF: {formatCpf(leito.pacienteAtual.cpf)}
                              </p>
                              <Link
                                to={`/pacientes/${leito.pacienteAtual.id}/prontuario`}
                                className="inline-flex items-center gap-1 text-[11px] font-medium text-teal-700 hover:underline pt-0.5"
                              >
                                <FileText className="w-3 h-3" />
                                Abrir Prontuário
                              </Link>
                            </div>
                          ) : (
                            <p className="text-xs text-slate-500 italic">Sem paciente alocado</p>
                          )}
                        </div>

                        {ehGestor && transicoes.length > 0 && (
                          <div className="flex flex-wrap gap-1.5 pt-1 border-t border-slate-200/60">
                            {transicoes.map((t) => (
                              <Button
                                key={t.status}
                                size="sm"
                                variant="secondary"
                                loading={atualizandoId === leito.id}
                                onClick={() => alterarStatus(leito, t.status)}
                                className="text-[11px] px-2.5 py-1"
                              >
                                {t.label}
                              </Button>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
