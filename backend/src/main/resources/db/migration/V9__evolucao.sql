CREATE TABLE evolucao_clinica (
    id           BIGSERIAL PRIMARY KEY,
    paciente_id  BIGINT      NOT NULL REFERENCES paciente (id),
    autor_id     BIGINT      NOT NULL REFERENCES usuario (id),
    texto        TEXT        NOT NULL,
    data_hora    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_evolucao_paciente ON evolucao_clinica (paciente_id, data_hora DESC);
