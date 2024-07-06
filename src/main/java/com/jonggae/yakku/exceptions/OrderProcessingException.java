package com.jonggae.yakku.exceptions;

public class OrderProcessingException extends RuntimeException{
    public OrderProcessingException(String message, Throwable cause){
        super(message, cause);
    }
}
