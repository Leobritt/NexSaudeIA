-- Dados de demonstração (só no perfil dev). Idempotente: pode rodar várias vezes.
-- Senha de todos os usuários: nexsaude123

INSERT INTO usuario (nome, email, senha_hash, perfil) VALUES
    ('Enf. Carla Souza',   'enfermeiro@nexsaude.local', '$2b$10$h8gfv.vTfuaAuTQXuyoX2.r6i2wOGQ6PP8G1Ma1tyagrPD9BxDqim', 'ENFERMEIRO'),
    ('Dr. Paulo Mendes',   'medico@nexsaude.local',     '$2b$10$h8gfv.vTfuaAuTQXuyoX2.r6i2wOGQ6PP8G1Ma1tyagrPD9BxDqim', 'MEDICO'),
    ('Rita Gestora',       'gestor@nexsaude.local',     '$2b$10$h8gfv.vTfuaAuTQXuyoX2.r6i2wOGQ6PP8G1Ma1tyagrPD9BxDqim', 'GESTOR_LEITOS'),
    ('Ana Auditora (DPO)', 'auditor@nexsaude.local',    '$2b$10$h8gfv.vTfuaAuTQXuyoX2.r6i2wOGQ6PP8G1Ma1tyagrPD9BxDqim', 'AUDITOR')
ON CONFLICT (email) DO NOTHING;

INSERT INTO setor (nome, tipo) VALUES
    ('Pronto-Socorro', 'OBSERVACAO'),
    ('Clínica Médica', 'ENFERMARIA'),
    ('UTI',            'TERAPIA_INTENSIVA')
ON CONFLICT (nome) DO NOTHING;

INSERT INTO leito (codigo, setor_id, status)
SELECT v.codigo, s.id, v.status
FROM (VALUES
    ('PS-01', 'Pronto-Socorro', 'LIVRE'),
    ('PS-02', 'Pronto-Socorro', 'LIVRE'),
    ('PS-03', 'Pronto-Socorro', 'HIGIENIZACAO'),
    ('PS-04', 'Pronto-Socorro', 'LIVRE'),
    ('PS-05', 'Pronto-Socorro', 'INTERDITADO'),
    ('CM-01', 'Clínica Médica', 'LIVRE'),
    ('CM-02', 'Clínica Médica', 'LIVRE'),
    ('CM-03', 'Clínica Médica', 'RESERVADO'),
    ('CM-04', 'Clínica Médica', 'LIVRE'),
    ('CM-05', 'Clínica Médica', 'HIGIENIZACAO'),
    ('CM-06', 'Clínica Médica', 'LIVRE'),
    ('UTI-01', 'UTI', 'LIVRE'),
    ('UTI-02', 'UTI', 'LIVRE'),
    ('UTI-03', 'UTI', 'INTERDITADO'),
    ('UTI-04', 'UTI', 'LIVRE')
) AS v (codigo, setor, status)
JOIN setor s ON s.nome = v.setor
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO paciente (nome, cpf, data_nascimento, sexo, medicamentos_uso_continuo, alergias) VALUES
    ('Maria das Graças Lima', '52601815906', '1948-07-12', 'F', '{varfarina}',            '{}'),
    ('João Batista Santos',   '08301661305', '1975-02-03', 'M', '{losartana}',            '{dipirona}'),
    ('Ana Clara Oliveira',    '18609139034', '1999-11-21', 'F', '{}',                     '{}'),
    ('Carlos Eduardo Rocha',  '99603082430', '1962-05-30', 'M', '{enalapril,metformina}', '{penicilina}'),
    ('Luíza Ferreira',        '62819482112', '1988-09-14', 'F', '{}',                     '{}')
ON CONFLICT (cpf) DO NOTHING;

-- Consentimento para 4 dos 5 pacientes (Luíza fica sem, para demonstrar a RN-002).
INSERT INTO consentimento_ia (paciente_id, concedido, registrado_por)
SELECT p.id, TRUE, 'seed'
FROM paciente p
WHERE p.cpf IN ('52601815906', '08301661305', '18609139034', '99603082430')
  AND NOT EXISTS (SELECT 1 FROM consentimento_ia c WHERE c.paciente_id = p.id);
