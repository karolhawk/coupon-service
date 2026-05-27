package com.empik.coupon.coupon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for creating a new coupon")
public record CreateCouponRequest(

        @Schema(example = "WIOSNA2026", description = "Unique coupon code (case-insensitive)")
        @NotBlank(message = "code must not be blank")
        @Size(min = 3, max = 32, message = "code must be 3..32 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "code may contain only letters, digits, '_' and '-'")
        String code,

        @Schema(example = "100", description = "Maximum number of redemptions allowed")
        @Min(value = 1, message = "maxUses must be at least 1")
        int maxUses,

        @Schema(example = "PL", description = "country code")
        @NotBlank(message = "countryCode must not be blank")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "countryCode (e.g. PL)")
        String countryCode
) {
}
