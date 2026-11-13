# Etapa 3 — API de pedidos: criar, consultar e mudar status

## Objetivo

Transformar o que já existe em banco (Etapa 1) e em identidade (Etapa 2) em **endpoints reais de pedido**:
`POST /pedidos`, `GET /pedidos`, `GET /pedidos/{id}` e `PATCH /pedidos/{id}/status`. Ao final, o fluxo
crítico do roteiro começa a existir de ponta a ponta no HTTP: o cliente cria o pedido informando o
`canalPedido`, o estoque da unidade é validado e baixado, e a cozinha avança o status pela máquina de
estados já escrita no domínio.

## Contexto de partida (Etapas 1 e 2 concluídas)

Do relatório da Etapa 1 ([`docs/etapa-1-persistencia-abnt.pdf`](../docs/etapa-1-persistencia-abnt.pdf)):

- 13 tabelas em `V1__schema.sql`, seed de filial/cardápio/estoque em `V2__seed.sql`.
- Domínio puro: `Order.changeStatus` com `ALLOWED_TRANSITIONS`, `Order.calculateTotal`,
  `InventoryItem.deductStock` (retorna `false`, não lança), `Order.cancel(reason)`.
- Índices já criados em `origin_channel`, `status`, `branch_id`, `created_at` — **não há migration nova nesta etapa**.
- `orders.idempotency_key` é `UNIQUE` e nullable; `payments.idempotency_key` é `NOT NULL`.

Do relatório da Etapa 2 ([`docs/etapa-2-seguranca-abnt.pdf`](../docs/etapa-2-seguranca-abnt.pdf)):

- `SecurityConfig` com `anyRequest().authenticated()`: **todo endpoint novo nasce protegido**.
- `AuthenticatedUser(id, fullName, email, role)` é o principal disponível via `@AuthenticationPrincipal`.
- Authorities `ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_KITCHEN`, `ROLE_CUSTOMER` já populadas pelo filtro.
- Erro padrão `ApiError(error, message, details, timestamp, path)` em 400/401/403/404/405/500.
- 20 testes verdes (5 de login, 8 de auth HTTP, 5 de persistência, 1 de DDL, 1 de contexto).

Lacunas verificadas no código atual:

- `OrderMapper` e `OrderItemMapper` têm **apenas** `toDomain` e `copyScalarsToEntity` — **não existe `toEntity`**.
- `OrderJpaRepository` tem filtro por canal e por canal+status, mas **não tem filtro só por status** nem
  consulta paginada com `items` carregado (a listagem estouraria `LazyInitializationException` com
  `open-in-view=false`).
- `InventoryItemJpaRepository.findByBranchIdAndMenuItemId` existe, mas **sem lock** — risco de venda acima do saldo em concorrência.
- `SecurityConfig` **não tem** `@EnableMethodSecurity`.

## Critério de pronto (DoD)

- `POST /pedidos` → `201` com pedido criado, status `RECEIVED`, total calculado pelo domínio e estoque baixado.
- `canalPedido` ausente ou inválido → `400` (`DADOS_INVALIDOS` / `CORPO_INVALIDO`).
- Produto ou unidade inexistente → `404`; estoque insuficiente → `409` com `details` do item e saldo disponível.
- `GET /pedidos?canalPedido=&status=&page=&limit=` → `200` paginado, apenas `ADMIN`/`STAFF`.
- `GET /pedidos/{id}` → `200` com itens; `CUSTOMER` só enxerga o próprio pedido (`403` nos demais).
- `PATCH /pedidos/{id}/status` → `200` quando a transição é válida; `409` quando não é.
- Movimentação `OUTBOUND` registrada em `inventory_movements` para cada baixa de estoque.
- `.\mvnw.cmd test` verde, incluindo no mínimo 10 cenários novos (6 positivos, 4 negativos).
- Relatório ABNT da etapa gerado em `docs/etapa-3-pedidos-abnt.pdf` a partir de `tools/abnt_pdf_etapa3.py`.

## Decisões arquiteturais

1. **Portas na aplicação, adaptadores na infraestrutura** (mesmo padrão de `AuthUserGateway`): a camada
   `application/` declara `OrderGateway`, `CatalogGateway` e `InventoryGateway`; `infrastructure/` implementa
   com os repositórios Spring Data já existentes.
