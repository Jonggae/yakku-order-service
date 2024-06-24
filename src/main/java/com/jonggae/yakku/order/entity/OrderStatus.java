package com.jonggae.yakku.order.entity;

public enum OrderStatus {
    PENDING_ORDER, // 주문 대기
    PENDING_PAYMENT, // 결제 대기
    PAID, // 결제 완료
    PREPARING_FOR_SHIPMENT, // 배송 준비중
    SHIPPED, // 배송 중
    DELIVERED, // 배송 완료
    CANCELLED,// 주문 취소
    RETURN_REQUESTED, // 반품 신청
    RETURN_IN_PROGRESS, // 반품 중
    RETURN_COMPLETED // 반품 완료
}
