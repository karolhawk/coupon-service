package com.empik.coupon.common.exception;

public class CouponNotFoundException extends CouponDomainException {
    public CouponNotFoundException(String code) {
        super(ErrorCode.COUPON_NOT_FOUND, "Coupon with code '" + code + "' was not found");
    }
}
