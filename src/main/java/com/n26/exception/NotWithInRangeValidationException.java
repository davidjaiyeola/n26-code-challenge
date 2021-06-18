package com.n26.exception;

public class NotWithInRangeValidationException extends RuntimeException {
    public NotWithInRangeValidationException(String msg) {
        super(msg);
    }

    public NotWithInRangeValidationException(String msg, Throwable t) {
        super(msg, t);
    }
}
