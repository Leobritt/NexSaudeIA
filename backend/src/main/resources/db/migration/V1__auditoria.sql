CREATE TABLE log_auditoria (
    id          BIGSERIAL PRIMARY KEY,
    data_hora   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    usuario     VARCHAR(150) NOT NULL,
    acao        VARCHAR(80)  NOT NULL,
    recurso     VARCHAR(80)  NOT NULL,
    id_recurso  VARCHAR(80),
    detalhes    JSONB
);

CREATE INDEX idx_log_auditoria_recurso ON log_auditoria (recurso, id_recurso);
CREATE INDEX idx_log_auditoria_usuario ON log_auditoria (usuario);

-- RN-006: registros de auditoria só podem ser inseridos, nunca alterados ou apagados.
CREATE FUNCTION log_auditoria_imutavel() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'log de auditoria é imutável';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_auditoria_imutavel
    BEFORE UPDATE OR DELETE ON log_auditoria
    FOR EACH ROW EXECUTE FUNCTION log_auditoria_imutavel();

CREATE TRIGGER trg_log_auditoria_sem_truncate
    BEFORE TRUNCATE ON log_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION log_auditoria_imutavel();
