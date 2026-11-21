import {
  AlertTriangle,
  ArrowLeft,
  ArrowRightLeft,
  BedDouble,
  CheckCircle2,
  ClipboardPlus,
  FileCheck2,
  HeartPulse,
  LogOut,
  Pill,
  Plus,
  ShieldAlert,
  ShieldCheck,
  Sparkles,
  Trash2,
} from 'lucide-react';
import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  AlertBanner,
  Button,
  Card,
  ManchesterBadge,
  Modal,
  Spinner,
} from '../components/ui';
import { useAuth } from '../contexts/AuthContext';
import {
  ApiError,
  atendimentosApi,
  internacoesApi,
  leitosApi,
  pacientesApi,
  prescricoesApi,
} from '../lib/api';
import {
  calcIdade,
  formatCpf,
  formatDate,
  formatDateTime,
  GRAVIDADE_META,
  ORIGEM_COR_LABEL,
  STATUS_ATENDIMENTO_META,
  STATUS_PRESCRICAO_LABEL,
  STATUS_VERIFICACAO_META,
} from '../lib/format';
import type {
  InternacaoDto,
  ItemPrescricaoDto,
  LeitoDto,
  PrescricaoDto,
  ProntuarioDto,
  SetorDto,
} from '../types/api';

const EMPTY_ITEM: ItemPrescricaoDto = {
  medicamento: '',
  dose: '',
  via: 'VO',
  frequencia: '',
};

