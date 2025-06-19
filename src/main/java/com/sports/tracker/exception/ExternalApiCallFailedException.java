package com.sports.tracker.exception;

public class ExternalApiCallFailedException extends ExternalApiException {
    public ExternalApiCallFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
