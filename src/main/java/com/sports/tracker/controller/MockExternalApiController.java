package com.sports.tracker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock controller to simulate external API responses.
 */
@RestController
@RequestMapping("/api/events")
public class MockExternalApiController {

    /**
     * Mock endpoint to simulate external API responses for event score.
     *
     * @param eventId The ID of the event.
     * @return A JSON object with eventId and currentScore.
     */
    @Operation(
            summary = "Mock External API - Get Event Score",
            description = "Simulates an external API by returning a random current score for the given eventId."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved event score.")
    @ApiResponse(responseCode = "500", description = "Internal server error.")
    @GetMapping("/{eventId}/score")
    public ResponseEntity<Map<String, String>> getEventScore(@PathVariable String eventId) {
        String score = generateRandomScore();
        Map<String, String> response = Map.of(
                "eventId", eventId,
                "currentScore", score
        );
        return ResponseEntity.ok(response);
    }

    private String generateRandomScore() {
        int home = ThreadLocalRandom.current().nextInt(0, 6);
        int away = ThreadLocalRandom.current().nextInt(0, 6);
        return home + ":" + away;
    }
}
