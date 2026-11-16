# NexSaúde IA

Sistema de gestão de leitos, triagem e prontuário eletrônico hospitalar com apoio de Inteligência Artificial.
Trabalho semestral de Tópicos Avançados de Programação (UCSAL).

A IA sugere, o profissional decide: a cor de risco na triagem (Protocolo de Manchester) e os alertas de
interação medicamentosa na prescrição só valem depois da confirmação humana.

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `backend/` | API REST em Java 21 + Spring Boot 4, PostgreSQL 16 ([README do backend](backend/README.md)) |
| `frontend/` | Interface em React (em desenvolvimento) |
| `docs/requisitos/` | Especificação de Requisitos (ERS) |
| `docs/superpowers/` | Design e plano de implementação do backend |

## Rodar

Pré-requisito: Docker.

```bash
docker compose up --build
```

- API: http://localhost:8080
- Swagger (contrato da API): http://localhost:8080/swagger-ui.html
- Usuários de teste (senha `nexsaude123`): `enfermeiro@`, `medico@`, `gestor@`, `auditor@` e `admin@nexsaude.local`

## Funcionalidades do MVP

- Login com perfis de acesso (RF-01)
- Cadastro de pacientes com consentimento para uso de IA (RF-02, RN-002)
- Triagem assistida por IA com confirmação do enfermeiro (RF-03, RN-001, RN-007)
- Prontuário com acesso por perfil e evoluções (RF-04, RN-005)
- Mapa de leitos, internação, transferência e alta (RF-05, RF-10, RN-003)
- Prescrição com verificação de interações por IA (RF-07, RF-08, RN-004)
- Trilha de auditoria imutável (RF-11, RN-006)