2. **Listagem devolve resumo, não o agregado inteiro.** `GET /pedidos` retorna `pedidoId`, `canalPedido`,
   `status`, `total`, `criadoEm`; os itens aparecem apenas em `GET /pedidos/{id}` (que usa
   `findWithItemsById` com `@EntityGraph`). Isso evita N+1 e paginação com `join fetch` de coleção.
3. **Filtro único no repositório**: um método JPQL com parâmetros opcionais `(:canal is null or ...)`
   em vez de quatro métodos derivados, mantendo uma consulta só.
4. **Autorização declarada em `SecurityConfig`** com `requestMatchers(HttpMethod.X, rota).hasAnyRole(...)` —
   fonte única de verdade, sem `@EnableMethodSecurity`. Alternativa descartada: `@PreAuthorize` (mais legível,
   porém espalha regra em dois arquivos).
5. **Cliente vem do token**: se quem cria é `CUSTOMER`, o `customer_id` é o `sub` do JWT (não confiar no body).
   `STAFF`/`ADMIN` podem informar `clienteId`; ausente = pedido de totem (cliente opcional no schema).
6. **Baixa de estoque dentro da mesma transação do pedido** (`@Transactional` no caso de uso): valida todos
   os itens, só então grava. Falha em qualquer item desfaz tudo.
7. **Lock pessimista na leitura do estoque** (`@Lock(PESSIMISTIC_WRITE)` em um segundo método do repositório),
   solução sem alteração de schema para o risco de duas comandas baixarem o mesmo saldo.
8. **Itens repetidos são agrupados por `produtoId`** antes da validação, somando quantidades — evita que
   duas linhas do mesmo produto passem individualmente e estourem o saldo.

## Pacotes e classes novas

```
application/order/
  OrderGateway              port: save(Order), findWithItems(UUID), search(SalesChannel, OrderStatus, PageRequest)
  CatalogGateway            port: findBranch(UUID), findMenuItem(UUID) -> MenuItemInfo(id, price, available)
  InventoryGateway          port: reserve(UUID branchId, UUID menuItemId, BigDecimal qty) -> ReservationResult
                                  (baixa com lock + registro de InventoryMovement OUTBOUND)
  CreateOrderService        orquestra validação de catálogo, reserva de estoque e persistência
  ChangeOrderStatusService  carrega pedido, aplica changeStatus/cancel e persiste
  QueryOrderService         listagem paginada e busca por id
  CreateOrderCommand        record(canalPedido, unidadeId, clienteId, itens, idempotencyKey)
  CreateOrderItemCommand    record(produtoId, quantidade, observacao)
  OrderSummaryView          record(id, canalPedido, status, total, criadoEm)

application/order/exception/
  ResourceNotFoundException      -> 404
  InsufficientStockException     -> 409 (carrega produtoId + disponivel para details)
  InvalidStatusTransitionException -> 409

api/order/
  OrderController           POST /pedidos, GET /pedidos, GET /pedidos/{id}, PATCH /pedidos/{id}/status
  CreateOrderRequest        @NotNull canalPedido/unidadeId, @NotEmpty @Valid itens
  CreateOrderItemRequest    @NotNull produtoId, @Min(1) quantidade
  ChangeOrderStatusRequest  @NotNull status, motivo opcional
  OrderResponse             pedido completo (pedidoId, canalPedido, status, unidadeId, total, itens, criadoEm)
  OrderSummaryResponse      resumo para listagem
  PageResponse<T>           content, page, size, totalElements, totalPages

infrastructure/persistence/order/
  OrderGatewayAdapter       OrderJpaRepository + OrderMapper
  CatalogGatewayAdapter     BranchJpaRepository + MenuItemJpaRepository
  InventoryGatewayAdapter   InventoryItemJpaRepository (lock) + InventoryMovementJpaRepository
```

Alterações em arquivos existentes:

- [`OrderJpaRepository`](../src/main/java/com/rdn/rdn_api/infrastructure/repository/OrderJpaRepository.java:21):
  adicionar `Page<OrderJpaEntity> search(SalesChannel, OrderStatus, Pageable)` e `@EntityGraph` em
  `findById` quando necessário.
