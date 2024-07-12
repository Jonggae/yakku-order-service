package com.jonggae.yakku.exceptions;

public class StockReservationFailException extends RuntimeException {
    public StockReservationFailException(String message) {
        super(message);
    }
}
