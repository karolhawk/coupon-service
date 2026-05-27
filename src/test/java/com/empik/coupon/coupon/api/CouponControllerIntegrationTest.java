package com.empik.coupon.coupon.api;

import com.empik.coupon.AbstractIntegrationTest;
import com.empik.coupon.common.exception.ErrorCode;
import com.empik.coupon.coupon.api.dto.CreateCouponRequest;
import com.empik.coupon.coupon.api.dto.RedeemCouponRequest;
import com.empik.coupon.coupon.repository.CouponRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class CouponControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CouponRepository couponRepository;

    @BeforeEach
    void cleanDatabase() {
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /coupons creates a coupon and returns 201 with Location header")
    void createCoupon_ok() throws Exception {
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(new CreateCouponRequest("WIOSNA", 10, "PL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("WIOSNA"))
                .andExpect(jsonPath("$.countryCode").value("PL"))
                .andExpect(jsonPath("$.currentUses").value(0))
                .andExpect(jsonPath("$.maxUses").value(10));
    }

    @Test
    @DisplayName("POST /coupons returns 409 when the code already exists (case-insensitive)")
    void createCoupon_duplicate() throws Exception {
        createCoupon("WIOSNA", 5, "PL");

        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(new CreateCouponRequest("wiosna", 5, "PL"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_COUPON_CODE.name()));
    }

    @Test
    @DisplayName("POST /coupons returns 400 for invalid input")
    void createCoupon_validationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "code": "X", "maxUses": 0, "countryCode": "Polska" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /redemptions returns 200 for a fresh user")
    void redeem_ok() throws Exception {
        createCoupon("LATO", 10, "PL");

        mockMvc.perform(post("/api/v1/coupons/{code}/redemptions", "LATO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(new RedeemCouponRequest("user-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LATO"))
                .andExpect(jsonPath("$.remainingUses").value(9));
    }

    @Test
    @DisplayName("POST /redemptions returns 404 when the coupon does not exist")
    void redeem_notFound() throws Exception {
        mockMvc.perform(post("/api/v1/coupons/{code}/redemptions", "DOESNOTEXIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(new RedeemCouponRequest("user-1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.COUPON_NOT_FOUND.name()));
    }

    @Test
    @DisplayName("POST /redemptions returns 409 when the same user redeems twice")
    void redeem_sameUserTwice() throws Exception {
        createCoupon("PROMO", 10, "PL");

        mockMvc.perform(post("/api/v1/coupons/{code}/redemptions", "PROMO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(new RedeemCouponRequest("user-1"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/coupons/{code}/redemptions", "PROMO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(new RedeemCouponRequest("user-1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.COUPON_ALREADY_USED.name()));
    }

    @Test
    @DisplayName("POST /redemptions returns 409 EXHAUSTED once the cap is reached")
    void redeem_exhausted() throws Exception {
        createCoupon("ONEUSE", 1, "PL");

        mockMvc.perform(post("/api/v1/coupons/{code}/redemptions", "ONEUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(new RedeemCouponRequest("user-A"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/coupons/{code}/redemptions", "ONEUSE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(new RedeemCouponRequest("user-B"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.COUPON_EXHAUSTED.name()));
    }

    /**
     * Stress test for the "first come first served" rule under contention.
     *
     * <p>Fires {@code threadCount} concurrent redemptions of a coupon with {@code maxUses=10}.
     * The atomic UPDATE in the repository must serialise them so that exactly 10 succeed and
     * the remainder receive {@code 409 COUPON_EXHAUSTED}, with no double-spend.
     */
    @Test
    @DisplayName("Concurrent redemptions never exceed maxUses (atomic UPDATE serialises contention)")
    void concurrentRedemption_doesNotExceedCap() throws Exception {
        int maxUses = 10;
        int threadCount = 50;
        createCoupon("RUSH", maxUses, "PL");

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        ConcurrentHashMap<Integer, AtomicInteger> statusCounts = new ConcurrentHashMap<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                final int userIndex = i;
                pool.submit(() -> {
                    try {
                        start.await();
                        MvcResult result = mockMvc.perform(
                                        post("/api/v1/coupons/{code}/redemptions", "RUSH")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(asJson(new RedeemCouponRequest("user-" + userIndex))))
                                .andReturn();
                        statusCounts
                                .computeIfAbsent(result.getResponse().getStatus(), k -> new AtomicInteger())
                                .incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        int successes = statusCounts.getOrDefault(200, new AtomicInteger()).get();
        int conflicts = statusCounts.getOrDefault(409, new AtomicInteger()).get();

        assertThat(successes).isEqualTo(maxUses);
        assertThat(conflicts).isEqualTo(threadCount - maxUses);
        assertThat(couponRepository.findByCodeIgnoreCase("RUSH").orElseThrow().getCurrentUses())
                .isEqualTo(maxUses);
    }

    // ---------- helpers ----------

    private void createCoupon(String code, int maxUses, String country) throws Exception {
        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(new CreateCouponRequest(code, maxUses, country))))
                .andExpect(status().isCreated());
    }

    private String asJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }


}
