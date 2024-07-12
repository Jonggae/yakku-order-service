package com.jonggae.yakku.order.entity;

public enum OrderStatus {
    PENDING_ORDER,             // 주문 대기
    ORDER_CREATED,             // 주문이 확정되어 생성됨

    PAYMENT_PENDING,           // 결제 대기 중 (결제 서비스로 요청 보냄)
    PAYMENT_PROCESSING,        // 결제 처리 중
    PAYMENT_FAILED,            // 결제 실패
    PAYMENT_COMPLETE,          // 결제 완료

    PREPARING_FOR_SHIPMENT,    // 배송 준비중
    SHIPPED,                   // 배송 중
    DELIVERED,                 // 배송 완료

    CANCELLED,                 // 주문 취소
    REFUND_REQUESTED,          // 환불 요청
    REFUND_PROCESSING,         // 환불 처리 중
    REFUND_COMPLETED           // 환불 완료
}