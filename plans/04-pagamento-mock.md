# Etapa 4 — Pagamento mock e efeito no status do pedido

> Entrega combinada com a Etapa 3 (API de pedidos). Este documento cobre o que a Etapa 3
> deliberadamente deixou de fora: a solicitação de pagamento ao provedor simulado, a
> idempotência e a atualização do status do pedido como consequência do resultado.

## Objetivo

Fechar o fluxo crítico do roteiro (pedido → pagamento mock → status) com um endpoint que envia a
cobrança a um provedor **simulado**, registra a tentativa em `payments` e reflete o resultado na
máquina de estados do pedido. Aprovar leva o pedido para a fila da cozinha; recusar cancela o pedido;
falha técnica mantém o pedido aguardando uma nova tentativa.

## Contexto de partida

O domínio de pagamento já está pronto desde a Etapa 1 e **não precisa ser reescrito**:

- [`Payment`](../src/main/java/com/rdn/rdn_api/domain/Payment.java:54) com construtor validado
  (`amount > 0`, `idempotencyKey` obrigatória), `markApproved`, `markDeclined`, `markFailed`,
  `isApproved` e `isSettled`; `requirePending()` impede alterar tentativa já finalizada.
- [`PaymentStatus`](../src/main/java/com/rdn/rdn_api/domain/PaymentStatus.java:10):
  `PENDING`, `APPROVED`, `DECLINED`, `FAILED`.
- [`PaymentJpaRepository`](../src/main/java/com/rdn/rdn_api/infrastructure/repository/PaymentJpaRepository.java:14)
  com `findByIdempotencyKey`, `existsByIdempotencyKey` e `findByOrderIdOrderByRequestedAtDesc`.
- `payments.idempotency_key` é `NOT NULL` + `UNIQUE`; `processed_at` é nullable de propósito.
- Pendências reais: `PaymentMapper` **não tem `toEntity`**, e o enum de status do pedido **não tem**
  estado de espera de pagamento.

## Critério de pronto (DoD)

- `POST /pedidos/{id}/pagamentos` com `Idempotency-Key` responde `201` e registra a tentativa.
- Simulação `APROVADO` → `payments.status = APPROVED` e `orders.status` passa de
  `AGUARDANDO_PAGAMENTO` para `RECEIVED`.
- Simulação `RECUSADO` → `payments.status = DECLINED` e pedido cancelado com motivo registrado.
- Simulação `FALHA` → `payments.status = FAILED` e pedido permanece `AGUARDANDO_PAGAMENTO` (retentável).
- Repetir a mesma `Idempotency-Key` devolve `200` com a tentativa já existente, sem nova linha em `payments`.
- Mesma chave usada em outro pedido → `409 CHAVE_IDEMPOTENCIA_CONFLITANTE`.
- `GET /pedidos/{id}/pagamentos` devolve o histórico em ordem decrescente de solicitação.
- Header `Idempotency-Key` ausente → `400 CABECALHO_OBRIGATORIO`.
- Suíte verde com os cenários de pagamento aprovado, recusado e falha.

## Decisões arquiteturais

1. **Um único status novo no domínio**: `AGUARDANDO_PAGAMENTO`. Aprovar **não** cria status
   `PAGO` — `RECEIVED` passa a significar "pagamento confirmado, na fila". Alternativa descartada:
   dois status novos (`AGUARDANDO_PAGAMENTO` + `PAGO`), que aumentariam a tabela de transições sem
   ganho de informação.
2. **Migration `V4` só para a constraint**: `V1` já está aplicada e não pode ser editada (quebraria o
   checksum do Flyway). A `V4` remove e recria `ck_orders_status` com os seis valores.
3. **Regra de simulação explícita** (`simular` no corpo): o mock é determinístico e testável, sem
   depender de valor ou de aleatoriedade. Em produção esse campo simplesmente não existiria — é a
   única concessão ao fato de o provedor ser simulado.
4. **`Idempotency-Key` como header obrigatório**: espelha o `NOT NULL` da coluna e é o padrão de
   mercado para POSTs de cobrança. Replay devolve `200`, criação devolve `201`.
5. **Valor nunca vem do cliente**: `payment.amount = order.getTotalAmount()`. O request não aceita
   `valor`, eliminando a classe de fraude "pago R$ 1 por um pedido de R$ 51,80".
6. **Gateway como porta** (`PaymentGateway`) com adaptador `MockPaymentGatewayAdapter` em
   `infrastructure/payment`: trocar o mock por um provedor real não toca na aplicação.
