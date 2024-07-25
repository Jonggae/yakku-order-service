package com.jonggae.yakku.order.entity;

public enum OrderResult {
    SUCCESS,
    STOCK_INSUFFICIENT,
    PAYMENT_FAILED,
    CONCURRENT_ORDER_CONFLICT,
    EXTERNAL_SERVICE_ERROR
}
