package com.sports.tracker.service;

import com.sports.tracker.model.EventStatus;
import com.sports.tracker.model.enums.Status;
import com.sports.tracker.scheduler.EventScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EventServiceTest {

    private EventScheduler eventScheduler;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventScheduler = mock(EventScheduler.class);
        eventService = new EventService(eventScheduler);
    }

    @Test
    void shouldScheduleEvent_whenStatusIsLive() {
        // given
        EventStatus liveStatus = new EventStatus("event-1", Status.LIVE);

        // when
        eventService.updateEventStatus(liveStatus);

        // then
        verify(eventScheduler).scheduleEvent("event-1");
        verify(eventScheduler, never()).cancelEvent(anyString());
    }

    @Test
    void shouldCancelEvent_whenStatusIsNotLive() {
        // given
        EventStatus finishedStatus = new EventStatus("event-2", Status.NOT_LIVE);

        // when
        eventService.updateEventStatus(finishedStatus);

        // then
        verify(eventScheduler).cancelEvent("event-2");
        verify(eventScheduler, never()).scheduleEvent(anyString());
    }
}
