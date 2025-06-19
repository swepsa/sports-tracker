package com.sports.tracker.config;

import jakarta.validation.constraints.NotNull;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for enabling dual port support in the embedded Tomcat server.
 * <p>
 * This setup allows the application to listen on an additional (secondary) HTTP port,
 * in addition to the default one.
 */
@Configuration
public class DualPortConfig {

    @Value("${custom.secondary-port}")
    private int secondaryPort;

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addAdditionalTomcatConnectors(createSecondaryConnector());
        return factory;
    }

    private @NotNull Connector createSecondaryConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(secondaryPort);
        return connector;
    }
}
