package com.empik.coupon.coupon.api.dto;

import com.empik.coupon.coupon.domain.Coupon;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Read model of a coupon")
public record CouponResponse(
        UUID id,
        String code,
        Instant createdAt,
        int maxUses,
        int currentUses,
        String countryCode
) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getCreatedAt(),
                coupon.getMaxUses(),
                coupon.getCurrentUses(),
                coupon.getCountryCode()
        );
    }
}
