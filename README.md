# RDN API

API Back-end para uma rede de lanchonetes multicanal. O projeto cobre o fluxo crítico
**Pedido → Pagamento mock → Atualização de status**, além de catálogo por unidade, fidelidade com
consentimento, promoções e auditoria de ações sensíveis.

## 1. Visão geral

- Pedidos podem nascer em `APP`, `TOTEM`, `BALCAO`, `PICKUP` ou `WEB`; o canal é obrigatório.
- O estoque é controlado por unidade e item, com baixa transacional e lock pessimista.
- O pagamento é simulado por um gateway determinístico (`APROVADO`, `RECUSADO`, `FALHA`), com
  `Idempotency-Key` obrigatório.
- Aprovar o pagamento leva o pedido para `RECEIVED`; recusar cancela; falha mantém o pedido
  aguardando nova tentativa.
- A fidelidade exige consentimento explícito. O cliente ganha pontos no pagamento aprovado e pode
  resgatar pontos como desconto em um pedido que ainda aguarda pagamento.
- Promoções são campanhas no banco; o pedido pode informar `promocaoId` e o desconto é calculado
  pelo domínio.
- Ações sensíveis são gravadas em `audit_logs` e consultáveis por `ADMIN`.

## 2. Arquitetura

Separação em camadas, com portas na aplicação e adaptadores na infraestrutura:

| Camada | Conteúdo |
|---|---|
| `domain` | Entidades e regras: `Order`, `Promotion`, `LoyaltyAccount`, `LoyaltyPolicy`, transições de status |
| `application` | Casos de uso e portas: pedidos, pagamento, fidelidade, promoções, catálogo e auditoria |
| `infrastructure` | JPA, mappers, repositórios Spring Data, JWT, gateway mock, auditoria e OpenAPI |
| `api` | Controllers, DTOs de request/response e `ApiError` |

O domínio não importa JPA. As entidades de persistência ficam em
`infrastructure/persistence/entity` e os mappers fazem a tradução.

## 3. Requisitos

- Java 25 (o `pom.xml` usa `java.version=25`).
- Maven Wrapper incluído (`mvnw` / `mvnw.cmd`) — não é necessário instalar Maven.
- PostgreSQL 16 ou Docker para o banco real.
- Portas padrão: `8080` para a API e `5432` para o PostgreSQL.
- Opcional: Python 3 com `reportlab` para gerar os relatórios ABNT em `docs/`.

> Se o terminal apontar para um JRE antigo, defina `JAVA_HOME` para um JDK 25 antes de rodar o
> wrapper. Exemplo no Windows:
>
> ```bat
> set "JAVA_HOME=C:\caminho\para\jdk-25"
> ```

## 4. Configuração

Copie o exemplo e ajuste os valores:

```bash
cp .env.example .env
```

| Variável | Default de desenvolvimento | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/rdn` | Conexão JDBC |
| `DB_USER` | `rdn` | Usuário do banco |
| `DB_PASSWORD` | `rdn` | Senha do banco |
| `JWT_SECRET` | valor de desenvolvimento no `application.properties` | Assinatura HS256, mínimo 32 bytes |
| `JWT_EXPIRATION_SECONDS` | `3600` | Validade do token |

**Nunca use o segredo default em produção.** Defina `JWT_SECRET` com pelo menos 32 bytes.

## 5. Banco de dados e migrations

O schema é criado exclusivamente pelo Flyway; o Hibernate apenas valida (`ddl-auto=validate`).

```bash
docker compose up -d
docker compose ps
```

Migrations aplicadas:

| Versão | Conteúdo |
|---|---|
| `V1__schema.sql` | 13 tabelas, constraints e índices |
| `V2__seed.sql` | Filial, cardápio, itens e estoque |
| `V3__seed_users.sql` | 4 usuários BCrypt, um por perfil |
| `V4__add_awaiting_payment_status.sql` | Status `AGUARDANDO_PAGAMENTO` |
| `V5__loyalty_promotion_audit.sql` | `audit_logs`, `orders.promotion_id`, check de sinal e 3 promoções |

Para recriar do zero, remova o volume e suba novamente:

```bash
docker compose down -v
docker compose up -d
```

## 6. Execução

Windows:

```bat
.\mvnw.cmd spring-boot:run
```

Unix:

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. O log deve mostrar as migrations do Flyway e
`Started RdnApiApplication`.

## 7. Swagger / OpenAPI

- UI: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/v3/api-docs`

