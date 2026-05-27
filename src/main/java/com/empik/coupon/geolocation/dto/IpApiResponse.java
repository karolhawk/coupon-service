package com.empik.coupon.geolocation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public record IpApiResponse(
        String status,
        String message,
        @JsonProperty("countryCode") String countryCode
) {
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
