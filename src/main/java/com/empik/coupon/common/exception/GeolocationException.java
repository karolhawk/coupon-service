package com.empik.coupon.common.exception;

public class GeolocationException extends CouponDomainException {
    public GeolocationException(String ip, Throwable cause) {
        super(ErrorCode.GEOLOCATION_UNAVAILABLE,
                "Could not determine country for IP '" + ip + "': " + cause.getMessage());
        initCause(cause);
    }

    public GeolocationException(String message) {
        super(ErrorCode.GEOLOCATION_UNAVAILABLE, message);
    }
}
