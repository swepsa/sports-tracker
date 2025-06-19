package com.sports.tracker.exception;

public class ExternalApiUnexpectedStatusException extends ExternalApiException {
    public ExternalApiUnexpectedStatusException(int statusCode) {
        super("Unexpected response status from external API: " + statusCode);
    }
}
