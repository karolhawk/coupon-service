package com.empik.coupon.common.exception;

public class InvalidCountryException extends CouponDomainException {
    public InvalidCountryException(String userCountry, String requiredCountry) {
        super(ErrorCode.INVALID_COUNTRY,
                "Coupon is restricted to country '" + requiredCountry
                        + "' but the request originated from '" + userCountry + "'");
    }
}
