# NexSaúde IA — Design do Backend

Data: 2026-11-16 · Status: aprovado

Fonte dos requisitos: [ERS do NexSaúde IA](../../requisitos/NexSaudeIA_ERS.pdf) (RF-01 a RF-12, RN-001 a RN-008, RNF-01 a RNF-08, UC-01 a UC-03).

## 1. Intenção e porquê

O NexSaúde IA é o trabalho semestral da disciplina Tópicos Avançados em Programação (UCSAL). É um sistema hospitalar de triagem, leitos e prontuário com apoio de IA generativa. A IA sempre sugere e um humano sempre decide (*human-in-the-loop*).

O backend é a peça avaliada na Unidade II ("Código do Back-End"). Ele precisa:

- rodar e ser demonstrado ao vivo em até 20 minutos de apresentação;
- ser fácil de explicar por qualquer membro do time, já que quem apresenta é sorteado;
- servir de contrato estável para os 3 devs que fazem o frontend em React, que trabalham em paralelo.

Restrições do roteiro: o código precisa ser em **Java** (Spring Boot é permitido), versionado em Git e com a gestão no Trello.

Time: 4 devs e 1 ScrumMaster. Um dev (o autor deste documento) cuida do backend. Os demais cuidam do frontend e da documentação.

## 2. Objetivos, não-objetivos e anti-objetivos

**Objetivos (MVP)**
- RF-01: login com perfis de acesso (sem MFA).
- RF-02: cadastro e identificação única do paciente por CPF, com o consentimento para uso de IA (RN-002).
- RF-03 + RN-001 + RN-007: triagem com sugestão de cor de Manchester gerada pela IA, que o enfermeiro confirma ou reclassifica. Há fallback manual quando a IA falha (UC-01, fluxo alternativo).
- RF-05 + RF-10 + RN-003: mapa de leitos por setor, alocação, transferência, alta e higienização.
- RF-07 + RF-08 + RN-004: prescrição com verificação de interação medicamentosa pela IA e bloqueio em caso de interação grave sem justificativa.
- RF-11 + RN-006: trilha de auditoria imutável para acesso/alteração clínica e para toda chamada à IA.
- RF-04 (versão reduzida): o prontuário é a visão consolidada do paciente (atendimentos, triagens, internações, prescrições) mais as evoluções em texto.

**Fora do MVP (só se sobrar tempo, nesta ordem)**
- RF-12 dashboard com indicadores (tempo médio de espera, taxa de ocupação).
- RF-09 assistente virtual (chatbot).
- RF-06 previsão de ocupação.

**Não-objetivos**: estes itens ficam na documentação como trabalho futuro, sem implementação.
- MFA, criptografia AES-256 em repouso, TLS 1.3 (RNF-03): em desenvolvimento fica HTTP local.
- HL7 FHIR (RNF-08), 500 usuários simultâneos (RNF-02), SLA de 99,9% e backups (RNF-07).
- Retenção de 20 anos e anonimização para treino (RN-008).
- Assinatura eletrônica com certificado ICP-Brasil. No lugar dela entra a "assinatura" simplificada da seção 6.

**Anti-objetivos**: falha mesmo que "funcione".
- A IA gravar uma decisão clínica sozinha.
- A demonstração depender de internet ou de chave paga para funcionar.
- Backend tão complexo que só o autor consiga explicar.
- Mudanças na API que quebrem o front sem aviso.

## 3. Restrições do ambiente

- A máquina do autor **ainda não tem Java, Maven nem Docker** instalados (verificado em 2026-11-16). O plano precisa começar pela instalação do JDK 21 e de Docker ou PostgreSQL. O Maven vem pelo Maven Wrapper (`./mvnw`), sem instalação global.
- A chave da Claude API (`ANTHROPIC_API_KEY`) é paga e talvez não exista durante o desenvolvimento.

## 4. Abordagens consideradas

| Abordagem | Trade-off | Decisão |
|---|---|---|
| **Monólito Spring Boot com pacotes por módulo** | Simples de rodar, explicar e testar. Os módulos deixam o C4 de componentes claro. | **Escolhida** |
| Microsserviços (um serviço para a IA, um para o clínico) | Melhor C4 de contêineres, mas multiplica deploy, configuração e pontos de falha para um único dev no backend. | Descartada |
| Pacotes por camada (`controller/`, `service/`, `repository/`) | Comum em tutoriais, mas espalha cada funcionalidade por 4 pastas e dificulta dividir e explicar. | Descartada |

