package com.sports.tracker.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration class for setting up Resilience4j Retry mechanism.
 * <p>
 * This class defines a retry policy and a named retry instance ("clientRetry")
 * that can be injected where needed (e.g., HTTP client, Kafka producer).
 */
@Slf4j
@Configuration
public class RetryConfigFactory {

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                                        .maxAttempts(3)
                                        .waitDuration(Duration.ofSeconds(2))
                                        .retryExceptions(Exception.class)
                                        .build();
        return RetryRegistry.of(config);
    }

    @Bean
    public Retry clientRetry(RetryRegistry retryRegistry) {
        Retry httpClientRetry = retryRegistry.retry("clientRetry");
        httpClientRetry.getEventPublisher()
                       .onRetry(e -> log.warn("Retry attempt after {} error: ", e.getNumberOfRetryAttempts(),
                               e.getLastThrowable()))
                       .onError(e -> log.error("Retry failed: ", e.getLastThrowable()));
        return httpClientRetry;
    }
}
