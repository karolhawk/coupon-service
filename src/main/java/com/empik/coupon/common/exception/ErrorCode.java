package com.empik.coupon.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND),
    COUPON_EXHAUSTED(HttpStatus.CONFLICT),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT),
    DUPLICATE_COUPON_CODE(HttpStatus.CONFLICT),
    INVALID_COUNTRY(HttpStatus.FORBIDDEN),
    GEOLOCATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
