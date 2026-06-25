# baundang.id — Digital Wedding Invitation Platform

A production-ready SaaS platform for creating and managing digital wedding invitations. Built as a Java microservices architecture, it handles everything from template browsing and order processing to guest RSVPs, WhatsApp notifications, and payment collection.

[![CI](https://github.com/jeimandei/DigitalInvitationStore/actions/workflows/ci.yml/badge.svg)](https://github.com/jeimandei/DigitalInvitationStore/actions/workflows/ci.yml)

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Pricing Tiers](#pricing-tiers)
- [CI / CD](#ci--cd)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

---

## Architecture Overview

```
                        ┌──────────────────────────────────┐
                        │        baundang-network           │
                        │                                   │
  Browser / Client ────▶│  Gateway :1080                    │
                        │    │                              │
                        │    ├──▶ Storefront   :1082        │
                        │    ├──▶ Auth         :1081        │
                        │    ├──▶ Template     :1083        │
                        │    ├──▶ Invitation   :1084        │
                        │    ├──▶ Order        :1085        │
                        │    ├──▶ Payment      :1086        │
                        │    ├──▶ Notification :1087        │
                        │    ├──▶ Admin        :1088        │
                        │    └──▶ Media        :1089        │
                        │                                   │
                        │  Infrastructure                   │
                        │    PostgreSQL  :5432              │
                        │    Redis       :6379              │
                        │    RabbitMQ    :5672 / 15672      │
                        │    MinIO       :9000 / 9001       │
                        │    Config      :8888              │
                        └──────────────────────────────────┘
```

All services communicate on a Podman bridge network (`baundang-network`) with DNS resolution enabled. The API Gateway is the single public entry point; internal services are not exposed to the internet.

---

## Services

| Service | Port | Description |
|---|---|---|
| **gateway-service** | 1080 | Spring Cloud Gateway — routing, JWT auth, rate limiting (Redis) |
| **auth-service** | 1081 | User registration, login, RS256 JWT issuance, refresh tokens, admin seeding |
| **storefront-service** | 1082 | Public website — landing, template catalogue, order flow, buyer login/register (Thymeleaf + HTMX + Alpine.js) |
| **template-service** | 1083 | Template CRUD, MinIO presigned URLs, Bible verse catalogue |
| **invitation-service** | 1084 | Invitation lifecycle, RSVP, guestbook, gift registry (Thymeleaf view) |
| **order-service** | 1085 | Order creation, status machine, revision requests |
| **payment-service** | 1086 | Midtrans Snap payment, public webhook handler |
| **notification-service** | 1087 | WhatsApp (Fonnte), email (SMTP), RabbitMQ consumers, broadcast |
| **admin-service** | 1088 | Back-office web UI — orders, invitations, templates, CSV export, broadcast |
| **media-service** | 1089 | Client-side MinIO presigned upload/download |
| **config-server** | 8888 | Spring Cloud Config — centralised YAML for all services |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.3, Spring Cloud 2025.0.0 |
| Build | Maven (multi-module, parent POM) |
| API Gateway | Spring Cloud Gateway with RS256 JWT filter |
| Database | PostgreSQL 16 — single instance, one schema per service |
| Schema migration | Flyway (per-service, `create-schemas: true`) |
| ORM | Spring Data JPA / Hibernate |
| Cache | Redis 7 (Spring Cache, `@Cacheable` / `@CacheEvict`) |
| Messaging | RabbitMQ 3.13 — topic exchanges per domain |
| Object storage | MinIO (presigned PUT/GET for client-side uploads) |
| Frontend | Thymeleaf, HTMX, Alpine.js |
| Payments | Midtrans Snap |
| WhatsApp | Fonnte API (`@Retryable`, Guava `RateLimiter`) |
| Containers | Podman + podman-compose (rootless OCI images) |
| Observability | AOP service-layer logging (`ServiceLoggingAspect`) + request/response interceptor |
| Code quality | Checkstyle 10 (Google style, 120-char lines) |
| CI | GitHub Actions — build + test + checkstyle on PRs |
| CD | GitHub Actions — SSH deploy with per-service build/restart via checkboxes |

---

## Getting Started

### Prerequisites

- **Podman** ≥ 4.x and **podman-compose**
- **Java 21** (Temurin recommended) and **Maven 3.9+** for local builds
- A `.env` file at the repo root (copy from `.env.example`)

### 1. Clone and configure

```bash
git clone https://github.com/jeimandei/DigitalInvitationStore.git
cd DigitalInvitationStore
cp .env.example .env
# Edit .env — set real passwords, API keys, and SMTP credentials
```

### 2. Build all modules

```bash
mvn -q clean package -DskipTests
```

### 3. Start the stack

```bash
podman compose --env-file .env up -d
```

Services start in dependency order. The config-server must be healthy before application services start; RabbitMQ must be healthy before messaging services start (all enforced via `condition: service_healthy` in compose).

### 4. Verify

```bash
curl http://localhost:1080/actuator/health   # gateway
curl http://localhost:1082/                  # storefront landing page
```

### 5. Run tests

```bash
mvn clean verify
```

---

## Configuration

All service configuration is managed by **Spring Cloud Config Server** (`config-server`). Each service bootstraps with only its name and the config server URL; everything else is served from `config-repo/`.

```
config-repo/
  application.yml          # shared config (datasource pool, Redis, RabbitMQ, scheduling)
  auth-service.yml
  gateway-service.yml
  invitation-service.yml
  order-service.yml
  payment-service.yml
  notification-service.yml
  storefront-service.yml
  template-service.yml
  admin-service.yml
  media-service.yml
```

### Environment variables (`.env`)

| Variable | Description |
|---|---|
| `DB_USER` / `DB_PASSWORD` | PostgreSQL credentials |
| `REDIS_PASSWORD` | Redis auth password |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | RabbitMQ credentials |
| `RABBITMQ_VHOST` | RabbitMQ virtual host (default: `baundang`) |
| `RABBITMQ_ERLANG_COOKIE` | RabbitMQ cluster cookie |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | MinIO root credentials |
| `CONFIG_SERVER_USER` / `CONFIG_SERVER_PASSWORD` | Config server HTTP basic auth |
| `JWT_SECRET` | 256-bit base64 secret for RS256 key generation |
| `ADMIN_SEED_KEY` | Secret key for seeding the first admin account via `/api/v1/auth/register-admin` |
| `MIDTRANS_SERVER_KEY` / `MIDTRANS_CLIENT_KEY` | Midtrans payment gateway keys |
| `WHATSAPP_API_TOKEN` | Fonnte WhatsApp API token |
| `ADMIN_WHATSAPP` | Admin WhatsApp number for notifications |
| `EMAIL_HOST` / `EMAIL_PORT` / `EMAIL_USERNAME` / `EMAIL_PASSWORD` | SMTP settings |

See [`.env.example`](.env.example) for the full list with placeholder values.

### Database

A single PostgreSQL instance hosts all schemas in database `baundang`:

| Schema | Owner service |
|---|---|
| `auth` | auth-service |
| `template` | template-service |
| `invitation` | invitation-service |
| `orders` | order-service |
| `payment` | payment-service |
| `notification` | notification-service |
| `admin` | admin-service |
| `media` | media-service |

Each service connects with `currentSchema=<schema>` in the JDBC URL so Flyway and Hibernate are fully isolated. `max_connections` is set to 200; HikariCP pool is 5 per service.

### Seeding the first admin account

Once the stack is running, use the seed endpoint to create the initial admin:

```bash
curl -X POST https://<your-domain>/api/v1/auth/register-admin \
  -H "Content-Type: application/json" \
  -H "X-Admin-Seed-Key: <ADMIN_SEED_KEY value from .env>" \
  -d '{"email":"admin@baundang.id","password":"YourPassword123"}'
```

The returned `accessToken` can be used immediately. The admin UI is at `/admin/login`.

---

## API Reference

All public endpoints are exposed through the gateway on port **1080**.

### Auth — `/api/v1/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/login` | — | Email + password → JWT tokens |
| `POST` | `/register` | — | Create buyer account → JWT tokens |
| `GET` | `/public-key` | — | RS256 public key (PEM) for token verification |
| `POST` | `/token/refresh` | — | Exchange refresh token → new access token |
| `POST` | `/order-token` | — | Issue short-lived order-scoped token |
| `POST` | `/register-admin` | Seed key (`X-Admin-Seed-Key` header) | Create admin account |

### Templates — `/api/v1/templates`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/` | — | Paginated template list (filter by category, priceLevel) |
| `GET` | `/{slug}` | — | Template detail |
| `GET` | `/{slug}/preview` | — | Redirect to MinIO presigned preview URL |
| `POST` | `/` | Admin | Create template |
| `PUT` | `/{id}` | Admin | Update template |
| `DELETE` | `/{id}` | Admin | Soft-delete template |
| `GET` | `/christian/verses` | — | Bible verse catalogue |

### Orders — `/api/v1/orders`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/` | Buyer JWT | Create order |
| `GET` | `/{id}` | Buyer / Admin | Order detail |
| `GET` | `/` | Admin | Paginated order list |
| `PUT` | `/{id}/status` | Admin | Update order status |
| `POST` | `/{id}/revisions` | Buyer | Request design revision |
| `PUT` | `/revisions/{id}/complete` | Admin | Mark revision complete |
| `GET` | `/{id}/revisions` | Buyer / Admin | List revisions |

### Payments — `/api/v1/payments`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/charge` | Buyer JWT | Create Midtrans Snap charge |
| `GET` | `/snap-token/{orderId}` | Buyer JWT | Get Snap token for existing order |
| `POST` | `/webhook/midtrans` | — (public) | Midtrans payment notification webhook |
| `POST` | `/gifts/charge` | — | Gift payment charge |

### Invitations — `/api/v1/invitations`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/{slug}/rsvp` | — | Submit RSVP |
| `GET` | `/{slug}/guestbook` | — | List approved guestbook entries |
| `POST` | `/{slug}/guestbook` | — | Submit guestbook message |
| `GET` | `/{slug}/events` | — | List wedding events |
| `GET` | `/{slug}/gift-accounts` | — | Get gift registry info |
| `POST` | `/{slug}/gift-confirm` | — | Confirm gift transfer |
| `GET` | `/{slug}/checkin/{code}` | — | Guest check-in lookup |
| `POST` | `/{slug}/checkin/{code}` | — | Mark guest checked in |

Invitation pages are rendered server-side at `/i/{slug}`.

### Media — `/api/v1/media`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/upload/presign` | Buyer JWT | Request presigned PUT URL for direct MinIO upload |
| `GET` | `/download/**` | Buyer JWT | Request presigned GET URL |
| `DELETE` | `/**` | Admin JWT | Delete object |
| `POST` | `/template/upload` | Admin JWT | Server-side template asset upload |

### Notifications — `/api/v1/notifications`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/broadcast` | Admin JWT | Broadcast WhatsApp message (`ALL_ACTIVE`, `EXPIRING_7D`) |

### Storefront pages

| Path | Description |
|---|---|
| `/` | Landing page |
| `/templates` | Template catalogue |
| `/templates/{slug}` | Template detail |
| `/pesan` | Order flow (package selection → details → confirmation) |
| `/masuk` | Buyer login (JWT stored in sessionStorage, supports `?redirect=`) |
| `/daftar` | Buyer registration |
| `/tentang` | About page |
| `/admin/login` | Admin login |
| `/admin` | Admin back-office (requires admin JWT) |
| `/i/{slug}` | Invitation viewer |

---

## Pricing Tiers

| Tier | Price | Highlights |
|---|---|---|
| **Dasar** | Rp 119.000 | 1 template, event info, RSVP, unique link |
| **Standar** ⭐ | Rp 199.000 | All templates, guestbook, gallery, countdown, interactive map |
| **Premium** | Rp 249.000 | All Standar features + music, gift registry, 2 revisions, priority support |

---

## CI / CD

### Continuous Integration (`.github/workflows/ci.yml`)

Runs on every pull request targeting `develop` or `main`:

1. **Build & Test** — `mvn clean verify`
2. **Checkstyle** — `mvn checkstyle:check` (Google style, 120-char limit)
3. Surefire reports uploaded as CI artifacts on failure

### Continuous Deployment (`.github/workflows/cd.yml`)

`workflow_dispatch` with per-service checkboxes:

- **build-service** — SSH in, pull latest, build selected services with Maven, rebuild container images, restart containers
- **restart-service** — SSH in, restart selected service containers without rebuilding
- **deploy-config** — SSH in, signal config-server to reload YAML from `config-repo/`
- **full-deploy** — Full stack teardown and fresh `podman compose up -d`

#### Required GitHub Secrets

| Secret | Value |
|---|---|
| `SSH_KEY` | Private SSH key for the deploy user |
| `SSH_USER` | Deploy user on the server |
| `SSH_HOST` | Server hostname or IP |
| `SSH_PORT` | SSH port |
| `DEPLOY_DIR` | Deployment directory (e.g. `/opt/baundang`) |

---

## Project Structure

```
DigitalInvitationStore/
├── pom.xml                   # Parent POM — dependency management, Checkstyle
├── common/                   # Shared library: ApiResponse, ServiceLoggingAspect, exceptions
├── config-server/            # Spring Cloud Config Server
├── config-repo/              # YAML configuration files for all services
├── gateway-service/          # API Gateway — routing, JWT filter, rate limiting
├── auth-service/             # Authentication & JWT issuance
├── storefront-service/       # Public-facing website (Thymeleaf + HTMX + Alpine.js)
├── template-service/         # Wedding template catalogue
├── invitation-service/       # Digital invitation pages and interactions
├── order-service/            # Order lifecycle management
├── payment-service/          # Payment processing (Midtrans)
├── notification-service/     # WhatsApp + email notifications
├── admin-service/            # Internal back-office web UI
├── media-service/            # Object storage (MinIO) proxy
├── checkstyle/
│   └── checkstyle.xml        # Checkstyle rules (Google style)
├── podman-compose.yml        # Full stack orchestration
└── .env.example              # Environment variable template
```

---

## Contributing

1. Fork the repository and create a feature branch off `develop`
2. Write code that passes `mvn clean verify` (build + tests + Checkstyle)
3. Open a pull request against `develop` — CI runs automatically
4. Merges to `main` trigger automatic deployment to production

### Code Style

This project enforces [Google Java Style](https://google.github.io/styleguide/javaguide.html) with two project-level adjustments:

- **Line length**: 120 characters (Google default is 100)
- **Indentation**: 4 spaces (Google uses 2)

Run `mvn checkstyle:check` locally before pushing.

---

## License

[MIT](LICENSE)
