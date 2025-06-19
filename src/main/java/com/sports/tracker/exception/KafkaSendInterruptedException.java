package com.sports.tracker.exception;

/**
 * Thrown when the thread is interrupted during a Kafka send operation.
 */
public class KafkaSendInterruptedException extends KafkaMessageSendException {
    public KafkaSendInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
