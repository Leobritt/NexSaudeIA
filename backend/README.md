# NexSaúde IA — Backend

API REST em Java 21 + Spring Boot 4, com PostgreSQL 16.

## Rodar localmente

Pré-requisitos: JDK 21 e Docker.

```bash
docker compose up -d postgres      # na raiz do repositório
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Swagger (contrato da API): http://localhost:8080/swagger-ui.html
- Para chamar rotas protegidas: faça `POST /api/auth/login`, copie o `token` e use o botão **Authorize** do Swagger.

## Usuários de desenvolvimento

| Perfil | E-mail | Senha |
|---|---|---|
| ADMIN | admin@nexsaude.local | nexsaude123 |

Os outros perfis entram com os dados de demonstração (perfil `dev`).

## IA

Por padrão a IA é **simulada** (regras fixas, sem custo e sem internet). Para usar a Claude API:

```bash
NEXSAUDE_IA_MODO=claude ANTHROPIC_API_KEY=sk-ant-... ./mvnw spring-boot:run
```

## Testes

```bash
./mvnw test     # precisa do Docker (Testcontainers sobe um PostgreSQL)
```

## Códigos de resposta

| Código | Quando |
|---|---|
| 400 | dados inválidos (`errors` lista os campos) |
| 401 | sem token, token inválido/expirado ou login recusado |
| 403 | perfil sem permissão |
| 404 | recurso não encontrado |
| 422 | regra de negócio violada (`detail` explica) |
| 501 | rota publicada no contrato, ainda não implementada |
