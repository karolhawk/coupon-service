package com.empik.coupon.common.web;

import com.empik.coupon.common.exception.ErrorCode;

import java.time.Instant;
import java.util.List;

public record ApiError(
        ErrorCode code,
        String message,
        Instant timestamp,
        String path,
        List<FieldViolation> details
) {

    public static ApiError of(ErrorCode code, String message, String path) {
        return new ApiError(code, message, Instant.now(), path, List.of());
    }

    public static ApiError of(ErrorCode code, String message, String path, List<FieldViolation> details) {
        return new ApiError(code, message, Instant.now(), path, details);
    }

    public record FieldViolation(String field, String message) {}
}
