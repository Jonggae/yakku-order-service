package com.jonggae.yakku.exceptions;

public class NotFoundProductException extends RuntimeException{

    public NotFoundProductException() {
        super("해당 상품을 찾을 수 없습니다.");
    }
}
