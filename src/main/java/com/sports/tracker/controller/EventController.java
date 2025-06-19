package com.sports.tracker.controller;

import com.sports.tracker.model.EventStatus;
import com.sports.tracker.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing event statuses.
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * Updates the status of an event to "live" or "not live".
     *
     * @param eventStatus The event status payload.
     * @return A response indicating successful update.
     */
    @Operation(summary = "Update Event Status",
            description = "Updates the status of an event to 'live' or 'not live'.")
    @ApiResponse(responseCode = "200", description = "Event status updated successfully.")
    @ApiResponse(responseCode = "500", description = "Internal server error.", content = @Content)
    @PostMapping("/status")
    public ResponseEntity<String> updateEventStatus(@RequestBody EventStatus eventStatus) {
        eventService.updateEventStatus(eventStatus);
        return ResponseEntity.ok("Event status updated successfully.");
    }
}