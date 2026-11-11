# Etapa 1 — Esquema persistente do Fluxo A (Pedido → Pagamento mock → Status)

## Objetivo

Colocar o modelo de domínio existente "de pé" no PostgreSQL: schema versionado por Flyway,
entidades JPA, repositórios e um teste de integração provando persistência real. Ao final, a
aplicação sobe, `./mvnw test` fica verde e o Flyway aplica V1/V2 em banco limpo.

## Critério de pronto (DoD)

- `docker compose up -d` sobe o Postgres; API conecta via variáveis de ambiente.
- `./mvnw spring-boot:run` inicia sem erro de DataSource e com `ddl-auto=validate` passando.
- Flyway aplica `V1__schema.sql` e `V2__seed.sql` do zero.
- Teste de integração persiste `Order` + `OrderItem` + `Payment`, relê do banco e valida totais/FKs.
- Nenhum controller criado nesta etapa (fica para a etapa de API).

## Decisão arquitetural: domínio limpo, JPA separado

O código atual em `src/main/java/com/rdn/rdn_api/domain/` declara explicitamente "POJO sem
anotação de persistência", e `Address` é `final` e imutável (não pode ser `@Entity`).

**Decisão:** manter `domain/` puro e criar entidades JPA em `infrastructure/persistence/entity/`
com mappers em `infrastructure/persistence/mapper/`. Consequência aceita: mais classes, porém
aderente ao requisito de camadas da skill e sem quebrar `Address` (que vira `@Embeddable`).

**Alternativa descartada:** anotar `@Entity` no próprio domínio. Exigiria tornar `Address` mutável
e exporia detalhe técnico no domínio.

## Enums

| Enum | Valores | Origem |
|---|---|---|
| `UserRole` | CUSTOMER, STAFF, KITCHEN, ADMIN | existe |
| `SalesChannel` | APP, TOTEM, BALCAO, PICKUP, WEB | existe |
| `OrderStatus` | RECEIVED, PREPARING, READY, DELIVERED, CANCELLED | existe |
| `MovementType` | INBOUND, OUTBOUND, LOSS, ADJUSTMENT | existe |
| `TransactionType` | EARNED, REDEEMED, EXPIRED | existe |
| `DiscountType` | PERCENTAGE, FIXED_AMOUNT | existe |
| `PaymentStatus` | PENDING, APPROVED, DECLINED, FAILED | **criar** |
| `PaymentMethod` | MOCK, CASH, CARD, PIX | **criar** |

Persistência: `@Enumerated(EnumType.STRING)` + coluna `varchar(20)` + `CHECK` no DDL.

## Tabelas (V1)

Tipos de data: usar `timestamp` **sem** timezone — é o mapeamento padrão do Hibernate para
`LocalDateTime`. Usar `timestamptz` causaria falha no `ddl-auto=validate`.

| Tabela | Colunas principais | Restrições |
|---|---|---|
| `users` | id uuid pk, full_name varchar(120), email varchar(160), password_hash varchar(100), role varchar(20), is_active boolean | unique(email); check role |
| `branches` | id uuid pk, name varchar(120), is_open boolean, street, number, complement, neighborhood, city, state, zip_code | complement nullable; demais endereço not null |
| `menus` | id uuid pk, branch_id uuid, title varchar(120), is_active boolean | unique(branch_id), fk branches |
| `menu_items` | id uuid pk, menu_id uuid, name varchar(120), description varchar(255), price numeric(10,2), is_available boolean | fk menus on delete cascade; check price >= 0 |
| `inventory_items` | id uuid pk, branch_id uuid, menu_item_id uuid, quantity_on_hand numeric(12,3), minimum_threshold numeric(12,3) | unique(branch_id, menu_item_id); fks |
| `inventory_movements` | id uuid pk, inventory_item_id uuid, type varchar(20), quantity numeric(12,3), timestamp, notes varchar(255) | fk inventory_items |
| `orders` | id uuid pk, customer_id uuid null, branch_id uuid, origin_channel varchar(20), status varchar(20), total_amount, discount_amount, created_at, cancellation_reason varchar(255), idempotency_key varchar(64) | fk users/branches; unique(idempotency_key); check origin_channel e status; índices em origin_channel, status, branch_id, created_at |
| `order_items` | id uuid pk, order_id uuid, menu_item_id uuid, quantity int, unit_price numeric(10,2), subtotal numeric(10,2), notes varchar(255) | fk orders on delete cascade; check quantity > 0; check unit_price >= 0 |
| `payments` | id uuid pk, order_id uuid, amount numeric(10,2), method varchar(20), status varchar(20), gateway_transaction_id varchar(80), gateway_raw_response text, idempotency_key varchar(80), requested_at, processed_at | fk orders; unique(idempotency_key); check amount > 0 |
| `loyalty_accounts` | id uuid pk, customer_id uuid, points_balance int, consent_given boolean, consent_timestamp | unique(customer_id); check points_balance >= 0 |
| `loyalty_transactions` | id uuid pk, loyalty_account_id uuid, order_id uuid null, points int, type varchar(20), created_at | fks; check type |
| `promotions` | id uuid pk, title, description, discount_type varchar(20), discount_value numeric(10,2), start_date, end_date, min_order_value numeric(10,2), is_active boolean | check discount_type |
| `auth_sessions` | id uuid pk (surrogate), user_id uuid, token_hash varchar(128), issued_at, expires_at | unique(token_hash); fk users |

