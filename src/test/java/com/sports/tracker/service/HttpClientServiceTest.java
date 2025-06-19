package com.sports.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.tracker.exception.ExternalApiCallFailedException;
import com.sports.tracker.exception.ExternalApiUnexpectedStatusException;
import com.sports.tracker.model.EventScore;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;

class HttpClientServiceTest {

    private static MockWebServer mockWebServer;

    private HttpClientService httpClientService;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        Retry retry = Retry.of("test", RetryConfig.custom()
                                                  .maxAttempts(2)
                                                  .waitDuration(Duration.ofMillis(100))
                                                  .build());

        httpClientService = Mockito.spy(new HttpClientService(retry, objectMapper));
        setField(httpClientService, mockWebServer.url("/events/{eventId}/score").toString());
    }

    @Test
    void shouldReturnEventScoreOnSuccess() {
        String responseBody = "{\"eventId\":\"e1\",\"currentScore\":\"2:1\"}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        EventScore score = httpClientService.callExternalApi("e1");
        assertNotNull(score);
        assertEquals("e1", score.eventId());
        assertEquals("2:1", score.currentScore());
    }

    @Test
    void shouldThrowOnNon200Status() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        ExternalApiCallFailedException e2 = assertThrows(ExternalApiCallFailedException.class,
                () -> httpClientService.callExternalApi("e2"));

        Throwable cause = e2.getCause();
        assertInstanceOf(ExternalApiUnexpectedStatusException.class, cause, "Expected cause to be ExternalApiUnexpectedStatusException");
        assertEquals("Unexpected response status from external API: 500", cause.getMessage());
        String url = mockWebServer.url("/events/%7BeventId%7D/score").toString();
        Mockito.verify(httpClientService, times(2)).performHttpCall(url);
    }

    @Test
    void shouldThrowOnMalformedJson() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{bad_json}"
                ));

        assertThrows(ExternalApiCallFailedException.class,
                () -> httpClientService.callExternalApi("e3"));

        String url = mockWebServer.url("/events/%7BeventId%7D/score").toString();
        Mockito.verify(httpClientService, times(2)).performHttpCall(url);
    }

    @Test
    void shouldHandleInterruptedException() {
        // we simulate by interrupting current thread before sending
        Thread.currentThread().interrupt();
        assertThrows(ExternalApiCallFailedException.class,
                () -> httpClientService.callExternalApi("e4"));
    }

    private void setField(Object target, Object value) {
        try {
            var field = HttpClientService.class.getDeclaredField("externalApiUrl");
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}