Para o C4 de contêineres, o sistema fica como: SPA React → API Spring Boot → PostgreSQL, mais a Claude API como sistema externo.

## 5. Decisões técnicas

- **Linguagem/Framework:** Java 21, Spring Boot 3.x, Maven com Maven Wrapper.
- **Dependências:** Spring Web, Spring Data JPA, Spring Security, Bean Validation, Flyway, driver PostgreSQL, springdoc-openapi (Swagger UI), biblioteca JWT, Anthropic Java SDK (`com.anthropic`), JUnit 5, Testcontainers (PostgreSQL), Spring Boot Test.
- **Banco:** PostgreSQL 16. O esquema é versionado por migrações Flyway (`V1__...sql`) e o Hibernate não gera o esquema (`ddl-auto=validate`).
- **Execução local:** `docker-compose.yml` na raiz sobe o PostgreSQL. O backend roda com `./mvnw spring-boot:run`.
- **Autenticação:** login com e-mail e senha (BCrypt) devolve um JWT com o perfil. Autorização por perfil com `@PreAuthorize`.
- **Contrato com o front:** OpenAPI gerado pelo springdoc em `/swagger-ui.html`. DTOs explícitos em `record`, sem expor entidades JPA. Erros no formato padrão `ProblemDetail` (RFC 7807). CORS liberado para `http://localhost:5173` (Vite).
- **Estrutura:**
  ```
  backend/src/main/java/br/ucsal/nexsaude/
    auth/        usuários, login, JWT, perfis
    paciente/    paciente, consentimento IA, prontuário (visão + evolução)
    triagem/     atendimento, triagem, confirmação/reclassificação
    leito/       setor, leito, internação, transferência, alta, higienização
    prescricao/  prescrição, itens, alertas de interação, justificativa
    auditoria/   log imutável + registro automático
    ia/          interface AssistenteClinicoIA, ClaudeIA, IASimulada, prompts versionados
    common/      erros, configuração, utilidades
  ```
  Cada módulo tem `XController`, `XService`, `XRepository`, entidades e `dto/`.

## 6. Modelo de domínio (resumo)

- **Usuario**: nome, e-mail, hash da senha, perfil ∈ {ADMIN, ENFERMEIRO, MEDICO, GESTOR_LEITOS, AUDITOR}, ativo. O perfil PACIENTE fica fora do MVP porque só é usado pelo chatbot.
- **Paciente**: nome, CPF (único), CNS opcional, data de nascimento, sexo, medicamentos de uso contínuo (texto/lista), alergias.
- **ConsentimentoIA**: paciente, concedido (sim/não), data, quem registrou. RN-002: sem consentimento ativo, a chamada à IA é bloqueada, **exceto** quando a triagem é marcada como "risco iminente de vida", o que fica registrado.
- **Atendimento**: paciente, data de chegada, status ∈ {AGUARDANDO_TRIAGEM, AGUARDANDO_ATENDIMENTO, EM_ATENDIMENTO, AGUARDANDO_LEITO, INTERNADO, FINALIZADO}, médico responsável, setor indicado para internação. *(Decisão do autor do design.)* Transições:
  - chegada (ENF) → AGUARDANDO_TRIAGEM
  - primeira triagem gravada → AGUARDANDO_ATENDIMENTO (entra na fila médica ordenada por cor e chegada)
  - `POST /atendimentos/{id}/iniciar` (MED) → EM_ATENDIMENTO; o médico vira o responsável (usado pela RN-005)
  - `POST /atendimentos/{id}/indicar-internacao` com setor (MED) → AGUARDANDO_LEITO
  - `POST /atendimentos/{id}/finalizar` (MED) → FINALIZADO (alta do pronto-socorro sem internação)
  - `POST /internacoes` (GESTOR) → INTERNADO; a alta da internação → FINALIZADO
