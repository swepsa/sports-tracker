package com.sports.tracker.scheduler;

import com.sports.tracker.model.EventScore;
import com.sports.tracker.service.HttpClientService;
import com.sports.tracker.service.KafkaProducerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

class EventSchedulerTest {

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private EventScheduler eventScheduler;

    private final EventScore dummyScore = new EventScore("event1", "1:2");

    private AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        eventScheduler = new EventScheduler(httpClientService, kafkaProducerService);
    }

    @AfterEach
    void tearDown() throws Exception {
        eventScheduler.shutdown();
        mocks.close();
    }

    @Test
    void scheduleEvent_shouldStartFetchingAndPublishing() throws Exception {
        when(httpClientService.callExternalApi("event1")).thenReturn(dummyScore);
        doNothing().when(kafkaProducerService).sendMessage(dummyScore);

        eventScheduler.scheduleEvent("event1");

        // Give it a moment to start the task and perform at least one iteration
        TimeUnit.MILLISECONDS.sleep(50);

        verify(httpClientService).callExternalApi("event1");
        verify(kafkaProducerService).sendMessage(dummyScore);
    }

    @Test
    void scheduleEvent_shouldNotScheduleIfAlreadyRunning() throws InterruptedException {
        eventScheduler.scheduleEvent("event1");
        eventScheduler.scheduleEvent("event1"); // should not re-schedule

        // Give it a moment to start the task and perform at least one iteration
        TimeUnit.MILLISECONDS.sleep(50);

        Assertions.assertEquals(1, getScheduledTaskCount());
    }

    @Test
    void cancelEvent_shouldCancelRunningTask() {
        eventScheduler.scheduleEvent("event2");

        eventScheduler.cancelEvent("event2");

        Assertions.assertEquals(0, getScheduledTaskCount());
    }

    @Test
    void cancelEvent_shouldLogWarningIfNoTask() {
        // No exception expected here, just log
        assertDoesNotThrow(() -> {
            // code that should not throw any exception
            eventScheduler.cancelEvent("nonexistent");
        });
    }

    @Test
    void shutdown_shouldCancelAllTasks() {
        eventScheduler.scheduleEvent("event3");
        eventScheduler.scheduleEvent("event4");

        eventScheduler.shutdown();
        await().atMost(100, TimeUnit.SECONDS).until(() ->
                getScheduledTaskCount() == 0);

        assertThat(getScheduledTaskCount())
                .as("All scheduled tasks should be cancelled after shutdown")
                .isZero();
    }

    private int getScheduledTaskCount() {
        try {
            var field = EventScheduler.class.getDeclaredField("scheduledTasks");
            field.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) field.get(eventScheduler);
            return map.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testScheduleEvent_handlesExceptionFromHttpClientService() throws Exception {
        String eventId = "testEvent";

        // Simulate exception thrown by HttpClientService
        when(httpClientService.callExternalApi(eventId)).thenThrow(new RuntimeException("API failure"));

        eventScheduler.scheduleEvent(eventId);

        // Wait a bit to let the virtual thread run and hit the exception
        // Since the thread runs an infinite loop with sleep, we wait shortly then cancel
        TimeUnit.MILLISECONDS.sleep(200);

        // Cancel task to clean up
        eventScheduler.cancelEvent(eventId);

        // Verify that callExternalApi was called at least once
        verify(httpClientService, atLeastOnce()).callExternalApi(eventId);

        // Verify no interactions with KafkaProducer since exception prevents sending
        verifyNoInteractions(kafkaProducerService);
    }
}
