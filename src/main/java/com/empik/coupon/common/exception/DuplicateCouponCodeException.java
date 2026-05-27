package com.empik.coupon.common.exception;

public class DuplicateCouponCodeException extends CouponDomainException {
    public DuplicateCouponCodeException(String code) {
        super(ErrorCode.DUPLICATE_COUPON_CODE,
                "Coupon with code '" + code + "' already exists");
    }
}
