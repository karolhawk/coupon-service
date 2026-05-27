package com.empik.coupon.common.exception;

public class CouponAlreadyUsedException extends CouponDomainException {
    public CouponAlreadyUsedException(String userId, String code) {
        super(ErrorCode.COUPON_ALREADY_USED,
                "User '" + userId + "' has already redeemed coupon '" + code + "'");
    }
}
