-- 승인 시작 시간
ALTER TABLE payment
    ADD COLUMN confirming_at DATETIME(6) NULL;

-- 기존 승인 진행 중 결제의 시간 보정
-- 실제 승인 시작 시간을 알 수 없으므로 생성 시간을 대신 사용
UPDATE payment
SET confirming_at = COALESCE(request_at, CURRENT_TIMESTAMP(6))
WHERE status = 'CONFIRMING'
  AND confirming_at IS NULL;

-- 스케줄러 대상 조회용 인덱스
CREATE INDEX idx_payment_status_confirming_at_id
    ON payment (status, confirming_at, payment_id);