UPDATE strategies
SET name = '수동 모의 투자 전략',
    trade_mode = 'SIMULATION',
    initial_budget_amount = 30000,
    max_order_amount = 30000,
    max_daily_order_amount = 30000,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = 1;

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
)
SELECT
    2,
    '수동 실전 투자 전략',
    'ACTIVE',
    TRUE,
    'LIVE',
    30000,
    30000,
    30000,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM strategies
    WHERE id = 2
);
