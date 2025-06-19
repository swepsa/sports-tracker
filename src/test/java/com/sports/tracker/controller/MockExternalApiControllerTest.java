package com.sports.tracker.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MockExternalApiController.class)
class MockExternalApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getEventScore_shouldReturnEventIdAndRandomScore() throws Exception {
        String eventId = "event123";

        mockMvc.perform(get("/api/events/{eventId}/score", eventId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.eventId").value(eventId))
               .andExpect(jsonPath("$.currentScore").value(matchesPattern("\\d:\\d")));
    }
}
