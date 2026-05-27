package com.empik.coupon.common.exception;

public abstract class CouponDomainException extends RuntimeException {

    private final ErrorCode errorCode;

    protected CouponDomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
