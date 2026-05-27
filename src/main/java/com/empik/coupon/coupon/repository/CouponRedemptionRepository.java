package com.empik.coupon.coupon.repository;

import com.empik.coupon.coupon.domain.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

    boolean existsByCouponIdAndUserId(UUID couponId, String userId);
}
