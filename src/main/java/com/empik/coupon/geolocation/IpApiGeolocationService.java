package com.empik.coupon.geolocation;

import com.empik.coupon.common.exception.GeolocationException;
import com.empik.coupon.geolocation.dto.IpApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Geolocation implementation backed by the free ip-api.com service.
 *
 * <p>Local/private IP ranges short-circuit to the configured fallback country (typical for
 * local development where {@code REMOTE_ADDR} is {@code 127.0.0.1} or an internal Docker IP).
 * In production set {@code coupon.geolocation.fallback-country} to empty to surface a
 * {@code 503 GEOLOCATION_UNAVAILABLE} instead of silently passing the check.
 */
public class IpApiGeolocationService implements GeolocationService {

    private static final Logger log = LoggerFactory.getLogger(IpApiGeolocationService.class);

    private final RestClient restClient;
    private final GeolocationProperties properties;

    public IpApiGeolocationService(RestClient restClient, GeolocationProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String resolveCountry(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new GeolocationException("IP address is required to resolve country");
        }

        if (isLocalAddress(ipAddress)) {
            if (properties.fallbackCountry() == null || properties.fallbackCountry().isBlank()) {
                throw new GeolocationException(
                        "Cannot determine country for local/private IP '" + ipAddress
                                + "' and no fallback country is configured");
            }
            log.debug("Resolved local IP {} to fallback country {}", ipAddress, properties.fallbackCountry());
            return properties.fallbackCountry().toUpperCase();
        }

        try {
            IpApiResponse response = restClient.get()
                    .uri("/json/{ip}?fields=status,message,countryCode", ipAddress)
                    .retrieve()
                    .body(IpApiResponse.class);

            if (response == null || !response.isSuccess() || response.countryCode() == null) {
                String msg = response != null ? response.message() : "no response";
                throw new GeolocationException(
                        "Geolocation provider returned failure for IP '" + ipAddress + "': " + msg);
            }
            return response.countryCode().toUpperCase();

        } catch (RestClientException e) {
            throw new GeolocationException(ipAddress, e);
        }
    }

    private boolean isLocalAddress(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            // If the string isn't a valid IP at all, let the upstream call fail explicitly.
            return false;
        }
    }
}
