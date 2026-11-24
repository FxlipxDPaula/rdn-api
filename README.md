# RDN API

API Back-end para uma rede de lanchonetes multicanal. O projeto cobre o fluxo crítico
**Pedido → Pagamento mock → Atualização de status**, além de catálogo por unidade, fidelidade com
consentimento, promoções e auditoria de ações sensíveis.

## 1. Visão geral

- Pedidos podem nascer em `APP`, `TOTEM`, `BALCAO`, `PICKUP` ou `WEB`; o canal é obrigatório.
- Usuários são criados por `POST /usuarios`, restrito a `ADMIN`; a senha é gravada apenas como hash BCrypt.
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
- Opcional: Python 3 com `reportlab` e `Pillow` para gerar os diagramas e os relatórios ABNT em
  `documentacao/docs/`.

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

O `docker-compose.yml` sobe apenas o PostgreSQL por padrão. Para subir também a API na imagem Docker
(útil para quem não tem JDK 25 instalado), use o profile `full`:

```bash
docker compose --profile full up -d --build
```

Nesse modo o Swagger fica em `http://localhost:8080/swagger-ui.html`, as migrations `V1`–`V5` rodam
no primeiro boot e o banco é o mesmo serviço `postgres` do compose. Sem o profile, use
`.\mvnw.cmd spring-boot:run` na seção 6.

> O profile `full` constrói a imagem a partir do [`Dockerfile`](Dockerfile:1). Esse build não foi
> exercitado no ambiente em que o projeto foi desenvolvido (daemon do Docker indisponível). Se ele
> falhar na sua máquina, use o wrapper da seção 6: é o caminho validado pela suíte de testes.

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

> Em clones feitos no Linux/macOS o bit de execução do wrapper pode vir desligado. Se aparecer
> `Permission denied`, rode `chmod +x mvnw` (ou invoque com `sh mvnw spring-boot:run`).

A API sobe em `http://localhost:8080`. O log deve mostrar as migrations do Flyway e
`Started RdnApiApplication`.

## 7. Swagger / OpenAPI

- UI: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/v3/api-docs`

O login é público. Para os demais endpoints, use o botão **Authorize** com o token retornado por
`POST /auth/login` (esquema `bearerAuth`). Os caminhos de documentação são liberados no
`SecurityConfig`; o restante continua autenticado.

O campo `servers` da especificação é relativo (`/`), então o **Try it out** chama sempre a mesma
origem que abriu o Swagger UI: `localhost` em desenvolvimento e a URL pública em deploy. Para fixar
uma URL absoluta, defina `APP_PUBLIC_URL` (ver `.env.example`).

### 7.1 Swagger como evidência (execução local)

A documentação viva é o Swagger servido pela própria aplicação em `/swagger-ui.html` e `/v3/api-docs`.
Essas são as rotas que comprovam o contrato da API; não é necessário publicar nada para avaliar o
projeto. Roteiro para quem for corrigir:

1. Suba o banco com `docker compose up -d` e a API com o wrapper (seção 6).
2. Abra `http://localhost:8080/swagger-ui.html` (JSON em `http://localhost:8080/v3/api-docs`).
3. Autentique em `POST /auth/login` com `customer@rdn.local` / `Customer@123` e use **Authorize** com o
   `accessToken` retornado.
4. Execute o fluxo da seção 9: pedido → pagamento mock → atualização de status.

#### Deploy público (opcional)

O deploy **não faz parte dos entregáveis** do roteiro: pede-se API executável conforme este README,
coleção versionada e rota de Swagger documentada. Os artefatos de publicação
([`Dockerfile`](Dockerfile:1) e [`render.yaml`](render.yaml:1)) continuam versionados como opção para
quem quiser uma URL permanente. Os três caminhos abaixo, em ordem crescente de esforço, são opcionais.

**A. Túnel instantâneo (5 minutos, URL temporária).** Requer a API rodando localmente com um
PostgreSQL acessível (Docker, seção 5).