- **Triagem**: atendimento, sintomas (texto), sinais vitais (PA sistólica/diastólica, temperatura, SpO₂, FC, FR), cor final, origem da cor ∈ {IA_CONFIRMADA, IA_RECLASSIFICADA, MANUAL}, enfermeiro, data. Há **várias por atendimento** (RN-007): reclassificar cria uma nova linha e a anterior nunca é sobrescrita. A fila mostra a triagem mais recente.
- **SugestaoIA**: tipo ∈ {TRIAGEM, INTERACAO}, entrada enviada (pseudonimizada), resposta bruta (JSON), cor/gravidade sugerida, confiança, justificativa, versão do prompt, modelo, implementação (CLAUDE/SIMULADA), status ∈ {PENDENTE, CONFIRMADA, REJEITADA, AJUSTADA}, sucesso/erro.
- **Setor / Leito**: setor (nome, tipo), leito (código, setor, status ∈ {LIVRE, OCUPADO, HIGIENIZACAO, INTERDITADO, RESERVADO}).
- **Internacao**: paciente, atendimento, leito atual, entrada, saída, motivo da alta. Uma transferência cria um registro de movimentação (`MovimentacaoLeito`) e o leito de origem vai para HIGIENIZACAO.
- **Prescricao / ItemPrescricao**: internação, médico, itens (medicamento, dose, via, frequência), status ∈ {RASCUNHO, ASSINADA}, justificativa de interação grave, data e hash da assinatura.
- **AlertaInteracao**: prescrição, medicamentos envolvidos, gravidade ∈ {LEVE, MODERADA, GRAVE}, descrição, recomendação.
- **EvolucaoClinica**: paciente/internação, autor, texto, data.
- **LogAuditoria**: data, usuário, ação, recurso, id do recurso, detalhes (JSON). Só aceita inserção.

**Assinatura eletrônica simplificada (RF-07):** ao assinar, o sistema grava o médico autenticado, a data/hora e o SHA-256 do conteúdo da prescrição. Depois de assinada, a prescrição não pode ser editada.

## 7. Regras de negócio no código

- **RN-001**: `POST /triagens/sugestao` só cria a `SugestaoIA` com status PENDENTE e não grava a cor. A `Triagem` só nasce em `POST /triagens` com a cor escolhida pelo enfermeiro e o id da sugestão (opcional). A origem é calculada comparando a cor final com a cor sugerida.
- **RN-002**: o `ia` checa o consentimento antes de chamar a IA. Sem consentimento e sem a flag de risco iminente, devolve 422 com mensagem clara e o front cai na triagem manual.
- **RN-003**: as transições de leito são validadas numa máquina de estados. Não existe HIGIENIZACAO→OCUPADO, só HIGIENIZACAO→LIVRE (por "higienização concluída"). A alta e a transferência levam o leito para HIGIENIZACAO.
- **RN-004**: assinar uma prescrição com um alerta GRAVE e sem justificativa devolve 422. Com justificativa, assina e registra no log.
- **Verificação obrigatória antes de assinar (decisão do autor do design):** a prescrição guarda o estado da verificação ∈ {NAO_VERIFICADA, VERIFICADA_IA, IA_INDISPONIVEL}. Editar os itens (`PUT /prescricoes/{id}/itens`, só em RASCUNHO) volta o estado para NAO_VERIFICADA. Assinar com NAO_VERIFICADA devolve 422, então não dá para pular `/verificar`. Quando a IA falha ou o paciente não tem consentimento, `/verificar` grava IA_INDISPONIVEL. Nesse caso, a assinatura exige justificativa, igual à interação GRAVE, e fica registrada como "assinada sem verificação de IA" no log.
- **RN-005 (simplificada)**: o histórico completo do prontuário fica visível para MEDICO e AUDITOR. ENFERMEIRO vê dados e triagens. GESTOR_LEITOS vê só identificação e internação.
- **RN-006**: `LogAuditoria` não tem endpoints de update/delete. No banco, um trigger impede UPDATE e DELETE na tabela.
- **RN-007**: coberta pelo modelo de `Triagem` acima.

## 8. Integração com IA

- Interface `AssistenteClinicoIA` com `sugerirTriagem(dados)` e `verificarInteracoes(itens, usoContinuo)`, que devolvem objetos estruturados.
- **ClaudeIA**: usa o Anthropic Java SDK, modelo `claude-opus-5-5`, com saída estruturada em JSON (cor, confiança, justificativa / lista de interações com gravidade). Fica ativa quando `nexsaude.ia.modo=claude` e `ANTHROPIC_API_KEY` estão definidos. O modelo é configurável em `application.yml`. Trocar para um modelo mais barato (ex.: `claude-haiku-5-5`) é decisão do time e só muda essa configuração.
- **OllamaIA** (acrescentada em 2026-11-16, a pedido do time, como alternativa gratuita): modelo aberto local via Ollama (`/api/chat` com `format` = JSON Schema), padrão `qwen2.5:3b`. Ativa com `nexsaude.ia.modo=ollama`; os dados não saem da máquina. Mesmo tratamento de falhas das outras implementações.
- **IASimulada**: implementação determinística por regras simples (ex.: SpO₂ < 90 ou PA sistólica < 90 → vermelho; febre alta + FC alta → laranja; …) e uma pequena tabela de pares conhecidos de interação (ex.: varfarina + AAS = GRAVE). É o **padrão** em desenvolvimento e o plano B da apresentação.
- **Fluxo (ERS §5.3):** front → backend → pseudonimização (envia idade e sexo, nunca nome nem CPF) → IA → grava `SugestaoIA` e log → front mostra a sugestão com um visual distinto (RNF-06, responsabilidade do front) → humano decide → só a decisão vira dado clínico.
- **Falhas:** timeout de 10 s. Se der erro, devolve uma resposta "IA indisponível" e registra no log. O front permite a triagem manual com origem MANUAL.
- **Prompts versionados:** ficam em `src/main/resources/prompts/triagem-v1.txt` etc. A versão é gravada em cada `SugestaoIA`.

