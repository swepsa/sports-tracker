package com.sports.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.tracker.exception.ExternalApiCallFailedException;
import com.sports.tracker.exception.ExternalApiUnexpectedStatusException;
import com.sports.tracker.model.EventScore;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Service responsible for making HTTP calls to an external API
 * to retrieve {@link EventScore} data.
 * <p>
 * Implements automatic retries using Resilience4j {@link Retry}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpClientService {

    private final Retry httpClientRetry;
    private final ObjectMapper objectMapper;

    @Value("${external.api.url}")
    private String externalApiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(Duration.ofSeconds(5))
                                                    .build();

    /**
     * Calls the external API to retrieve the score of the given event.
     * This method wraps the call in a retry strategy.
     *
     * @param eventId the ID of the event to fetch the score for
     * @return {@link EventScore} retrieved from the external API
     * @throws ExternalApiCallFailedException if the HTTP call fails or an unexpected error occurs
     * @throws ExternalApiUnexpectedStatusException if the API returns a non-200 HTTP status
     */
    public EventScore callExternalApi(String eventId) {
        String url = externalApiUrl.replace("{eventId}", eventId);
        Supplier<EventScore> supplier = Retry.decorateSupplier(httpClientRetry, () -> performHttpCall(url));
        return supplier.get();
    }

    /**
     * Visible for testing only.
     */
    EventScore performHttpCall(String url) {
        try {
            log.info("Calling external API: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(url))
                                             .GET()
                                             .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode != 200) {
                log.warn("Unexpected response status: {}", statusCode);
                throw new ExternalApiUnexpectedStatusException(statusCode);
            }

            log.info("Received successful response from external API: {}", response.body());
            return objectMapper.readValue(response.body(), EventScore.class);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiCallFailedException("External API call was interrupted", e);
        } catch (IOException e) {
            log.error("I/O error during external API call", e);
            throw new ExternalApiCallFailedException("I/O error during external API call", e);
        } catch (Exception e) {
            log.error("Unexpected error during external API call", e);
            throw new ExternalApiCallFailedException("Unexpected error during external API call", e);
        }
    }
}
