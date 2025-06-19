package com.sports.tracker.service;

import com.sports.tracker.exception.KafkaMessageSendException;
import com.sports.tracker.model.EventScore;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;


    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, String>> recordCaptor;

    private AutoCloseable mocks;

    private static final String TOPIC = "live-sports-events";
    private static final String EVENT_ID = "event123";
    private static final String SCORE = "2-1";

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);

        RetryConfig config = RetryConfig.custom()
                                        .maxAttempts(3)
                                        .waitDuration(java.time.Duration.ofMillis(10))
                                        .build();
        Retry realRetry = Retry.of("testRetry", config);

        kafkaProducerService = new KafkaProducerService(kafkaTemplate, realRetry,TOPIC);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void sendMessage_success() {
        // Given
        EventScore eventScore = new EventScore(EVENT_ID, SCORE);

        RecordMetadata recordMetadata = new RecordMetadata(null, 0, 0, 0L, 0, 0);
        ProducerRecord<String, String> event123 = new ProducerRecord<>(TOPIC, EVENT_ID, SCORE);
        SendResult<String, String> sendResult = new SendResult<>(
                new ProducerRecord<>(TOPIC, EVENT_ID, SCORE), recordMetadata);

        // CompletableFuture for send
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(event123)).thenReturn(future);

        // When
        assertDoesNotThrow(() -> kafkaProducerService.sendMessage(eventScore));

        // Then
        verify(kafkaTemplate).send(recordCaptor.capture());
        ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();

        assertEquals(TOPIC, capturedRecord.topic());
        assertEquals(EVENT_ID, capturedRecord.key());
        assertEquals(SCORE, capturedRecord.value());
    }

    @Test
    void sendMessage_interruptedException_throwsKafkaSendInterruptedException() {
        // Given
        EventScore eventScore = new EventScore(EVENT_ID, SCORE);
        ProducerRecord<String, String> event123 = new ProducerRecord<>(TOPIC, EVENT_ID, SCORE);

        when(kafkaTemplate.send(event123))
                .thenReturn(new CompletableFuture<>() {
                    @Override
                    public SendResult<String, String> get() throws InterruptedException {
                        throw new InterruptedException("Simulated interrupt");
                    }
                });

        // When
        assertThatThrownBy(() -> kafkaProducerService.sendMessage(eventScore))
                .isInstanceOf(KafkaMessageSendException.class)
                .hasMessageContaining("interrupted");

        // Then
        verify(kafkaTemplate).send(event123);
    }

    @Test
    void sendMessage_executionException_throwsKafkaMessageSendException() {
        // Given
        EventScore eventScore = new EventScore(EVENT_ID, SCORE);
        ProducerRecord<String, String> event123 = new ProducerRecord<>(TOPIC, EVENT_ID, SCORE);

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new ExecutionException(new RuntimeException("fail")));

        when(kafkaTemplate.send(event123)).thenReturn(future);

        // When
        assertThatThrownBy(() -> kafkaProducerService.sendMessage(eventScore))
                .isInstanceOf(KafkaMessageSendException.class)
                .hasMessageContaining("Kafka send failed");

        // Then
        verify(kafkaTemplate, times(3)).send(event123);
    }
}