```bat
:: opção 1 — Cloudflare Tunnel, sem conta e sem instalar nada além do binário
cloudflared tunnel --url http://localhost:8080

:: opção 2 — ngrok, exige conta gratuita e authtoken
ngrok http 8080
```

O terminal imprime uma URL `https://algo.trycloudflare.com` ou `https://algo.ngrok-free.app`.
Acesse `<URL>/swagger-ui.html`. A URL morre quando o processo termina — serve para gravação de
vídeo ou validação pontual, não como link permanente no PDF.

**B. Deploy no Render via Docker (URL permanente, recomendado).** O repositório já traz
[`Dockerfile`](Dockerfile:1) (multi-stage, usuário sem privilégios) e [`render.yaml`](render.yaml:1)
(blueprint do Web Service).

1. Crie um PostgreSQL gratuito no Neon ou Supabase e copie a URL JDBC
   (`jdbc:postgresql://host/db?sslmode=require`), o usuário e a senha.
2. No Render: **New → Blueprint**, aponte para o repositório e aplique o `render.yaml`.
3. Preencha no dashboard as variáveis pedidas: `DB_URL`, `DB_USER`, `DB_PASSWORD`. O
   `JWT_SECRET` é gerado pelo Render; a porta vem de `PORT` (já lida por
   [`application.properties`](src/main/resources/application.properties:5)).
4. O Flyway aplica `V1`–`V5` no primeiro boot. Acompanhe o log até `Started RdnApiApplication`.
5. Swagger público: o blueprint declara o serviço `rdn-api`. Se o subdomínio estiver livre, as rotas
   ficam em `https://<seu-servico>.onrender.com/swagger-ui.html` e `.../v3/api-docs`. O
   `healthCheckPath` usa `/v3/api-docs`, que é público no `SecurityConfig`. Ajuste o environment
   público do Postman se o subdomínio for outro.
6. Confirme o deploy **antes** de citar a URL em qualquer documento:

   ```bat
   curl -s -o NUL -w "status=%{http_code}" https://<seu-servico>.onrender.com/v3/api-docs
   ```

   Esperado: `status=200`. URL que não responde `200` não entra no PDF.

7. Opcionalmente, gere o PDF citando a URL pública com `RDN_PUBLIC_URL`. Sem a variável, o relatório
   usa as evidências locais — que é o modo adotado nesta entrega:

   ```bat
   set "RDN_PUBLIC_URL=https://<seu-servico>.onrender.com"
   python documentacao/tools/abnt_pdf_final.py
   ```

O plano gratuito do Render hiberna após ~15 minutos sem tráfego; a primeira requisição após a
pausa demora ~50 s (cold start). Avise o avaliador ou aqueça a URL antes da correção.

**C. Deploy completo com Docker Compose (VPS própria).** O [`docker-compose.yml`](docker-compose.yml:1)
mantém só o PostgreSQL; adicione um serviço da API construído a partir do `Dockerfile` e exponha a
porta atrás de um proxy reverso (Caddy/Nginx com TLS). É o caminho com mais controle e o único que
não depende de provedor terceirizado.

> Se você publicar, lembre que os usuários de seed (`ADMIN`, `STAFF`, `KITCHEN`, `CUSTOMER`, seção 8)
> passam a existir em um banco público. Use `JWT_SECRET` forte e, se o prazo permitir, uma `V6` de
> seed só para o ambiente publicado.

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
8. Com o token de `ADMIN`, crie um usuário em `POST /usuarios` e autentique com ele em `POST /auth/login`.

## 10. Endpoints

