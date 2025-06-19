package com.sports.tracker.exception;

/**
 * Thrown when sending a message to Kafka fails.
 */
public class KafkaMessageSendException extends RuntimeException {
    public KafkaMessageSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
