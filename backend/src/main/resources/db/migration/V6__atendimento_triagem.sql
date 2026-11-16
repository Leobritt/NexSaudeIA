CREATE TABLE atendimento (
    id                    BIGSERIAL PRIMARY KEY,
    paciente_id           BIGINT      NOT NULL REFERENCES paciente (id),
    chegada               TIMESTAMPTZ NOT NULL DEFAULT now(),
    status                VARCHAR(25) NOT NULL CHECK (status IN ('AGUARDANDO_TRIAGEM', 'AGUARDANDO_ATENDIMENTO',
                              'EM_ATENDIMENTO', 'AGUARDANDO_LEITO', 'INTERNADO', 'FINALIZADO')),
    cor_atual             VARCHAR(10),
    medico_responsavel_id BIGINT REFERENCES usuario (id),
    setor_indicado_id     BIGINT,
    finalizado_em         TIMESTAMPTZ
);

CREATE INDEX idx_atendimento_status ON atendimento (status);
-- Um paciente só tem um atendimento ativo por vez.
CREATE UNIQUE INDEX uq_atendimento_ativo ON atendimento (paciente_id) WHERE status <> 'FINALIZADO';

-- RN-007: cada (re)classificação é uma nova linha; nada é sobrescrito.
CREATE TABLE triagem (
    id              BIGSERIAL PRIMARY KEY,
    atendimento_id  BIGINT       NOT NULL REFERENCES atendimento (id),
    sintomas        TEXT         NOT NULL,
    pa_sistolica    INTEGER,
    pa_diastolica   INTEGER,
    temperatura     DOUBLE PRECISION,
    spo2            INTEGER,
    fc              INTEGER,
    fr              INTEGER,
    cor             VARCHAR(10)  NOT NULL,
    origem          VARCHAR(20)  NOT NULL CHECK (origem IN ('IA_CONFIRMADA', 'IA_RECLASSIFICADA', 'MANUAL')),
    sugestao_id     BIGINT REFERENCES sugestao_ia (id),
    enfermeiro_id   BIGINT       NOT NULL REFERENCES usuario (id),
    data_hora       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_triagem_atendimento ON triagem (atendimento_id, data_hora);
