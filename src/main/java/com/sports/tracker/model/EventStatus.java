package com.sports.tracker.model;

import com.sports.tracker.model.enums.Status;

/**
 * Represents the status of an event.
 *
 * @param eventId The unique identifier of the event.
 * @param status  The current status of the event ("live" or "not live").
 */
public record EventStatus(
        String eventId,
        Status status) {
}