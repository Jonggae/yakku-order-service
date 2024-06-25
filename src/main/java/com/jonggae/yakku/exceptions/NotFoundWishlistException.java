package com.jonggae.yakku.exceptions;

public class NotFoundWishlistException extends RuntimeException {
    public NotFoundWishlistException() {
        super("위시리스트를 찾을 수 없습니다.");
    }
}
