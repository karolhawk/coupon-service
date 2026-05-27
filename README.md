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
