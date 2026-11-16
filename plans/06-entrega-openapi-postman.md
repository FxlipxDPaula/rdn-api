# Etapa 6 — Entrega: OpenAPI, README, Postman e validação final

> Última etapa do roteiro. As Etapas 1 a 5 entregam o sistema executável; esta etapa entrega o
> **pacote corrigível**: documentação viva da API, README reprodutível, coleção de testes executável,
> validação em PostgreSQL real quando o ambiente permitir e o relatório final único em ABNT.

## Objetivo

Fechar a entrega técnica exigida pelo roteiro do Projeto Multidisciplinar:

1. **Swagger/OpenAPI** refletindo os endpoints reais, com exemplos, códigos de status e o padrão de
   erro `ApiError`.
2. **README** que permita a qualquer avaliador subir o banco, aplicar as migrations, iniciar a API,
   autenticar, abrir o Swagger e rodar os testes sem adivinhação.
3. **Coleção Postman/Insomnia** com no mínimo 10 cenários executáveis (6 positivos e 4 negativos),
   organizada por recurso e com evidência de autenticação, fluxo crítico e erros.
4. **Validação em PostgreSQL real** via `docker-compose.yml`, incluindo Flyway `V1`–`V5` e
   `ddl-auto=validate`. Se o Docker continuar indisponível na máquina, registrar a limitação de forma
   explícita e manter a evidência automatizada do `SchemaDdlTest`.
5. **Relatório final ABNT único**, com todas as seções do roteiro, DER, diagramas, contratos, LGPD,
   plano de testes, conclusão e referências.

## Contexto de partida (Etapas 1 a 5 concluídas)

- API executável em Spring Boot 4.1.2 com Java 25, PostgreSQL 16 e H2 em modo PostgreSQL nos testes.
- 13 tabelas originais + `audit_logs` na `V5`, seeds de filial/cardápio/estoque/usuários/promoções.
- Fluxo A fechado: `POST /pedidos` → `POST /pedidos/{id}/pagamentos` → status do pedido, com
  idempotência, lock de estoque e padrão `ApiError`.
- Fidelidade, promoções, catálogo e auditoria entregues na Etapa 5.
- [`docker-compose.yml`](../docker-compose.yml:1) e [`.env.example`](../.env.example:1) prontos.
- Relatórios ABNT das Etapas 1 a 4 em `docs/`, além do da Etapa 5.

Lacunas de entrega verificadas:

| Lacuna | Evidência |
|---|---|
| `springdoc-openapi` não está no `pom.xml` | [`pom.xml`](../pom.xml:32) só tem starters de web, JPA, Flyway, segurança e validação |
| Swagger ficaria bloqueado pelo `anyRequest().authenticated()` | [`SecurityConfig`](../src/main/java/com/rdn/rdn_api/infrastructure/security/SecurityConfig.java:56) não libera `/v3/api-docs/**` nem `/swagger-ui/**` |
| Não existe `README.md` na raiz | listagem do workspace não contém README |
| Não existe coleção Postman/Insomnia | nenhum `.json` de coleção no repositório |
| Relatório final consolidado não existe | `docs/` tem PDFs por etapa, não o arquivo único de entrega |
| Validação em PostgreSQL real pendente | Etapa 1 confirmou ausência de Docker na máquina de desenvolvimento |

## Critério de pronto (DoD)

- `GET /v3/api-docs` responde `200` sem token e `GET /swagger-ui.html` abre a UI.
- A especificação contém todos os recursos: Auth, Catálogo, Pedidos, Pagamentos, Fidelidade,
  Promoções e Auditoria, com `bearerAuth` e exemplos de erro.
- `README.md` cobre requisitos, `.env`, banco/migrations, execução, Swagger, usuários de teste,
  testes, Postman, segurança/LGPD, decisões, limitações e links.
- Coleção Postman com pelo menos 10 cenários (6 positivos, 4 negativos) e environment; execução
  validada contra a API local.
- Quando houver Docker: `docker compose up -d`, Flyway `V1`–`V5` aplicadas, `ddl-auto=validate`
  passando e smoke test do fluxo A executado no PostgreSQL 16.
- Relatório final em PDF único, com capa, sumário, seções do roteiro, referências e declaração de uso
  de IA; arquivo renomeado para `<RU>_Projeto_Back_End.PDF` no momento da submissão.
