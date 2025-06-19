package com.sports.tracker.scheduler;

import com.sports.tracker.model.EventScore;
import com.sports.tracker.service.HttpClientService;
import com.sports.tracker.service.KafkaProducerService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Scheduler responsible for periodically fetching event scores and publishing them to Kafka.
 * Uses virtual threads for lightweight concurrent task management.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventScheduler {

    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final HttpClientService httpClientService;
    private final KafkaProducerService kafkaProducerService;

    // Map of eventId to their running scheduled Future task
    private final Map<String, Future<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Schedule periodic fetch and publish task for a given eventId.
     * If a task is already running for the eventId, it won't be scheduled again.
     *
     * @param eventId the ID of the event to schedule
     */
    public void scheduleEvent(String eventId) {
        if (scheduledTasks.containsKey(eventId)) {
            log.info("Task already scheduled for eventId: {}", eventId);
            return;
        }

        Future<?> future = virtualThreadExecutor.submit(() -> {
            log.info("Started virtual thread for eventId: {}", eventId);
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    fetchAndPublish(eventId);
                    Thread.sleep(10_000); // Sleep for 10 seconds
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                log.info("Virtual thread interrupted for eventId: {}", eventId);
            } catch (Exception e) {
                log.error("Error while fetching/publishing for eventId: {}", eventId, e);
            } finally {
                // Ensure task is removed from scheduledTasks map when done
                scheduledTasks.remove(eventId);
                log.info("Task completed or stopped for eventId: {}", eventId);
            }
        });

        scheduledTasks.put(eventId, future);
        log.info("Scheduled virtual thread for eventId: {}", eventId);
    }

    /**
     * Cancels the scheduled task for the given eventId if it exists.
     *
     * @param eventId the ID of the event whose task should be cancelled
     */
    public void cancelEvent(String eventId) {
        Future<?> future = scheduledTasks.remove(eventId);
        if (future != null) {
            future.cancel(true);
            log.info("Cancelled virtual thread for eventId: {}", eventId);
        } else {
            log.warn("No task found to cancel for eventId: {}", eventId);
        }
    }

    /**
     * Fetches the latest EventScore for the given eventId and publishes it via Kafka.
     *
     * @param eventId the event ID to fetch and publish
     */
    private void fetchAndPublish(String eventId) {
        EventScore eventScore = httpClientService.callExternalApi(eventId);
        kafkaProducerService.sendMessage(eventScore);
    }

    /**
     * Gracefully shutdown all scheduled tasks on bean destruction.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down EventScheduler. Cancelling all scheduled tasks.");
        scheduledTasks.values().forEach(future -> future.cancel(true));
        virtualThreadExecutor.shutdownNow();
    }
}
