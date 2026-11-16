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

Por padrão a IA é **simulada** (regras fixas, sem custo e sem internet). Para usar a Claude API:

```bash
NEXSAUDE_IA_MODO=claude ANTHROPIC_API_KEY=sk-ant-... ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Com Docker, crie um arquivo `.env` na raiz (ele não vai para o Git) com `NEXSAUDE_IA_MODO=claude` e
`ANTHROPIC_API_KEY=...` e rode `docker compose up --build`. Se a IA falhar, recusar ou passar de 10 s,
o sistema mostra "IA indisponível" e segue com a triagem/verificação manual.

## Testes

```bash
cd backend
./mvnw test     # 129 testes; precisa do Docker (Testcontainers sobe um PostgreSQL)
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
