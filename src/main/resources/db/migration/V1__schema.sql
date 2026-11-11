-- Schema inicial do rdn-api.
-- Tipos escolhidos para serem portaveis entre PostgreSQL (producao) e H2 em
-- modo PostgreSQL (testes automatizados): UUID, VARCHAR, BOOLEAN, NUMERIC,
-- TIMESTAMP e CHECK constraints.

CREATE TABLE users (
    id UUID NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'STAFF', 'KITCHEN', 'ADMIN'))
);

CREATE TABLE branches (
    id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    is_open BOOLEAN NOT NULL,
    street VARCHAR(120) NOT NULL,
    street_number VARCHAR(20) NOT NULL,
    complement VARCHAR(120),
    neighborhood VARCHAR(80) NOT NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    CONSTRAINT pk_branches PRIMARY KEY (id)
);

CREATE TABLE menus (
    id UUID NOT NULL,
    branch_id UUID NOT NULL,
    title VARCHAR(120),
    is_active BOOLEAN NOT NULL,
    CONSTRAINT pk_menus PRIMARY KEY (id),
    CONSTRAINT uk_menus_branch UNIQUE (branch_id),
    CONSTRAINT fk_menus_branch FOREIGN KEY (branch_id) REFERENCES branches (id)
);

CREATE TABLE menu_items (
    id UUID NOT NULL,
    menu_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    price NUMERIC(10, 2) NOT NULL,
    is_available BOOLEAN NOT NULL,
    CONSTRAINT pk_menu_items PRIMARY KEY (id),
    CONSTRAINT fk_menu_items_menu FOREIGN KEY (menu_id) REFERENCES menus (id) ON DELETE CASCADE,
    CONSTRAINT ck_menu_items_price CHECK (price >= 0)
);

CREATE INDEX idx_menu_items_menu ON menu_items (menu_id);

CREATE TABLE inventory_items (
    id UUID NOT NULL,
    branch_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    quantity_on_hand NUMERIC(12, 3) NOT NULL,
    minimum_threshold NUMERIC(12, 3) NOT NULL,
    CONSTRAINT pk_inventory_items PRIMARY KEY (id),
    CONSTRAINT uk_inventory_branch_item UNIQUE (branch_id, menu_item_id),
    CONSTRAINT fk_inventory_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_inventory_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT ck_inventory_quantity CHECK (quantity_on_hand >= 0),
    CONSTRAINT ck_inventory_threshold CHECK (minimum_threshold >= 0)
);

CREATE INDEX idx_inventory_items_branch ON inventory_items (branch_id);

CREATE TABLE inventory_movements (
    id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity NUMERIC(12, 3) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    notes VARCHAR(255),
    CONSTRAINT pk_inventory_movements PRIMARY KEY (id),
    CONSTRAINT fk_movements_inventory_item FOREIGN KEY (inventory_item_id)
        REFERENCES inventory_items (id) ON DELETE CASCADE,
    CONSTRAINT ck_movements_type CHECK (type IN ('INBOUND', 'OUTBOUND', 'LOSS', 'ADJUSTMENT'))
);

CREATE INDEX idx_movements_inventory_item ON inventory_movements (inventory_item_id);

CREATE TABLE orders (
    id UUID NOT NULL,
    customer_id UUID,
    branch_id UUID NOT NULL,
    origin_channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    discount_amount NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    cancellation_reason VARCHAR(255),
    idempotency_key VARCHAR(64),
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_orders_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT ck_orders_channel CHECK (origin_channel IN ('APP', 'TOTEM', 'BALCAO', 'PICKUP', 'WEB')),
    CONSTRAINT ck_orders_status CHECK (status IN ('RECEIVED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_orders_total CHECK (total_amount >= 0),
    CONSTRAINT ck_orders_discount CHECK (discount_amount >= 0)
);

CREATE INDEX idx_orders_origin_channel ON orders (origin_channel);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_branch ON orders (branch_id);
CREATE INDEX idx_orders_created_at ON orders (created_at);

CREATE TABLE order_items (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    notes VARCHAR(255),
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_subtotal CHECK (subtotal >= 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE payments (
    id UUID NOT NULL,
    order_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway_transaction_id VARCHAR(80),
    gateway_raw_response VARCHAR(1000),
    idempotency_key VARCHAR(80) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_payments_method CHECK (method IN ('MOCK', 'CASH', 'CARD', 'PIX')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'APPROVED', 'DECLINED', 'FAILED')),
    CONSTRAINT ck_payments_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_order ON payments (order_id);

CREATE TABLE loyalty_accounts (
    id UUID NOT NULL,
    customer_id UUID NOT NULL,
    points_balance INTEGER NOT NULL,
    consent_given BOOLEAN NOT NULL,
    consent_timestamp TIMESTAMP,
    CONSTRAINT pk_loyalty_accounts PRIMARY KEY (id),
    CONSTRAINT uk_loyalty_accounts_customer UNIQUE (customer_id),
    CONSTRAINT fk_loyalty_accounts_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT ck_loyalty_points CHECK (points_balance >= 0)
);

CREATE TABLE loyalty_transactions (
    id UUID NOT NULL,
    loyalty_account_id UUID NOT NULL,
    order_id UUID,
    points INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_loyalty_transactions PRIMARY KEY (id),
    CONSTRAINT fk_loyalty_tx_account FOREIGN KEY (loyalty_account_id)
        REFERENCES loyalty_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_loyalty_tx_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_loyalty_tx_type CHECK (type IN ('EARNED', 'REDEEMED', 'EXPIRED'))
);

CREATE INDEX idx_loyalty_tx_account ON loyalty_transactions (loyalty_account_id);

CREATE TABLE promotions (
    id UUID NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL,
    discount_value NUMERIC(10, 2) NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    min_order_value NUMERIC(10, 2),
    is_active BOOLEAN NOT NULL,
    CONSTRAINT pk_promotions PRIMARY KEY (id),
    CONSTRAINT ck_promotions_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT ck_promotions_discount_value CHECK (discount_value >= 0)
);

CREATE TABLE auth_sessions (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_auth_sessions PRIMARY KEY (id),
    CONSTRAINT uk_auth_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_sessions_user ON auth_sessions (user_id);
