package com.empik.coupon.coupon.api;

import com.empik.coupon.common.web.ApiError;
import com.empik.coupon.common.web.ClientIpResolver;
import com.empik.coupon.coupon.api.dto.CouponResponse;
import com.empik.coupon.coupon.api.dto.CreateCouponRequest;
import com.empik.coupon.coupon.api.dto.RedeemCouponRequest;
import com.empik.coupon.coupon.api.dto.RedeemCouponResponse;
import com.empik.coupon.coupon.domain.Coupon;
import com.empik.coupon.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons", description = "Discount coupon management")
public class CouponController {

    private final CouponService couponService;
    private final ClientIpResolver clientIpResolver;

    public CouponController(CouponService couponService, ClientIpResolver clientIpResolver) {
        this.couponService = couponService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    @Operation(summary = "Create a new coupon")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Coupon created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Coupon code already exists",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = couponService.createCoupon(
                request.code(), request.maxUses(), request.countryCode());

        URI location = UriComponentsBuilder.fromPath("/api/v1/coupons/{code}")
                .buildAndExpand(coupon.getCode())
                .toUri();

        return ResponseEntity.created(location).body(CouponResponse.from(coupon));
    }

    @PostMapping("/{code}/redemptions")
    @Operation(summary = "Redeem a coupon for a user",
            description = "The caller's country is derived from the request IP (or "
                    + "X-Forwarded-For when behind a trusted proxy).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon redeemed"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Coupon not valid in caller's country",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Coupon not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Coupon exhausted or already used by this user",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Geolocation provider unavailable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RedeemCouponResponse> redeem(
            @PathVariable("code") String code,
            @Valid @RequestBody RedeemCouponRequest request,
            HttpServletRequest httpRequest) {

        String ip = clientIpResolver.resolve(httpRequest);
        CouponService.RedemptionResult result = couponService.redeem(code, request.userId(), ip);

        return ResponseEntity.status(HttpStatus.OK).body(new RedeemCouponResponse(
                result.code(),
                result.userId(),
                result.redeemedAt(),
                result.remainingUses()
        ));
    }
}
