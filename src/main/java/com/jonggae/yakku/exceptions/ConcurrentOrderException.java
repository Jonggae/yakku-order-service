package com.jonggae.yakku.exceptions;

public class ConcurrentOrderException extends RuntimeException {
    public ConcurrentOrderException(String message) {
        super(message);
    }
}
