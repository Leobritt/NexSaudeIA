import { AlertTriangle, CheckCircle2, HeartPulse, RefreshCw, Sparkles } from 'lucide-react';
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  AlertBanner,
  Button,
  Card,
  EmptyState,
  ManchesterBadge,
  Spinner,
} from '../components/ui';
import { ApiError, atendimentosApi, pacientesApi, triagensApi } from '../lib/api';
import { COR_RISCO_META, formatDateTime } from '../lib/format';
import type {
  AtendimentoDto,
  CorRisco,
  PacienteDto,
  SugestaoTriagemDto,
} from '../types/api';

const CORES: CorRisco[] = ['VERMELHO', 'LARANJA', 'AMARELO', 'VERDE', 'AZUL'];

export function TriagemPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialAtendimentoId = searchParams.get('atendimentoId') ?? '';
  const initialPacienteId = searchParams.get('pacienteId');

  const [fila, setFila] = useState<AtendimentoDto[]>([]);
  const [loadingFila, setLoadingFila] = useState(true);
  const [paciente, setPaciente] = useState<PacienteDto | null>(null);

  const [atendimentoId, setAtendimentoId] = useState(initialAtendimentoId);
  const [sintomas, setSintomas] = useState('');
  const [sinais, setSinais] = useState({
    paSistolica: '120',
    paDiastolica: '80',
    temperatura: '36.8',
    spo2: '97',
    fc: '84',
    fr: '16',
  });
  const [riscoIminente, setRiscoIminente] = useState(false);

  const [sugestao, setSugestao] = useState<SugestaoTriagemDto | null>(null);
  const [consultandoIa, setConsultandoIa] = useState(false);
  const [corSelecionada, setCorSelecionada] = useState<CorRisco | ''>('');
  const [gravando, setGravando] = useState(false);

  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const carregarFila = useCallback(async () => {
    setLoadingFila(true);
    try {
      const data = await atendimentosApi.fila();
      setFila(data);
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao carregar fila.');
    } finally {
      setLoadingFila(false);
    }
  }, []);

  useEffect(() => {
    void carregarFila();
  }, [carregarFila]);

  useEffect(() => {
    if (initialPacienteId) {
      pacientesApi
        .detalhar(Number(initialPacienteId))
        .then(setPaciente)
        .catch(() => setPaciente(null));
    } else {
      setPaciente(null);
    }
  }, [initialPacienteId]);

  function buildSinais() {
    const num = (v: string) => (v.trim() === '' ? null : Number(v));
    return {
      paSistolica: num(sinais.paSistolica),
      paDiastolica: num(sinais.paDiastolica),
      temperatura: num(sinais.temperatura),
      spo2: num(sinais.spo2),
      fc: num(sinais.fc),
      fr: num(sinais.fr),
    };
  }

  async function consultarSugestaoIA() {
    if (!atendimentoId || !sintomas.trim()) {
      setErro('Informe o ID do atendimento e os sintomas antes de consultar a IA.');
      return;
    }
    setErro(null);
    setConsultandoIa(true);
    try {
      const res = await triagensApi.sugerir({
        atendimentoId: Number(atendimentoId),
        sintomas: sintomas.trim(),
        sinais: buildSinais(),
        riscoIminente,
      });
      setSugestao(res);
      if (!res.indisponivel && res.cor) {
        setCorSelecionada(res.cor);
      }
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao consultar sugestão da IA.');
    } finally {
      setConsultandoIa(false);
    }
  }

  async function handleGravar(e: FormEvent) {
    e.preventDefault();
    if (!atendimentoId || !corSelecionada) {
      setErro('Selecione o atendimento e a classificação de risco definitiva.');
      return;
    }
    setErro(null);
    setGravando(true);
    try {
      const triagem = await triagensApi.gravar({
        atendimentoId: Number(atendimentoId),
        sintomas: sintomas.trim(),
        sinais: buildSinais(),
        cor: corSelecionada,
        sugestaoId: sugestao?.sugestaoId ?? null,
      });
      setSucesso(
        `Triagem #${triagem.id} gravada com cor ${COR_RISCO_META[triagem.cor].label} (${triagem.origem}).`,
      );
      setSugestao(null);
      setSintomas('');
      setCorSelecionada('');
      setAtendimentoId('');
      setSearchParams({});
      await carregarFila();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao gravar triagem.');
    } finally {
      setGravando(false);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-900">Triagem — Protocolo de Manchester</h2>
        <p className="text-sm text-slate-500">
          A IA sugere a cor de risco; a decisão e gravação definitiva são sempre do(a) enfermeiro(a) (RN-001).
        </p>
      </div>

      {erro && <AlertBanner type="error" message={erro} onClose={() => setErro(null)} />}
      {sucesso && <AlertBanner type="success" message={sucesso} onClose={() => setSucesso(null)} />}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-7 space-y-4">
          <Card className="p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-slate-900 flex items-center gap-2">
                <HeartPulse className="w-4 h-4 text-teal-600" />
                Registrar Triagem / Reclassificação
              </h3>
              {paciente && (
                <span className="text-xs font-medium text-teal-700 bg-teal-50 px-2.5 py-1 rounded-full">
                  Paciente: {paciente.nome}
                </span>
              )}
            </div>

            {paciente && !paciente.consentimentoIa && (
              <div className="mb-4 p-3 rounded-lg bg-amber-50 border border-amber-200 text-xs text-amber-900 flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-amber-600" />
                <span>
                  Este paciente <strong>não possui consentimento para IA</strong> (RN-002). Você pode
                  realizar triagem manual ou marcar <em>Risco iminente de vida</em> caso aplicável.
                </span>
              </div>
            )}

            <form onSubmit={handleGravar} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-700 mb-1">
                    ID do Atendimento *
                  </label>
                  <input
                    type="number"
                    required
                    min={1}
                    placeholder="Ex.: 1 (ou abra em Pacientes)"
                    value={atendimentoId}
                    onChange={(e) => setAtendimentoId(e.target.value)}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
                  />
                </div>
                <div className="flex items-end pb-2">
                  <label className="inline-flex items-center gap-2 text-xs text-slate-700 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={riscoIminente}
                      onChange={(e) => setRiscoIminente(e.target.checked)}
                      className="rounded border-slate-300 text-teal-600"
                    />
                    Risco iminente de vida (RN-002)
                  </label>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">
                  Queixa principal e sintomas *
                </label>
                <textarea
                  required
                  rows={3}
                  placeholder="Ex.: Dor no peito há 2 horas, sudorese e falta de ar..."
                  value={sintomas}
                  onChange={(e) => setSintomas(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
                />
              </div>

              <div>
                <p className="text-xs font-medium text-slate-700 mb-2">Sinais Vitais</p>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                  <div>
                    <label className="block text-[11px] text-slate-500">PA Sistólica (mmHg)</label>
                    <input
                      type="number"
                      min={0}
                      max={300}
                      value={sinais.paSistolica}
                      onChange={(e) => setSinais({ ...sinais, paSistolica: e.target.value })}
                      className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] text-slate-500">PA Diastólica (mmHg)</label>
                    <input
                      type="number"
                      min={0}
                      max={250}
                      value={sinais.paDiastolica}
                      onChange={(e) => setSinais({ ...sinais, paDiastolica: e.target.value })}
                      className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] text-slate-500">Temperatura (°C)</label>
                    <input
                      type="number"
                      step="0.1"
                      min={25}
                      max={45}
                      value={sinais.temperatura}
                      onChange={(e) => setSinais({ ...sinais, temperatura: e.target.value })}
                      className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] text-slate-500">SpO₂ (%)</label>
                    <input
                      type="number"
                      min={0}
                      max={100}
                      value={sinais.spo2}
                      onChange={(e) => setSinais({ ...sinais, spo2: e.target.value })}
                      className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] text-slate-500">FC (bpm)</label>
                    <input
                      type="number"
                      min={0}
                      max={300}
                      value={sinais.fc}
                      onChange={(e) => setSinais({ ...sinais, fc: e.target.value })}
                      className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] text-slate-500">FR (irpm)</label>
                    <input
                      type="number"
                      min={0}
                      max={80}
                      value={sinais.fr}
                      onChange={(e) => setSinais({ ...sinais, fr: e.target.value })}
                      className="w-full px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm"
                    />
                  </div>
                </div>
              </div>

              <div className="pt-1">
                <Button
                  type="button"
                  variant="secondary"
                  loading={consultandoIa}
                  onClick={consultarSugestaoIA}
                >
                  <Sparkles className="w-4 h-4 text-teal-600" />
                  Consultar Sugestão da IA
                </Button>
              </div>

              {sugestao && (
                <div
                  className={`p-4 rounded-xl border ${
                    sugestao.indisponivel
                      ? 'bg-amber-50 border-amber-200'
                      : 'bg-teal-50/70 border-teal-200'
                  }`}
                >
                  {sugestao.indisponivel ? (
                    <div className="text-sm text-amber-900 space-y-1">
                      <p className="font-semibold flex items-center gap-1.5">
                        <AlertTriangle className="w-4 h-4" />
                        IA indisponível no momento
                      </p>
                      <p className="text-xs">
                        {sugestao.motivo || 'Selecione a cor manualmente abaixo para prosseguir.'}
                      </p>
                    </div>
                  ) : (
                    <div className="space-y-2">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-xs font-semibold uppercase tracking-wide text-teal-800">
                          Sugestão da IA ({sugestao.implementacao})
                        </span>
                        {sugestao.confianca !== null && (
                          <span className="text-xs text-teal-700">
                            Confiança: {Math.round(sugestao.confianca * 100)}%
                          </span>
                        )}
                      </div>
                      <div>
                        <ManchesterBadge cor={sugestao.cor} showTempo />
                      </div>
                      {sugestao.justificativa && (
                        <p className="text-xs text-slate-700">{sugestao.justificativa}</p>
                      )}
                    </div>
                  )}
                </div>
              )}

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-2">
                  Classificação Definitiva do Enfermeiro *
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {CORES.map((cor) => {
                    const meta = COR_RISCO_META[cor];
                    const selected = corSelecionada === cor;
                    return (
                      <button
                        key={cor}
                        type="button"
                        onClick={() => setCorSelecionada(cor)}
                        className={`flex items-center justify-between px-3 py-2 rounded-lg border text-left text-xs transition-all cursor-pointer ${
                          selected
                            ? `${meta.border} ring-2 ring-offset-1 ring-teal-500 bg-slate-50 font-semibold`
                            : 'border-slate-200 hover:border-slate-300 bg-white'
                        }`}
                      >
                        <span className="flex items-center gap-2">
                          <span className={`w-3 h-3 rounded-full ${meta.dot}`} />
                          {meta.label}
                        </span>
                        <span className="text-[11px] text-slate-500">{meta.tempo}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="flex justify-end pt-2">
                <Button type="submit" loading={gravando}>
                  <CheckCircle2 className="w-4 h-4" />
                  Confirmar e Gravar Triagem
                </Button>
              </div>
            </form>
          </Card>
        </div>

        <div className="lg:col-span-5 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-slate-900">Fila Aguardando Médico</h3>
            <Button size="sm" variant="ghost" onClick={() => void carregarFila()}>
              <RefreshCw className="w-3.5 h-3.5" />
              Atualizar
            </Button>
          </div>

          {loadingFila ? (
            <Spinner />
          ) : fila.length === 0 ? (
            <EmptyState
              title="Fila vazia"
              description="Nenhum paciente triado aguardando médico no momento."
            />
          ) : (
            <div className="space-y-2.5">
              {fila.map((item) => (
                <Card key={item.id} className="p-3.5 space-y-2">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{item.paciente.nome}</p>
                      <p className="text-xs text-slate-500">
                        Atendimento #{item.id} • Chegada: {formatDateTime(item.chegada)}
                      </p>
                    </div>
                    <ManchesterBadge cor={item.corAtual} />
                  </div>
                  <div className="flex items-center justify-between pt-1">
                    <Link
                      to={`/pacientes/${item.paciente.id}/prontuario`}
                      className="text-xs text-teal-700 hover:underline font-medium"
                    >
                      Ver prontuário
                    </Link>
                    <button
                      type="button"
                      onClick={() => {
                        setAtendimentoId(String(item.id));
                        setSearchParams({
                          atendimentoId: String(item.id),
                          pacienteId: String(item.paciente.id),
                        });
                      }}
                      className="text-xs text-slate-600 hover:text-slate-900 font-medium cursor-pointer"
                    >
                      Reclassificar
                    </button>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
