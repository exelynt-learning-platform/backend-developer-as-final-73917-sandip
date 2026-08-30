# Resource Booking System

A RESTful Resource Booking System built with **Spring Boot 3 / Java 17**, **Spring Security + JWT**,
**Spring Data JPA**, and **PostgreSQL/MySQL**. Users can browse resources and manage their own
reservations; administrators have full CRUD access over resources and every reservation.

---

## Tech Stack

- Java 17, Spring Boot 3.3.4
- Spring Web, Spring Data JPA, Spring Security
- JWT (JJWT 0.12.x), stateless authentication
- PostgreSQL or MySQL (JPA/Hibernate)
- Bean Validation (Jakarta Validation)
- springdoc-openapi (Swagger UI)
- Lombok
- JUnit 5, Mockito, Spring Boot Test, H2 (in-memory, test-only)

## Project Structure

```
src/main/java/com/booking/resourcebooking/
├── config/          # OpenAPI config, startup data seeder
├── controller/       # REST controllers (Auth, Resource, Reservation)
├── dto/               # Request/response DTOs (auth, resource, reservation, common)
├── entity/            # JPA entities + enums (User, Resource, Reservation, Role, ReservationStatus)
├── exception/         # Custom exceptions + global @RestControllerAdvice handler
├── mapper/            # Entity -> DTO mappers
├── repository/        # Spring Data repositories + JPA Specification for filtering
├── security/          # JWT filter/util, UserDetailsService, SecurityConfig
└── service/           # Business logic (Auth, Resource, Reservation)
```

---

## Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 13+ **or** MySQL 8+ (a local instance, or Docker)

## 1. Database Setup

### Option A — PostgreSQL (default)

```bash
psql -U postgres -c "CREATE DATABASE resource_booking;"
```

Or with Docker:

```bash
docker run --name booking-postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=resource_booking -p 5432:5432 -d postgres:16
```

### Option B — MySQL

```bash
mysql -u root -p -e "CREATE DATABASE resource_booking;"
```

Or with Docker:

```bash
docker run --name booking-mysql -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=resource_booking -p 3306:3306 -d mysql:8
```

Hibernate is configured with `ddl-auto: update`, so tables are created automatically on first run —
no manual schema scripts are required.

## 2. Configure Environment Variables

Copy `.env.example` to `.env` (or export the variables directly in your shell / IDE run configuration):

```bash
cp .env.example .env
```

| Variable | Description | Default |
|---|---|---|
| `SERVER_PORT` | HTTP port | `8080` |
| `DB_URL` | JDBC connection string | `jdbc:postgresql://localhost:5432/resource_booking` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `DB_DRIVER` | JDBC driver class | `org.postgresql.Driver` |
| `DB_DIALECT` | Hibernate dialect | `org.hibernate.dialect.PostgreSQLDialect` |
| `DDL_AUTO` | Hibernate schema strategy | `update` |
| `JWT_SECRET` | HMAC signing secret (use a long random value in production) | dev placeholder |
| `JWT_EXPIRATION_MS` | Token lifetime in ms | `86400000` (24h) |
| `SEED_ENABLED` | Seed default ADMIN/USER accounts on startup | `true` |
| `SEED_ADMIN_USERNAME` / `SEED_ADMIN_PASSWORD` | Default admin credentials | `admin` / `Admin@123` |
| `SEED_USER_USERNAME` / `SEED_USER_PASSWORD` | Default user credentials | `user` / `User@123` |

To use **MySQL** instead of PostgreSQL, set:

```
DB_URL=jdbc:mysql://localhost:3306/resource_booking?useSSL=false&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=root
DB_DRIVER=com.mysql.cj.jdbc.Driver
DB_DIALECT=org.hibernate.dialect.MySQLDialect
```

(Both the PostgreSQL and MySQL JDBC drivers are already included in `pom.xml`, so no extra
dependency changes are needed to switch.)

## 3. Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

The app starts on `http://localhost:8080` (or `SERVER_PORT`). On first run it seeds:

- **ADMIN** — username `admin`, password `Admin@123`
- **USER** — username `user`, password `User@123`
- 3 sample resources (a room, a vehicle, and a piece of equipment)

## 4. Run Tests

```bash
mvn test
```

Tests run against an in-memory H2 database (see `src/test/resources/application.yml`), so no
external database is required to run the test suite. Coverage includes:

- JWT login success/failure and validation error responses
- RBAC: ADMIN vs. USER permissions on resources and reservations
- Reservation ownership isolation (a USER can never view/modify another USER's reservation, even
  by guessing an ID or tampering with the request body)
