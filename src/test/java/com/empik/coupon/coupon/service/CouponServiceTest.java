package com.empik.coupon.coupon.service;

import com.empik.coupon.common.exception.CouponAlreadyUsedException;
import com.empik.coupon.common.exception.CouponExhaustedException;
import com.empik.coupon.common.exception.CouponNotFoundException;
import com.empik.coupon.common.exception.DuplicateCouponCodeException;
import com.empik.coupon.common.exception.InvalidCountryException;
import com.empik.coupon.coupon.domain.Coupon;
import com.empik.coupon.coupon.repository.CouponRedemptionRepository;
import com.empik.coupon.coupon.repository.CouponRepository;
import com.empik.coupon.geolocation.GeolocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock CouponRepository couponRepository;
    @Mock CouponRedemptionRepository redemptionRepository;
    @Mock GeolocationService geolocationService;

    @InjectMocks CouponService couponService;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        coupon = Coupon.create("WIOSNA2026", 100, "PL");
    }

    @Test
    @DisplayName("createCoupon stores a new coupon with uppercase code")
    void createCoupon_persists() {
        when(couponRepository.countByCodeIgnoreCase("wiosna")).thenReturn(0L);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        Coupon result = couponService.createCoupon("wiosna", 10, "pl");

        assertThat(result.getCode()).isEqualTo("WIOSNA");
        assertThat(result.getCountryCode()).isEqualTo("PL");
        assertThat(result.getCurrentUses()).isZero();
    }

    @Test
    @DisplayName("createCoupon rejects a duplicate code (case-insensitive)")
    void createCoupon_duplicate_throws() {
        when(couponRepository.countByCodeIgnoreCase("WIOSNA")).thenReturn(1L);

        assertThatThrownBy(() -> couponService.createCoupon("WIOSNA", 10, "PL"))
                .isInstanceOf(DuplicateCouponCodeException.class);

        verify(couponRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCoupon translates a concurrent DB unique-violation to DuplicateCouponCodeException")
    void createCoupon_concurrentDuplicate_throws() {
        when(couponRepository.countByCodeIgnoreCase(anyString())).thenReturn(0L);
        when(couponRepository.save(any(Coupon.class)))
                .thenThrow(new DataIntegrityViolationException("ux_coupons_code_lower"));

        assertThatThrownBy(() -> couponService.createCoupon("WIOSNA", 10, "PL"))
                .isInstanceOf(DuplicateCouponCodeException.class);
    }

    @Test
    @DisplayName("redeem returns a success result and reports remaining uses")
    void redeem_success() {
        when(geolocationService.resolveCountry("1.2.3.4")).thenReturn("PL");
        when(couponRepository.findByCodeIgnoreCase("wiosna2026")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsByCouponIdAndUserId(coupon.getId(), "user-1")).thenReturn(false);
        when(couponRepository.incrementUsageIfAvailable(coupon.getId())).thenReturn(1);

        CouponService.RedemptionResult result =
                couponService.redeem("wiosna2026", "user-1", "1.2.3.4");

        assertThat(result.code()).isEqualTo("WIOSNA2026");
        assertThat(result.userId()).isEqualTo("user-1");
        assertThat(result.remainingUses()).isEqualTo(99);
        verify(redemptionRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("redeem throws CouponNotFoundException for unknown code")
    void redeem_notFound() {
        when(geolocationService.resolveCountry(anyString())).thenReturn("PL");
        when(couponRepository.findByCodeIgnoreCase("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.redeem("nope", "user-1", "1.2.3.4"))
                .isInstanceOf(CouponNotFoundException.class);

        verify(couponRepository, never()).incrementUsageIfAvailable(any());
    }

    @Test
    @DisplayName("redeem refuses requests from a disallowed country and does not consume a use")
    void redeem_invalidCountry() {
        when(geolocationService.resolveCountry("5.5.5.5")).thenReturn("DE");
        when(couponRepository.findByCodeIgnoreCase("WIOSNA2026")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.redeem("WIOSNA2026", "user-1", "5.5.5.5"))
                .isInstanceOf(InvalidCountryException.class);

        verify(couponRepository, never()).incrementUsageIfAvailable(any());
        verify(redemptionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("redeem short-circuits when the user has already redeemed this coupon")
    void redeem_alreadyUsed_shortCircuit() {
        when(geolocationService.resolveCountry(anyString())).thenReturn("PL");
        when(couponRepository.findByCodeIgnoreCase("WIOSNA2026")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsByCouponIdAndUserId(coupon.getId(), "user-1")).thenReturn(true);

        assertThatThrownBy(() -> couponService.redeem("WIOSNA2026", "user-1", "1.2.3.4"))
                .isInstanceOf(CouponAlreadyUsedException.class);

        verify(couponRepository, never()).incrementUsageIfAvailable(any());
    }

    @Test
    @DisplayName("redeem throws CouponExhaustedException when the atomic increment reports zero rows")
    void redeem_exhausted() {
        when(geolocationService.resolveCountry(anyString())).thenReturn("PL");
        when(couponRepository.findByCodeIgnoreCase("WIOSNA2026")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsByCouponIdAndUserId(any(), any())).thenReturn(false);
        when(couponRepository.incrementUsageIfAvailable(coupon.getId())).thenReturn(0);

        assertThatThrownBy(() -> couponService.redeem("WIOSNA2026", "user-1", "1.2.3.4"))
                .isInstanceOf(CouponExhaustedException.class);

        verify(redemptionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("redeem maps a race-condition DB unique violation into CouponAlreadyUsedException")
    void redeem_concurrentAlreadyUsed() {
        when(geolocationService.resolveCountry(anyString())).thenReturn("PL");
        when(couponRepository.findByCodeIgnoreCase("WIOSNA2026")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsByCouponIdAndUserId(any(), any())).thenReturn(false);
        when(couponRepository.incrementUsageIfAvailable(coupon.getId())).thenReturn(1);
        when(redemptionRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("ux_redemption_per_user"));

        assertThatThrownBy(() -> couponService.redeem("WIOSNA2026", "user-1", "1.2.3.4"))
                .isInstanceOf(CouponAlreadyUsedException.class);
    }

    @Test
    @DisplayName("redeem uses case-insensitive lookup")
    void redeem_caseInsensitiveLookup() {
        when(geolocationService.resolveCountry(anyString())).thenReturn("PL");
        when(couponRepository.findByCodeIgnoreCase("WiOsNa2026")).thenReturn(Optional.of(coupon));
        when(redemptionRepository.existsByCouponIdAndUserId(any(), any())).thenReturn(false);
        when(couponRepository.incrementUsageIfAvailable(any())).thenReturn(1);

        couponService.redeem("WiOsNa2026", "user-1", "1.2.3.4");

        verify(couponRepository).findByCodeIgnoreCase(eq("WiOsNa2026"));
    }
}
