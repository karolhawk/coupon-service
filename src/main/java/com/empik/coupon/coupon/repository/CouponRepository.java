package com.empik.coupon.coupon.repository;

import com.empik.coupon.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    /**
     * Case-insensitive lookup. The unique index on {@code LOWER(code)} keeps this O(log n).
     */
    @Query("SELECT c FROM Coupon c WHERE LOWER(c.code) = LOWER(:code)")
    Optional<Coupon> findByCodeIgnoreCase(@Param("code") String code);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE LOWER(c.code) = LOWER(:code)")
    long countByCodeIgnoreCase(@Param("code") String code);

    @Modifying
    @Query("""
            UPDATE Coupon c
               SET c.currentUses = c.currentUses + 1
             WHERE c.id = :id
               AND c.currentUses < c.maxUses
            """)
    int incrementUsageIfAvailable(@Param("id") UUID id);
}