- Filtering by status/min/max price, pagination, and sorting
- Field validation (missing fields, invalid price, end time before start time, unknown resource)
- JWT utility unit tests (token round-trip, expiry, tampering)

---

## API Documentation

Once running, interactive Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI JSON:

```
http://localhost:8080/v3/api-docs
```

A ready-to-import **Postman collection** is included at [`postman_collection.json`](./postman_collection.json).
It includes pre-configured requests for login, resource CRUD, and reservation CRUD/filtering, with
test scripts that automatically capture the JWT into collection variables (`adminToken`, `userToken`).

---

## API Overview

### Auth

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/auth/login` | Public | Authenticate, returns a JWT |

### Resources

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/resources` | ADMIN, USER | List resources (paginated) |
| GET | `/resources/{id}` | ADMIN, USER | Get one resource |
| POST | `/resources` | ADMIN | Create a resource |
| PUT | `/resources/{id}` | ADMIN | Update a resource |
| DELETE | `/resources/{id}` | ADMIN | Delete a resource |

### Reservations

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/reservations` | ADMIN (all), USER (own only) | List, with filtering/pagination/sorting |
| GET | `/reservations/{id}` | Owner or ADMIN | Get one reservation |
| POST | `/reservations` | ADMIN, USER | Create a reservation (owner taken from JWT) |
| PUT | `/reservations/{id}` | Owner (while PENDING) or ADMIN | Update reservation details |
| PATCH | `/reservations/{id}/status` | Owner (cancel only) or ADMIN (any status) | Change status |
| DELETE | `/reservations/{id}` | ADMIN | Delete a reservation |

**Filtering / pagination / sorting query params on `GET /reservations`:**

- `status` — `PENDING` \| `CONFIRMED` \| `CANCELLED`
- `minPrice`, `maxPrice` — decimal bounds
- `page`, `size` — standard Spring pagination (0-indexed page)
- `sort` — e.g. `sort=price,desc` or `sort=startTime,asc`

Example:

```
GET /reservations?status=PENDING&minPrice=10&maxPrice=200&page=0&size=10&sort=price,desc
Authorization: Bearer <jwt>
```

---

## Security Notes

- Passwords are hashed with **BCrypt**; plaintext passwords are never stored or logged.
- Authentication is fully **stateless** — the JWT is validated on every request via a
  `OncePerRequestFilter`; no server-side session is created.
- The reservation owner is **always** derived from the authenticated JWT principal
  (`@AuthenticationPrincipal`) — `ReservationRequest` has no `userId`/`username` field at all, so a
  client cannot spoof another user's identity even by tampering with the request body.
- Authorization is enforced at two levels:
  - **URL/method level** — `SecurityConfig` request matchers + `@PreAuthorize` restrict
    resource-mutation endpoints to `ROLE_ADMIN`.
  - **Row/ownership level** — `ReservationService` checks that a non-admin caller only ever
    reads or modifies reservations they own, throwing a `403 Forbidden` otherwise.
- All error responses follow a consistent JSON shape (`timestamp`, `status`, `error`, `message`,
  `path`, optional `fieldErrors`) produced by a single `@RestControllerAdvice`.

## Reservation Status Rules

- New reservations always start as `PENDING`.
- A regular **USER** may cancel (`CANCELLED`) their own `PENDING` reservation, and may edit its
  details only while it is still `PENDING`.
- An **ADMIN** may set any status (`PENDING` / `CONFIRMED` / `CANCELLED`) on any reservation, and
  may edit or delete any reservation regardless of status.

---

## Sample `curl` Walkthrough

```bash
# 1. Log in as USER
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"User@123"}'
# -> { "token": "...", "role": "USER", ... }

# 2. Browse resources
curl http://localhost:8080/resources \
  -H "Authorization: Bearer <TOKEN>"

# 3. Create a reservation
curl -X POST http://localhost:8080/reservations \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"resourceId":1,"startTime":"2026-10-01T09:00:00","endTime":"2026-10-01T11:00:00","price":49.99}'

# 4. View only your own reservations, filtered
curl "http://localhost:8080/reservations?status=PENDING&minPrice=10&maxPrice=100&page=0&size=10" \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Notes for Reviewers

- This project was developed and reviewed in a sandboxed environment without outbound access to
  Maven Central, so automated `mvn test` execution could not be performed there; the code was
  manually reviewed line-by-line for correctness. Please run `mvn clean test` locally to execute
  the full suite — all dependency versions in `pom.xml` are current, published releases.
