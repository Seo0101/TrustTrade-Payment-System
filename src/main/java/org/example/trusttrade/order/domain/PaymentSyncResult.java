package org.example.trusttrade.order.domain;


public enum PaymentSyncResult {
    SUCCESS,   // 결제 성공 확정
    FAILED,    // 결제 실패 확정
    PENDING,    // 아직 상태 확정 불가
    NEEDS_SYNC //동기화 필요
}
