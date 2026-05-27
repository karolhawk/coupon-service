package com.empik.coupon.coupon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for redeeming a coupon. The user IP is taken from the request itself.")
public record RedeemCouponRequest(

        @Schema(example = "user-42", description = "Caller-provided user identifier")
        @NotBlank(message = "userId must not be blank")
        @Size(max = 128, message = "userId must not exceed 128 characters")
        String userId
) {
}
