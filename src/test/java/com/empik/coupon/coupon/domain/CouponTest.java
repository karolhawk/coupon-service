package com.empik.coupon.coupon.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    @Test
    void create_normalisesCodeAndCountryToUppercase() {
        Coupon coupon = Coupon.create("wiosna2026", 5, "pl");

        assertThat(coupon.getCode()).isEqualTo("WIOSNA2026");
        assertThat(coupon.getCountryCode()).isEqualTo("PL");
        assertThat(coupon.getCurrentUses()).isZero();
    }

    @Test
    void create_rejectsZeroOrNegativeMaxUses() {
        assertThatThrownBy(() -> Coupon.create("X", 0, "PL"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Coupon.create("X", -1, "PL"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejectsNonAlpha2CountryCode() {
        assertThatThrownBy(() -> Coupon.create("X", 1, "POL"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isUsableFrom_matchesCaseInsensitively() {
        Coupon coupon = Coupon.create("X", 1, "PL");
        assertThat(coupon.isUsableFrom("pl")).isTrue();
        assertThat(coupon.isUsableFrom("PL")).isTrue();
        assertThat(coupon.isUsableFrom("DE")).isFalse();
    }
}