O login é público. Para os demais endpoints, use o botão **Authorize** com o token retornado por
`POST /auth/login` (esquema `bearerAuth`). Os caminhos de documentação são liberados no
`SecurityConfig`; o restante continua autenticado.

## 8. Usuários de teste

Criados pela `V3__seed_users.sql`, somente para desenvolvimento:

| E-mail | Senha | Perfil |
|---|---|---|
| `admin@rdn.local` | `Admin@123` | `ADMIN` |
| `staff@rdn.local` | `Staff@123` | `STAFF` |
| `kitchen@rdn.local` | `Kitchen@123` | `KITCHEN` |
| `customer@rdn.local` | `Customer@123` | `CUSTOMER` |

IDs fixos: `...000c` (admin), `...000d` (staff), `...000e` (kitchen), `...000f` (customer).
A filial é `...0001`, o cardápio `...0002` e os itens `...0003` a `...0005`. As promoções semeadas
na `V5` são `...00c1` (10%), `...00c2` (R$ 5,00) e `...00c3` (expirada, para teste negativo).

## 9. Fluxo rápido

1. `POST /auth/login` com `customer@rdn.local` / `Customer@123` e copie o `accessToken`.
2. `GET /promocoes` e escolha uma campanha vigente.
3. `POST /pedidos` com `canalPedido`, `unidadeId`, `formaPagamento=MOCK`, `promocaoId` e itens.
4. `POST /pedidos/{id}/pagamentos` com `Idempotency-Key` e `{ "formaPagamento":"MOCK", "simular":"APROVADO" }`.
5. `GET /fidelidade` e `GET /fidelidade/transacoes` para ver os pontos.
6. Crie outro pedido com `MOCK` e use `POST /fidelidade/resgates` com `{ "pedidoId":"...", "pontos":100 }`.
7. Com o token de `ADMIN`, consulte `GET /auditoria?acao=ORDER_CREATED`.

## 10. Endpoints

| Método | Rota | Perfis | Finalidade |
|---|---|---|---|
| `POST` | `/auth/login` | público | Login e emissão de JWT |
| `GET` | `/auth/me` | autenticado | Usuário do token |
| `GET` | `/unidades` | autenticado | Unidades abertas |
| `GET` | `/unidades/{id}/cardapio` | autenticado | Cardápio da unidade |
| `GET` | `/promocoes` | autenticado | Campanhas ativas e vigência |
| `POST` | `/pedidos` | `CUSTOMER`, `STAFF`, `ADMIN` | Cria pedido com promoção opcional |
| `GET` | `/pedidos` | `ADMIN`, `STAFF` | Lista com filtros `canalPedido`, `status`, `page`, `limit` |
| `GET` | `/pedidos/{id}` | autenticado | Detalha pedido com itens |
| `PATCH` | `/pedidos/{id}/status` | `KITCHEN`, `STAFF`, `ADMIN` | Transição de status |
| `POST` | `/pedidos/{id}/pagamentos` | `CUSTOMER`, `STAFF`, `ADMIN` | Cobrança mock idempotente |
| `GET` | `/pedidos/{id}/pagamentos` | autenticado | Histórico de tentativas |
| `GET` | `/fidelidade` | `CUSTOMER` | Saldo e consentimento |
| `GET` | `/fidelidade/transacoes` | `CUSTOMER` | Extrato paginado |
| `PUT` | `/fidelidade/consentimento` | `CUSTOMER` | Opt-in/opt-out |
| `POST` | `/fidelidade/resgates` | `CUSTOMER` | Resgate como desconto |
| `GET` | `/auditoria` | `ADMIN` | Eventos com filtros |

Erros seguem o formato `ApiError` (`error`, `message`, `details`, `timestamp`, `path`). Códigos
usados: `200`, `201`, `400`, `401`, `403`, `404`, `405`, `409` e `500`.

## 11. Testes

```bat
.\mvnw.cmd test
```

A suíte usa H2 em modo PostgreSQL e não exige Docker. Cobre domínio, casos de uso e HTTP real com
JWT, incluindo:

