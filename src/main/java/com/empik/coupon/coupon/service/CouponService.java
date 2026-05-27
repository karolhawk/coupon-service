package com.empik.coupon.coupon.service;

import com.empik.coupon.common.exception.CouponAlreadyUsedException;
import com.empik.coupon.common.exception.CouponExhaustedException;
import com.empik.coupon.common.exception.CouponNotFoundException;
import com.empik.coupon.common.exception.DuplicateCouponCodeException;
import com.empik.coupon.common.exception.InvalidCountryException;
import com.empik.coupon.coupon.domain.Coupon;
import com.empik.coupon.coupon.domain.CouponRedemption;
import com.empik.coupon.coupon.repository.CouponRedemptionRepository;
import com.empik.coupon.coupon.repository.CouponRepository;
import com.empik.coupon.geolocation.GeolocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for coupon lifecycle operations.
 *
 * <p><b>Concurrency — max uses cap:</b> enforced by an atomic
 * {@code UPDATE ... WHERE current_uses < max_uses} in
 * {@link CouponRepository#incrementUsageIfAvailable}. The DB row lock serialises concurrent
 * attempts; zero rows updated means the cap was hit ({@link CouponExhaustedException}).
 *
 * <p><b>Concurrency — one redemption per user:</b> the {@code UNIQUE (coupon_id, user_id)}
 * constraint is the authoritative guard. The application-level pre-check is an optimisation
 * that avoids burning a usage slot on a request that will fail anyway.
 *
 * <p>Both mutations happen inside a single {@code @Transactional} boundary, so a failed
 * redemption insert rolls back the counter increment automatically.
 */
@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;
    private final GeolocationService geolocationService;

    public CouponService(CouponRepository couponRepository,
                         CouponRedemptionRepository redemptionRepository,
                         GeolocationService geolocationService) {
        this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository;
        this.geolocationService = geolocationService;
    }

    @Transactional
    public Coupon createCoupon(String code, int maxUses, String countryCode) {
        if (couponRepository.countByCodeIgnoreCase(code) > 0) {
            throw new DuplicateCouponCodeException(code);
        }
        Coupon coupon = Coupon.create(code, maxUses, countryCode);
        try {
            Coupon saved = couponRepository.save(coupon);
            log.info("Created coupon code={} country={} maxUses={}", saved.getCode(),
                    saved.getCountryCode(), saved.getMaxUses());
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCouponCodeException(code);
        }
    }

    /**
     * Redeems a coupon on behalf of a user.
     *
     * <p>Country validation happens before any mutation so no usage slot is consumed on a
     * country mismatch. The application-level "already used" check short-circuits the common
     * retry case without touching the counter; the DB unique constraint is the authoritative
     * guard for the concurrent case.
     */
    @Transactional
    public RedemptionResult redeem(String rawCode, String userId, String ipAddress) {
        String userCountry = geolocationService.resolveCountry(ipAddress);

        Coupon coupon = couponRepository.findByCodeIgnoreCase(rawCode)
                .orElseThrow(() -> new CouponNotFoundException(rawCode));

        if (!coupon.isUsableFrom(userCountry)) {
            throw new InvalidCountryException(userCountry, coupon.getCountryCode());
        }

        if (redemptionRepository.existsByCouponIdAndUserId(coupon.getId(), userId)) {
            throw new CouponAlreadyUsedException(userId, coupon.getCode());
        }

        int updated = couponRepository.incrementUsageIfAvailable(coupon.getId());
        if (updated == 0) {
            throw new CouponExhaustedException(coupon.getCode());
        }

        CouponRedemption redemption = CouponRedemption.of(coupon.getId(), userId, ipAddress, userCountry);
        try {
            redemptionRepository.saveAndFlush(redemption);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request from the same user beat us to the insert between our exists()
            // check and now. The transaction rollback undoes the counter increment.
            throw new CouponAlreadyUsedException(userId, coupon.getCode());
        }

        int remaining = coupon.getMaxUses() - (coupon.getCurrentUses() + 1);
        log.info("Redeemed coupon code={} user={} country={} remaining={}",
                coupon.getCode(), userId, userCountry, remaining);

        return new RedemptionResult(coupon.getCode(), userId, redemption.getRedeemedAt(), remaining);
    }

    public record RedemptionResult(String code, String userId, java.time.Instant redeemedAt, int remainingUses) {}
}
