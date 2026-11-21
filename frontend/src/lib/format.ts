import type {
  CorRisco,
  GravidadeInteracao,
  OrigemCor,
  Perfil,
  StatusAtendimento,
  StatusLeito,
  StatusPrescricao,
  StatusVerificacao,
} from '../types/api';

export function formatCpf(cpf: string): string {
  const digits = cpf.replace(/\D/g, '');
  if (digits.length !== 11) return cpf;
  return digits.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  // LocalDate (YYYY-MM-DD)
  if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }
  return new Date(iso).toLocaleDateString('pt-BR');
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function calcIdade(dataNascimento: string): number {
  const [y, m, d] = dataNascimento.split('-').map(Number);
  const hoje = new Date();
  let idade = hoje.getFullYear() - y;
  const mes = hoje.getMonth() + 1 - m;
  if (mes < 0 || (mes === 0 && hoje.getDate() < d)) idade--;
  return idade;
}

export const PERFIL_META: Record<Perfil, { label: string; color: string }> = {
  ADMIN: { label: 'Administrador', color: 'bg-purple-100 text-purple-800 border-purple-200' },
  ENFERMEIRO: { label: 'Enfermeiro(a)', color: 'bg-teal-100 text-teal-800 border-teal-200' },
  MEDICO: { label: 'Médico(a)', color: 'bg-blue-100 text-blue-800 border-blue-200' },
  GESTOR_LEITOS: { label: 'Gestor de Leitos', color: 'bg-amber-100 text-amber-800 border-amber-200' },
  AUDITOR: { label: 'Auditor (DPO)', color: 'bg-slate-100 text-slate-800 border-slate-300' },
};

export const COR_RISCO_META: Record<
  CorRisco,
  { label: string; tempo: string; badge: string; dot: string; border: string }
> = {
  VERMELHO: {
    label: 'Vermelho — Emergência',
    tempo: 'Imediato (0 min)',
    badge: 'bg-red-600 text-white',
    dot: 'bg-red-600',
    border: 'border-red-500',
  },
  LARANJA: {
    label: 'Laranja — Muito Urgente',
    tempo: 'Até 10 min',
    badge: 'bg-orange-500 text-white',
    dot: 'bg-orange-500',
    border: 'border-orange-500',
  },
  AMARELO: {
    label: 'Amarelo — Urgente',
    tempo: 'Até 60 min',
    badge: 'bg-amber-400 text-slate-950',
    dot: 'bg-amber-400',
    border: 'border-amber-400',
  },
  VERDE: {
    label: 'Verde — Pouco Urgente',
    tempo: 'Até 120 min',
    badge: 'bg-emerald-600 text-white',
    dot: 'bg-emerald-600',
    border: 'border-emerald-500',
  },
  AZUL: {
    label: 'Azul — Não Urgente',
    tempo: 'Até 240 min',
    badge: 'bg-sky-600 text-white',
    dot: 'bg-sky-600',
    border: 'border-sky-500',
  },
};

export const ORIGEM_COR_LABEL: Record<OrigemCor, string> = {
  IA_CONFIRMADA: 'IA Confirmada',
  IA_RECLASSIFICADA: 'IA Reclassificada',
  MANUAL: 'Manual',
};

export const STATUS_ATENDIMENTO_META: Record<StatusAtendimento, { label: string; color: string }> = {
  AGUARDANDO_TRIAGEM: { label: 'Aguardando Triagem', color: 'bg-slate-100 text-slate-700' },
  AGUARDANDO_ATENDIMENTO: { label: 'Aguardando Médico', color: 'bg-amber-100 text-amber-800' },
  EM_ATENDIMENTO: { label: 'Em Atendimento', color: 'bg-blue-100 text-blue-800' },
  AGUARDANDO_LEITO: { label: 'Aguardando Leito', color: 'bg-purple-100 text-purple-800' },
  INTERNADO: { label: 'Internado', color: 'bg-indigo-100 text-indigo-800' },
  FINALIZADO: { label: 'Finalizado', color: 'bg-emerald-100 text-emerald-800' },
};

export const STATUS_LEITO_META: Record<
  StatusLeito,
  { label: string; cardBg: string; badge: string }
> = {
  LIVRE: {
    label: 'Livre',
    cardBg: 'bg-emerald-50/70 border-emerald-200 hover:border-emerald-300',
    badge: 'bg-emerald-100 text-emerald-800',
  },
  OCUPADO: {
    label: 'Ocupado',
    cardBg: 'bg-rose-50/70 border-rose-200 hover:border-rose-300',
    badge: 'bg-rose-100 text-rose-800',
  },
  HIGIENIZACAO: {
    label: 'Higienização',
    cardBg: 'bg-amber-50/70 border-amber-200 hover:border-amber-300',
    badge: 'bg-amber-100 text-amber-800',
  },
  RESERVADO: {
    label: 'Reservado',
    cardBg: 'bg-sky-50/70 border-sky-200 hover:border-sky-300',
    badge: 'bg-sky-100 text-sky-800',
  },
  INTERDITADO: {
    label: 'Interditado',
    cardBg: 'bg-slate-100 border-slate-300 hover:border-slate-400',
    badge: 'bg-slate-200 text-slate-800',
  },
};

/** Transições manuais permitidas no mapa de leitos (espelha TransicaoLeito.java). */
export const TRANSICOES_LEITO_MANUAIS: Record<StatusLeito, { status: StatusLeito; label: string }[]> = {
  LIVRE: [
    { status: 'RESERVADO', label: 'Reservar' },
    { status: 'INTERDITADO', label: 'Interditar' },
  ],
  RESERVADO: [{ status: 'LIVRE', label: 'Liberar Reserva' }],
  OCUPADO: [],
  HIGIENIZACAO: [{ status: 'LIVRE', label: 'Concluir Higienização' }],
  INTERDITADO: [{ status: 'HIGIENIZACAO', label: 'Enviar p/ Higienização' }],
};

export const STATUS_PRESCRICAO_LABEL: Record<StatusPrescricao, string> = {
  RASCUNHO: 'Rascunho',
  ASSINADA: 'Assinada',
};

export const STATUS_VERIFICACAO_META: Record<StatusVerificacao, { label: string; color: string }> = {
  NAO_VERIFICADA: { label: 'Não Verificada', color: 'bg-slate-100 text-slate-700' },
  VERIFICADA_IA: { label: 'Verificada pela IA', color: 'bg-emerald-100 text-emerald-800' },
  IA_INDISPONIVEL: { label: 'IA Indisponível', color: 'bg-amber-100 text-amber-800' },
};

export const GRAVIDADE_META: Record<GravidadeInteracao, { label: string; box: string; badge: string }> = {
  LEVE: {
    label: 'Leve',
    box: 'bg-sky-50 border-sky-200 text-sky-900',
    badge: 'bg-sky-100 text-sky-800',
  },
  MODERADA: {
    label: 'Moderada',
    box: 'bg-amber-50 border-amber-200 text-amber-900',
    badge: 'bg-amber-100 text-amber-800',
  },
  GRAVE: {
    label: 'Grave',
    box: 'bg-red-50 border-red-300 text-red-900',
    badge: 'bg-red-600 text-white',
  },
};
