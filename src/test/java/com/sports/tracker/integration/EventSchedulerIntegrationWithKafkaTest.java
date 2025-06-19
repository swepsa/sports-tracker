package com.sports.tracker.integration;

import com.sports.tracker.scheduler.EventScheduler;
import com.sports.tracker.service.HttpClientService;
import com.sports.tracker.service.KafkaProducerService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@ContextConfiguration(initializers = EventSchedulerIntegrationWithKafkaTest.Initializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventSchedulerIntegrationWithKafkaTest {

    private static final String TOPIC = "live-sports-events";
    private static final KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    private MockWebServer mockWebServer;
    private KafkaConsumer<String, String> consumer;

    @Autowired
    private EventScheduler eventScheduler;

    @Autowired
    private HttpClientService httpClientService;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @AfterAll
    static void stopKafka() {
        kafkaContainer.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        // Start the mock web server simulating external API
        mockWebServer = new MockWebServer();
        mockWebServer.start(8081); // Must match `external.api.url` in configuration

        // Configure Kafka consumer for testing
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "testGroup");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterEach
    void tearDown() throws Exception {
        eventScheduler.shutdown();

        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }

        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void testEventSchedulerPublishesToKafka() {
        String eventId = "test-event";

        // Mock response from external API
        String jsonResponse = "{\"eventId\":\"" + eventId + "\", \"currentScore\":\"3:1\"}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        // Trigger scheduled task
        eventScheduler.scheduleEvent(eventId);

        // Wait for message to be received in Kafka
        AtomicReference<ConsumerRecords<String, String>> receivedRecords = new AtomicReference<>();

        await().atMost(50, TimeUnit.SECONDS).until(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            if (!records.isEmpty()) {
                receivedRecords.set(records);
                return true;
            }
            return false;
        });

        // Verify message content
        assertThat(receivedRecords.get())
                .isNotEmpty()
                .anyMatch(rec -> rec.value().contains("3:1"));
    }

    /**
     * Initializes the Spring context with overridden properties from Testcontainers and MockWebServer.
     */
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        static {
            kafkaContainer.start();
        }

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "spring.kafka.bootstrap-servers=" + kafkaContainer.getBootstrapServers()//,
            ).applyTo(context.getEnvironment());
        }
    }
}
