# DER — Modelo de dados do rdn-api

Diagrama derivado de [`V1__schema.sql`](../../src/main/resources/db/migration/V1__schema.sql:1)
e do mapeamento JPA em `src/main/java/com/rdn/rdn_api/infrastructure/persistence/entity/`.
O banco alvo é PostgreSQL; os tipos foram escolhidos para também rodarem em H2
(modo PostgreSQL) nos testes.

```mermaid
erDiagram
    USERS ||--o{ ORDERS : "realiza"
    USERS ||--o| LOYALTY_ACCOUNTS : "possui"
    USERS ||--o{ AUTH_SESSIONS : "mantem"

    BRANCHES ||--o| MENUS : "possui"
    MENUS ||--o{ MENU_ITEMS : "contem"

    BRANCHES ||--o{ INVENTORY_ITEMS : "estoca"
    MENU_ITEMS ||--o{ INVENTORY_ITEMS : "controlado_em"
    INVENTORY_ITEMS ||--o{ INVENTORY_MOVEMENTS : "movimenta"

    BRANCHES ||--o{ ORDERS : "recebe"
    ORDERS ||--o{ ORDER_ITEMS : "contem"
    MENU_ITEMS ||--o{ ORDER_ITEMS : "referenciado_por"
    ORDERS ||--o{ PAYMENTS : "cobra"
    PROMOTIONS ||--o{ ORDERS : "aplicada_em"

    LOYALTY_ACCOUNTS ||--o{ LOYALTY_TRANSACTIONS : "lancamentos"
    ORDERS ||--o{ LOYALTY_TRANSACTIONS : "origina"

    USERS {
        uuid id PK
        varchar full_name
        varchar email UK
        varchar password_hash
        varchar role
        boolean is_active
    }

    BRANCHES {
        uuid id PK
        varchar name
        boolean is_open
        varchar street
        varchar street_number
        varchar complement
        varchar neighborhood
        varchar city
        varchar state
        varchar zip_code
    }

    MENUS {
        uuid id PK
        uuid branch_id FK
        varchar title
        boolean is_active
    }

    MENU_ITEMS {
        uuid id PK
        uuid menu_id FK
        varchar name
        numeric price
        boolean is_available
    }

    INVENTORY_ITEMS {
        uuid id PK
        uuid branch_id FK
        uuid menu_item_id FK
        numeric quantity_on_hand
        numeric minimum_threshold
    }

    INVENTORY_MOVEMENTS {
        uuid id PK
        uuid inventory_item_id FK
        varchar type
        numeric quantity
        timestamp occurred_at
    }

    ORDERS {
        uuid id PK
        uuid customer_id FK
        uuid branch_id FK
        uuid promotion_id FK
        varchar origin_channel
        varchar status
        numeric total_amount
        numeric discount_amount
        timestamp created_at
        varchar idempotency_key UK
    }

    ORDER_ITEMS {
        uuid id PK
        uuid order_id FK
        uuid menu_item_id FK
        integer quantity
        numeric unit_price
        numeric subtotal
    }

    PAYMENTS {
        uuid id PK
        uuid order_id FK
        numeric amount
        varchar method
        varchar status
        varchar gateway_transaction_id
        varchar idempotency_key UK
        timestamp requested_at
    }

    LOYALTY_ACCOUNTS {
        uuid id PK
        uuid customer_id FK
        integer points_balance
        boolean consent_given
        timestamp consent_timestamp
    }

    LOYALTY_TRANSACTIONS {
        uuid id PK
        uuid loyalty_account_id FK
        uuid order_id FK
        integer points
        varchar type
        timestamp created_at
    }

    PROMOTIONS {
        uuid id PK
        varchar title
        varchar discount_type
        numeric discount_value
        timestamp start_date
        timestamp end_date
        numeric min_order_value
        boolean is_active
    }

    AUTH_SESSIONS {
        uuid id PK
        uuid user_id FK
        varchar token_hash UK
        timestamp issued_at
        timestamp expires_at
    }

    AUDIT_LOGS {
        uuid id PK
        timestamp occurred_at
        uuid actor_id
        varchar actor_email
        varchar actor_role
        varchar action
        varchar resource
        uuid resource_id
        varchar details
        varchar ip_address
    }
```

## Restrições relevantes

| Regra | Onde está |
|---|---|
| Um usuário por e-mail | `users.email` UNIQUE |
| Um cardápio por filial | `menus.branch_id` UNIQUE |
| Estoque é sempre por filial + item | `inventory_items (branch_id, menu_item_id)` UNIQUE |
| Saldo de estoque não negativo | `ck_inventory_quantity` |
| Pedido sempre tem canal e status válidos | `ck_orders_channel`, `ck_orders_status` |
| Pedido não tem total negativo | `ck_orders_total` |
| Item de pedido tem quantidade positiva e preço copiado | `ck_order_items_quantity`, `unit_price` |
| Pagamento com valor positivo e estados conhecidos | `ck_payments_amount`, `ck_payments_status` |
| Idempotência de pedido e de pagamento | `uk_orders_idempotency_key`, `uk_payments_idempotency_key` |
| Token guardado como hash | `auth_sessions.token_hash` |
| Pontos nunca negativos | `ck_loyalty_points` |
| Promoção aplicada é registrada no pedido | `orders.promotion_id` FK |
| Extrato de fidelidade tem sinal coerente | `ck_loyalty_tx_sign` |
| Auditoria guarda o ator como snapshot, sem FK | `audit_logs.actor_id` + `actor_email` |

## Decisões de modelagem

- **Endereço é value object embutido**: as colunas ficam em `branches` porque
  `Address` é imutável e sem identidade própria.
- **Pagamento desacoplado do pedido**: `payments.order_id` permite múltiplas
  tentativas (aprovada, recusada, falha) sem alterar o pedido.
- **Item de pedido guarda preço unitário**: alteração futura no cardápio não
  reescreve o histórico financeiro.
- **Promoção aplicada no agregado**: `orders.promotion_id` registra a campanha
  usada e `discount_amount` guarda o valor; o cálculo continua no domínio.
- **Pontos assinados no extrato**: `EARNED` grava positivo e `REDEEMED`/`EXPIRED`
  gravam negativo, permitindo conferir o saldo por soma.
- **Auditoria é trilha à parte**: `audit_logs` não tem FK para `users` e guarda
  apenas snapshot do ator e detalhe curto, sem senha, token ou payload completo.
- **`occurred_at`** substitui `timestamp` como nome de coluna em
  `inventory_movements` para não colidir com palavra reservada.
- **Seed de usuários adiado**: `password_hash` exige BCrypt, dependência que
  entra com a camada de segurança. A migration seguinte cria os usuários com
  hash válido gerado por `BCryptPasswordEncoder`.
- **Campanhas na `V5`**: três promoções fixas, sendo uma expirada de propósito
  para o teste negativo de `PROMOCAO_NAO_APLICAVEL`.
