-- Dados iniciais para demonstrar o fluxo de pedido em ambiente local.
-- Usuarios nao sao criados aqui: a coluna password_hash exige BCrypt, que so
-- passa a existir no classpath quando a camada de seguranca for adicionada.
-- O seed de usuarios sera criado na migration seguinte, com hash valido gerado
-- por BCryptPasswordEncoder.

INSERT INTO branches (id, name, is_open, street, street_number, complement, neighborhood, city, state, zip_code)
VALUES ('00000000-0000-0000-0000-000000000001', 'RDN Centro', TRUE,
        'Rua das Laranjeiras', '100', 'Loja 1', 'Centro', 'Sao Paulo', 'SP', '01000-000');

INSERT INTO menus (id, branch_id, title, is_active)
VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001',
        'Cardapio Centro', TRUE);

INSERT INTO menu_items (id, menu_id, name, description, price, is_available)
VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002',
        'X-Burger', 'Pao, carne, queijo e molho da casa', 25.90, TRUE),
       ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000002',
        'Batata Frita', 'Porcao individual', 12.00, TRUE),
       ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000002',
        'Refrigerante', 'Lata 350ml', 7.50, TRUE);

INSERT INTO inventory_items (id, branch_id, menu_item_id, quantity_on_hand, minimum_threshold)
VALUES ('00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000003', 100.000, 5.000),
       ('00000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000004', 100.000, 5.000),
       ('00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000005', 100.000, 5.000);

INSERT INTO inventory_movements (id, inventory_item_id, type, quantity, occurred_at, notes)
VALUES ('00000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000006',
        'INBOUND', 100.000, CURRENT_TIMESTAMP, 'Carga inicial de estoque'),
       ('00000000-0000-0000-0000-00000000000A', '00000000-0000-0000-0000-000000000007',
        'INBOUND', 100.000, CURRENT_TIMESTAMP, 'Carga inicial de estoque'),
       ('00000000-0000-0000-0000-00000000000B', '00000000-0000-0000-0000-000000000008',
        'INBOUND', 100.000, CURRENT_TIMESTAMP, 'Carga inicial de estoque');