Nota: `AuthSession` no domínio não tem `id` nem hash — a entidade JPA adiciona o `id` surrogate e
guarda `token_hash` em vez do token puro (LGPD). O mapper faz a tradução.

## Completar o domínio (antes do mapeamento)

1. Criar `domain/PaymentStatus.java` e `domain/PaymentMethod.java`.
2. Criar `domain/Payment.java`: `id`, `order`, `amount`, `method`, `status`,
   `gatewayTransactionId`, `gatewayRawResponse`, `idempotencyKey`, `requestedAt`, `processedAt`;
   métodos `markApproved(String txId, String raw)`, `markDeclined(String raw)`, `isApproved()`.
3. `Order`: adicionar `idempotencyKey`; adicionar `changeStatus(OrderStatus)` que valida transição
   (RECEIVED → PREPARING → READY → DELIVERED, e qualquer estado não final → CANCELLED).
4. Corrigir regras que hoje impedem os cenários de teste da skill:
   - `InventoryItem.deductStock` deve recusar saldo negativo (habilita `409` de estoque insuficiente);
   - `OrderItem`: quantidade deve ser > 0;
   - `Order.calculateTotal`: total não pode ficar negativo (desconto limitado à soma dos itens).

## Seed (V2)

- 1 filial "RDN Centro" (aberta) + endereço completo; 1 cardápio ativo.
- 3 itens: X-Burger 25.90, Batata Frita 12.00, Refrigerante 7.50.
- Estoque 100 de cada item + movimento INBOUND correspondente.
- Sem pedidos e sem pagamentos no seed (os testes criam).
- **Usuários foram adiados para a migration seguinte:** `password_hash` exige BCrypt,
  dependência que só entra na etapa de segurança. Criar hash inválido agora quebraria o login
  depois. Nenhuma ferramenta local (Docker, psql, python-bcrypt) está disponível para gerá-lo.

## Configuração

`application.properties`:

- `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/rdn}`
- `spring.datasource.username=${DB_USER:rdn}`, `spring.datasource.password=${DB_PASSWORD:rdn}`
- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.jpa.open-in-view=false`
- `spring.flyway.enabled=true`, `spring.flyway.locations=classpath:db/migration`

Arquivos de apoio: `.env.example` e `docker-compose.yml` (postgres:16 com volume e healthcheck).

## Teste de integração

**Decisão tomada durante a execução:** Testcontainers foi descartado porque Docker não existe
nesta máquina. O teste roda em **H2 com `MODE=PostgreSQL`**, com Flyway desativado e schema
gerado pelo Hibernate; a validade do DDL real é coberta por um segundo teste que executa
`V1__schema.sql` e `V2__seed.sql` via JDBC puro num banco vazio.

Em `src/test/java/com/rdn/rdn_api/infrastructure/repository/OrderPersistenceIntegrationTest.java`:

1. Persiste `Order` com 2 itens e 1 `Payment`, faz flush/clear e relê: totais, FKs e status.
2. `OrderJpaRepository.findByOriginChannelAndStatus(TOTEM, PREPARING, pageable)` retorna o pedido.
3. `users.email` duplicado lança `DataIntegrityViolationException`.
4. `orders.idempotency_key` duplicado é rejeitado.
5. Estoque por filial + item é recuperado por `findByBranchIdAndMenuItemId`.

Em `src/test/java/com/rdn/rdn_api/infrastructure/persistence/SchemaDdlTest.java`:

6. Executa V1 + V2 em banco limpo e confere as contagens do seed.

## Riscos

| Risco | Mitigação |
|---|---|
| Nomes de artefatos mudaram no Spring Boot 4 (starters de teste, Testcontainers) | Validar no `pom.xml` antes de escrever o teste |
| `ddl-auto=validate` divergindo do DDL | Usar `timestamp` (sem tz), `numeric(10,2)`, `varchar` com tamanho explícito |
| Docker não disponível na máquina | Confirmado na execução: adotado H2 em modo PostgreSQL para testes + `SchemaDdlTest` validando V1/V2 |
| Postgres real nunca exercitado localmente | `docker-compose.yml` entregue; rodar `mvnw spring-boot:run` com o compose para validar `ddl-auto=validate` antes da entrega |
| Flyway não suportar o Postgres alvo | `flyway-database-postgresql` já está no `pom.xml` |
