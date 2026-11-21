import type {
  AtendimentoDto,
  EvolucaoDto,
  InternacaoDto,
  ItemPrescricaoDto,
  LeitoDto,
  LogAuditoriaDto,
  LoginRequest,
  LoginResponse,
  NovoUsuarioRequest,
  PacienteDto,
  PacienteRequest,
  Page,
  PrescricaoDto,
  ProblemDetail,
  ProntuarioDto,
  SetorDto,
  StatusLeito,
  SugestaoTriagemDto,
  SugestaoTriagemRequest,
  TriagemDto,
  TriagemRequest,
  UsuarioDto,
} from '../types/api';

const STORAGE_KEY = 'nexsaude_auth';

export class ApiError extends Error {
  status: number;
  errors?: Record<string, string>;

  constructor(status: number, message: string, errors?: Record<string, string>) {
    super(message);
    this.status = status;
    this.errors = errors;
  }
}

export function getStoredAuth(): LoginResponse | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as LoginResponse;
    if (parsed.expiraEm && new Date(parsed.expiraEm).getTime() <= Date.now()) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function setStoredAuth(auth: LoginResponse | null): void {
  if (!auth) {
    localStorage.removeItem(STORAGE_KEY);
  } else {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  }
}

const BASE_URL = import.meta.env.VITE_API_URL ?? '';

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const auth = getStoredAuth();
  const headers = new Headers(options.headers);
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (auth?.token) {
    headers.set('Authorization', `Bearer ${auth.token}`);
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (res.status === 401 && !path.startsWith('/api/auth/login')) {
    setStoredAuth(null);
    window.location.href = '/login';
    throw new ApiError(401, 'Sessão expirada. Faça login novamente.');
  }

  if (!res.ok) {
    let message = `Erro HTTP ${res.status}`;
    let errors: Record<string, string> | undefined;
    try {
      const body = (await res.json()) as ProblemDetail;
      if (body.detail) message = body.detail;
      else if (body.title) message = body.title;
      if (body.errors) {
        errors = body.errors;
        const firstField = Object.entries(body.errors)[0];
        if (firstField) {
          message = `${message}: ${firstField[0]} ${firstField[1]}`;
        }
      }
    } catch {
      // ignore non-JSON error bodies
    }
    if (res.status === 403 && message.startsWith('Erro HTTP')) {
      message = 'Você não tem permissão para executar esta operação.';
    }
    throw new ApiError(res.status, message, errors);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const authApi = {
  login: (body: LoginRequest) =>
    request<LoginResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
};

export const usuariosApi = {
  listar: () => request<UsuarioDto[]>('/api/usuarios'),
  criar: (body: NovoUsuarioRequest) =>
    request<UsuarioDto>('/api/usuarios', { method: 'POST', body: JSON.stringify(body) }),
};

export const pacientesApi = {
  pesquisar: (params?: { cpf?: string; nome?: string }) => {
    const qs = new URLSearchParams();
    if (params?.cpf) qs.set('cpf', params.cpf);
    if (params?.nome) qs.set('nome', params.nome);
    const query = qs.toString();
    return request<PacienteDto[]>(`/api/pacientes${query ? `?${query}` : ''}`);
  },
  detalhar: (id: number) => request<PacienteDto>(`/api/pacientes/${id}`),
  criar: (body: PacienteRequest) =>
    request<PacienteDto>('/api/pacientes', { method: 'POST', body: JSON.stringify(body) }),
  definirConsentimento: (id: number, concedido: boolean) =>
    request<PacienteDto>(`/api/pacientes/${id}/consentimento-ia`, {
      method: 'PUT',
      body: JSON.stringify({ concedido }),
    }),
  prontuario: (id: number) => request<ProntuarioDto>(`/api/pacientes/${id}/prontuario`),
  registrarEvolucao: (id: number, texto: string) =>
    request<EvolucaoDto>(`/api/pacientes/${id}/evolucoes`, {
      method: 'POST',
      body: JSON.stringify({ texto }),
    }),
};

export const atendimentosApi = {
  abrir: (pacienteId: number) =>
    request<AtendimentoDto>('/api/atendimentos', {
      method: 'POST',
      body: JSON.stringify({ pacienteId }),
    }),
  fila: () => request<AtendimentoDto[]>('/api/atendimentos/fila'),
  aguardandoLeito: () => request<AtendimentoDto[]>('/api/atendimentos/aguardando-leito'),
  iniciar: (id: number) =>
    request<AtendimentoDto>(`/api/atendimentos/${id}/iniciar`, { method: 'POST' }),
  indicarInternacao: (id: number, setorId: number) =>
    request<AtendimentoDto>(`/api/atendimentos/${id}/indicar-internacao`, {
      method: 'POST',
      body: JSON.stringify({ setorId }),
    }),
  finalizar: (id: number) =>
    request<AtendimentoDto>(`/api/atendimentos/${id}/finalizar`, { method: 'POST' }),
};

export const triagensApi = {
  sugerir: (body: SugestaoTriagemRequest) =>
    request<SugestaoTriagemDto>('/api/triagens/sugestao', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  gravar: (body: TriagemRequest) =>
    request<TriagemDto>('/api/triagens', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
};

export const leitosApi = {
  setores: () => request<SetorDto[]>('/api/setores'),
  leitos: (setorId?: number) =>
    request<LeitoDto[]>(`/api/leitos${setorId ? `?setor=${setorId}` : ''}`),
  alterarStatus: (id: number, status: StatusLeito) =>
    request<LeitoDto>(`/api/leitos/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    }),
};

export const internacoesApi = {
  internar: (atendimentoId: number, leitoId: number) =>
    request<InternacaoDto>('/api/internacoes', {
      method: 'POST',
      body: JSON.stringify({ atendimentoId, leitoId }),
    }),
  transferir: (id: number, leitoDestinoId: number) =>
    request<InternacaoDto>(`/api/internacoes/${id}/transferencia`, {
      method: 'POST',
      body: JSON.stringify({ leitoDestinoId }),
    }),
  alta: (id: number, motivo: string) =>
    request<InternacaoDto>(`/api/internacoes/${id}/alta`, {
      method: 'POST',
      body: JSON.stringify({ motivo }),
    }),
};

export const prescricoesApi = {
  criar: (internacaoId: number, itens: ItemPrescricaoDto[]) =>
    request<PrescricaoDto>('/api/prescricoes', {
      method: 'POST',
      body: JSON.stringify({ internacaoId, itens }),
    }),
  detalhar: (id: number) => request<PrescricaoDto>(`/api/prescricoes/${id}`),
  atualizarItens: (id: number, itens: ItemPrescricaoDto[]) =>
    request<PrescricaoDto>(`/api/prescricoes/${id}/itens`, {
      method: 'PUT',
      body: JSON.stringify({ itens }),
    }),
  verificar: (id: number) =>
    request<PrescricaoDto>(`/api/prescricoes/${id}/verificar`, { method: 'POST' }),
  assinar: (id: number, justificativa?: string) =>
    request<PrescricaoDto>(`/api/prescricoes/${id}/assinar`, {
      method: 'POST',
      body: JSON.stringify(justificativa ? { justificativa } : {}),
    }),
};

export const auditoriaApi = {
  listar: (params: {
    usuario?: string;
    recurso?: string;
    de?: string;
    ate?: string;
    page?: number;
    size?: number;
  }) => {
    const qs = new URLSearchParams();
    if (params.usuario) qs.set('usuario', params.usuario);
    if (params.recurso) qs.set('recurso', params.recurso);
    if (params.de) qs.set('de', params.de);
    if (params.ate) qs.set('ate', params.ate);
    if (params.page !== undefined) qs.set('page', String(params.page));
    if (params.size !== undefined) qs.set('size', String(params.size));
    const query = qs.toString();
    return request<Page<LogAuditoriaDto>>(`/api/auditoria${query ? `?${query}` : ''}`);
  },
};
