package com.jonggae.yakku.exceptions;

public class NotFoundMemberException extends RuntimeException {
    public NotFoundMemberException() {
        super("회원 정보를 찾을 수 없습니다.");
    }
    public NotFoundMemberException(String message, Throwable cause) {
        super(message, cause);
    }
    public NotFoundMemberException(String message) {
        super(message);
    }
    public NotFoundMemberException(Throwable cause) {
        super(cause);
    }
}