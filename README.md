# baundang.id — Digital Wedding Invitation Platform

A production-ready SaaS platform for creating and managing digital wedding invitations. Built as a Java microservices architecture, it covers the full journey: browsing templates, ordering and paying online, filling a guided questionnaire, receiving a themed invitation page, managing guests with QR check-in, collecting RSVPs and digital gift envelopes (amplop), and running the whole operation from an admin back office.

[![CI](https://github.com/jeimandei/DigitalInvitationStore/actions/workflows/ci.yml/badge.svg)](https://github.com/jeimandei/DigitalInvitationStore/actions/workflows/ci.yml)

---

## Table of Contents

- [Feature Overview](#feature-overview)
- [Architecture Overview](#architecture-overview)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Order Lifecycle & Event Flow](#order-lifecycle--event-flow)
- [Invitation Themes](#invitation-themes)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Pricing Tiers](#pricing-tiers)
- [CI / CD](#ci--cd)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

---

## Feature Overview

### For couples (buyers)

- **Template catalogue** — browse active templates by category, view details, order straight from a template
- **Online ordering** — pick a package (Dasar / Standar / Premium), pay via **Midtrans Snap** (QRIS, GoPay, OVO, bank transfer); order status updates automatically from the payment webhook
- **Order tracking**
  - **Public tracker** (`/lacak`) — check any order by order number + email/WhatsApp, no login needed
  - **Pesanan Saya** (`/pesanan-saya`) — logged-in dashboard with a progress timeline (Dipesan → Dibayar → Dikerjakan → Selesai), pay-now link, and revision counters
- **Intake questionnaire** — after payment, a guided, **tier-aware** wizard collects everything needed to build the invitation (couple names, matrimony & reception details, colour palette, love story, gallery, premium extras). Questions are fully configurable by the admin.
- **Kelola Undangan** (`/pesanan/{orderId}/kelola`) — self-service portal for the couple:
  - **Tamu** — add/remove guests, copy personal invitation links, download per-guest QR codes
  - **RSVP** — see who confirmed attendance
  - **Kehadiran** — live check-in stats and attendance rate
  - **Amplop** — digital gift totals and sender list
  - **Buku Tamu** — moderate (approve) guestbook messages
- **Revisions** — request design revisions within the package quota (Premium: 2)
- **Email + WhatsApp notifications** at every step: order placed (with payment link), payment received, invitation ready (with shareable link), revision completed

### For wedding guests

- **Invitation page** (`/i/{slug}`) — server-rendered, mobile-first, with cover/open animation, couple profile, event details with embedded Google Maps, love story, countdown-style layout
- **Four visual themes** — each invitation renders in one of four distinct style presets (see [Invitation Themes](#invitation-themes))
- **Personal greeting** — links with `?to=Nama+Tamu` greet the guest by name on the cover
- **Optional PIN gate** — invitations can require a PIN before opening
- **RSVP** — confirm attendance with guest count and a message
- **Buku Tamu (guestbook)** — leave wishes; entries appear after approval
- **Kirim Amplop (digital gift)** — floating button opens a gift modal with a free-form amount (plus quick-pick chips); payment goes through Midtrans Snap, no account needed. A bank/e-wallet **gift registry** page (`/i/{slug}/gift`) is also available with manual transfer confirmation.
- **QR check-in** — each guest gets a unique QR code; door staff scan it at `/i/{slug}/scan` (html5-qrcode camera scanner with manual code fallback) which opens the check-in page showing name, group, table number and allotted seats, then records the actual head count

### For admins

- **Dashboard** — today's orders, revenue, invitations, buyers at a glance
- **Order management** — searchable/filterable order list, status machine (PENDING → PAID → IN_REVISION → COMPLETED / CANCELLED), internal notes, CSV export
- **Bangun Undangan** (invitation builder) — structured content editor beside the client's questionnaire answers, with one-click **"Isi dari Kuesioner"** to fill the form from intake answers; sets couple info, matrimony/reception details, love story, cover photo, maps, colour palette, music, gift registry, **theme preset**, **custom slug**, and **access PIN**; per-guest link helper with copy button
- **Guest management** — add guests with group/table/seat allocation, high-res printable QR codes (click to enlarge, download, print-all sheet)
- **RSVP / Attendance / Guestbook / Gifts** — per-invitation views mirroring the client portal, plus guestbook approval and gift-account setup
- **Template management** — create/edit templates with category, style preset, price level, thumbnail; activate/deactivate toggle controls what the storefront shows
- **Intake question builder** — add/edit/delete questionnaire questions (text, textarea, date, time, select, colour, number), set section, minimum tier, required flag and sort order
- **WhatsApp broadcast** — message all active couples or those expiring within 7 days

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

All services communicate on a Podman bridge network (`baundang-network`) with DNS resolution enabled. The API Gateway is the single public entry point; internal services are not exposed to the internet. The gateway validates RS256 JWTs and forwards identity via `X-User-Id` / `X-User-Role` headers, which each service turns into its Spring Security principal.

---

## Services

| Service | Port | Description |
|---|---|---|
| **gateway-service** | 1080 | Spring Cloud Gateway — routing, JWT auth filter (`JwtAuth` / `JwtAuth=ADMIN`), rate limiting (Redis) |
| **auth-service** | 1081 | User registration, login, RS256 JWT issuance, refresh tokens, admin seeding |
| **storefront-service** | 1082 | Public website — landing, catalogue, order flow, payment pages, order tracking, buyer login/register, intake wizard, Kelola Undangan portal (Thymeleaf + HTMX + Alpine.js) |
| **template-service** | 1083 | Template CRUD, style presets, MinIO presigned URLs, Bible verse catalogue |
| **invitation-service** | 1084 | Invitation lifecycle & themed rendering, RSVP, guestbook, gift registry, digital gifts, guest list & QR check-in, client self-service API |
| **order-service** | 1085 | Order creation, status machine, revisions, public tracking lookup, intake questionnaire |
| **payment-service** | 1086 | Midtrans Snap order payment + public webhook handler, digital gift (amplop) charges |
| **notification-service** | 1087 | WhatsApp (Fonnte) + email (SMTP) notifications, RabbitMQ consumers, broadcast, expiry reminder scheduler |
| **admin-service** | 1088 | Back-office web UI — dashboard, orders, invitation builder, guests/QR, templates, intake builder, broadcast, CSV export |
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
| Database | PostgreSQL 16 — single instance, one schema per service, native enums |
| Schema migration | Flyway (per-service, `create-schemas: true`) |
| ORM | Spring Data JPA / Hibernate (+ hypersistence JSONB for invitation content) |
| Cache | Redis 7 (Spring Cache with `JavaTimeModule`-aware JSON serialization) |
| Messaging | RabbitMQ 3.13 — topic exchanges per domain |
| Object storage | MinIO (presigned PUT/GET for client-side uploads) |
| Frontend | Thymeleaf, HTMX, Alpine.js, Tailwind (storefront/admin), qrcodejs, html5-qrcode |
| Payments | Midtrans Snap (orders + digital gifts) |
| WhatsApp | Fonnte API (`@Retryable`, Guava `RateLimiter`) |
| Email | Spring Mail (SMTP) |
| Containers | Podman + podman-compose (rootless OCI images) |
| Observability | AOP service-layer logging (`ServiceLoggingAspect`) + request/response interceptor |
| Code quality | Checkstyle 10 (Google style, 120-char lines), bound to `validate` phase |
| Testing | JUnit 5 + `@WebMvcTest` (MockMvc) controller tests per service |
| CI | GitHub Actions — build + test + checkstyle on PRs |
| CD | GitHub Actions — SSH deploy with per-service build/restart via checkboxes |

---

## Order Lifecycle & Event Flow

The services are stitched together with RabbitMQ topic exchanges. The end-to-end happy path:

```
Buyer orders on /pesan
  └─ order-service creates order (PENDING) ──▶ order.created
       ├─ notification-service → "Pesanan Diterima" email + WA with payment link
       └─ Buyer pays via Midtrans Snap on /bayar/{orderId}
            └─ payment-service webhook ──▶ order.paid (payment)
                 └─ order-service marks PAID ──▶ order.paid (enriched)
                      ├─ invitation-service auto-creates invitation (slug, content from event)
                      └─ notification-service → payment emails/WA (buyer + admin)
Buyer fills intake questionnaire → admin builds invitation (Bangun Undangan)
Admin marks order COMPLETED ──▶ order.completed
  └─ notification-service → "Undangan Siap" email + WA with /i/{slug} link
```

| Routing key | Publisher | Consumers | Purpose |
|---|---|---|---|
| `order.created` | order-service | notification | Order confirmation + payment link |
| `order.paid` | payment-service, order-service | order, invitation, notification | Mark paid, create invitation, notify buyer & admin |
| `order.completed` | order-service | notification | "Invitation ready" email/WA with link |
| `order.revised` | order-service | notification | Alert admin of a revision request |
| `revision.completed` | order-service | notification | Tell buyer their revision is done |
| `rsvp.submitted` | invitation-service | notification | WA the couple about a new RSVP |
| `gift.confirmed` | invitation-service | notification | WA the couple about a gift confirmation |
| `invitation.expiring` | notification scheduler | notification | Expiry reminders (7-day window) |

---

## Invitation Themes

Every invitation page renders in one of four visual presets, selected per invitation in **Bangun Undangan** (stored as `stylePreset` in the invitation's content JSON, default `GRACE`). Theming is pure CSS custom properties on `data-theme` — colours, fonts, ornaments and section styling all switch per preset, with fonts loaded conditionally.

| Preset | Mood | Palette | Typography |
|---|---|---|---|
| **GRACE** | Romantic, soft | Blush rose + rose gold | Cormorant Garamond italic + Nunito |
| **COVENANT** | Formal, classic | Deep navy + antique gold | Crimson Text + Raleway |
| **EDEN** | Natural, warm | Sage green + earth tones | DM Serif Display + DM Sans |
| **GLORIA** | Bold, elegant | Dark charcoal + bright gold | Cinzel + Raleway |

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

`verify` runs Checkstyle (validate phase), compiles, and executes every module's test suite.

---

## Configuration

All service configuration is managed by **Spring Cloud Config Server** (`config-server`). Each service bootstraps with only its name and the config server URL; everything else is served from `config-repo/`. Gateway routes (path allowlists, `JwtAuth` filters) also live here — changing them requires a **deploy-config** plus a gateway restart.

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

| Schema | Owner service | Notable tables |
|---|---|---|
| `auth` | auth-service | users, refresh tokens |
| `template` | template-service | templates (category, style preset, price level), verses |
| `invitation` | invitation-service | invitations (JSONB content), rsvp, guestbook, guests (QR check-in), gift accounts, gifts, gift confirmations |
| `orders` | order-service | orders, revisions, intake_question, order_intake |
| `payment` | payment-service | payments, gift payments |
| `notification` | notification-service | notification log |
| `admin` | admin-service | admin notes |
| `media` | media-service | — |

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
| `POST` | `/` | Admin | Create template (name, slug, category, style preset, price level, thumbnail) |
| `PUT` | `/{id}` | Admin | Update template / toggle active |
| `DELETE` | `/{id}` | Admin | Soft-delete template |
| `GET` | `/christian/verses` | — | Bible verse catalogue |

### Orders — `/api/v1/orders`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/` | — / Buyer JWT | Create order (templateId, tier, couple name, contacts) |
| `GET` | `/mine` | Buyer JWT | List the caller's orders (powers Pesanan Saya) |
| `GET` | `/{id}` | Buyer / Admin | Order detail (ownership enforced) |
| `GET` | `/public/{id}` | — | Public order summary (payment result pages) |
| `GET` | `/public/lookup?orderNumber=&contact=` | — | Public tracking by order number + matching email/WA |
| `GET` | `/` | Admin | Paginated order list (status + search filters) |
| `PUT` | `/{id}/status` | Admin | Update order status (publishes paid/completed events) |
| `POST` | `/{id}/revisions` | Buyer | Request design revision |
| `PUT` | `/revisions/{id}/complete` | Admin | Mark revision complete |
| `GET` | `/{id}/revisions` | Buyer / Admin | List revisions |

### Intake questionnaire

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/orders/{orderId}/intake/questions` | Buyer / Admin | Questions for the order's tier |
| `GET` | `/api/v1/orders/{orderId}/intake` | Buyer / Admin | Saved answers |
| `PUT` | `/api/v1/orders/{orderId}/intake` | Buyer / Admin | Save answers (`{answers, submitted}`) |
| `GET` | `/api/v1/admin/intake/questions` | Admin | List all question definitions |
| `POST` | `/api/v1/admin/intake/questions` | Admin | Create question (section, label, fieldKey, inputType, options, minTier, required, sortOrder, active) |
| `PUT` | `/api/v1/admin/intake/questions/{id}` | Admin | Update question |
| `DELETE` | `/api/v1/admin/intake/questions/{id}` | Admin | Delete question |

### Payments — `/api/v1/payments`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/charge` | Buyer JWT | Create Midtrans Snap charge for an order |
| `GET` | `/snap-token/{orderId}` | — | Get Snap token for an existing order |
| `POST` | `/webhook/midtrans` | — (public) | Midtrans payment notification webhook |
| `POST` | `/gifts/charge` | — (public) | Digital gift (amplop) charge — `{invitationId, senderName, message, amount ≥ 20000}` |

### Invitations — public `/api/v1/invitations`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/{slug}/rsvp` | — | Submit RSVP (name, attendance, guest count, message) |
| `GET` | `/{slug}/guestbook` | — | List approved guestbook entries |
| `POST` | `/{slug}/guestbook` | — | Submit guestbook message (pending approval) |
| `GET` | `/{slug}/events` | — | List wedding events |
| `GET` | `/{slug}/gift-accounts` | — | Gift registry info (bank / e-wallet / QRIS) |
| `POST` | `/{slug}/gift-confirm` | — | Confirm a manual gift transfer |
| `GET` | `/{slug}/checkin/{code}` | — | Guest lookup by invite code |
| `POST` | `/{slug}/checkin/{code}` | — | Record check-in with actual head count |

### Invitations — client portal `/api/v1/invitations/my/{orderId}` (Buyer JWT, ownership enforced)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Invitation summary (slug, couple name, status) |
| `GET` / `POST` | `/guests` | List / add guests |
| `DELETE` | `/guests/{guestId}` | Remove guest |
| `GET` | `/rsvp` | RSVP responses |
| `GET` | `/attendance` | Check-in statistics |
| `GET` | `/gifts` | Digital gift summary + entries |
| `GET` | `/guestbook` | All guestbook entries (incl. pending) |
| `PUT` | `/guestbook/{entryId}/approve` | Approve a guestbook entry |

Every call verifies the authenticated user's id matches the `buyerId` stored on the invitation — otherwise `401`.

### Invitations — admin `/api/v1/admin/invitations` (Admin JWT)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` , `/{id}` | List / detail |
| `PUT` | `/{id}/content` | Merge-patch invitation content JSON (builder save) |
| `PUT` | `/{id}/status` | DRAFT / ACTIVE / EXPIRED |
| `PUT` | `/{id}/slug` | Custom URL slug |
| `GET` / `POST` | `/{id}/guests` · `DELETE /{id}/guests/{guestId}` | Guest management |
| `GET` | `/{id}/attendance` | Check-in stats |
| `GET` | `/{id}/rsvp` · `/{id}/guestbook` · `PUT /{id}/approve-guestbook/{entryId}` | RSVP + guestbook moderation |
| `PUT` | `/{id}/gift-accounts` · `GET /{id}/gifts` | Gift registry setup + digital gift summary |
| `GET` | `/active-phones` | WA numbers of all active invitations (broadcast) |

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
| `/templates` · `/templates/{slug}` | Template catalogue and detail |
| `/pesan` | Order flow (package selection → details → confirmation) |
| `/bayar/{orderId}` | Midtrans Snap payment page (+ `/bayar/selesai`, `/bayar/pending`, `/bayar/gagal` result pages) |
| `/lacak` | Public order tracker (order number + contact, no login) |
| `/masuk` · `/daftar` | Buyer login / registration (JWT in sessionStorage, `?redirect=` supported; nav shows **Masuk** or **Pesanan Saya** based on login state) |
| `/pesanan-saya` | Buyer order dashboard with progress timeline |
| `/pesanan/{orderId}/intake` | Post-payment questionnaire wizard |
| `/pesanan/{orderId}/kelola` | Kelola Undangan — client portal (Tamu / RSVP / Kehadiran / Amplop / Buku Tamu) |
| `/tentang` | About page |
| `/admin/login` · `/admin` | Admin back office (requires admin JWT) |

### Invitation pages (invitation-service)

| Path | Description |
|---|---|
| `/i/{slug}` | Themed invitation viewer (`?to=` personal greeting, PIN gate, floating amplop button) |
| `/i/{slug}/gift` | Digital gift page (standalone) |
| `/i/{slug}/scan` | QR check-in scanner for door staff (camera + manual code) |
| `/i/{slug}/checkin/{code}` | Guest check-in page (name, group, table, seat count) |

---

## Pricing Tiers

| Tier | Price | Highlights |
|---|---|---|
| **Dasar** | Rp 119.000 | 1 template, event info, RSVP, unique link |
| **Standar** ⭐ | Rp 199.000 | All templates, guestbook, gallery, countdown, interactive map |
| **Premium** | Rp 249.000 | All Standar features + music, gift registry, 2 revisions, priority support |

The intake questionnaire automatically shows/hides questions based on the order's tier (`minTier` per question).

---

## CI / CD

### Continuous Integration (`.github/workflows/ci.yml`)

Runs on every pull request targeting `develop` or `main`:

1. **Build & Test** — `mvn clean verify` (Checkstyle runs at the `validate` phase, then compile + all module test suites)
2. **Checkstyle** — `mvn checkstyle:check` (Google style, 120-char limit)
3. Surefire reports uploaded as CI artifacts

Controller tests use `@WebMvcTest` with `@AutoConfigureMockMvc(addFilters = false)` so requests reach the real handler mappings; they cover endpoint routing, auth/ownership enforcement (401/404), and request-body validation (400) across all services.

### Continuous Deployment (`.github/workflows/cd.yml`)

`workflow_dispatch` with an action selector plus per-service checkboxes:

- **full-deploy** — git pull + `mvn package` + `podman compose up --build` for the whole stack
- **build-service** — rebuild + recreate the selected service container(s) (`podman rm -f` + `--force-recreate`)
- **restart-service** — restart selected containers without rebuilding
- **restart-all** — remove every container and recreate from current images (no rebuild)
- **deploy-config** — sync `config-repo/` to the server and refresh the config server (follow with a gateway restart when routes changed)
- **deploy-env** — push `.env` from the `ENV_FILE` secret to the server

Deploys are serialized via a `deploy` concurrency group (queued, not cancelled).

#### Required GitHub Secrets

| Secret | Value |
|---|---|
| `SSH_KEY` | Private SSH key for the deploy user |
| `SSH_USER` | Deploy user on the server |
| `SSH_HOST` | Server hostname or IP |
| `SSH_PORT` | SSH port |
| `SSH_PATH` | Deployment directory (e.g. `/opt/baundang`) |
| `ENV_FILE` | Full contents of the production `.env` (for deploy-env) |

---

## Project Structure

```
DigitalInvitationStore/
├── pom.xml                   # Parent POM — dependency management, Checkstyle
├── common/                   # Shared library: ApiResponse, GlobalExceptionHandler, logging aspect/interceptor, exceptions
├── config-server/            # Spring Cloud Config Server
├── config-repo/              # YAML configuration files for all services (incl. gateway routes)
├── gateway-service/          # API Gateway — routing, JWT filter, rate limiting
├── auth-service/             # Authentication & JWT issuance
├── storefront-service/       # Public-facing website (Thymeleaf + HTMX + Alpine.js)
├── template-service/         # Wedding template catalogue
├── invitation-service/       # Invitation pages (4 themes), RSVP/guestbook/gifts/guests/check-in, client portal API
├── order-service/            # Order lifecycle, revisions, public lookup, intake questionnaire
├── payment-service/          # Midtrans payments (orders + digital gifts)
├── notification-service/     # WhatsApp + email notifications, event consumers, expiry scheduler
├── admin-service/            # Internal back-office web UI (incl. Bangun Undangan & intake builder)
├── media-service/            # Object storage (MinIO) proxy
├── checkstyle/
│   └── checkstyle.xml        # Checkstyle rules (Google style)
├── docs/
│   └── server-setup.sh       # Server provisioning script
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