## 9. API (esboço, prefixo `/api`)

| Método e rota | Perfil | Uso |
|---|---|---|
| `POST /auth/login` | público | devolve o JWT |
| `GET/POST /usuarios` | ADMIN | gestão de usuários |
| `GET/POST /pacientes`, `GET /pacientes/{id}`, `GET /pacientes?cpf=` | ENF, MED | cadastro e busca |
| `PUT /pacientes/{id}/consentimento-ia` | ENF, MED | RN-002 |
| `GET /pacientes/{id}/prontuario` | por RN-005 | visão consolidada |
| `POST /pacientes/{id}/evolucoes` | MED | evolução |
| `POST /atendimentos`, `GET /atendimentos/fila` | ENF, MED | chegada e fila por cor |
| `POST /atendimentos/{id}/iniciar`, `/indicar-internacao`, `/finalizar` | MED | fluxo do atendimento |
| `GET /atendimentos/aguardando-leito` | GESTOR | pacientes com indicação de internação (só identificação + setor) |
| `POST /triagens/sugestao` | ENF | pede sugestão à IA |
| `POST /triagens` | ENF | grava a triagem definitiva/reclassificação |
| `GET /setores`, `GET /leitos?setor=` | GESTOR, MED | mapa de leitos |
| `POST /internacoes` | GESTOR | aloca leito (UC-02) |
| `POST /internacoes/{id}/transferencia`, `/alta` | GESTOR, MED | RF-10 |
| `PATCH /leitos/{id}/status` | GESTOR | higienização, interdição, reserva |
| `POST /prescricoes`, `PUT /prescricoes/{id}/itens`, `POST /prescricoes/{id}/verificar` | MED | rascunho, edição e IA |
| `POST /prescricoes/{id}/assinar` | MED | RN-004 |
| `GET /auditoria` | AUDITOR | consulta paginada com filtros |

## 9.1 Trabalho em paralelo com o front (decisões do autor do design)

- **Cada dev roda tudo localmente.** O `docker-compose.yml` sobe PostgreSQL **e** o backend (Dockerfile multi-stage), então os devs de front só precisam de Docker, sem Java. Não há servidor compartilhado.
- **Contrato primeiro:** o primeiro marco depois do esqueleto é publicar todos os DTOs e controllers do MVP com o Swagger funcionando, mesmo que parte deles devolva dados fixos. Depois disso, cada módulo ganha a implementação real sem mudar o contrato.
- **Mapa de leitos "em tempo real" (RF-05):** o front consulta `GET /leitos` a cada 10 s (polling). WebSocket/SSE fica como trabalho futuro.

## 10. Dados de exemplo

Uma migração `V99__seed_dev.sql` (só no perfil `dev`) cria um usuário de cada perfil, 3 setores com ~15 leitos e alguns pacientes, deixando a demonstração pronta. As senhas de teste ficam documentadas no README do backend.

## 11. Testes

- Testes unitários das regras de negócio (máquina de estados de leito, RN-001, RN-004, IASimulada).
- Testes de integração por módulo com Testcontainers + MockMvc (fluxo UC-01, UC-02 e UC-03 de ponta a ponta com a IASimulada).
- O ClaudeIA só é testado manualmente quando houver chave, sem rodar no CI.

## 12. Decisões deixadas para o builder

- Biblioteca JWT específica (jjwt ou o `spring-security-oauth2-resource-server` com chave simétrica).
- Regras exatas da IASimulada, desde que cubram as 5 cores e pelo menos 3 pares de interação.
- Paginação, nomes de colunas e detalhes de DTOs.
- Texto dos prompts.