- autenticação, `401` e `403`;
- criação, listagem, detalhe e transição de status;
- estoque insuficiente, produto inexistente e parâmetro inválido;
- pagamento aprovado, recusado, falha e replay idempotente;
- promoção aplicada, expirada e inexistente;
- consentimento, acúmulo e resgate de pontos;
- catálogo de unidades e cardápio;
- auditoria restrita a `ADMIN`;
- `SchemaDdlTest` aplicando `V1`–`V5` em banco limpo e `OpenApiDocsTest` validando
  `/v3/api-docs` e `/swagger-ui.html`.

## 12. Postman / Insomnia

Arquivos:

- `postman/rdn-api.postman_collection.json`
- `postman/rdn-api.postman_environment.json`

Importe os dois no Postman, selecione o environment e execute na ordem:

`01-Auth` → `02-Catalogo` → `03-Pedidos` → `04-Pagamento` → `05-Fidelidade` → `06-Promocoes` →
`07-Auditoria` → `08-Erros`.

A coleção possui 20 cenários (T01–T20), com pelo menos 6 positivos e 4 negativos. Os scripts salvam
`token`, `pedidoId` e `pagamentoId` automaticamente. Para o cenário de replay, a chave é fixa; nos
demais, use `{{$guid}}`.

## 13. Segurança e LGPD

- Senhas são armazenadas apenas como hash BCrypt; a senha pura não é persistida.
- Autenticação stateless com JWT HS256; autoridades derivadas de `UserRole`.
- Autorização por perfil em `SecurityConfig`: cliente, operação, cozinha e administração.
- O consentimento de fidelidade é explícito e registrado com timestamp; o opt-out congela o uso
  sem apagar o saldo.
- A auditoria guarda apenas o necessário: ator, ação, recurso, detalhe curto e IP. Nunca senha,
  token ou payload completo de pagamento.
- `audit_logs` não tem FK para `users`; o ator é um snapshot para preservar o histórico.
- Retenção e anonimização de `audit_logs` são uma política operacional; o projeto documenta a
  necessidade, mas não automatiza a rotina.

## 14. Decisões e limitações

- O fluxo crítico entregue é o **Fluxo A** do roteiro. O Fluxo B (entrada/saída de estoque via
  endpoints administrativos) não foi implementado; a restrição de venda por saldo existe.
- O provedor de pagamento é simulado. O campo `simular` existe apenas no mock; em produção ele não
  existiria.
- Não há refresh token, logout ou revogação de JWT; `auth_sessions` permanece no schema sem uso.
- CRUD administrativo de filial, cardápio e promoções não faz parte do MVP.
- O resgate de pontos é aplicado como desconto em pedido `AGUARDANDO_PAGAMENTO`; não há conversão
  em dinheiro nem transferência de pontos.
- Docker não está instalado na máquina de desenvolvimento usada neste trabalho. O
  `docker-compose.yml` está pronto e o `SchemaDdlTest` cobre `V1`–`V5`; a validação em PostgreSQL
  real deve ser executada pelo avaliador conforme a seção 5.

## 15. Evidências

| Evidência | Onde |
|---|---|
| Repositório público | `<preencher com a URL pública do repositório>` |
| Swagger local | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Coleção Postman | `postman/rdn-api.postman_collection.json` |
| Environment Postman | `postman/rdn-api.postman_environment.json` |
| DER | [`docs/DER.md`](docs/DER.md) |
| Relatórios por etapa | `docs/etapa-1-*.pdf` a `docs/etapa-5-*.pdf` |
| Relatório final ABNT | `docs/projeto-back-end-final-abnt.pdf` |

## 16. Estrutura do projeto

```text
src/main/java/com/rdn/rdn_api/
  api/            controllers, DTOs e tratamento de erro
  application/    casos de uso e portas
  domain/         entidades e regras de negócio
  infrastructure/ JPA, segurança, pagamento mock, auditoria e OpenAPI
src/main/resources/
  db/migration/   V1 a V5
  application.properties
src/test/java/    testes unitários e de integração
plans/            planos das etapas
docs/             DER e relatórios ABNT
tools/            geradores de PDF ABNT
postman/          coleção e environment
```
