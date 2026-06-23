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
  Browser / Client ────▶│  Gateway :8080                    │
                        │    │                              │
                        │    ├──▶ Storefront   :8082        │
                        │    ├──▶ Auth         :8081        │
                        │    ├──▶ Template     :8083        │
                        │    ├──▶ Invitation   :8084        │
                        │    ├──▶ Order        :8085        │
                        │    ├──▶ Payment      :8086        │
                        │    ├──▶ Notification :8087        │
                        │    ├──▶ Admin        :8088        │
                        │    └──▶ Media        :8089        │
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
| **gateway-service** | 8080 | Spring Cloud Gateway — routing, JWT auth, rate limiting (Redis) |
| **auth-service** | 8081 | User registration, login, RS256 JWT issuance, refresh tokens |
| **storefront-service** | 8082 | Public landing page, template catalogue, pricing (Thymeleaf + HTMX) |
| **template-service** | 8083 | Template CRUD, MinIO presigned URLs, Bible verse catalogue |
| **invitation-service** | 8084 | Invitation lifecycle, RSVP, guestbook, gift registry (Thymeleaf view) |
| **order-service** | 8085 | Order creation, status machine, revision requests |
| **payment-service** | 8086 | Midtrans Snap payment, webhook handling |
| **notification-service** | 8087 | WhatsApp (Fonnte), email (SMTP), RabbitMQ consumers, broadcast |
| **admin-service** | 8088 | Back-office web UI — orders, invitations, templates, CSV export |
| **media-service** | 8089 | Client-side MinIO presigned upload/download |
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
| Code quality | Checkstyle 10 (Google style, 120-char lines) |
| CI | GitHub Actions — build + test + checkstyle on PRs |
| CD | GitHub Actions — SSH deploy on push to `main` |

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
podman compose -f podman-compose.yml up -d
```

Services start in dependency order. The config-server must be healthy before application services start (health-check enforced in compose).

### 4. Verify

```bash
curl http://localhost:8080/actuator/health   # gateway
curl http://localhost:8082/                  # storefront landing page
```

### 5. Run tests

```bash
mvn clean verify
```

All 88 `@WebMvcTest` unit tests cover every controller across all 9 application services.

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
```

### Environment variables (`.env`)

| Variable | Description |
|---|---|
| `DB_USER` / `DB_PASSWORD` | PostgreSQL credentials |
| `REDIS_PASSWORD` | Redis auth password |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | RabbitMQ credentials |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | MinIO root credentials |
| `CONFIG_SERVER_USER` / `CONFIG_SERVER_PASSWORD` | Config server HTTP basic auth |
| `JWT_SECRET` | 256-bit base64 secret for RS256 key generation |
| `MIDTRANS_SERVER_KEY` / `MIDTRANS_CLIENT_KEY` | Midtrans payment gateway keys |
| `WHATSAPP_API_TOKEN` | Fonnte WhatsApp API token |
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

---

## API Reference

All public endpoints are exposed through the gateway on port **8080**.

### Auth — `/api/v1/auth`

| Method | Path | Description |
|---|---|---|
| `POST` | `/login` | Email + password → JWT tokens |
| `POST` | `/register` | Create buyer account → JWT tokens |
| `GET` | `/public-key` | RS256 public key (PEM) for token verification |
| `POST` | `/token/refresh` | Exchange refresh token → new access token |
| `POST` | `/order-token` | Issue short-lived order-scoped token |

### Templates — `/api/v1/templates`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/` | — | Paginated template list (filter by category, priceLevel) |
| `GET` | `/{slug}` | — | Template detail |
| `GET` | `/{slug}/preview` | — | Redirect to MinIO presigned preview URL |
| `POST` | `/` | Admin | Create template |
| `PUT` | `/{id}` | Admin | Update template |
| `DELETE` | `/{id}` | Admin | Soft-delete template |
| `GET` | `/christian/verses` | — | Bible verse catalogue (filter by translation, category) |

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