- `.\mvnw.cmd test` verde e repositório sem `.env`, `target/` ou segredo real versionado.
- Histórico de commits com pelo menos 5 commits de evolução e tag de entrega.

## Decisões arquiteturais

1. **springdoc como única dependência de documentação.** Starter
   `org.springdoc:springdoc-openapi-starter-webmvc-ui` fixado em versão compatível com Spring Boot 4.
   Antes de anotar controllers, fazer um spike: adicionar a dependência, compilar, subir e abrir
   `/v3/api-docs`. Se a versão 3.x do springdoc não resolver para o Boot 4.1.2, registrar o erro e
   avaliar a alternativa de manter um `docs/openapi.yaml` versionado e servido como recurso estático,
   sem inventar endpoints. A prioridade é o contrato real, não a ferramenta.
2. **Swagger liberado no `SecurityConfig` apenas nos caminhos de documentação.** `/v3/api-docs/**`,
   `/swagger-ui/**` e `/swagger-ui.html` ficam públicos; o resto continua autenticado. O botão
   `Authorize` da UI recebe o JWT do `/auth/login`.
3. **Configuração OpenAPI centralizada.** Bean `OpenAPI` em `infrastructure/config/OpenApiConfig`
   com título, versão, descrição, servidor local e `SecurityScheme` HTTP bearer. Anotações pontuais
   (`@Operation`, `@ApiResponses`, `@Schema`) nos controllers e DTOs; os records já geram os schemas.
4. **README como fonte de verdade operacional.** O roteiro avalia a capacidade de o corretor rodar o
   projeto; o README replica os comandos exatos (`.\mvnw.cmd` no Windows, `./mvnw` em Unix) e deixa
   claro o que é seed de desenvolvimento e o que é decisão de projeto.
5. **Postman com variáveis e scripts de captura.** `baseUrl`, `token`, `pedidoId`, `pagamentoId`,
   `promocaoId`, `unidadeId` e `produtoId` no environment; scripts pós-requisição salvam token e ids.
   A coleção é a evidência executável do plano de testes; prints são complemento, não substituto.
6. **PostgreSQL real é a validação final, não a única.** A suíte continua em H2 para ser reproduzível
   sem Docker; o `SchemaDdlTest` prova o DDL. A execução em PostgreSQL confirma `ddl-auto=validate`,
   tipos, FKs e o seed. Se o Docker não existir na máquina, o README declara a limitação e ensina o
   corretor a executar a validação.
7. **Relatório final reaproveita os scripts ABNT por etapa.** Criar `tools/abnt_pdf_final.py` a partir
   de [`tools/abnt_pdf.py`](../tools/abnt_pdf.py:1), consolidando os conteúdos das Etapas 1 a 5 em um
   único documento com capa, sumário e referências. O PDF da Etapa 6 não substitui o arquivo único.
8. **Declaração de uso de IA.** O roteiro exige declarar ferramenta, prompts e trechos aproveitados.
   O relatório final terá uma seção própria, objetiva, sem expor dados pessoais ou segredos.

## Artefatos da etapa

```text
src/main/java/com/rdn/rdn_api/infrastructure/config/
  OpenApiConfig              OpenAPI bean + SecurityScheme bearer

README.md                    raiz do repositorio
postman/
  rdn-api.postman_collection.json
  rdn-api.postman_environment.json
docs/
  projeto-back-end-final-abnt.pdf
tools/
  abnt_pdf_final.py

Alteracoes:
  pom.xml                    dependencia springdoc
  application.properties     caminhos do springdoc
  SecurityConfig             liberar /v3/api-docs/** e /swagger-ui/**
  Controllers                anotacoes OpenAPI e exemplos
```

## Contratos e configuração

### `pom.xml`

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>VERSAO_COMPATIVEL_COM_BOOT_4</version>
</dependency>
```

Antes de escrever a versão, validar a resolução com o Maven Wrapper; a versão não pode ser chutada.

### `application.properties`

```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha
```

### `SecurityConfig`

Ordem antes do `anyRequest()`:

```java
.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
```

### `OpenApiConfig`

- `Info`: título `RDN API`, versão `1.0.0`, descrição curta do fluxo A e do mock de pagamento.
- `Server`: `http://localhost:8080`.
- `SecurityScheme`: `bearerAuth`, tipo `HTTP`, esquema `bearer`, formato `JWT`.
- `SecurityRequirement` global, exceto `/auth/login`.

