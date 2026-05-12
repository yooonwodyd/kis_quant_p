INSERT INTO strategies (
    id,
    name,
    status,
    enabled,
    trade_mode,
    initial_budget_amount,
    max_order_amount,
    max_daily_order_amount,
    created_at,
    updated_at
) VALUES (
    1,
    '이동평균 전략',
    'ACTIVE',
    TRUE,
    'SIMULATION',
    30000,
    30000,
    30000,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
