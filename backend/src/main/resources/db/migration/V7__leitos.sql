CREATE TABLE setor (
    id    BIGSERIAL PRIMARY KEY,
    nome  VARCHAR(100) NOT NULL UNIQUE,
    tipo  VARCHAR(30)  NOT NULL
);

CREATE TABLE leito (
    id        BIGSERIAL PRIMARY KEY,
    codigo    VARCHAR(30) NOT NULL UNIQUE,
    setor_id  BIGINT      NOT NULL REFERENCES setor (id),
    status    VARCHAR(15) NOT NULL CHECK (status IN ('LIVRE', 'OCUPADO', 'HIGIENIZACAO', 'INTERDITADO', 'RESERVADO')),
    version   BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE internacao (
    id              BIGSERIAL PRIMARY KEY,
    atendimento_id  BIGINT      NOT NULL UNIQUE REFERENCES atendimento (id),
    paciente_id     BIGINT      NOT NULL REFERENCES paciente (id),
    leito_id        BIGINT      NOT NULL REFERENCES leito (id),
    entrada         TIMESTAMPTZ NOT NULL DEFAULT now(),
    saida           TIMESTAMPTZ,
    motivo_alta     TEXT
);

-- Um leito tem no máximo uma internação ativa.
CREATE UNIQUE INDEX uq_internacao_leito_ativa ON internacao (leito_id) WHERE saida IS NULL;

CREATE TABLE movimentacao_leito (
    id                BIGSERIAL PRIMARY KEY,
    internacao_id     BIGINT       NOT NULL REFERENCES internacao (id),
    leito_origem_id   BIGINT       NOT NULL REFERENCES leito (id),
    leito_destino_id  BIGINT       NOT NULL REFERENCES leito (id),
    data_hora         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    usuario           VARCHAR(150) NOT NULL
);

ALTER TABLE atendimento
    ADD CONSTRAINT fk_atendimento_setor FOREIGN KEY (setor_indicado_id) REFERENCES setor (id);
