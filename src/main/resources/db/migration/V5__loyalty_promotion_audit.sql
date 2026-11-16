-- Etapa 5: auditoria de acoes sensiveis, promocao aplicada ao pedido e
-- consistencia do extrato de fidelidade.
--
-- V1 a V4 ja estao aplicadas e nao podem ser editadas (checksum do Flyway).
-- Tipos escolhidos para rodar em PostgreSQL e em H2 no modo PostgreSQL:
-- UUID, VARCHAR, TIMESTAMP, NUMERIC e CHECK constraints.

CREATE TABLE audit_logs (
    id UUID NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    actor_id UUID,
    actor_email VARCHAR(160),
    actor_role VARCHAR(20),
    action VARCHAR(60) NOT NULL,
    resource VARCHAR(40) NOT NULL,
    resource_id UUID,
    details VARCHAR(1000),
    ip_address VARCHAR(45),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT ck_audit_logs_actor_role CHECK (
        actor_role IS NULL OR actor_role IN ('CUSTOMER', 'STAFF', 'KITCHEN', 'ADMIN'))
);

CREATE INDEX idx_audit_logs_occurred_at ON audit_logs (occurred_at);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_resource ON audit_logs (resource, resource_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_email);

-- A promocao aplicada ao pedido fica registrada para rastreabilidade do desconto.
ALTER TABLE orders ADD COLUMN promotion_id UUID;
ALTER TABLE orders ADD CONSTRAINT fk_orders_promotion
    FOREIGN KEY (promotion_id) REFERENCES promotions (id);
CREATE INDEX idx_orders_promotion ON orders (promotion_id);

-- Extrato de fidelidade usa pontos assinados: EARNED positivo, REDEEMED/EXPIRED negativo.
ALTER TABLE loyalty_transactions ADD CONSTRAINT ck_loyalty_tx_sign CHECK (
    (type = 'EARNED' AND points > 0) OR
    (type IN ('REDEEMED', 'EXPIRED') AND points < 0));
CREATE INDEX idx_loyalty_tx_order ON loyalty_transactions (order_id);

-- Campanhas de demonstracao. Datas fixas para o teste nao depender do relogio.
-- A terceira campanha esta ativa de proposito, mas com janela encerrada, para
-- exercitar o 409 PROMOCAO_NAO_APLICAVEL.
INSERT INTO promotions (id, title, description, discount_type, discount_value,
                        start_date, end_date, min_order_value, is_active)
VALUES ('00000000-0000-0000-0000-0000000000c1',
        'Semana do Burger', '10% em pedidos acima de R$ 30,00',
        'PERCENTAGE', 10.00, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 30.00, TRUE),
       ('00000000-0000-0000-0000-0000000000c2',
        'Combo Fidelidade', 'R$ 5,00 de desconto acima de R$ 40,00',
        'FIXED_AMOUNT', 5.00, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 40.00, TRUE),
       ('00000000-0000-0000-0000-0000000000c3',
        'Campanha Encerrada', 'Campanha expirada usada em teste negativo',
        'PERCENTAGE', 15.00, '2025-01-01 00:00:00', '2025-12-31 23:59:59', NULL, TRUE);