- [`InventoryItemJpaRepository`](../src/main/java/com/rdn/rdn_api/infrastructure/repository/InventoryItemJpaRepository.java:14):
  adicionar `findForUpdateByBranchIdAndMenuItemId` com `@Lock(PESSIMISTIC_WRITE)`.
- [`OrderMapper`](../src/main/java/com/rdn/rdn_api/infrastructure/persistence/mapper/OrderMapper.java:18):
  adicionar `toDomainShallow(OrderJpaEntity)` (mesmo padrão de `BranchMapper.toDomainShallow`) e `toEntity`.
- [`OrderItemMapper`](../src/main/java/com/rdn/rdn_api/infrastructure/persistence/mapper/OrderItemMapper.java:12):
  adicionar `toEntity(OrderItem)`.
- [`GlobalExceptionHandler`](../src/main/java/com/rdn/rdn_api/api/error/GlobalExceptionHandler.java:28):
  quatro novos `@ExceptionHandler`.
- [`SecurityConfig`](../src/main/java/com/rdn/rdn_api/infrastructure/security/SecurityConfig.java:55):
  regras por `HttpMethod` antes do `anyRequest()`.

## Contratos

### `POST /pedidos` — `CUSTOMER`, `STAFF`, `ADMIN`

```json
{
  "canalPedido": "TOTEM",
  "unidadeId": "00000000-0000-0000-0000-0000000000b1",
  "formaPagamento": "MOCK",
  "itens": [
    { "produtoId": "00000000-0000-0000-0000-0000000000i1", "quantidade": 2, "observacao": "sem cebola" }
  ]
}
```

`201`:

```json
{
  "pedidoId": "uuid",
  "canalPedido": "TOTEM",
  "status": "RECEIVED",
  "unidadeId": "uuid",
  "total": 51.80,
  "itens": [
    { "produtoId": "uuid", "quantidade": 2, "precoUnitario": 25.90, "subtotal": 51.80 }
  ],
  "criadoEm": "2026-11-13T12:00:00"
}
```

Status: `201`, `400` (`DADOS_INVALIDOS`, `CORPO_INVALIDO`), `401`, `403`,
`404` (`UNIDADE_NAO_ENCONTRADA`, `PRODUTO_NAO_ENCONTRADO`), `409` (`ESTOQUE_INSUFICIENTE`), `500`.

### `GET /pedidos?canalPedido=TOTEM&status=PREPARING&page=0&limit=10` — `ADMIN`, `STAFF`

`200`:

```json
{
  "content": [
    { "pedidoId": "uuid", "canalPedido": "TOTEM", "status": "PREPARING", "total": 51.80, "criadoEm": "2026-11-13T12:00:00" }
  ],
  "page": 0, "size": 10, "totalElements": 1, "totalPages": 1
}
```

### `GET /pedidos/{id}` — autenticado; `CUSTOMER` apenas o próprio pedido

`200` com o `OrderResponse` completo, `403` se o pedido for de outro cliente, `404` se não existir.

### `PATCH /pedidos/{id}/status` — `KITCHEN`, `STAFF`, `ADMIN`

```json
{ "status": "PREPARING" }
```

Cancelamento envia `{ "status": "CANCELLED", "motivo": "cliente desistiu" }` e usa `Order.cancel(reason)`.
Status: `200`, `400` (status nulo), `403`, `404`, `409` (`TRANSICAO_INVALIDA` ou `ESTOQUE_INSUFICIENTE`).

## Regras de negócio a implementar

1. `canalPedido` é obrigatório (requisito de multicanalidade do roteiro) e aceita APP, TOTEM, BALCAO, PICKUP, WEB.
2. Unidade e produto devem existir; produto com `is_available = false` → `404 PRODUTO_NAO_ENCONTRADO`.
3. Estoque é por unidade: `inventory_items(branch_id, menu_item_id)`; ausência de linha = disponível zero → `409`.
4. Total é sempre calculado por `Order.calculateTotal()` — nunca aceitar `total` do cliente.
5. Pedido novo nasce em `RECEIVED`, **exceto** quando `formaPagamento=MOCK`: nesse caso nasce em
   `AGUARDANDO_PAGAMENTO` (valor que entra na Etapa 4, ver [`plans/04-pagamento-mock.md`](04-pagamento-mock.md)).
   O domínio continua impedindo qualquer status inicial diferente desses dois.
