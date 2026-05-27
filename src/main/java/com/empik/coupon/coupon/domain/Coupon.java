package com.empik.coupon.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Coupon aggregate root.
 *
 * <p>The {@code code} field is stored in upper-case form. Case-insensitive uniqueness is enforced
 * by a {@code UNIQUE INDEX ON LOWER(code)} at the database level.
 *
 * <p>Concurrency: incrementing {@code currentUses} is performed by an atomic JPQL UPDATE
 * (see {@code CouponRepository#incrementUsageIfAvailable}). The {@code @Version} field is kept
 * as a defensive measure for any future mutation paths that go through {@code EntityManager.merge}.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "code"})
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "coupons")
public class Coupon extends AbstractPersistableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 64, updatable = false)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "max_uses", nullable = false, updatable = false)
    private int maxUses;

    @Column(name = "current_uses", nullable = false)
    private int currentUses;

    @Column(name = "country_code", nullable = false, length = 2, updatable = false)
    private String countryCode;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    private Coupon(UUID id, String code, Instant createdAt, int maxUses, String countryCode) {
        this.id = id;
        this.code = code;
        this.createdAt = createdAt;
        this.maxUses = maxUses;
        this.currentUses = 0;
        this.countryCode = countryCode;
    }

    public static Coupon create(String code, int maxUses, String countryCode) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(countryCode, "countryCode");
        if (maxUses <= 0) {
            throw new IllegalArgumentException("maxUses must be greater than 0");
        }
        if (countryCode.length() != 2) {
            throw new IllegalArgumentException("countryCode must be ISO 3166-1 alpha-2");
        }
        return new Coupon(
                UUID.randomUUID(),
                code.toUpperCase(),
                Instant.now(),
                maxUses,
                countryCode.toUpperCase()
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    public boolean isExhausted() {
        return currentUses >= maxUses;
    }

    public boolean isUsableFrom(String userCountry) {
        return countryCode.equalsIgnoreCase(userCountry);
    }

}
