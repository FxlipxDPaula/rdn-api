-- Etapa 4: o pedido passa a poder nascer aguardando o pagamento mock.
--
-- V1__schema.sql ja esta aplicada e nao pode ser editada (o checksum do Flyway
-- deixaria de bater e a aplicacao nao subiria). Por isso a constraint e
-- removida e recriada aqui com o novo valor.

ALTER TABLE orders DROP CONSTRAINT ck_orders_status;

ALTER TABLE orders ADD CONSTRAINT ck_orders_status CHECK (status IN
    ('AGUARDANDO_PAGAMENTO', 'RECEIVED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED'));