### Anotações mínimas por controller

| Controller | Exemplos que precisam aparecer |
|---|---|
| [`AuthController`](../src/main/java/com/rdn/rdn_api/api/auth/AuthController.java:1) | login `200/400/401/403`, `/auth/me` `200/401` |
| [`OrderController`](../src/main/java/com/rdn/rdn_api/api/order/OrderController.java:36) | criação com `promocaoId`, `400/401/403/404/409`, listagem paginada |
| [`PaymentController`](../src/main/java/com/rdn/rdn_api/api/payment/PaymentController.java:34) | `Idempotency-Key`, replay `200`, criação `201`, recusa/falha |
| `LoyaltyController` | consentimento, saldo, extrato e resgate |
| `PromotionController` | campanhas ativas e flag de vigência |
| `CatalogController` | unidades e cardápio por unidade |
| `AuditController` | consulta `ADMIN`, filtros e paginação |

- Erros sempre documentados no schema `ApiError` (`error`, `message`, `details`, `timestamp`, `path`).
- Não incluir `passwordHash`, token ou payload bruto de gateway nos schemas de resposta.

## README — estrutura obrigatória

1. **Visão geral**: rede de lanchonetes, multicanalidade (`canalPedido`), Fluxo A e pagamento mock.
2. **Arquitetura**: Domain, Application, Infrastructure, API e o padrão porta/adaptador.
3. **Requisitos**: Java 25, Maven Wrapper, PostgreSQL 16 ou Docker, portas 8080/5432.
4. **Configuração**: copiar `.env.example`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` e
   `JWT_EXPIRATION_SECONDS`; aviso de que o segredo default é só desenvolvimento.
5. **Banco e migrations**: `docker compose up -d`; Flyway `V1`–`V5`; `ddl-auto=validate`; volume e
   healthcheck; como recriar o banco do zero.
6. **Execução**: `.\mvnw.cmd spring-boot:run`; nota sobre `JAVA_HOME` apontando para o JDK 25.
7. **Swagger**: `http://localhost:8080/swagger-ui.html`, rota do JSON `/v3/api-docs`, botão
   `Authorize` com o token do login.
8. **Usuários de teste**: tabela de `V3__seed_users.sql` com e-mail, senha e perfil; aviso de que são
   dados de desenvolvimento.
9. **Fluxo rápido**: login → `GET /promocoes` → `POST /pedidos` com `promocaoId` →
   `POST /pedidos/{id}/pagamentos` → `GET /fidelidade` → `POST /fidelidade/resgates` →
   `GET /auditoria` como `ADMIN`.
10. **Testes**: `.\mvnw.cmd test`; H2 em modo PostgreSQL; o que o `SchemaDdlTest` cobre.
11. **Postman**: importar coleção e environment, ordem sugerida, variáveis e `Idempotency-Key`.
12. **Endpoints**: tabela método/rota/perfis/status.
13. **Segurança e LGPD**: BCrypt, JWT, roles, consentimento, minimização, auditoria e retenção.
14. **Decisões e limites**: Fluxo A escolhido, mock determinístico, sem refresh/logout, sem CRUD de
    estoque, Docker/PostgreSQL quando aplicável.
15. **Evidências**: URL do repositório, Swagger local, coleção Postman, PDF, DER.
16. **Estrutura do projeto**: árvore resumida de `src/main/java` e `src/test/java`.

## Postman — cenários mínimos

Environment:

| Variável | Valor |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `token` | preenchido pelo script do login |
| `pedidoId` | preenchido pela criação do pedido |
| `pagamentoId` | preenchido pelo pagamento |
| `unidadeId` | `00000000-0000-0000-0000-000000000001` |
| `produtoId` | `00000000-0000-0000-0000-000000000003` |
| `promocaoId` | `00000000-0000-0000-0000-0000000000c1` |
| `customerEmail` / `customerPassword` | `customer@rdn.local` / `Customer@123` |
| `adminEmail` / `adminPassword` | `admin@rdn.local` / `Admin@123` |

Coleção organizada em pastas: `01-Auth`, `02-Catalogo`, `03-Pedidos`, `04-Pagamento`,
`05-Fidelidade`, `06-Promocoes`, `07-Auditoria`, `08-Erros`.

