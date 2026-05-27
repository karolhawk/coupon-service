package com.empik.coupon.common.exception;

public class CouponExhaustedException extends CouponDomainException {
    public CouponExhaustedException(String code) {
        super(ErrorCode.COUPON_EXHAUSTED, "Coupon '" + code + "' has reached its maximum number of uses");
    }
}
