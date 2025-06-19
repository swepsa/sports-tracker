package com.sports.tracker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the current score of an event.
 *
 * @param eventId      the unique identifier of the event
 * @param currentScore the current score of the event, e.g., "2:1"
 */
public record EventScore(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("currentScore") String currentScore) {
}