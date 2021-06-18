package com.n26.exception;

public class FutureDateValidationException extends RuntimeException {
    public FutureDateValidationException(String msg) {
        super(msg);
    }

    public FutureDateValidationException(String msg, Throwable t) {
        super(msg, t);
    }
}