| ID | Cenário | Método/rota | Pré-condição | Esperado | Tipo |
|---|---|---|---|---|---|
| T01 | Login do cliente | `POST /auth/login` | seed aplicado | `200` + `accessToken` | positivo |
| T02 | Login do admin | `POST /auth/login` | seed aplicado | `200` + perfil `ADMIN` | positivo |
| T03 | Listar promoções | `GET /promocoes` | token | `200` com campanhas | positivo |
| T04 | Listar unidades | `GET /unidades` | token | `200` com RDN Centro | positivo |
| T05 | Consultar cardápio | `GET /unidades/{id}/cardapio` | token | `200` com 3 itens | positivo |
| T06 | Criar pedido com promoção | `POST /pedidos` | token do cliente, estoque | `201`, desconto e total | positivo |
| T07 | Pagar com mock aprovado | `POST /pedidos/{id}/pagamentos` | pedido `AGUARDANDO_PAGAMENTO`, `Idempotency-Key` | `201`, `APPROVED`, `RECEIVED` | positivo |
| T08 | Consultar pedido | `GET /pedidos/{id}` | pedido pago | `200`, status `RECEIVED` | positivo |
| T09 | Consultar fidelidade | `GET /fidelidade` | pagamento aprovado | `200` com pontos | positivo |
| T10 | Consultar extrato | `GET /fidelidade/transacoes` | pontos acumulados | `200` com `EARNED` | positivo |
| T11 | Resgatar pontos | `POST /fidelidade/resgates` | novo pedido aguardando, saldo | `200`, total reduzido | positivo |
| T12 | Consultar auditoria | `GET /auditoria` | token `ADMIN` | `200` com ações | positivo |
| T13 | Acesso sem token | `GET /pedidos` | sem header | `401 NAO_AUTENTICADO` | negativo |
| T14 | Cliente listando pedidos | `GET /pedidos` | token `CUSTOMER` | `403 ACESSO_NEGADO` | negativo |
| T15 | Pedido sem canal | `POST /pedidos` | token, body sem `canalPedido` | `400 DADOS_INVALIDOS` | negativo |
| T16 | Estoque insuficiente | `POST /pedidos` | quantidade acima do saldo | `409 ESTOQUE_INSUFICIENTE` | negativo |
| T17 | Promoção expirada | `POST /pedidos` | `promocaoId` da campanha de 2025 | `409 PROMOCAO_NAO_APLICAVEL` | negativo |
| T18 | Resgate sem consentimento | `POST /fidelidade/resgates` | cliente sem opt-in | `409 CONSENTIMENTO_NECESSARIO` | negativo |
| T19 | Query param inválido | `GET /pedidos?canalPedido=XYZ` | token `STAFF` | `400 PARAMETRO_INVALIDO` | negativo |
| T20 | Replay de pagamento | `POST /pedidos/{id}/pagamentos` | mesma `Idempotency-Key` | `200` sem duplicar | positivo |

- Scripts de teste da coleção: salvar `token` no login, `pedidoId` na criação, `pagamentoId` no
  pagamento; validar `statusCode` e trechos do JSON.
- Para o replay (T20), usar uma chave fixa no environment; para os demais, `{{$guid}}`.
- A coleção deve ser importável sem edição manual além do environment.

## Validação em PostgreSQL real

Sequência:

1. `docker compose up -d` e aguardar `healthy`.
2. Exportar `DB_URL=jdbc:postgresql://localhost:5432/rdn`, `DB_USER=rdn`, `DB_PASSWORD=rdn` e
   `JWT_SECRET` com pelo menos 32 bytes.
3. `.\mvnw.cmd spring-boot:run` e confirmar no log as migrations `V1`–`V5` e `Started RdnApiApplication`.
4. Conferir `flyway_schema_history` (5 migrations com `success = true`) e a existência de
   `audit_logs` e `orders.promotion_id`.
5. Executar a coleção Postman contra `http://localhost:8080` e conferir as linhas em `orders`,
   `payments`, `loyalty_accounts`, `loyalty_transactions` e `audit_logs`.
6. Reiniciar a aplicação uma vez para confirmar que `ddl-auto=validate` continua passando com o
   schema já existente.

Se o Docker não estiver disponível na máquina:

- Registrar no README, na seção de limitações, que a validação em PostgreSQL não foi executada
  localmente e que o `docker-compose.yml` está pronto para o avaliador.
- Anexar a evidência do `SchemaDdlTest` (V1–V5 em banco limpo) e do `mvnw test` verde.
- Não declarar no relatório uma validação que não aconteceu.

