-- Toda chamada à IA fica registrada aqui, com entrada pseudonimizada e resposta bruta (ERS 5.3/5.4).
CREATE TABLE sugestao_ia (
    id              BIGSERIAL PRIMARY KEY,
    tipo            VARCHAR(20)  NOT NULL CHECK (tipo IN ('TRIAGEM', 'INTERACAO')),
    paciente_id     BIGINT       NOT NULL REFERENCES paciente (id),
    data_hora       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    entrada         JSONB        NOT NULL,
    resposta_bruta  TEXT,
    cor             VARCHAR(10),
    confianca       DOUBLE PRECISION,
    justificativa   TEXT,
    interacoes      JSONB,
    versao_prompt   VARCHAR(30)  NOT NULL,
    modelo          VARCHAR(60)  NOT NULL,
    implementacao   VARCHAR(20)  NOT NULL,
    sucesso         BOOLEAN      NOT NULL,
    motivo_falha    TEXT,
    risco_iminente  BOOLEAN      NOT NULL DEFAULT FALSE,
    status          VARCHAR(12)  NOT NULL CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'REJEITADA', 'AJUSTADA'))
);
