import { FileText, Play, RefreshCw } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AlertBanner,
  Button,
  Card,
  EmptyState,
  ManchesterBadge,
  Spinner,
} from '../components/ui';
import { ApiError, atendimentosApi } from '../lib/api';
import { calcIdade, formatCpf, formatDateTime } from '../lib/format';
import type { AtendimentoDto } from '../types/api';

export function AtendimentosPage() {
  const navigate = useNavigate();
  const [fila, setFila] = useState<AtendimentoDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [iniciandoId, setIniciandoId] = useState<number | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro(null);
    try {
      const data = await atendimentosApi.fila();
      setFila(data);
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao carregar fila médica.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function iniciarAtendimento(a: AtendimentoDto) {
    setErro(null);
    setIniciandoId(a.id);
    try {
      await atendimentosApi.iniciar(a.id);
      navigate(`/pacientes/${a.paciente.id}/prontuario`);
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Não foi possível iniciar atendimento.');
      setIniciandoId(null);
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Fila de Atendimento Médico</h2>
          <p className="text-sm text-slate-500">
            Pacientes triados ordenados por gravidade (Protocolo de Manchester) e ordem de chegada
          </p>
        </div>
        <Button variant="secondary" onClick={() => void carregar()}>
          <RefreshCw className="w-4 h-4" />
          Atualizar Fila
        </Button>
      </div>

      {erro && <AlertBanner type="error" message={erro} onClose={() => setErro(null)} />}

      {loading ? (
        <Spinner />
      ) : fila.length === 0 ? (
        <EmptyState
          title="Nenhum paciente aguardando atendimento"
          description="Quando a enfermagem finalizar a triagem, os pacientes aparecerão aqui."
        />
      ) : (
        <div className="space-y-3">
          {fila.map((a) => (
            <Card
              key={a.id}
              className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2.5">
                  <ManchesterBadge cor={a.corAtual} showTempo />
                  <h3 className="font-semibold text-slate-900">{a.paciente.nome}</h3>
                </div>
                <div className="text-xs text-slate-500 flex flex-wrap gap-x-4 gap-y-1">
                  <span>Atendimento #{a.id}</span>
                  <span>CPF: {formatCpf(a.paciente.cpf)}</span>
                  <span>{calcIdade(a.paciente.dataNascimento)} anos</span>
                  <span>Chegada: {formatDateTime(a.chegada)}</span>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <Link
                  to={`/pacientes/${a.paciente.id}/prontuario`}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-slate-300 text-slate-700 bg-white hover:bg-slate-50"
                >
                  <FileText className="w-3.5 h-3.5" />
                  Prontuário
                </Link>
                <Button
                  size="sm"
                  loading={iniciandoId === a.id}
                  onClick={() => iniciarAtendimento(a)}
                >
                  <Play className="w-3.5 h-3.5" />
                  Iniciar Atendimento
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
