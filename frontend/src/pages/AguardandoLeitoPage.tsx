import { BedDouble, CheckCircle2, RefreshCw } from 'lucide-react';
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertBanner,
  Button,
  Card,
  EmptyState,
  ManchesterBadge,
  Modal,
  Spinner,
} from '../components/ui';
import { ApiError, atendimentosApi, internacoesApi, leitosApi } from '../lib/api';
import { formatCpf, formatDateTime } from '../lib/format';
import type { AtendimentoDto, LeitoDto, SetorDto } from '../types/api';

export function AguardandoLeitoPage() {
  const [lista, setLista] = useState<AtendimentoDto[]>([]);
  const [setores, setSetores] = useState<SetorDto[]>([]);
  const [leitosLivres, setLeitosLivres] = useState<LeitoDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const [selecionado, setSelecionado] = useState<AtendimentoDto | null>(null);
  const [leitoId, setLeitoId] = useState<string>('');
  const [internando, setInternando] = useState(false);

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro(null);
    try {
      const [a, s, l] = await Promise.all([
        atendimentosApi.aguardandoLeito(),
        leitosApi.setores(),
        leitosApi.leitos(),
      ]);
      setLista(a);
      setSetores(s);
      setLeitosLivres(l.filter((item) => item.status === 'LIVRE'));
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao carregar fila de leitos.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  function nomeSetor(id: number | null): string {
    if (!id) return 'Não informado';
    return setores.find((s) => s.id === id)?.nome ?? `Setor #${id}`;
  }

  function abrirModalInternacao(a: AtendimentoDto) {
    setSelecionado(a);
    const preferido = leitosLivres.find((l) => l.setorId === a.setorIndicadoId) ?? leitosLivres[0];
    setLeitoId(preferido ? String(preferido.id) : '');
  }

  async function handleInternar(e: FormEvent) {
    e.preventDefault();
    if (!selecionado || !leitoId) return;
    setInternando(true);
    setErro(null);
    try {
      const res = await internacoesApi.internar(selecionado.id, Number(leitoId));
      setSucesso(
        `Paciente ${res.paciente.nome} internado no leito ${res.leitoCodigo}.`,
      );
      setSelecionado(null);
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao efetivar internação.');
    } finally {
      setInternando(false);
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Pacientes Aguardando Leito</h2>
          <p className="text-sm text-slate-500">
            Solicitações de internação indicadas pela equipe médica
          </p>
        </div>
        <Button variant="secondary" onClick={() => void carregar()}>
          <RefreshCw className="w-4 h-4" />
          Atualizar
        </Button>
      </div>

      {erro && <AlertBanner type="error" message={erro} onClose={() => setErro(null)} />}
      {sucesso && <AlertBanner type="success" message={sucesso} onClose={() => setSucesso(null)} />}

      {loading ? (
        <Spinner />
      ) : lista.length === 0 ? (
        <EmptyState
          title="Nenhum paciente aguardando leito"
          description="Todas as solicitações de internação foram alocadas."
        />
      ) : (
        <div className="space-y-3">
          {lista.map((a) => (
            <Card
              key={a.id}
              className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1.5">
                <div className="flex flex-wrap items-center gap-2">
                  <ManchesterBadge cor={a.corAtual} />
                  <h3 className="font-semibold text-slate-900">{a.paciente.nome}</h3>
                </div>
                <div className="text-xs text-slate-600 flex flex-wrap gap-x-4 gap-y-1">
                  <span>CPF: {formatCpf(a.paciente.cpf)}</span>
                  <span>Setor indicado: <strong>{nomeSetor(a.setorIndicadoId)}</strong></span>
                  {a.medicoResponsavel && <span>Médico: {a.medicoResponsavel}</span>}
                  <span>Chegada: {formatDateTime(a.chegada)}</span>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <Link
                  to={`/pacientes/${a.paciente.id}/prontuario`}
                  className="px-3 py-1.5 rounded-lg text-xs font-medium border border-slate-300 text-slate-700 bg-white hover:bg-slate-50"
                >
                  Prontuário
                </Link>
                <Button size="sm" onClick={() => abrirModalInternacao(a)}>
                  <BedDouble className="w-3.5 h-3.5" />
                  Alocar Leito
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={!!selecionado}
        onClose={() => setSelecionado(null)}
        title="Alocar Paciente em Leito"
      >
        {selecionado && (
          <form onSubmit={handleInternar} className="space-y-4">
            <div className="text-sm text-slate-700 bg-slate-50 p-3 rounded-lg border border-slate-200">
              <p className="font-semibold text-slate-900">{selecionado.paciente.nome}</p>
              <p className="text-xs text-slate-500 mt-0.5">
                Setor indicado: {nomeSetor(selecionado.setorIndicadoId)}
              </p>
            </div>

            {leitosLivres.length === 0 ? (
              <AlertBanner
                type="warning"
                message="Não há leitos LIVRES disponíveis no momento."
              />
            ) : (
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Selecionar Leito Livre *
                </label>
                <select
                  required
                  value={leitoId}
                  onChange={(e) => setLeitoId(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
                >
                  {leitosLivres.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.codigo} — {nomeSetor(l.setorId)}
                      {l.setorId === selecionado.setorIndicadoId ? ' (Setor Indicado)' : ''}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="secondary" onClick={() => setSelecionado(null)}>
                Cancelar
              </Button>
              <Button type="submit" loading={internando} disabled={leitosLivres.length === 0}>
                <CheckCircle2 className="w-4 h-4" />
                Confirmar Internação
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
