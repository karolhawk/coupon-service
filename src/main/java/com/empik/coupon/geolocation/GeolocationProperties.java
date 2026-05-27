package com.empik.coupon.geolocation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coupon.geolocation")
public record GeolocationProperties(
        String provider,
        String baseUrl,
        int timeoutMs,
        String fallbackCountry
) {
}
