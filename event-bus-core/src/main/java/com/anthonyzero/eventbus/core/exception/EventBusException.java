package com.anthonyzero.eventbus.core.exception;

public class EventBusException extends RuntimeException {

    public EventBusException(String message) {
        super(message);
    }

    public EventBusException(Throwable cause) {
        super(cause);
    }

    public EventBusException(String message, Throwable cause) {
        super(message, cause);
    }

}
