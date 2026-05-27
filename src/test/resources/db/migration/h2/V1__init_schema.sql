-- Coupons table
CREATE TABLE coupons (
    id            UUID         PRIMARY KEY,
    code          VARCHAR(64)  NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now(),
    max_uses      INTEGER      NOT NULL CHECK (max_uses > 0),
    current_uses  INTEGER      NOT NULL DEFAULT 0 CHECK (current_uses >= 0),
    country_code  VARCHAR(2)   NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT current_uses_not_exceeding_max CHECK (current_uses <= max_uses)
);

-- H2 does not support function-based indexes; the application always uppercases codes on write.
CREATE UNIQUE INDEX ux_coupons_code_lower ON coupons (code);

CREATE TABLE coupon_redemptions (
    id          UUID         PRIMARY KEY,
    coupon_id   UUID         NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    user_id     VARCHAR(128) NOT NULL,
    redeemed_at TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now(),
    ip_address  VARCHAR(45)  NOT NULL,
    country     VARCHAR(2)   NOT NULL,
    CONSTRAINT ux_redemption_per_user UNIQUE (coupon_id, user_id)
);

CREATE INDEX ix_redemption_coupon ON coupon_redemptions (coupon_id);
CREATE INDEX ix_redemption_user   ON coupon_redemptions (user_id);
