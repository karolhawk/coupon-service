package com.empik.coupon.geolocation;

/**
 * No-network implementation that returns the configured fallback country for every IP.
 * Wire up via {@code coupon.geolocation.provider=static}, typically in tests or air-gapped envs.
 */
public class StaticGeolocationService implements GeolocationService {

    private final String defaultCountry;

    public StaticGeolocationService(String defaultCountry) {
        if (defaultCountry == null || defaultCountry.length() != 2) {
            throw new IllegalArgumentException(
                    "Static geolocation requires a 2-letter country code; got: " + defaultCountry);
        }
        this.defaultCountry = defaultCountry.toUpperCase();
    }

    @Override
    public String resolveCountry(String ipAddress) {
        return defaultCountry;
    }
}
