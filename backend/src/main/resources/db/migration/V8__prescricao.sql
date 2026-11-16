CREATE TABLE prescricao (
    id                  BIGSERIAL PRIMARY KEY,
    internacao_id       BIGINT       NOT NULL REFERENCES internacao (id),
    medico_id           BIGINT       NOT NULL REFERENCES usuario (id),
    criada_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status              VARCHAR(10)  NOT NULL CHECK (status IN ('RASCUNHO', 'ASSINADA')),
    status_verificacao  VARCHAR(20)  NOT NULL CHECK (status_verificacao IN ('NAO_VERIFICADA', 'VERIFICADA_IA', 'IA_INDISPONIVEL')),
    sugestao_id         BIGINT REFERENCES sugestao_ia (id),
    justificativa       TEXT,
    assinada_por_id     BIGINT REFERENCES usuario (id),
    assinada_em         TIMESTAMPTZ,
    hash_assinatura     CHAR(64)
);

CREATE TABLE item_prescricao (
    prescricao_id  BIGINT       NOT NULL REFERENCES prescricao (id) ON DELETE CASCADE,
    ordem          INTEGER      NOT NULL,
    medicamento    VARCHAR(150) NOT NULL,
    dose           VARCHAR(60)  NOT NULL,
    via            VARCHAR(30)  NOT NULL,
    frequencia     VARCHAR(60)  NOT NULL,
    PRIMARY KEY (prescricao_id, ordem)
);

CREATE TABLE alerta_interacao (
    prescricao_id  BIGINT      NOT NULL REFERENCES prescricao (id) ON DELETE CASCADE,
    ordem          INTEGER     NOT NULL,
    medicamentos   TEXT[]      NOT NULL,
    gravidade      VARCHAR(10) NOT NULL CHECK (gravidade IN ('LEVE', 'MODERADA', 'GRAVE')),
    descricao      TEXT        NOT NULL,
    recomendacao   TEXT,
    PRIMARY KEY (prescricao_id, ordem)
);
