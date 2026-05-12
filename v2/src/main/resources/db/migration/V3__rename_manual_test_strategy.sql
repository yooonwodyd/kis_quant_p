UPDATE strategies
SET name = '수동 주문 테스트 전략',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = 1
  AND name = '이동평균 전략';
