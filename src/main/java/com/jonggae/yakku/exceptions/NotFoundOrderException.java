package com.jonggae.yakku.exceptions;

public class NotFoundOrderException extends RuntimeException {
    public NotFoundOrderException() {
        super("주문이 비어있습니다.");
    }
}