| Method | Path | Description |
|---|---|---|
| `POST` | `/charge` | Create Midtrans Snap charge |
| `GET` | `/snap-token/{orderId}` | Get Snap token for existing order |
| `POST` | `/webhook/midtrans` | Midtrans payment notification webhook |

### Invitations — `/api/v1/invitations`

| Method | Path | Description |
|---|---|---|
| `POST` | `/{slug}/rsvp` | Submit RSVP |
| `GET` | `/{slug}/guestbook` | List approved guestbook entries |
| `POST` | `/{slug}/guestbook` | Submit guestbook message |
| `GET` | `/{slug}/events` | List wedding events |
| `GET` | `/{slug}/gift-accounts` | Get gift registry info |
| `POST` | `/{slug}/gift-confirm` | Confirm gift transfer |

Invitation pages are rendered server-side at `/i/{slug}`.

### Media — `/api/v1/media`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/upload/presign` | Buyer | Request presigned PUT URL for direct MinIO upload |
| `GET` | `/download/**` | Buyer | Request presigned GET URL |
| `DELETE` | `/**` | Admin | Delete object |
| `POST` | `/template/upload` | Admin | Server-side template asset upload |

### Notifications — `/api/v1/notifications`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/broadcast` | Internal | Broadcast WhatsApp message (`ALL_ACTIVE`, `EXPIRING_7D`) |

### Admin UI

The admin back-office is a Thymeleaf web application accessible via the gateway under `/admin`. It provides order management, invitation approval, template toggling, guestbook moderation, revision handling, WhatsApp broadcast, and CSV export.

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

1. **Build & Test** — `mvn clean verify` (Java 25 / Temurin on the runner)
2. **Checkstyle** — `mvn checkstyle:check` (Google style, 120-char limit)
3. Surefire reports uploaded as CI artifacts on failure

### Continuous Deployment (`.github/workflows/cd.yml`)

Triggered on push to `main`:

1. SSH into the production server using `webfactory/ssh-agent`
2. `git pull origin main`
3. `mvn -q -DskipTests clean package`
4. `podman compose pull && podman compose up -d --remove-orphans`
5. `podman image prune -f`

#### Required GitHub Secrets

| Secret | Value |
|---|---|
| `SSH_KEY` | Private SSH key for the deploy user |
| `SSH_USER` | Deploy user on the server |
| `SSH_URL` | Server hostname or IP |
| `SSH_PATH` | Deployment directory (e.g. `/opt/baundang`) |

#### First-time server provisioning

```bash
SSH_PATH=/opt/baundang \
REPO_URL=git@github.com:jeimandei/DigitalInvitationStore.git \
  bash docs/server-setup.sh
```

This installs Java, Maven, Podman, clones the repo, and starts the initial stack.

---

## Project Structure

```
DigitalInvitationStore/
├── pom.xml                   # Parent POM — dependency management, Checkstyle
├── common/                   # Shared library: ApiResponse, PagedResponse, exceptions
├── config-server/            # Spring Cloud Config Server
├── config-repo/              # YAML configuration files for all services
├── gateway-service/          # API Gateway — routing, JWT filter, rate limiting
├── auth-service/             # Authentication & JWT issuance
├── storefront-service/       # Public-facing website (Thymeleaf + HTMX)
├── template-service/         # Wedding template catalogue
├── invitation-service/       # Digital invitation pages and interactions
├── order-service/            # Order lifecycle management
├── payment-service/          # Payment processing (Midtrans)
├── notification-service/     # WhatsApp + email notifications
├── admin-service/            # Internal back-office web UI
├── media-service/            # Object storage (MinIO) proxy
├── checkstyle/
│   └── checkstyle.xml        # Checkstyle rules (Google style)
├── docs/
│   └── server-setup.sh       # One-time server provisioning script
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
