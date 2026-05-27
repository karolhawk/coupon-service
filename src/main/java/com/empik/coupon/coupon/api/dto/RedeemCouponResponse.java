package com.empik.coupon.coupon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Result of a successful coupon redemption")
public record RedeemCouponResponse(
        String code,
        String userId,
        Instant redeemedAt,
        int remainingUses
) {
}
