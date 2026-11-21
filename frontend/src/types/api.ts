export type Perfil = 'ADMIN' | 'ENFERMEIRO' | 'MEDICO' | 'GESTOR_LEITOS' | 'AUDITOR';

export type CorRisco = 'VERMELHO' | 'LARANJA' | 'AMARELO' | 'VERDE' | 'AZUL';

export type OrigemCor = 'IA_CONFIRMADA' | 'IA_RECLASSIFICADA' | 'MANUAL';

export type StatusAtendimento =
  | 'AGUARDANDO_TRIAGEM'
  | 'AGUARDANDO_ATENDIMENTO'
  | 'EM_ATENDIMENTO'
  | 'AGUARDANDO_LEITO'
  | 'INTERNADO'
  | 'FINALIZADO';

export type StatusLeito = 'LIVRE' | 'OCUPADO' | 'HIGIENIZACAO' | 'INTERDITADO' | 'RESERVADO';

export type StatusPrescricao = 'RASCUNHO' | 'ASSINADA';

export type StatusVerificacao = 'NAO_VERIFICADA' | 'VERIFICADA_IA' | 'IA_INDISPONIVEL';

export type GravidadeInteracao = 'LEVE' | 'MODERADA' | 'GRAVE';

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  nome: string;
  perfil: Perfil;
  expiraEm: string;
}

export interface UsuarioDto {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  ativo: boolean;
}

export interface NovoUsuarioRequest {
  nome: string;
  email: string;
  senha: string;
  perfil: Perfil;
}

export interface PacienteResumoDto {
  id: number;
  nome: string;
  cpf: string;
  dataNascimento: string;
}

export interface PacienteDto {
  id: number;
  nome: string;
  cpf: string;
  cns: string | null;
  dataNascimento: string;
  sexo: 'F' | 'M' | 'O';
  medicamentosUsoContinuo: string[];
  alergias: string[];
  consentimentoIa: boolean;
}

export interface PacienteRequest {
  nome: string;
  cpf: string;
  cns?: string;
  dataNascimento: string;
  sexo: 'F' | 'M' | 'O';
  medicamentosUsoContinuo: string[];
  alergias: string[];
}

export interface SinaisVitais {
  paSistolica: number | null;
  paDiastolica: number | null;
  temperatura: number | null;
  spo2: number | null;
  fc: number | null;
  fr: number | null;
}

export interface AtendimentoDto {
  id: number;
  paciente: PacienteResumoDto;
  chegada: string;
  status: StatusAtendimento;
  corAtual: CorRisco | null;
  medicoResponsavel: string | null;
  setorIndicadoId: number | null;
}

export interface SugestaoTriagemRequest {
  atendimentoId: number;
  sintomas: string;
  sinais: SinaisVitais;
  riscoIminente: boolean;
}

export interface SugestaoTriagemDto {
  sugestaoId: number | null;
  cor: CorRisco | null;
  confianca: number | null;
  justificativa: string | null;
  implementacao: string | null;
  indisponivel: boolean;
  motivo: string | null;
}

export interface TriagemRequest {
  atendimentoId: number;
  sintomas: string;
  sinais: SinaisVitais;
  cor: CorRisco;
  sugestaoId?: number | null;
}

export interface TriagemDto {
  id: number;
  atendimentoId: number;
  cor: CorRisco;
  origem: OrigemCor;
  sintomas: string;
  sinais: SinaisVitais;
  enfermeiro: string;
  dataHora: string;
}

export interface SetorDto {
  id: number;
  nome: string;
  tipo: string;
}

export interface LeitoDto {
  id: number;
  codigo: string;
  setorId: number;
  status: StatusLeito;
  pacienteAtual: PacienteResumoDto | null;
}

export interface InternacaoDto {
  id: number;
  atendimentoId: number;
  paciente: PacienteResumoDto;
  leitoId: number;
  leitoCodigo: string;
  entrada: string;
  saida: string | null;
  motivoAlta: string | null;
}

export interface ItemPrescricaoDto {
  medicamento: string;
  dose: string;
  via: string;
  frequencia: string;
}

export interface AlertaDto {
  medicamentos: string[];
  gravidade: GravidadeInteracao;
  descricao: string;
  recomendacao: string;
}

export interface PrescricaoDto {
  id: number;
  internacaoId: number;
  status: StatusPrescricao;
  statusVerificacao: StatusVerificacao;
  itens: ItemPrescricaoDto[];
  alertas: AlertaDto[];
  medico: string;
  justificativa: string | null;
  assinadaEm: string | null;
  hashAssinatura: string | null;
}

export interface EvolucaoDto {
  id: number;
  autor: string;
  texto: string;
  dataHora: string;
}

export interface ProntuarioDto {
  paciente: PacienteDto;
  atendimentos: AtendimentoDto[];
  triagens: TriagemDto[];
  internacoes: InternacaoDto[];
  prescricoes: PrescricaoDto[];
  evolucoes: EvolucaoDto[];
}

export interface LogAuditoriaDto {
  id: number;
  dataHora: string;
  usuario: string;
  acao: string;
  recurso: string;
  idRecurso: string | null;
  detalhes: Record<string, unknown> | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}
