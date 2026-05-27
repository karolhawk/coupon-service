package com.empik.coupon.geolocation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(GeolocationProperties.class)
public class GeolocationConfig {

    @Bean
    public GeolocationService geolocationService(GeolocationProperties properties) {
        return switch (properties.provider().toLowerCase()) {
            case "static" -> new StaticGeolocationService(
                    properties.fallbackCountry() == null || properties.fallbackCountry().isBlank()
                            ? "PL"
                            : properties.fallbackCountry());
            case "ip-api", "ipapi" -> new IpApiGeolocationService(buildRestClient(properties), properties);
            default -> throw new IllegalStateException(
                    "Unknown geolocation provider: " + properties.provider());
        };
    }

    private RestClient buildRestClient(GeolocationProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.timeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.timeoutMs()));
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }
}