export function ProntuarioPage() {
  const { id } = useParams<{ id: string }>();
  const pacienteId = Number(id);
  const { hasRole } = useAuth();
  const navigate = useNavigate();

  const ehMedico = hasRole('MEDICO');
  const ehEnfermeiro = hasRole('ENFERMEIRO');
  const ehGestor = hasRole('GESTOR_LEITOS');

  const [prontuario, setProntuario] = useState<ProntuarioDto | null>(null);
  const [setores, setSetores] = useState<SetorDto[]>([]);
  const [leitosLivres, setLeitosLivres] = useState<LeitoDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  // Evolução
  const [textoEvolucao, setTextoEvolucao] = useState('');
  const [salvandoEvolucao, setSalvandoEvolucao] = useState(false);

  // Indicar internação
  const [modalInternacaoAtendimentoId, setModalInternacaoAtendimentoId] = useState<number | null>(
    null,
  );
  const [setorIndicadoId, setSetorIndicadoId] = useState<string>('');
  const [indicando, setIndicando] = useState(false);

  // Transferência / Alta
  const [modalTransferir, setModalTransferir] = useState<InternacaoDto | null>(null);
  const [leitoDestinoId, setLeitoDestinoId] = useState<string>('');
  const [modalAlta, setModalAlta] = useState<InternacaoDto | null>(null);
  const [motivoAlta, setMotivoAlta] = useState('');
  const [processandoInternacao, setProcessandoInternacao] = useState(false);

  // Prescrição (Criar / Editar itens)
  const [modalPrescricaoInternacaoId, setModalPrescricaoInternacaoId] = useState<number | null>(
    null,
  );
  const [editandoPrescricaoId, setEditandoPrescricaoId] = useState<number | null>(null);
  const [itensPrescricao, setItensPrescricao] = useState<ItemPrescricaoDto[]>([{ ...EMPTY_ITEM }]);
  const [salvandoPrescricao, setSalvandoPrescricao] = useState(false);

  // Assinatura de Prescrição
  const [modalAssinar, setModalAssinar] = useState<PrescricaoDto | null>(null);
  const [justificativaAssinatura, setJustificativaAssinatura] = useState('');
  const [assinando, setAssinando] = useState(false);
  const [verificandoId, setVerificandoId] = useState<number | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro(null);
    try {
      const data = await pacientesApi.prontuario(pacienteId);
      setProntuario(data);

      if (ehMedico || ehGestor) {
        const [s, l] = await Promise.all([leitosApi.setores(), leitosApi.leitos()]);
        setSetores(s);
        setLeitosLivres(l.filter((item) => item.status === 'LIVRE'));
        if (s.length > 0 && !setorIndicadoId) setSetorIndicadoId(String(s[0].id));
      }
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao carregar prontuário.');
    } finally {
      setLoading(false);
    }
  }, [pacienteId, ehMedico, ehGestor, setorIndicadoId]);

  useEffect(() => {
    if (!Number.isNaN(pacienteId)) {
      void carregar();
    }
  }, [pacienteId, carregar]);

  async function toggleConsentimento() {
    if (!prontuario) return;
    setErro(null);
    try {
      const atualizado = await pacientesApi.definirConsentimento(
        prontuario.paciente.id,
        !prontuario.paciente.consentimentoIa,
      );
      setProntuario({ ...prontuario, paciente: atualizado });
      setSucesso(
        `Consentimento de IA ${atualizado.consentimentoIa ? 'concedido' : 'revogado'}.`,
      );
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao atualizar consentimento.');
    }
  }

  async function handleEvolucao(e: FormEvent) {
    e.preventDefault();
    if (!textoEvolucao.trim()) return;
    setSalvandoEvolucao(true);
    setErro(null);
    try {
      await pacientesApi.registrarEvolucao(pacienteId, textoEvolucao.trim());
      setTextoEvolucao('');
      setSucesso('Evolução clínica registrada.');
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao registrar evolução.');
    } finally {
      setSalvandoEvolucao(false);
    }
  }

  async function iniciarAtendimento(atendimentoId: number) {
    setErro(null);
    try {
      await atendimentosApi.iniciar(atendimentoId);
      setSucesso('Atendimento iniciado.');
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao iniciar atendimento.');
    }
  }

  async function finalizarAtendimento(atendimentoId: number) {
    setErro(null);
    try {
      await atendimentosApi.finalizar(atendimentoId);
      setSucesso('Atendimento finalizado (alta do pronto-socorro).');
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao finalizar atendimento.');
    }
  }

  async function handleIndicarInternacao(e: FormEvent) {
    e.preventDefault();
    if (!modalInternacaoAtendimentoId || !setorIndicadoId) return;
    setIndicando(true);
    setErro(null);
    try {
      await atendimentosApi.indicarInternacao(modalInternacaoAtendimentoId, Number(setorIndicadoId));
      setModalInternacaoAtendimentoId(null);
      setSucesso('Internação indicada. Paciente entrou na fila de leitos.');
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao indicar internação.');
    } finally {
      setIndicando(false);
    }
  }

  async function handleTransferir(e: FormEvent) {
    e.preventDefault();
    if (!modalTransferir || !leitoDestinoId) return;
    setProcessandoInternacao(true);
    setErro(null);
    try {
      await internacoesApi.transferir(modalTransferir.id, Number(leitoDestinoId));
      setModalTransferir(null);
      setSucesso('Paciente transferido de leito.');
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao transferir paciente.');
    } finally {
      setProcessandoInternacao(false);
    }
  }

  async function handleAlta(e: FormEvent) {
    e.preventDefault();
    if (!modalAlta || !motivoAlta.trim()) return;
    setProcessandoInternacao(true);
    setErro(null);
    try {
      await internacoesApi.alta(modalAlta.id, motivoAlta.trim());
      setModalAlta(null);
      setMotivoAlta('');
      setSucesso('Alta hospitalar registrada. Leito encaminhado para higienização.');
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao registrar alta.');
    } finally {
      setProcessandoInternacao(false);
    }
  }

  function abrirNovaPrescricao(internacaoId: number) {
    setEditandoPrescricaoId(null);
    setModalPrescricaoInternacaoId(internacaoId);
    setItensPrescricao([{ ...EMPTY_ITEM }]);
  }

  function abrirEditarPrescricao(p: PrescricaoDto) {
    setModalPrescricaoInternacaoId(null);
    setEditandoPrescricaoId(p.id);
    setItensPrescricao(p.itens.length > 0 ? p.itens.map((i) => ({ ...i })) : [{ ...EMPTY_ITEM }]);
  }

  async function handleSalvarPrescricao(e: FormEvent) {
    e.preventDefault();
    setSalvandoPrescricao(true);
    setErro(null);
    try {
      if (editandoPrescricaoId) {
        await prescricoesApi.atualizarItens(editandoPrescricaoId, itensPrescricao);
        setSucesso('Itens da prescrição atualizados.');
      } else if (modalPrescricaoInternacaoId) {
        await prescricoesApi.criar(modalPrescricaoInternacaoId, itensPrescricao);
        setSucesso('Prescrição criada em rascunho.');
      }
      setModalPrescricaoInternacaoId(null);
      setEditandoPrescricaoId(null);
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao salvar prescrição.');
    } finally {
      setSalvandoPrescricao(false);
    }
  }

  async function verificarInteracoes(prescricaoId: number) {
    setErro(null);
    setVerificandoId(prescricaoId);
    try {
      const res = await prescricoesApi.verificar(prescricaoId);
      setSucesso(
        res.statusVerificacao === 'IA_INDISPONIVEL'
          ? 'IA indisponível: você poderá assinar informando uma justificativa clínica.'
          : `Verificação concluída: ${res.alertas.length} alerta(s) encontrado(s).`,
      );
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao verificar interações.');
    } finally {
      setVerificandoId(null);
    }
  }

  function precisaJustificativa(p: PrescricaoDto): boolean {
    return (
      p.statusVerificacao === 'IA_INDISPONIVEL' ||
      p.alertas.some((a) => a.gravidade === 'GRAVE')
    );
  }

  async function handleAssinar(e: FormEvent) {
    e.preventDefault();
    if (!modalAssinar) return;
    setAssinando(true);
    setErro(null);
    try {
      await prescricoesApi.assinar(modalAssinar.id, justificativaAssinatura.trim() || undefined);
      setModalAssinar(null);
      setJustificativaAssinatura('');
      setSucesso('Prescrição assinada eletronicamente.');
      await carregar();
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro ao assinar prescrição.');
    } finally {
      setAssinando(false);
    }
  }

  if (loading) return <Spinner label="Carregando prontuário..." />;
  if (!prontuario) {
    return (
      <div className="space-y-4">
        <AlertBanner type="error" message={erro || 'Prontuário não encontrado.'} />
        <Button variant="secondary" onClick={() => navigate(-1)}>
          <ArrowLeft className="w-4 h-4" /> Voltar
        </Button>
      </div>
    );
  }

  const { paciente, atendimentos, triagens, internacoes, prescricoes, evolucoes } = prontuario;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="p-2 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 cursor-pointer"
          aria-label="Voltar"
        >
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div>
          <h2 className="text-xl font-bold text-slate-900">Prontuário Eletrônico</h2>
          <p className="text-xs text-slate-500">
            Visualização filtrada conforme perfil de acesso (RN-005)
          </p>
        </div>
      </div>

      {erro && <AlertBanner type="error" message={erro} onClose={() => setErro(null)} />}
      {sucesso && <AlertBanner type="success" message={sucesso} onClose={() => setSucesso(null)} />}

      {/* Cabeçalho do Paciente */}
      <Card className="p-5">
        <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2.5">
              <h3 className="text-lg font-bold text-slate-900">{paciente.nome}</h3>
              <span className="text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded-full">
                {calcIdade(paciente.dataNascimento)} anos • Sexo {paciente.sexo}
              </span>
              {(ehEnfermeiro || ehMedico) ? (
                <button
                  type="button"
                  onClick={toggleConsentimento}
                  className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border cursor-pointer ${
                    paciente.consentimentoIa
                      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      : 'bg-amber-50 text-amber-800 border-amber-200'
                  }`}
                >
                  {paciente.consentimentoIa ? (
                    <>
                      <ShieldCheck className="w-3.5 h-3.5" /> IA Autorizada
                    </>
                  ) : (
                    <>
                      <ShieldAlert className="w-3.5 h-3.5" /> Sem Consentimento IA
                    </>
                  )}
                </button>
              ) : (
                <span
                  className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border ${
                    paciente.consentimentoIa
                      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      : 'bg-amber-50 text-amber-800 border-amber-200'
                  }`}
                >
                  {paciente.consentimentoIa ? 'IA Autorizada' : 'Sem Consentimento IA'}
                </span>
              )}
            </div>

            <div className="text-xs text-slate-600 flex flex-wrap gap-x-5 gap-y-1">
              <span>CPF: {formatCpf(paciente.cpf)}</span>
              <span>Nascimento: {formatDate(paciente.dataNascimento)}</span>
              {paciente.cns && <span>CNS: {paciente.cns}</span>}
            </div>

            <div className="flex flex-wrap gap-2 pt-1">
              <span className="text-xs bg-red-50 text-red-700 border border-red-200 px-2.5 py-0.5 rounded-md">
                <strong>Alergias:</strong>{' '}
                {paciente.alergias.length > 0 ? paciente.alergias.join(', ') : 'Nenhuma informada'}
              </span>
              <span className="text-xs bg-slate-100 text-slate-700 border border-slate-200 px-2.5 py-0.5 rounded-md">
                <strong>Uso contínuo:</strong>{' '}
                {paciente.medicamentosUsoContinuo.length > 0
                  ? paciente.medicamentosUsoContinuo.join(', ')
                  : 'Nenhum'}
              </span>
            </div>
          </div>
        </div>
      </Card>

      {/* Atendimentos */}
      {atendimentos.length > 0 && (
        <section className="space-y-3">
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500">
            Atendimentos
          </h3>
          <div className="space-y-2.5">
            {atendimentos.map((a) => {
              const st = STATUS_ATENDIMENTO_META[a.status];
              return (
                <Card
                  key={a.id}
                  className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-sm font-semibold text-slate-900">
                        Atendimento #{a.id}
                      </span>
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${st.color}`}>
                        {st.label}
                      </span>
                      {a.corAtual && <ManchesterBadge cor={a.corAtual} />}
                    </div>
                    <p className="text-xs text-slate-500">
                      Chegada: {formatDateTime(a.chegada)}
                      {a.medicoResponsavel && ` • Médico: ${a.medicoResponsavel}`}
                    </p>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    {ehEnfermeiro && a.status === 'AGUARDANDO_TRIAGEM' && (
                      <Link
                        to={`/triagem?atendimentoId=${a.id}&pacienteId=${paciente.id}`}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-teal-600 text-white hover:bg-teal-700"
                      >
                        <HeartPulse className="w-3.5 h-3.5" />
                        Realizar Triagem
                      </Link>
                    )}
                    {ehMedico && a.status === 'AGUARDANDO_ATENDIMENTO' && (
                      <Button size="sm" onClick={() => iniciarAtendimento(a.id)}>
                        Iniciar Atendimento
                      </Button>
                    )}
                    {ehMedico && a.status === 'EM_ATENDIMENTO' && (
                      <>
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => setModalInternacaoAtendimentoId(a.id)}
                        >
                          <BedDouble className="w-3.5 h-3.5" />
                          Indicar Internação
                        </Button>
                        <Button size="sm" onClick={() => finalizarAtendimento(a.id)}>
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Finalizar (Alta PS)
                        </Button>
                      </>
                    )}
                  </div>
                </Card>
              );
            })}
          </div>
        </section>
      )}

      {/* Triagens */}
      {triagens.length > 0 && (
        <section className="space-y-3">
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500">
            Histórico de Triagens
          </h3>
          <div className="space-y-2.5">
            {triagens.map((t) => (
              <Card key={t.id} className="p-4 space-y-2">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <ManchesterBadge cor={t.cor} />
                    <span className="text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded-full">
                      {ORIGEM_COR_LABEL[t.origem]}
                    </span>
                  </div>
                  <span className="text-xs text-slate-500">
                    {formatDateTime(t.dataHora)} • {t.enfermeiro}
                  </span>
                </div>
                <p className="text-sm text-slate-800">{t.sintomas}</p>
                <div className="text-xs text-slate-600 flex flex-wrap gap-x-4 gap-y-1 pt-1">
                  <span>
                    PA: {t.sinais.paSistolica ?? '—'}/{t.sinais.paDiastolica ?? '—'} mmHg
                  </span>
                  <span>Temp: {t.sinais.temperatura ?? '—'} °C</span>
                  <span>SpO₂: {t.sinais.spo2 ?? '—'}%</span>
                  <span>FC: {t.sinais.fc ?? '—'} bpm</span>
                  <span>FR: {t.sinais.fr ?? '—'} irpm</span>
                </div>
              </Card>
            ))}
          </div>
        </section>
      )}

      {/* Internações */}
      {internacoes.length > 0 && (
        <section className="space-y-3">
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500">Internações</h3>
          <div className="space-y-2.5">
            {internacoes.map((i) => {
              const ativa = !i.saida;
              return (
                <Card
                  key={i.id}
                  className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-slate-900 text-sm">
                        Internação #{i.id} — Leito {i.leitoCodigo}
                      </span>
                      <span
                        className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                          ativa
                            ? 'bg-indigo-100 text-indigo-800'
                            : 'bg-slate-100 text-slate-600'
                        }`}
                      >
                        {ativa ? 'Ativa' : 'Encerrada'}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500">
                      Entrada: {formatDateTime(i.entrada)}
                      {i.saida && ` • Saída: ${formatDateTime(i.saida)}`}
                      {i.motivoAlta && ` • Motivo: ${i.motivoAlta}`}
                    </p>
                  </div>

                  {ativa && (ehMedico || ehGestor) && (
                    <div className="flex flex-wrap gap-2">
                      {ehMedico && (
                        <Button size="sm" onClick={() => abrirNovaPrescricao(i.id)}>
                          <Pill className="w-3.5 h-3.5" />
                          Nova Prescrição
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => {
                          setModalTransferir(i);
                          setLeitoDestinoId(leitosLivres[0] ? String(leitosLivres[0].id) : '');
                        }}
                      >
                        <ArrowRightLeft className="w-3.5 h-3.5" />
                        Transferir
                      </Button>
                      <Button size="sm" variant="danger" onClick={() => setModalAlta(i)}>
                        <LogOut className="w-3.5 h-3.5" />
                        Dar Alta
                      </Button>
                    </div>
                  )}
                </Card>
              );
            })}
          </div>
        </section>
      )}

      {/* Prescrições (visível para MEDICO quando houver ou puder criar) */}
      {ehMedico && (
        <section className="space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500">
              Prescrições Médicas
            </h3>
          </div>

          {prescricoes.length === 0 ? (
            <Card className="p-4 text-xs text-slate-500">
              Nenhuma prescrição registrada. Para prescrever, o paciente precisa ter uma internação ativa.
            </Card>
          ) : (
            <div className="space-y-3">
              {prescricoes.map((p) => {
                const verifMeta = STATUS_VERIFICACAO_META[p.statusVerificacao];
                const rascunho = p.status === 'RASCUNHO';
                return (
                  <Card key={p.id} className="p-4 space-y-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold text-slate-900 text-sm">
                          Prescrição #{p.id}
                        </span>
                        <span
                          className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                            rascunho
                              ? 'bg-slate-100 text-slate-700'
                              : 'bg-emerald-100 text-emerald-800'
                          }`}
                        >
                          {STATUS_PRESCRICAO_LABEL[p.status]}
                        </span>
                        <span
                          className={`px-2 py-0.5 rounded-full text-xs font-medium ${verifMeta.color}`}
                        >
                          {verifMeta.label}
                        </span>
                      </div>
                      <span className="text-xs text-slate-500">
                        {p.medico}
                        {p.assinadaEm && ` • Assinada em ${formatDateTime(p.assinadaEm)}`}
                      </span>
                    </div>

                    <div className="overflow-x-auto">
                      <table className="w-full text-xs text-left border-collapse">
                        <thead>
                          <tr className="border-b border-slate-200 text-slate-500">
                            <th className="py-1.5 pr-3 font-medium">Medicamento</th>
                            <th className="py-1.5 pr-3 font-medium">Dose</th>
                            <th className="py-1.5 pr-3 font-medium">Via</th>
                            <th className="py-1.5 font-medium">Frequência</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {p.itens.map((item, idx) => (
                            <tr key={idx}>
                              <td className="py-1.5 pr-3 font-medium text-slate-800">
                                {item.medicamento}
                              </td>
                              <td className="py-1.5 pr-3 text-slate-600">{item.dose}</td>
                              <td className="py-1.5 pr-3 text-slate-600">{item.via}</td>
                              <td className="py-1.5 text-slate-600">{item.frequencia}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>

                    {p.alertas.length > 0 && (
                      <div className="space-y-2 pt-1">
                        <p className="text-xs font-semibold text-slate-700">
                          Alertas de Interação Medicamentosa (IA):
                        </p>
                        {p.alertas.map((alerta, idx) => {
                          const g = GRAVIDADE_META[alerta.gravidade];
                          return (
                            <div key={idx} className={`p-3 rounded-lg border text-xs space-y-1 ${g.box}`}>
                              <div className="flex items-center gap-2">
                                <span className={`px-2 py-0.5 rounded-full font-semibold ${g.badge}`}>
                                  {g.label}
                                </span>
                                <span className="font-semibold">{alerta.medicamentos.join(' + ')}</span>
                              </div>
                              <p>{alerta.descricao}</p>
                              {alerta.recomendacao && (
                                <p className="opacity-90">
                                  <strong>Recomendação:</strong> {alerta.recomendacao}
                                </p>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    )}

                    {p.justificativa && (
                      <p className="text-xs text-slate-600 bg-slate-50 p-2.5 rounded-lg border border-slate-200">
                        <strong>Justificativa médica:</strong> {p.justificativa}
                      </p>
                    )}

                    {p.hashAssinatura && (
                      <p className="text-[11px] font-mono text-slate-400 truncate">
                        Assinatura digital: {p.hashAssinatura}
                      </p>
                    )}

                    {rascunho && (
                      <div className="flex flex-wrap gap-2 pt-2 border-t border-slate-100">
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() => abrirEditarPrescricao(p)}
                        >
                          Editar Itens
                        </Button>
                        <Button
                          size="sm"
                          variant="secondary"
                          loading={verificandoId === p.id}
                          onClick={() => verificarInteracoes(p.id)}
                        >
                          <Sparkles className="w-3.5 h-3.5 text-teal-600" />
                          Verificar Interações (IA)
                        </Button>
                        <Button
                          size="sm"
                          disabled={p.statusVerificacao === 'NAO_VERIFICADA'}
                          onClick={() => {
                            setModalAssinar(p);
                            setJustificativaAssinatura('');
                          }}
                        >
                          <FileCheck2 className="w-3.5 h-3.5" />
                          Assinar Prescrição
                        </Button>
                      </div>
                    )}
                  </Card>
                );
              })}
            </div>
          )}
        </section>
      )}

      {/* Evoluções Clínicas */}
      {(ehMedico || evolucoes.length > 0) && (
        <section className="space-y-3">
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500">
            Evoluções Clínicas
          </h3>

          {ehMedico && (
            <Card className="p-4">
              <form onSubmit={handleEvolucao} className="space-y-3">
                <label className="block text-xs font-medium text-slate-700">
                  Nova Evolução Clínica
                </label>
                <textarea
                  rows={3}
                  required
                  placeholder="Descreva a evolução do quadro clínico, conduta e plano..."
                  value={textoEvolucao}
                  onChange={(e) => setTextoEvolucao(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
                />
                <div className="flex justify-end">
                  <Button type="submit" size="sm" loading={salvandoEvolucao}>
                    <ClipboardPlus className="w-3.5 h-3.5" />
                    Registrar Evolução
                  </Button>
                </div>
              </form>
            </Card>
          )}

          {evolucoes.length > 0 && (
            <div className="space-y-2.5">
              {evolucoes.map((ev) => (
                <Card key={ev.id} className="p-4 space-y-1.5">
                  <div className="flex items-center justify-between text-xs text-slate-500">
                    <span className="font-semibold text-slate-700">{ev.autor}</span>
                    <span>{formatDateTime(ev.dataHora)}</span>
                  </div>
                  <p className="text-sm text-slate-800 whitespace-pre-line">{ev.texto}</p>
                </Card>
              ))}
            </div>
          )}
        </section>
      )}

      {/* Modal Indicar Internação */}
      <Modal
        open={modalInternacaoAtendimentoId !== null}
        onClose={() => setModalInternacaoAtendimentoId(null)}
        title="Indicar Internação"
      >
        <form onSubmit={handleIndicarInternacao} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">
              Setor de destino indicado *
            </label>
            <select
              required
              value={setorIndicadoId}
              onChange={(e) => setSetorIndicadoId(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
            >
              {setores.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.nome} ({s.tipo})
                </option>
              ))}
            </select>
          </div>
          <div className="flex justify-end gap-2">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setModalInternacaoAtendimentoId(null)}
            >
              Cancelar
            </Button>
            <Button type="submit" loading={indicando}>
              Confirmar Indicação
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Transferir Leito */}
      <Modal
        open={!!modalTransferir}
        onClose={() => setModalTransferir(null)}
        title="Transferir Paciente de Leito"
      >
        <form onSubmit={handleTransferir} className="space-y-4">
          {leitosLivres.length === 0 ? (
            <AlertBanner type="warning" message="Não há leitos LIVRES disponíveis para transferência." />
          ) : (
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Leito de destino *
              </label>
              <select
                required
                value={leitoDestinoId}
                onChange={(e) => setLeitoDestinoId(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
              >
                {leitosLivres.map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.codigo} — {setores.find((s) => s.id === l.setorId)?.nome ?? `Setor #${l.setorId}`}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setModalTransferir(null)}>
              Cancelar
            </Button>
            <Button type="submit" loading={processandoInternacao} disabled={leitosLivres.length === 0}>
              Confirmar Transferência
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Alta Hospitalar */}
      <Modal
        open={!!modalAlta}
        onClose={() => setModalAlta(null)}
        title="Registrar Alta Hospitalar"
      >
        <form onSubmit={handleAlta} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-700 mb-1">
              Motivo / Resumo da alta *
            </label>
            <textarea
              required
              rows={3}
              placeholder="Ex.: Alta médica melhorada com orientações ambulatoriais."
              value={motivoAlta}
              onChange={(e) => setMotivoAlta(e.target.value)}
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setModalAlta(null)}>
              Cancelar
            </Button>
            <Button type="submit" variant="danger" loading={processandoInternacao}>
              Confirmar Alta
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Criar/Editar Prescrição */}
      <Modal
        open={modalPrescricaoInternacaoId !== null || editandoPrescricaoId !== null}
        onClose={() => {
          setModalPrescricaoInternacaoId(null);
          setEditandoPrescricaoId(null);
        }}
        title={editandoPrescricaoId ? 'Editar Itens da Prescrição' : 'Nova Prescrição Médica'}
        maxWidth="max-w-2xl"
      >
        <form onSubmit={handleSalvarPrescricao} className="space-y-4">
          <div className="space-y-3">
            {itensPrescricao.map((item, idx) => (
              <div
                key={idx}
                className="p-3 rounded-lg border border-slate-200 bg-slate-50/70 space-y-2"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-600">Item #{idx + 1}</span>
                  {itensPrescricao.length > 1 && (
                    <button
                      type="button"
                      onClick={() =>
                        setItensPrescricao((prev) => prev.filter((_, i) => i !== idx))
                      }
                      className="text-xs text-red-600 hover:text-red-800 inline-flex items-center gap-1 cursor-pointer"
                    >
                      <Trash2 className="w-3.5 h-3.5" /> Remover
                    </button>
                  )}
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <input
                    required
                    placeholder="Medicamento (ex.: AAS)"
                    value={item.medicamento}
                    onChange={(e) =>
                      setItensPrescricao((prev) =>
                        prev.map((it, i) => (i === idx ? { ...it, medicamento: e.target.value } : it)),
                      )
                    }
                    className="px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm bg-white"
                  />
                  <input
                    required
                    placeholder="Dose (ex.: 100 mg)"
                    value={item.dose}
                    onChange={(e) =>
                      setItensPrescricao((prev) =>
                        prev.map((it, i) => (i === idx ? { ...it, dose: e.target.value } : it)),
                      )
                    }
                    className="px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm bg-white"
                  />
                  <input
                    required
                    placeholder="Via (ex.: VO, EV)"
                    value={item.via}
                    onChange={(e) =>
                      setItensPrescricao((prev) =>
                        prev.map((it, i) => (i === idx ? { ...it, via: e.target.value } : it)),
                      )
                    }
                    className="px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm bg-white"
                  />
                  <input
                    required
                    placeholder="Frequência (ex.: 1x ao dia)"
                    value={item.frequencia}
                    onChange={(e) =>
                      setItensPrescricao((prev) =>
                        prev.map((it, i) => (i === idx ? { ...it, frequencia: e.target.value } : it)),
                      )
                    }
                    className="px-2.5 py-1.5 border border-slate-300 rounded-lg text-sm bg-white"
                  />
                </div>
              </div>
            ))}
          </div>

          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() => setItensPrescricao((prev) => [...prev, { ...EMPTY_ITEM }])}
          >
            <Plus className="w-3.5 h-3.5" /> Adicionar Medicamento
          </Button>

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-200">
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                setModalPrescricaoInternacaoId(null);
                setEditandoPrescricaoId(null);
              }}
            >
              Cancelar
            </Button>
            <Button type="submit" loading={salvandoPrescricao}>
              Salvar Rascunho
            </Button>
          </div>
        </form>
      </Modal>

      {/* Modal Assinar Prescrição */}
      <Modal
        open={!!modalAssinar}
        onClose={() => setModalAssinar(null)}
        title="Assinar Prescrição Eletronicamente"
      >
        {modalAssinar && (
          <form onSubmit={handleAssinar} className="space-y-4">
            {precisaJustificativa(modalAssinar) && (
              <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 text-xs text-amber-900 flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-amber-600" />
                <span>
                  Atenção (RN-004): esta prescrição possui{' '}
                  <strong>interação medicamentosa GRAVE</strong> ou foi verificada com{' '}
                  <strong>IA indisponível</strong>. A justificativa clínica é obrigatória para assinar.
                </span>
              </div>
            )}

            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Justificativa clínica{' '}
                {precisaJustificativa(modalAssinar) ? '(obrigatória) *' : '(opcional)'}
              </label>
              <textarea
                rows={3}
                required={precisaJustificativa(modalAssinar)}
                placeholder="Informe a justificativa para manutenção da conduta..."
                value={justificativaAssinatura}
                onChange={(e) => setJustificativaAssinatura(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm"
              />
            </div>

            <div className="flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setModalAssinar(null)}>
                Cancelar
              </Button>
              <Button type="submit" loading={assinando}>
                <FileCheck2 className="w-4 h-4" />
                Assinar Prescrição
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