| Método | Rota | Perfis | Finalidade |
|---|---|---|---|
| `POST` | `/auth/login` | público | Login e emissão de JWT |
| `GET` | `/auth/me` | autenticado | Usuário do token |
| `POST` | `/usuarios` | `ADMIN` | Cadastro de usuário com senha hasheada |
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
- cadastro de usuário: `201`, login com a nova senha, `409` de e-mail duplicado e `400` de validação;
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
- `postman/rdn-api.postman_environment.json` (ambiente local, `baseUrl = http://localhost:8080`)
- `postman/rdn-api.postman_environment.public.json` (extra para uma API publicada; ajuste o `baseUrl`)

O environment local já aponta para `http://localhost:8080` e é o caminho validado. O arquivo público é
um extra para quem publicar a API pela seção 7.1. Nada mais muda: os scripts preenchem `token` e IDs
automaticamente.

Importe os dois no Postman, selecione o environment e execute na ordem:

`01-Auth` → `02-Catalogo` → `03-Pedidos` → `04-Pagamento` → `05-Fidelidade` → `06-Promocoes` →
`07-Auditoria` → `08-Erros`.

A coleção possui 29 requisições: os cenários T01–T25 do roteiro mais quatro chamadas de apoio
(`T02b`, `T11a`, `T18a`, `T21a`) que preparam pré-condições de outros testes. Há pelo menos 6 positivos
e 4 negativos. A pasta `08-Erros` concentra T21 e T22 (negativos), T23 (cadastro positivo), T24 e T25
(negativos); os demais cenários permanecem nas pastas de recurso. Os scripts salvam `token`, `pedidoId`,
`pagamentoId`, `pedidoRecusaId` e `novoUsuarioEmail` automaticamente. Para o replay, a chave é fixa;
nos demais cenários, use `{{$guid}}`.

## 13. Segurança e LGPD

- Senhas são armazenadas apenas como hash BCrypt; a senha pura não é persistida.
- O cadastro de usuários é restrito a `ADMIN`; a senha em texto puro não é persistida nem devolvida.
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
- Não há listagem, edição ou desativação de usuários; o escopo entregue é apenas o cadastro por `ADMIN`.
- Os artefatos de container (`Dockerfile`, `render.yaml` e o service `api` do compose) são opcionais e
  ficam fora do caminho de avaliação. O daemon do Docker não estava em execução na máquina do autor,
  então o build da imagem não foi exercitado aqui: a validação registrada é a das migrations
  (`SchemaDdlTest`, `V1`–`V5` em banco limpo) e a dos testes de integração sobre H2. Para validar em
  PostgreSQL real, use o `docker compose up -d` da seção 5.

## 15. Evidências

| Evidência | Onde |
|---|---|
| Repositório público | https://github.com/FxlipxDPaula/rdn-api |
| Swagger UI (rota) | `http://localhost:8080/swagger-ui.html` após executar a seção 6 |
| OpenAPI JSON (rota) | `http://localhost:8080/v3/api-docs` — mesma origem do Swagger UI (`servers` relativo) |
| Execução sem JDK 25 | `docker compose --profile full up -d --build` (profile `full`, seção 5) |
| Coleção Postman | `postman/rdn-api.postman_collection.json` |
| Environment Postman | `postman/rdn-api.postman_environment.json` |

## 16. Estrutura do projeto

```text
Dockerfile        imagem multi-stage usada no deploy (seção 7.1)
render.yaml       blueprint do Web Service no Render
src/main/java/com/rdn/rdn_api/
  api/            controllers, DTOs e tratamento de erro (inclui api/user)
  application/    casos de uso e portas (inclui application/user)
  domain/         entidades e regras de negócio
  infrastructure/ JPA, segurança, pagamento mock, auditoria e OpenAPI (inclui persistence/user)
src/main/resources/
  db/migration/   V1 a V5
  application.properties
src/test/java/    testes unitários e de integração
documentacao/     material academico e de apoio (fora do runtime)
documentacao/docs/      DER, diagramas e relatorios ABNT
documentacao/plans/     planos das etapas
documentacao/tools/     geradores de diagramas e PDF ABNT
postman/          coleção e environment
```
