package com.sports.tracker.service;

import com.sports.tracker.model.EventStatus;
import com.sports.tracker.model.enums.Status;
import com.sports.tracker.scheduler.EventScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to handle event status updates and manage scheduling of event processing tasks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventScheduler eventScheduler;

    /**
     * Updates the scheduling of event processing based on the event status.
     * If the status is LIVE, schedules the event; otherwise cancels the scheduled event.
     *
     * @param eventStatus the updated status of the event
     */
    public void updateEventStatus(EventStatus eventStatus) {
        String eventId = eventStatus.eventId();
        if (Status.LIVE.equals(eventStatus.status())) {
            log.info("Scheduling event with ID: {}", eventId);
            eventScheduler.scheduleEvent(eventId);
        } else {
            log.info("Cancelling event with ID: {}", eventId);
            eventScheduler.cancelEvent(eventId);
        }
    }
}
