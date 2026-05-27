package com.empik.coupon.geolocation;

import com.empik.coupon.common.exception.GeolocationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticGeolocationServiceTest {

    @Test
    void resolvesEveryIpToConfiguredCountry() {
        StaticGeolocationService service = new StaticGeolocationService("PL");

        assertThat(service.resolveCountry("1.2.3.4")).isEqualTo("PL");
        assertThat(service.resolveCountry("127.0.0.1")).isEqualTo("PL");
        assertThat(service.resolveCountry("invalid")).isEqualTo("PL");
    }

    @Test
    void rejectsInvalidCountryCode() {
        assertThatThrownBy(() -> new StaticGeolocationService("POLSKA"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCountryCode() {
        assertThatThrownBy(() -> new StaticGeolocationService(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
