-- Lock otimista: uma atualização feita sobre dados velhos é recusada (409) em vez de sobrescrever.
ALTER TABLE atendimento ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE prescricao  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
