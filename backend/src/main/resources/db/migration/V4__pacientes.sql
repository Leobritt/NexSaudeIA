CREATE TABLE paciente (
    id                         BIGSERIAL PRIMARY KEY,
    nome                       VARCHAR(150) NOT NULL,
    cpf                        VARCHAR(11)  NOT NULL UNIQUE CHECK (cpf ~ '^[0-9]{11}$'),
    cns                        VARCHAR(15),
    data_nascimento            DATE         NOT NULL,
    sexo                       VARCHAR(1)   NOT NULL CHECK (sexo IN ('F', 'M', 'O')),
    medicamentos_uso_continuo  TEXT[]       NOT NULL DEFAULT '{}',
    alergias                   TEXT[]       NOT NULL DEFAULT '{}',
    criado_em                  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_paciente_nome ON paciente (lower(nome));

-- RN-002: cada mudança de consentimento é uma nova linha; a mais recente vale.
CREATE TABLE consentimento_ia (
    id              BIGSERIAL PRIMARY KEY,
    paciente_id     BIGINT       NOT NULL REFERENCES paciente (id),
    concedido       BOOLEAN      NOT NULL,
    data_hora       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    registrado_por  VARCHAR(150) NOT NULL
);

CREATE INDEX idx_consentimento_paciente ON consentimento_ia (paciente_id, data_hora DESC);
