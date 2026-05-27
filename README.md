# Coupon Service

REST service for managing discount coupons. Built as a recruitment assignment.

**Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL · Flyway · Maven

### Scope

The implementation covers exactly what the requirements specify: **creating coupons** and
**redeeming coupons**. No extra endpoints were added. In the future, we could add other endpoints 
like get coupons for admin purposes.

One small addition beyond the literal requirements: HTTP Basic Auth is required to create a
coupon (`ADMIN` role). Exposing coupon creation without any authentication would be a
security issue in any real-world system, and adding it is a single configuration class with
no impact on the redemption flow, which remains fully public.

---

## Running the application

### Docker Compose (recommended)

```bash
docker compose up --build
```

Application: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

> `COUPON_FALLBACK_COUNTRY=PL` in `docker-compose.yml` makes requests from localhost
> (which have no routable IP for geolocation) resolve to Poland. Leave it empty in production.

### Local Maven

Requires a running PostgreSQL instance (or `docker compose up postgres`):

```bash
mvn spring-boot:run
```

### Tests

```bash
mvn test
```

Integration tests use an in-memory H2 database — no Docker required.

---

## API

Full OpenAPI documentation is available at `http://localhost:8080/swagger-ui.html`.

### Create a coupon — `POST /api/v1/coupons`

Requires HTTP Basic Auth with the `ADMIN` role (`admin / admin` by default, configurable via
`ADMIN_USERNAME` / `ADMIN_PASSWORD` environment variables).

```bash
curl -u admin:admin -X POST http://localhost:8080/api/v1/coupons \
  -H 'Content-Type: application/json' \
  -d '{"code":"SUMMER2026","maxUses":100,"countryCode":"PL"}'
```

Response `201 Created`:

```json
{
  "id": "...",
  "code": "SUMMER2026",
  "createdAt": "2026-05-24T12:00:00Z",
  "maxUses": 100,
  "currentUses": 0,
  "countryCode": "PL"
}
```

### Redeem a coupon — `POST /api/v1/coupons/{code}/redemptions`

Public endpoint — no authentication required.

```bash
curl -X POST http://localhost:8080/api/v1/coupons/SUMMER2026/redemptions \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user-42"}'
```

Response `200 OK`:

```json
{
  "code": "SUMMER2026",
  "userId": "user-42",
  "redeemedAt": "2026-05-24T12:01:00Z",
  "remainingUses": 99
}
```

### Error codes

All errors come back in the same shape (`ApiError`) so clients have a consistent contract.

| HTTP | `code`                    | When                                                         |
|------|---------------------------|--------------------------------------------------------------|
| 400  | `VALIDATION_ERROR`        | Bad request body or malformed JSON                           |
| 403  | `INVALID_COUNTRY`         | Client's IP doesn't match the coupon's allowed countries     |
| 404  | `COUPON_NOT_FOUND`        | No coupon with that code                                     |
| 409  | `COUPON_EXHAUSTED`        | `maxUses` limit already reached                              |
| 409  | `COUPON_ALREADY_USED`     | This user already redeemed this coupon                       |
| 409  | `DUPLICATE_COUPON_CODE`   | A coupon with that code already exists (case-insensitive)    |
| 503  | `GEOLOCATION_UNAVAILABLE` | Couldn't determine the client's country from their IP        |
| 500  | `INTERNAL_ERROR`          | Something unexpected broke                                   |

---

## How it's structured

Feature-based packages (`coupon`, `geolocation`, `common`) rather than the classic
controller/service/repository split. Each feature owns its full vertical slice.

```
src/main/java/com/empik/coupon
├── config/               ← Security, OpenAPI
├── common/
│   ├── exception/        ← typed domain exceptions + ErrorCode enum
│   └── web/              ← ApiError, GlobalExceptionHandler, ClientIpResolver
├── coupon/
│   ├── api/              ← controller + DTOs
│   ├── domain/           ← JPA entities
│   ├── repository/       ← Spring Data JPA
│   └── service/          ← business logic lives here
└── geolocation/          ← interface + two implementations (ip-api, static)
```

A few other choices worth mentioning:
- Entities never leak into the HTTP layer — always mapped to DTOs first
- Constructor injection everywhere, no field `@Autowired`

---

## The concurrency part

Two race conditions needed solving.

### Problem 1 — multiple users grabbing the last slot

If ten users all hit redeem at the same millisecond on a coupon with one slot left,
a naive "read → check → increment" approach lets all ten through. Classic race condition.

The fix is a single atomic statement in the database:

```sql
UPDATE coupons
   SET current_uses = current_uses + 1
 WHERE id = :id
   AND current_uses < max_uses
```

The database row lock serialises everything. If the update touches zero rows, the coupon
was already full — `409 COUPON_EXHAUSTED`. No application-level check needed.

### Problem 2 — same user redeeming twice concurrently

Two requests from the same user can both pass the "already used?" check before either
writes a redemption record. The real guard is the database:

```sql
UNIQUE (coupon_id, user_id)
```

The app-level check (`existsByCouponIdAndUserId`) is just an optimisation to avoid burning
a usage slot on a request that the DB will reject anyway. If a race slips past it, the
`DataIntegrityViolationException` from the insert is caught and translated to
`409 COUPON_ALREADY_USED` — and the counter increment rolls back atomically with the failed insert.

---

## Geolocation

The `GeolocationService` interface has two implementations, selected via `coupon.geolocation.provider`:

- **`ip-api`** (default) — calls `http://ip-api.com/json/{ip}`, free tier is 45 req/min
- **`static`** — always returns the same country; used in tests to avoid network calls

Local and private IPs (like `127.0.0.1` from local curl) fall back to `COUPON_FALLBACK_COUNTRY`
if it's set, otherwise the service throws `503 GEOLOCATION_UNAVAILABLE`. The fallback is
intentionally left empty for production.

`ClientIpResolver` reads `X-Forwarded-For` to get the real client IP when running behind a proxy.