6. Transição inválida → `409`; `IllegalArgumentException` de `changeStatus` (status nulo) → `400`.
7. Cada baixa gera `InventoryMovement` com `type = OUTBOUND`, `quantity`, `timestamp` e `notes` citando o pedido.

## Testes

Unidade (`LoginServiceTest` como precedente, Mockito):

1. `CreateOrderServiceTest` — sucesso: baixa estoque e salva pedido com total correto.
2. Estoque insuficiente → `InsufficientStockException`, sem salvar pedido.
3. Produto inexistente → `ResourceNotFoundException`.
4. Itens duplicados são agrupados antes da validação.
5. `ChangeOrderStatusServiceTest` — `RECEIVED → PREPARING` ok; `RECEIVED → DELIVERED` → `InvalidStatusTransitionException`; `CANCELLED` grava motivo.

Integração (`AuthControllerIntegrationTest` como precedente, `@SpringBootTest` + MockMvc + H2, token obtido via `/auth/login`):

6. `POST /pedidos` sem token → `401`.
7. `POST /pedidos` com `CUSTOMER` e payload válido → `201`.
8. `POST /pedidos` sem `canalPedido` → `400` com `details` do campo.
9. `POST /pedidos` com estoque insuficiente → `409` `ESTOQUE_INSUFICIENTE`.
10. `GET /pedidos?canalPedido=TOTEM` como `STAFF` → `200` paginado e filtrado.
11. `GET /pedidos` como `CUSTOMER` → `403`.
12. `PATCH /pedidos/{id}/status` como `KITCHEN` → `200`; transição inválida → `409`.
13. `GET /pedidos/{id}` de outro cliente → `403`.

## Riscos

| Risco | Mitigação |
|---|---|
| `LazyInitializationException` na listagem (`open-in-view=false`, `items` LAZY) | listagem usa `toDomainShallow` e não toca em `items`; detalhe usa `findWithItemsById` |
| `@EntityGraph` de coleção + `Page` faz paginação em memória no Hibernate | não paginar coleção: resumo na lista, itens só no detalhe |
| Overselling em concorrência (sem `@Version` na entidade de estoque) | `@Lock(PESSIMISTIC_WRITE)` na leitura para baixa |
| `enum` inválido no JSON vira `HttpMessageNotReadableException` | já mapeado para `400 CORPO_INVALIDO`; documentar no contrato |
| Total divergente entre request e domínio | `total` não é aceito no request; sempre `calculateTotal()` |
| `PaymentMethod`/`formaPagamento` do roteiro sem efeito nesta etapa | `formaPagamento=MOCK` já define o status inicial; o fluxo de cobrança é feito na Etapa 4, entregue junto |
| Testes de integração dependem do seed V2 (unidade/produto) | criar filial/menu/itens/estoque no `@BeforeEach`, como o teste de auth faz com usuários |

## Fora de escopo desta etapa (entregue junto ou depois)

- **Etapa 4 — Pagamento mock**: **incluída na mesma entrega**, com plano próprio em
  [`plans/04-pagamento-mock.md`](04-pagamento-mock.md): `POST /pedidos/{id}/pagamentos`, idempotência
  (`payments.idempotency_key NOT NULL`), aprovação/recusa/falha e efeito no status
  (`AGUARDANDO_PAGAMENTO` entra no enum via migration `V4`).
- **Etapa 5 — Fidelidade, promoções e auditoria**: consentimento, pontos, log de ações sensíveis.
- **Etapa 6 — Entrega**: `springdoc-openapi` (não está no `pom.xml`), README, coleção Postman com ≥10 cenários, PostgreSQL real.

## Documentação desta etapa

Seguir o formato dos dois relatórios anteriores, reaproveitando `tools/abnt_pdf.py`:
criar `tools/abnt_pdf_etapa3.py` com as seções Introdução, Arquitetura e escopo da Etapa 3,
Modelagem e implementação, Decisões e trade-offs, Conceitos fundamentais, Erros comuns,
Exercícios, Checklist, Considerações finais e próximo passo, Referências — gerando
`docs/etapa-3-pedidos-abnt.pdf`.