## Relatório final ABNT

Estrutura do PDF único:

1. Capa, folha de aprovação quando aplicável e sumário.
2. Introdução e objetivos.
3. Análise e requisitos: RF/RNF, multicanalidade, fidelização, promoções, pagamento mock e LGPD.
4. Diagramas: casos de uso, DER, classes de domínio e sequência do fluxo Pedido → Pagamento →
   Status; incluir o fluxo de fidelidade e o de auditoria.
5. API e endpoints: tabelas por recurso, contratos, status codes e padrão `ApiError`.
6. LGPD, privacidade e segurança: base legal do consentimento, minimização, hash, JWT, roles,
   auditoria e retenção.
7. Entrega técnica: repositório, README, Swagger, coleção Postman e instruções de execução.
8. Plano de testes: tabela T01–T20 com pré-condição, entrada, saída e evidência na coleção.
9. Conclusão: o que foi implementado, o que ficou proposto e por quê; aderência ao Fluxo A.
10. Referências ABNT.
11. Declaração de uso de IA: ferramenta, prompts e trechos aproveitados, conforme o roteiro.

- Base: [`tools/abnt_pdf.py`](../tools/abnt_pdf.py:1); criar `tools/abnt_pdf_final.py`.
- Saída de trabalho: `docs/projeto-back-end-final-abnt.pdf`.
- Submissão: renomear para `<RU>_Projeto_Back_End.PDF`.
- Revisar sumário, numeração de páginas e links internos antes de gerar a versão final.

## Checklist final de entrega

- [ ] `.\mvnw.cmd test` verde.
- [ ] `.\mvnw.cmd spring-boot:run` sobe com PostgreSQL e `ddl-auto=validate`.
- [ ] Flyway `V1`–`V5` aplicadas do zero.
- [ ] `/swagger-ui.html` e `/v3/api-docs` acessíveis sem token.
- [ ] Swagger com `bearerAuth` e exemplos de erro.
- [ ] README com todos os comandos e links.
- [ ] Coleção Postman importada e executada com pelo menos 10 cenários.
- [ ] Environment do Postman versionado sem segredos reais.
- [ ] `.env` e `target/` fora do repositório.
- [ ] Relatório final gerado, revisado e renomeado com o RU.
- [ ] DER atualizado e coerente com as migrations.
- [ ] Declaração de uso de IA incluída.
- [ ] Repositório público com pelo menos 5 commits e tag de entrega.
- [ ] Links do repositório, Swagger, Postman e PDF testados em janela anônima.

## Riscos

| Risco | Mitigação |
|---|---|
| springdoc sem versão compatível com Spring Boot 4.1.2 | spike de dependência antes das anotações; se falhar, registrar e usar OpenAPI versionado como fallback, sem inventar endpoints |
| Swagger bloqueado pela segurança | liberar apenas os caminhos de documentação e testar sem token |
| Coleção Postman divergir dos contratos | executar a coleção contra a API local após cada mudança de contrato; manter IDs fixos do seed |
| Docker indisponível na máquina | documentar a limitação, manter `SchemaDdlTest` e instruir o avaliador no README |
| PDF sem seção obrigatória ou sem referências | checklist seção a seção antes de gerar; revisar sumário e paginação |
| Segredo real versionado | `.env` no `.gitignore`, `.env.example` apenas com valores de desenvolvimento |
| Links privados ou quebrados | testar repositório/Swagger/Postman em janela anônima; README com URLs explícitas |
| Relatório declarar validação não executada | separar claramente "executado" de "instruções para execução" |

## Fora de escopo

- Deploy em nuvem.
- Provedor de pagamento real.
- Aplicação front-end.
- CRUD administrativo de cardápio, estoque e promoções.
- Refresh token, logout e revogação de JWT.
- Retenção automática de `audit_logs`.

## Ordem de execução

1. Spike do springdoc e configuração do `OpenApiConfig`.
2. Liberar os caminhos no `SecurityConfig` e validar a UI.
3. Anotar controllers e DTOs; revisar o JSON gerado.
4. Escrever o README completo.
5. Criar e executar a coleção Postman.
6. Validar em PostgreSQL real ou registrar a limitação.
7. Atualizar o DER e gerar o relatório final ABNT.
8. Rodar o checklist de entrega e congelar a tag.
