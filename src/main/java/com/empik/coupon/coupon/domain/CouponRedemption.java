package com.empik.coupon.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted record of a single coupon redemption.
 *
 * <p>The {@code (coupon_id, user_id)} pair is unique at the database level, which is the
 * authoritative guard against the "same user redeems twice" race condition.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemption extends AbstractPersistableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "coupon_id", nullable = false, updatable = false)
    private UUID couponId;

    @Column(name = "user_id", nullable = false, length = 128, updatable = false)
    private String userId;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Instant redeemedAt;

    @Column(name = "ip_address", nullable = false, length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "country", nullable = false, length = 2, updatable = false)
    private String country;

    private CouponRedemption(UUID couponId, String userId, String ipAddress, String country) {
        this.id = UUID.randomUUID();
        this.couponId = couponId;
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.country = country;
        this.redeemedAt = Instant.now();
    }

    public static CouponRedemption of(UUID couponId, String userId, String ipAddress, String country) {
        return new CouponRedemption(
                Objects.requireNonNull(couponId, "couponId"),
                Objects.requireNonNull(userId, "userId"),
                Objects.requireNonNull(ipAddress, "ipAddress"),
                Objects.requireNonNull(country, "country").toUpperCase()
        );
    }

    @Override
    public UUID getId() {
        return id;
    }
}
