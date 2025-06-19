package com.sports.tracker.controller;

import com.sports.tracker.model.EventStatus;
import com.sports.tracker.model.enums.Status;
import com.sports.tracker.service.EventService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Test
    void testUpdateEventStatus() throws Exception {
        // Mock the service method to do nothing
        Mockito.doNothing().when(eventService).updateEventStatus(any(EventStatus.class));

        String jsonPayload = """
                {
                  "eventId": "123",
                  "status": "LIVE"
                }
                """;

        mockMvc.perform(post("/events/status")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(jsonPayload))
               .andExpect(status().isOk())
               .andExpect(content().string("Event status updated successfully."));

        // Verify that the service method was called exactly once
        Mockito.verify(eventService, Mockito.times(1)).updateEventStatus(any(EventStatus.class));
    }

    @Test
    void shouldHandleUnhandledException() throws Exception {
        // given
        EventStatus status = new EventStatus("event-123", Status.NOT_LIVE);
        String jsonPayload = """
                {
                  "eventId": "event-123",
                  "status": "NOT_LIVE"
                }
                """;

        // when
        doThrow(new RuntimeException("Unexpected failure")).when(eventService).updateEventStatus(status);

        // then
        mockMvc.perform(post("/events/status")
                       .contentType("application/json")
                       .content(jsonPayload))
               .andExpect(status().isInternalServerError())
               .andExpect(content().string("Unexpected failure"));
    }
}
