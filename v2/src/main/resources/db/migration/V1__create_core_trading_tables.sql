CREATE TABLE strategies (
    id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL,
    trade_mode VARCHAR(30) NOT NULL,
    initial_budget_amount BIGINT NOT NULL,
    max_order_amount BIGINT NOT NULL,
    max_daily_order_amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE orders (
    id BIGINT NOT NULL,
    strategy_id BIGINT NOT NULL,
    symbol VARCHAR(6) NOT NULL,
    side VARCHAR(10) NOT NULL,
    trade_mode VARCHAR(30) NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    quantity BIGINT NOT NULL,
    order_price BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    kis_order_no VARCHAR(30) NULL,
    kis_order_org_no VARCHAR(30) NULL,
    requested_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6) NULL,
    last_synced_at DATETIME(6) NULL,
    reject_code VARCHAR(100) NULL,
    reject_message VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_orders_strategy_symbol_status ON orders(strategy_id, symbol, status);
CREATE INDEX idx_orders_status_last_synced_at ON orders(status, last_synced_at);
CREATE INDEX idx_orders_kis_order_no ON orders(kis_order_no);

CREATE TABLE executions (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    strategy_id BIGINT NOT NULL,
    symbol VARCHAR(6) NOT NULL,
    side VARCHAR(10) NOT NULL,
    execution_source VARCHAR(30) NOT NULL,
    executed_quantity BIGINT NOT NULL,
    executed_price BIGINT NOT NULL,
    executed_amount BIGINT NOT NULL,
    fee BIGINT NOT NULL DEFAULT 0,
    tax BIGINT NOT NULL DEFAULT 0,
    executed_at DATETIME(6) NOT NULL,
    dedup_key VARCHAR(200) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_executions_dedup_key UNIQUE (dedup_key)
) ENGINE=InnoDB;

CREATE TABLE strategy_positions (
    strategy_id BIGINT NOT NULL,
    symbol VARCHAR(6) NOT NULL,
    quantity BIGINT NOT NULL,
    avg_price BIGINT NOT NULL,
    realized_pnl BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (strategy_id, symbol)
) ENGINE=InnoDB;

CREATE TABLE strategy_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    strategy_id BIGINT NOT NULL,
    level VARCHAR(10) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    payload_json JSON NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_strategy_logs_strategy_created_at ON strategy_logs(strategy_id, created_at);