7. **Falha técnica não cancela o pedido**: `FAILED` é condição transitória; o cliente repete com nova
   `Idempotency-Key` (ou a mesma, se a resposta anterior não chegou).

## Pacotes e classes novas

```
application/payment/
  PaymentGateway            port: ChargeResult charge(ChargeRequest)
  ChargeRequest             record(orderId, amount, method, idempotencyKey, SimulationMode simular)
  ChargeResult              record(approved, failed, transactionId, rawResponse, message)
  SimulationMode            enum APROVADO, RECUSADO, FALHA (mock)
  RequestPaymentService     @Transactional: carrega pedido, trata idempotência, chama gateway, atualiza pedido
  PaymentView               record(id, pedidoId, status, valor, formaPagamento, transacaoGateway, solicitadoEm, processadoEm)

application/payment/exception/
  OrderNotAwaitingPaymentException    -> 409 PEDIDO_NAO_AGUARDA_PAGAMENTO
  IdempotencyConflictException        -> 409 CHAVE_IDEMPOTENCIA_CONFLITANTE
  PaymentFailedException              -> 201 com status FAILED (não é erro HTTP)

infrastructure/payment/
  MockPaymentGatewayAdapter implements PaymentGateway
  PaymentGatewayAdapter     PaymentJpaRepository + PaymentMapper (persistência da tentativa)

api/payment/
  PaymentController         POST /pedidos/{id}/pagamentos, GET /pedidos/{id}/pagamentos
  RequestPaymentRequest     formaPagamento @NotNull, simular opcional
  PaymentResponse           contrato de resposta
```

Alterações em arquivos existentes:

- [`OrderStatus`](../src/main/java/com/rdn/rdn_api/domain/OrderStatus.java:1): novo valor `AGUARDANDO_PAGAMENTO`.
- [`Order`](../src/main/java/com/rdn/rdn_api/domain/Order.java:52): transição
  `AGUARDANDO_PAGAMENTO → {RECEIVED, CANCELLED}`; métodos `awaitPayment()` (só a partir de status nulo)
  e `confirmPayment()` (delega para `changeStatus(RECEIVED)`).
- [`PaymentMapper`](../src/main/java/com/rdn/rdn_api/infrastructure/persistence/mapper/PaymentMapper.java:34):
  adicionar `toEntity(Payment)`.
- [`GlobalExceptionHandler`](../src/main/java/com/rdn/rdn_api/api/error/GlobalExceptionHandler.java:28):
  handlers para `OrderNotAwaitingPaymentException` e `IdempotencyConflictException`.
- [`SecurityConfig`](../src/main/java/com/rdn/rdn_api/infrastructure/security/SecurityConfig.java:55):
  `POST /pedidos/*/pagamentos` para `CUSTOMER`, `STAFF`, `ADMIN`; `GET` para autenticados.
- `src/main/resources/db/migration/V4__add_awaiting_payment_status.sql` (novo).
- [`SchemaDdlTest`](../src/test/java/com/rdn/rdn_api/infrastructure/persistence/SchemaDdlTest.java:1):
  passar a validar a aplicação de V1→V4.

```sql
-- V4__add_awaiting_payment_status.sql
ALTER TABLE orders DROP CONSTRAINT ck_orders_status;
ALTER TABLE orders ADD CONSTRAINT ck_orders_status CHECK (status IN
    ('AGUARDANDO_PAGAMENTO', 'RECEIVED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED'));
```

## Contratos

### `POST /pedidos/{id}/pagamentos` — `CUSTOMER`, `STAFF`, `ADMIN`

Headers: `Authorization: Bearer <token>` e `Idempotency-Key: <uuid ou string estável>`

```json
{ "formaPagamento": "MOCK", "simular": "APROVADO" }
```

`201`:

```json
{
  "pagamentoId": "uuid",
  "pedidoId": "uuid",
  "status": "APPROVED",
  "valor": 51.80,
  "formaPagamento": "MOCK",
  "transacaoGateway": "MOCK-3f2a...",
  "pedidoStatus": "RECEIVED",
  "solicitadoEm": "2026-11-13T12:00:00",
  "processadoEm": "2026-11-13T12:00:01"
}
```

Recusa (`simular: "RECUSADO"`) responde `201` com `status = DECLINED` e `pedidoStatus = CANCELLED`.
Falha (`simular: "FALHA"`) responde `201` com `status = FAILED` e `pedidoStatus = AGUARDANDO_PAGAMENTO`.

Replay da mesma `Idempotency-Key` → `200` com o corpo da tentativa original.

