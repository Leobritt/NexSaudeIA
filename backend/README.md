# NexSaúde IA — Backend

API REST em Java 21 + Spring Boot 4, com PostgreSQL 16.

## Rodar tudo com um comando (para o time de front)

Pré-requisito: só o Docker.

```bash
docker compose up --build     # na raiz do repositório
```

A API fica em http://localhost:8080, já com os dados de demonstração.

## Rodar para desenvolver o backend

Pré-requisitos: JDK 21 e Docker.

```bash
docker compose up -d postgres      # na raiz do repositório
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Swagger (contrato da API): http://localhost:8080/swagger-ui.html
- Para chamar rotas protegidas: faça `POST /api/auth/login`, copie o `token` e use o botão **Authorize** do Swagger.

## Usuários de desenvolvimento

Todos com a senha `nexsaude123`. Só o admin existe fora do perfil `dev`.

| Perfil | E-mail |
|---|---|
| ADMIN | admin@nexsaude.local |
| ENFERMEIRO | enfermeiro@nexsaude.local |
| MEDICO | medico@nexsaude.local |
| GESTOR_LEITOS | gestor@nexsaude.local |
| AUDITOR | auditor@nexsaude.local |

O perfil `dev` também cria 3 setores (Pronto-Socorro, Clínica Médica, UTI) com 15 leitos e 5 pacientes.
Maria das Graças usa varfarina (prescrever AAS a ela mostra a interação grave). Luíza não deu consentimento
para IA (mostra a RN-002).

## IA

A IA fica atrás de uma interface e tem três implementações, escolhidas por `NEXSAUDE_IA_MODO`:

| Modo | Custo | Quando usar |
|---|---|---|
| `simulada` (padrão) | grátis | desenvolvimento e plano B da apresentação (regras fixas, sem internet) |
| `ollama` | grátis | IA de verdade rodando na própria máquina; os dados do paciente não saem do computador |
| `claude` | pago | Claude API (precisa de `ANTHROPIC_API_KEY`) |

Se a IA falhar, recusar ou passar do tempo limite, o sistema mostra "IA indisponível" e segue com a
triagem/verificação manual.

### Ollama (gratuita, local)

Crie um arquivo `.env` na raiz (ele não vai para o Git):

```
NEXSAUDE_IA_MODO=ollama
NEXSAUDE_IA_TIMEOUT_SEG=60
```

Depois, na raiz:

```bash
docker compose --profile ollama up --build -d
docker compose exec ollama ollama pull qwen2.5:3b     # só na primeira vez (~2 GB)
```

- Precisa de uns 4 GB de RAM livres. Sem placa de vídeo, cada resposta leva de alguns segundos a ~30 s,
  por isso o tempo limite maior. A primeira chamada é a mais lenta (o modelo é carregado na memória).
- Para outro modelo: `NEXSAUDE_IA_OLLAMA_MODELO=llama3.2:3b` no `.env` e o `ollama pull` correspondente.
- Rodando o backend fora do Docker, o Ollama é procurado em `http://localhost:11434`.

### Claude (paga)

No `.env`: `NEXSAUDE_IA_MODO=claude` e `ANTHROPIC_API_KEY=...`. Sem Docker:

```bash
NEXSAUDE_IA_MODO=claude ANTHROPIC_API_KEY=sk-ant-... ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Testes

```bash
cd backend
./mvnw test     # 137 testes; precisa do Docker (Testcontainers sobe um PostgreSQL)
```

Testes unitários terminam em `Test`; os de integração (API + banco real) terminam em `IT`.

## Observação sobre o banco local

Use sempre o perfil `dev` no mesmo volume do Docker. Se precisar zerar os dados: `docker compose down -v`.

## Códigos de resposta

| Código | Quando |
|---|---|
| 400 | dados inválidos (`errors` lista os campos) |
| 401 | sem token, token inválido/expirado ou login recusado |
| 403 | perfil sem permissão |
| 404 | recurso não encontrado |
| 409 | conflito com outra operação simultânea (ex.: dois pacientes no mesmo leito) |
| 422 | regra de negócio violada (`detail` explica) |
