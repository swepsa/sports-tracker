package com.sports.tracker.service;

import com.sports.tracker.exception.KafkaMessageSendException;
import com.sports.tracker.exception.KafkaSendInterruptedException;
import com.sports.tracker.model.EventScore;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Service responsible for publishing messages to a Kafka topic
 * with retry support using Resilience4j.
 */
@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Retry retry;
    private final String topic;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, Retry retry, @Value("${kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.retry = retry;
        this.topic = topic;
    }

    /**
     * Publishes the given {@link EventScore} to the configured Kafka topic.
     * Automatically retries the operation using Resilience4j.
     *
     * @param eventScore the event data to be sent
     */
    public void sendMessage(EventScore eventScore) {
        Supplier<SendResult<String, String>> retriableSend = Retry.decorateSupplier(retry,
                () -> sendKafkaMessage(eventScore));
        retriableSend.get(); // Trigger the supplier with retry
    }

    private SendResult<String, String> sendKafkaMessage(EventScore eventScore) {
        String key = eventScore.eventId();
        String message = eventScore.currentScore();

        try {
            log.info("Sending Kafka message: topic={}, key={}, message={}", topic, key, message);
            SendResult<String, String> result = kafkaTemplate
                    .send(new ProducerRecord<>(topic, key, message))
                    .get();

            log.info("Kafka message sent: key={}, offset={}", key, result.getRecordMetadata().offset());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Kafka send interrupted for eventId={}", key, e);
            throw new KafkaSendInterruptedException("Kafka send was interrupted", e);
        } catch (ExecutionException e) {
            log.warn("Temporary failure publishing eventId={}", key, e);
            throw new KafkaMessageSendException("Kafka send failed", e);
        }
    }

}