Status: `200` (replay), `201`, `400` (`CABECALHO_OBRIGATORIO`, `DADOS_INVALIDOS`), `401`, `403`,
`404` (`PEDIDO_NAO_ENCONTRADO`), `409` (`PEDIDO_NAO_AGUARDA_PAGAMENTO`, `CHAVE_IDEMPOTENCIA_CONFLITANTE`), `500`.

### `GET /pedidos/{id}/pagamentos`

`200` com lista (ordem decrescente por `requestedAt`); `CUSTOMER` só acessa o próprio pedido (`403`);
`404` se o pedido não existir.

## Regras de negócio a implementar

1. Só pedido em `AGUARDANDO_PAGAMENTO` aceita nova cobrança; qualquer outro status → `409`.
2. `amount` é sempre `order.getTotalAmount()`; pedido sem itens (total zero) → `409`.
3. Idempotência por chave: mesma chave + mesmo pedido = replay; mesma chave + outro pedido = `409`.
4. Aprovado → `order.confirmPayment()`; recusado → `order.cancel("pagamento recusado pelo provedor")`;
   falha → nenhuma transição de status.
5. Toda tentativa grava `gateway_transaction_id` (quando houver) e `gateway_raw_response` (auditoria).
6. Um pedido pode acumular tentativas recusadas/falhas e uma aprovada — o histórico é o registro.

## Testes

Unidade:

1. `MockPaymentGatewayAdapterTest` — `APROVADO`, `RECUSADO` e `FALHA` produzem o resultado esperado e um `transactionId` no formato `MOCK-*`.
2. `RequestPaymentServiceTest` — aprovado muda status para `RECEIVED`; recusado cancela com motivo; replay devolve a mesma tentativa sem chamar o gateway; chave de outro pedido → `IdempotencyConflictException`; pedido em `PREPARING` → `OrderNotAwaitingPaymentException`.

Integração (MockMvc + H2, token via `/auth/login`):

3. Pagamento aprovado → `201`, `pedidoStatus = RECEIVED` e `GET /pedidos/{id}` confirma o novo status.
4. Pagamento recusado → `201` com `DECLINED` e pedido `CANCELLED`.
5. Replay da mesma chave → `200`, e `GET /pedidos/{id}/pagamentos` tem apenas 1 registro.
6. Sem `Idempotency-Key` → `400`.
7. Pedido já cancelado → `409 PEDIDO_NAO_AGUARDA_PAGAMENTO`.
8. `CUSTOMER` pagando pedido de outro cliente → `403`.

## Riscos

| Risco | Mitigação |
|---|---|
| Editar `V1` em vez de criar `V4` quebra checksum e a aplicação não sobe | `V4` só faz `DROP`/`ADD CONSTRAINT`; `V1` permanece intocada |
| `SchemaDdlTest` falha ao não conhecer o novo status | atualizar o teste para V1→V4 e validar o valor no `CHECK` |
| Duas requisições simultâneas com a mesma chave passam pelo `existsByIdempotencyKey` | capturar `DataIntegrityViolationException` da constraint `uk_payments_idempotency_key` e reler a tentativa existente |
| Mock "decidir" pelo cliente pode parecer fraude em produção | campo `simular` documentado como exclusivo do mock; validação por `PaymentMethod.MOCK` |
| Retry após `FAILED` gerar cobrança dupla | nova tentativa usa **nova** chave; a mesma chave sempre devolve o registro anterior |
| Pedido cancelado com pagamento aprovado (corrida) | `requirePending()` + transação única: aprovação e mudança de status são atômicas |

## Fora de escopo (Etapas 5 e 6)

- **Etapa 5 — Fidelidade, promoções e auditoria**: consentimento em `loyalty_accounts`, pontos por
  pedido aprovado, `Promotion` aplicada ao pedido, log de ações sensíveis.
- **Etapa 6 — Entrega**: `springdoc-openapi` (ainda ausente do [`pom.xml`](../pom.xml:32)), README
  reprodutível, coleção Postman com ≥10 cenários, execução em PostgreSQL real via Docker.

## Documentação desta etapa

Criar `tools/abnt_pdf_etapa4.py` reaproveitando a estrutura de `tools/abnt_pdf.py` e gerar
`docs/etapa-4-pagamento-abnt.pdf` com as seções: Introdução, Arquitetura e escopo da Etapa 4,
Modelagem e implementação (status novo, gateway mock, idempotência), Decisões e trade-offs,
Conceitos fundamentais, Erros comuns, Exercícios, Checklist, Considerações finais e próximo passo,
Referências. Registrar explicitamente que o provedor é simulado e que `simular` é um campo de teste.
