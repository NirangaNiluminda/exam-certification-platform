package com.examplatform.outbox;

public class OutboxDispatchException extends RuntimeException {
    public OutboxDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
