# baundang.id — Digital Wedding Invitation Platform

A production-ready SaaS platform for creating and managing digital wedding invitations. Built as a Java microservices architecture, it covers the full journey: browsing templates, ordering and paying online, filling a guided questionnaire, receiving a themed invitation page, managing guests with QR check-in, collecting RSVPs and digital gift envelopes (amplop), and running the whole operation from an admin back office.

[![CI](https://github.com/jeimandei/DigitalInvitationStore/actions/workflows/ci.yml/badge.svg)](https://github.com/jeimandei/DigitalInvitationStore/actions/workflows/ci.yml)

---

## Table of Contents

- [Feature Overview](#feature-overview)
- [Architecture Overview](#architecture-overview)
- [Services in Depth](#services-in-depth)
- [Tech Stack](#tech-stack)
- [Order Lifecycle & Event Flow](#order-lifecycle--event-flow)
- [Invitation Themes](#invitation-themes)
- [Security Model](#security-model)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Database Schemas](#database-schemas)
- [API Reference](#api-reference)
- [Pages](#pages)
- [Pricing Tiers](#pricing-tiers)
- [Scheduled Jobs](#scheduled-jobs)
- [CI / CD](#ci--cd)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

---

## Feature Overview

### For couples (buyers)

- **Template catalogue** — browse active templates filtered by category and price level; only admin-activated templates are shown publicly
- **Online ordering** — pick a package (Dasar / Standar / Premium); the Midtrans Snap transaction is created **automatically the moment the order is placed** (payment-service consumes `order.created`), so the payment link is instantly available; login is optional at checkout
- **Order tracking**
  - **Public tracker** (`/lacak`) — look up any order by order number + the email or WhatsApp used on it (WA numbers match on digit suffix, so `08…` and `628…` both work), no login needed
  - **Pesanan Saya** (`/pesanan-saya`) — logged-in dashboard with a progress timeline (Dipesan → Dibayar → Dikerjakan → Selesai), pay-now link, revision counters
- **Intake questionnaire** — after payment, a guided, **tier-aware** wizard collects everything needed to build the invitation. Questions are stored in the database and fully editable by the admin (input types: TEXT, TEXTAREA, DATE, TIME, SELECT, COLOR, NUMBER; each question has a `minTier` so Premium-only questions hide for lower tiers). Draft-save and submit are separate actions.
- **Kelola Undangan** (`/pesanan/{orderId}/kelola`) — self-service portal for the couple:
  - **Tamu** — add/remove guests (name, group, table, seat allocation), copy personal invitation links, download per-guest QR codes
  - **RSVP** — see who confirmed attendance, with head counts and messages
  - **Kehadiran** — live check-in stats: invited, total seats, checked-in guests/heads, attendance percentage
  - **Amplop** — digital gift totals and sender list
  - **Buku Tamu** — approve pending guestbook messages
- **Revisions** — request design revisions on PAID/IN_REVISION orders within the package quota (Premium: 2); each request bumps the order to IN_REVISION until the admin completes it
- **Email + WhatsApp notifications** at every step: order placed (with payment link), payment received, invitation ready (with shareable link), revision completed

### For wedding guests

- **Invitation page** (`/i/{slug}`) — server-rendered, mobile-first, with a full-screen cover ("Buka Undangan"), couple profile, structured event cards with embedded Google Maps (from stored lat/lng), love story, and per-view counter
- **Four visual themes** — GRACE / COVENANT / EDEN / GLORIA (see [Invitation Themes](#invitation-themes))
- **Personal greeting** — links with `?to=Nama+Tamu` greet the guest by name on the cover
- **Optional PIN gate** — if the admin sets an `accessPin`, guests must enter it before the invitation opens
- **Christian wedding support** — invitations can carry a `christian` content block (Bible verse with reference/translation/text, ceremony type, church name/address/time); template-service ships a Bible verse catalogue (NIV/KJV/TB/BIS, categorised LOVE/COVENANT/BLESSING)
- **RSVP** — confirm attendance (`hadir` / `tidak_hadir`) with guest count and message; the couple gets a WhatsApp ping
- **Buku Tamu (guestbook)** — leave wishes; entries appear only after approval (couple or admin)
- **Kirim Amplop (digital gift)** — floating button opens a modal with a free-form amount (min Rp 20.000, plus quick-pick chips) → Midtrans Snap, no account needed. Successful payment flows back via webhook → `gift.paid` event → recorded on the invitation. A **gift registry** page (`/i/{slug}/gift`) also shows bank/GoPay/OVO/QRIS details with manual transfer confirmation (`gift-confirm`), which pings the couple on WhatsApp.
- **QR check-in** — every guest has a unique 24-hex invite code. Door staff open `/i/{slug}/scan` (html5-qrcode camera scanner with manual-code fallback), scan the guest's QR, land on the check-in page (name, group, table, allotted seats) and record the actual head count.

### For admins

- **Dashboard** — orders today, revenue, invitations, buyers at a glance
- **Order management** — searchable (couple name / order number / email) and status-filterable list, status machine, internal admin notes per order, CSV export
- **Bangun Undangan** (invitation builder) — structured content editor beside the client's questionnaire answers, with one-click **"Isi dari Kuesioner"**; sets couple info, matrimony/reception details, love story, cover photo, maps URL, colour palette, music, gift registry, **theme preset**, **custom slug** (validated `[a-z0-9-]`, uniqueness-checked), and **access PIN**; per-guest link helper. Saves are **merge-patched** into the invitation's JSONB content (existing keys preserved).
- **Guest management** — add guests with group/table/seat allocation, high-res QR codes (click to enlarge, download, print-all sheet)
- **RSVP / Attendance / Guestbook / Gifts** — per-invitation views, guestbook approval, gift-account setup (bank, GoPay, OVO, QRIS image), digital gift summary; RSVP CSV export
- **Template management** — create/edit templates (name, slug, description, category GENERAL/CHRISTIAN/WEDDING/BIRTHDAY/GRADUATION/CORPORATE/OTHER, style preset, price level 1–3, thumbnail, JSONB config, key-value feature list); activate/deactivate toggle controls storefront visibility; delete is a soft-delete (deactivation)
- **Intake question builder** — full CRUD on questionnaire questions with section, input type, options, min tier, required flag, sort order, active flag
- **WhatsApp broadcast** — message all active couples (`ALL_ACTIVE`) or those expiring within 7 days (`EXPIRING_7D`); plus a `/test-wa` endpoint for verifying Fonnte connectivity
- **Revision completion** — one click returns an IN_REVISION order to PAID and notifies the buyer

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

All services communicate on a Podman bridge network (`baundang-network`) with DNS resolution enabled. The API Gateway is the single public entry point; internal services are never exposed directly. Compose enforces start order via `service_healthy` conditions (config-server and RabbitMQ must be healthy before application services boot).

---

## Services in Depth

| Service | Port | What it actually does |
|---|---|---|
| **gateway-service** | 1080 | Spring Cloud Gateway (WebFlux). Custom `JwtAuth` filter factory: verifies RS256 JWTs (public key fetched from auth-service at startup with exponential-backoff retry), accepts tokens from the `Authorization` header **or** the `admin_token` cookie (browser navigation), redirects browser navigations without a token to `/admin/login` while returning 401 to API/HTMX calls, enforces optional role (`JwtAuth=ADMIN`), and injects `X-User-Id` / `X-User-Role` headers downstream. Redis rate limiting keyed by client IP (`X-Forwarded-For`-aware). Permissive CORS. Global request/latency logging with upstream route resolution. All routes live in `config-repo/gateway-service.yml`. |
| **auth-service** | 1081 | Registration/login with BCrypt (strength 12). Issues RS256 JWTs signed with an **RSA-4096 key pair generated on first boot** and persisted to a key volume (private key chmod'd owner-only). Token lifetimes: admin 8 h, buyer 24 h, order-scoped token 60 min. Refresh tokens are opaque 32-byte values stored **SHA-256-hashed** and **rotated on every refresh** (old one revoked). A 03:00 cron purges expired/revoked tokens. `register-admin` is gated by the `X-Admin-Seed-Key` header. Serves its public key as PEM for the gateway. |
| **storefront-service** | 1082 | Public web UI (Thymeleaf + Tailwind + Alpine.js + HTMX): landing, catalogue (with HTMX pagination fragments), template detail, order flow, Midtrans payment page + selesai/pending/gagal result pages (enriched with public order detail), public tracker, login/register (JWT kept in `sessionStorage`, nav switches Masuk ⇄ Pesanan Saya), Pesanan Saya, intake wizard, Kelola Undangan portal, about page, robots.txt + sitemap.xml. Calls template/order services via `RestClient`. |
| **template-service** | 1083 | Template CRUD with slug uniqueness, categories, style presets, per-template key-value features and JSONB config; `ChristianTemplateConfig` (motif, colour palette, hymn preset) 1-to-1 with templates; Bible verse catalogue with translation/category filters; MinIO presigned preview URLs (`previews/{slug}`); activate/deactivate; public list shows only active templates unless `includeInactive` (admin). |
| **invitation-service** | 1084 | The heart of the product. Consumes `order.paid` → **auto-creates the invitation** (slugified couple name + 6-char order-id suffix, ACTIVE for 180 days, whole event payload stored as JSONB content). Consumes `gift.paid` → records digital gifts. Renders the themed invitation, gift, scanner and check-in pages. Public APIs for RSVP (publishes `rsvp.submitted`), guestbook (moderated), events, gift accounts, gift confirmation (publishes `gift.confirmed`), and check-in. Client self-service API under `/api/v1/invitations/my/**` gated on buyer ownership. Admin API for content merge-patch, status, custom slug, guests, attendance, gifts. Redis caching: invitation-by-slug 5 min, approved guestbook 1 min (evicted on writes). Two cron jobs (see [Scheduled Jobs](#scheduled-jobs)). |
| **order-service** | 1085 | Order creation with `BND-yyyyMMdd-XXXX` order numbers (collision-checked), tier pricing from config, revision quota per tier ({0, 0, 2}), anonymous checkout support. Consumes the payment `order.paid` event → idempotently marks PAID → republishes an **enriched** `order.paid` (couple/contact/slug/template data) for invitation + notification. Admin status changes publish `order.paid`/`order.completed` as appropriate. Public order summary + tracking lookup. Revision request/complete workflow. Intake questionnaire definition (admin CRUD) + per-order answers with buyer/admin access checks. |
| **payment-service** | 1086 | Midtrans Snap integration (sandbox/production switch). Consumes `order.created` → creates the Snap transaction immediately (`BND-{orderId}` Midtrans order id, item detail “Undangan Digital – Paket X”). Public webhook (3 URLs: base, `recurring`, `pay-account`) validated with the Midtrans **SHA-512 signature** (`order_id+status_code+gross_amount+serverKey`); handles capture/settlement/cancel/deny/expire with fraud-status checks; raw notification stored as JSONB. On success publishes the bare `order.paid`. **Digital gifts**: `GIFT-{uuid}` order ids skip signature-by-prefix routing to the gift handler; success publishes `gift.paid`. |
| **notification-service** | 1087 | All buyer/admin comms. WhatsApp via Fonnte (`@Retryable` 3×/2 s backoff, Guava `RateLimiter` at 20 msg/min, bulk send support). Email via Spring Mail (SMTP). Every send is recorded in a `notifications` audit table (channel, template key, payload, SENT/FAILED). Consumers: `order.created` (payment link mail+WA), `order.paid` (buyer mail+WA, **admin WA ping**), `order.completed` (invitation-ready mail+WA), `order.revised` (admin WA), `revision.completed` (buyer WA), `rsvp.submitted` (couple WA), `gift.confirmed` (couple WA), `invitation.expiring` (couple WA). Daily expiry scheduler + admin broadcast endpoint + `/test-wa`. |
| **admin-service** | 1088 | Server-rendered back office (Thymeleaf + Tailwind + Alpine). Aggregates the other services through internal REST clients (orders, invitations, templates, notifications, intake). Pages: dashboard, orders (detail/status/notes/CSV), templates (create/edit/toggle/delete), invitations (detail/status/build/slug/rsvp/guests/attendance/gifts/gift-account/guestbook), intake question builder, WA broadcast, buyers list, RSVP CSV export, revision completion. Own schema stores only `admin_notes`. |
| **media-service** | 1089 | MinIO façade with **three buckets**: `templates`, `couples`, `admin`. Buyer presigned PUT uploads are forced into `couples/{slug}` folders (regex-validated), filenames sanitized and UUID-prefixed; content-type whitelist + per-type size limits; presigned GET for downloads (bucket resolved from key prefix); admin-only delete and server-side template upload. |
| **config-server** | 8888 | Spring Cloud Config (native profile) serving `config-repo/`, protected by basic auth. |
| **common** | — | Shared library auto-configured into every service: `ApiResponse`/`PagedResponse` envelopes, `GlobalExceptionHandler` (typed exceptions → 404/400/401 JSON), `SlugUtils`, `ContentCachingFilter` + `RequestLoggingInterceptor` (request/response body logging with binary skip + 2 KB truncation), `ServiceLoggingAspect` (AOP timing on every `@Service`/`@Repository` method with sensitive-arg masking). |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.3, Spring Cloud 2025.0.0 |
| Build | Maven (multi-module, parent POM) |
| API Gateway | Spring Cloud Gateway (WebFlux) with RS256 JWT filter + Redis rate limiter |
| Database | PostgreSQL 16 — single instance, one schema per service, native enums, `pgcrypto`, `set_updated_at` triggers |
| Schema migration | Flyway (per-service, `create-schemas: true`) |
| ORM | Spring Data JPA / Hibernate + hypersistence `JsonBinaryType` for JSONB columns |
| Cache | Redis 7 — Spring Cache with a `JavaTimeModule`-aware polymorphic JSON serializer |
| Messaging | RabbitMQ 3.13 — topic exchanges (orders / rsvp / invitations), durable queues, Jackson JSON converter |
| Object storage | MinIO — presigned PUT/GET, three buckets (templates / couples / admin) |
| Frontend | Thymeleaf, HTMX, Alpine.js, Tailwind, qrcodejs (QR generation), html5-qrcode (camera scanning) |
| Payments | Midtrans Snap (orders + digital gifts), SHA-512 webhook signature validation |
| WhatsApp | Fonnte API — `@Retryable`, Guava `RateLimiter` (20 msg/min), bulk send |
| Email | Spring Mail (SMTP) |
| Containers | Podman + podman-compose (rootless OCI images, healthcheck-ordered startup) |
| Observability | AOP service-layer logging + request/response interceptor + gateway route logging; actuator health/info/metrics/prometheus |
| Code quality | Checkstyle 10 (Google style, 120-char lines, 4-space indent), bound to the `validate` phase |
| Testing | JUnit 5 + `@WebMvcTest`/MockMvc controller tests in every service |
| CI / CD | GitHub Actions — PR build+test+checkstyle; SSH deploy with per-service actions |

---

## Order Lifecycle & Event Flow

```
Buyer orders on /pesan
  └─ order-service creates order (PENDING, BND-yyyyMMdd-XXXX) ──▶ order.created
       ├─ payment-service creates the Midtrans Snap transaction immediately
       └─ notification-service → "Pesanan Diterima" email + WA with payment link
Buyer pays on /bayar/{orderId} (Snap popup)
  └─ Midtrans webhook → payment-service (SHA-512 signature check) ──▶ order.paid (bare)
       └─ order-service marks PAID (idempotent) ──▶ order.paid (enriched)
            ├─ invitation-service auto-creates the invitation (slug, 180-day active window)
            └─ notification-service → payment email/WA to buyer + WA ping to admin
Buyer fills intake questionnaire → admin builds the invitation (Bangun Undangan)
Admin marks order COMPLETED ──▶ order.completed
  └─ notification-service → "Undangan Siap" email + WA with the /i/{slug} link
```

| Routing key | Publisher | Consumers | Purpose |
|---|---|---|---|
| `order.created` | order-service | payment, notification | Create Snap transaction; send confirmation + payment link |
| `order.paid` | payment-service (bare), order-service (enriched) | order, invitation, notification | Mark paid; create invitation; notify buyer & admin |
| `order.completed` | order-service | notification | "Invitation ready" email/WA |
| `order.revised` | order-service | notification | WA the admin about a revision request |
| `revision.completed` | order-service | notification | WA the buyer that the revision is done |
| `payment.failed` | payment-service | (logged) | Failed/expired transactions |
| `gift.paid` | payment-service | invitation | Record a successful digital gift on the invitation |
| `rsvp.submitted` | invitation-service | notification | WA the couple about a new RSVP |
| `gift.confirmed` | invitation-service | notification | WA the couple about a manual gift confirmation |
| `invitation.expiring` | invitation + notification schedulers | notification | Expiry reminders (7-day window) |

---

## Invitation Themes

Every invitation page renders in one of four visual presets, selected per invitation in **Bangun Undangan** (stored as `stylePreset` in the invitation's content JSON, default `GRACE`; templates also carry a preset for cataloguing). Theming is pure CSS custom properties keyed on `data-theme` — colours, fonts, ornaments, buttons and section styling all switch per preset, and each theme loads only its own Google Fonts pair.

| Preset | Mood | Palette | Typography |
|---|---|---|---|
| **GRACE** | Romantic, soft | Blush rose + rose gold | Cormorant Garamond italic + Nunito |
| **COVENANT** | Formal, classic | Deep navy + antique gold | Crimson Text + Raleway |
| **EDEN** | Natural, warm | Sage green + earth tones | DM Serif Display + DM Sans |
| **GLORIA** | Bold, elegant | Dark charcoal + bright gold | Cinzel + Raleway |

### Invitation content JSON

The invitation's single JSONB `content` column carries everything the renderer needs. Notable keys: `coupleName`, `groomFullName`/`brideFullName`, `matrimonyDate/Time/Venue` (with legacy `akad*` fallback), `receptionDate/Time/Venue`, `loveStory`, `coverPhotoUrl`, `mapsEmbedUrl`, `colorPalette`, `backgroundMusic`, `giftRegistry`, `stylePreset`, `accessPin`, `buyerId` (ownership), plus two structured blocks:

- `events[]` — `{name, date, time, venue_name, venue_address, venue_lat, venue_lng, dress_code}`; lat/lng automatically produce Google Maps embed + directions links
- `christian` — `{bibleVerse: {reference, translation, text}, ceremonyType, churchName, churchAddress, churchTime}`

---

## Security Model

- **Edge**: the gateway is the only public entry. `JwtAuth` (any valid JWT) / `JwtAuth=ADMIN` filters guard routes declared in `config-repo/gateway-service.yml`. Valid tokens become `X-User-Id` / `X-User-Role` headers.
- **Services** trust those headers (they are unreachable except through the gateway) and convert them into Spring Security principals via a `GatewayHeaderFilter`, then enforce per-endpoint rules (`permitAll` for public invitation interactions, `hasRole('ADMIN')` for admin APIs, `authenticated` + in-controller ownership checks for buyer data).
- **Ownership checks**: orders/intake verify `buyerId == X-User-Id`; the client portal verifies the `buyerId` stored inside the invitation content; guests/guestbook entries are verified to belong to the invitation being operated on.
- **Webhooks**: Midtrans notifications are authenticated by signature, not by network position.
- **Secrets**: BCrypt-12 passwords, hashed+rotated refresh tokens, seed-key-gated admin registration, private JWT key readable only by its owner.

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

All service configuration is served by **Spring Cloud Config Server** from `config-repo/`. Each service bootstraps with only its name and the config server URL. Gateway routes also live here — changing them requires a **deploy-config** plus a gateway restart.

```
config-repo/
  application.yml          # shared: Hikari pool (5/2), JPA validate, Flyway, RabbitMQ, Redis, scheduling pool
  auth-service.yml         gateway-service.yml      invitation-service.yml
  order-service.yml        payment-service.yml      notification-service.yml
  storefront-service.yml   template-service.yml     admin-service.yml
  media-service.yml
```

### Environment variables (`.env`)

| Variable | Description |
|---|---|
| `DB_USER` / `DB_PASSWORD` | PostgreSQL credentials |
| `REDIS_PASSWORD` | Redis auth password |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` / `RABBITMQ_VHOST` / `RABBITMQ_ERLANG_COOKIE` | RabbitMQ |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | MinIO root credentials |
| `CONFIG_SERVER_USER` / `CONFIG_SERVER_PASSWORD` | Config server basic auth |
| `JWT_SECRET` | Seed material for JWT key generation |
| `ADMIN_SEED_KEY` | Secret for `/api/v1/auth/register-admin` |
| `MIDTRANS_SERVER_KEY` / `MIDTRANS_CLIENT_KEY` | Midtrans keys (sandbox/production flag in config) |
| `WHATSAPP_API_TOKEN` | Fonnte WhatsApp API token |
| `ADMIN_WHATSAPP` | Admin number for order/revision pings |
| `EMAIL_HOST` / `EMAIL_PORT` / `EMAIL_USERNAME` / `EMAIL_PASSWORD` | SMTP settings |

See [`.env.example`](.env.example) for the full list.

### Seeding the first admin account

```bash
curl -X POST https://<your-domain>/api/v1/auth/register-admin \
  -H "Content-Type: application/json" \
  -H "X-Admin-Seed-Key: <ADMIN_SEED_KEY value from .env>" \
  -d '{"email":"admin@baundang.id","password":"YourPassword123"}'
```

The admin UI is at `/admin/login`.

---

## Database Schemas

A single PostgreSQL instance hosts all schemas in database `baundang`; each service connects with `currentSchema=<schema>` so Flyway and Hibernate stay isolated. `max_connections` 200; HikariCP 5 per service. Native enums (`order_status_enum`, `invitation_status_enum`), `pgcrypto` UUIDs and `set_updated_at` triggers are used where relevant.

| Schema | Tables |
|---|---|
| `auth` | `users` (ADMIN/BUYER), `refresh_tokens` (hashed, revocable) |
| `template` | `templates` (JSONB config, style preset, price level, active flag), `template_features` (key-value), `christian_template_configs`, `bible_verses` |
| `invitation` | `invitations` (unique order_id & couple_slug, JSONB content, 180-day active window, view counter, partial index on active), `rsvp_responses`, `guestbook_entries` (approved flag), `gift_accounts`, `gift_confirmations`, `guests` (unique invite_code, check-in fields), `gifts` (Midtrans-paid amplop) |
| `orders` | `orders` (BND order numbers, tier 1–3, revision counters, couple_slug), `order_revisions` (REQUESTED/IN_PROGRESS/COMPLETED), `intake_question` (seeded with 16 defaults), `order_intake` (JSONB answers per order) |
| `payment` | `payments` (snap token, raw webhook JSONB, status), `gift_payments` (GIFT- order ids) |
| `notification` | `notifications` (audit of every WA/email send) |
| `admin` | `admin_notes` (free-form notes on any entity) |

---

## API Reference

All public endpoints go through the gateway on port **1080**. Responses use a uniform envelope: `{success, data, message, timestamp}`; list endpoints add `{content, page, size, totalElements, totalPages, last}`.

### Auth — `/api/v1/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/login` | — | Email + password → `{access_token, refresh_token, token_type, expires_in}` |
| `POST` | `/register` | — | Create buyer account (password 8–72 chars) → token pair |
| `GET` | `/public-key` | — | RS256 public key (PEM) |
| `POST` | `/token/refresh` | — | Rotate refresh token → new pair (old token revoked) |
| `POST` | `/order-token` | — | Access token + order id → 60-min order-scoped token |
| `POST` | `/register-admin` | `X-Admin-Seed-Key` | Create admin account |

### Templates — `/api/v1/templates`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/` | — | Paginated list (`category`, `priceLevel`, `includeInactive`, max size 50); active-only for public |
| `GET` | `/{slug}` | — | Detail incl. features map + JSONB config (active templates only) |
| `GET` | `/{slug}/preview` | — | 302 → MinIO presigned preview URL |
| `POST` / `PUT /{id}` | | Admin | Create / update (slug uniqueness enforced) |
| `PUT` | `/{id}/active?active=` | Admin | Activate / deactivate |
| `DELETE` | `/{id}` | Admin | Soft-delete (deactivate) |
| `GET` | `/christian/verses` | — | Bible verses (`translation` NIV/KJV/TB/BIS, `category` LOVE/COVENANT/BLESSING) |

### Orders — `/api/v1/orders`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/` | optional JWT | Create order `{templateId?, tier 1–3, coupleName, contactWhatsapp (8–15 digits), contactEmail, notes?}`; anonymous checkout allowed |
| `GET` | `/mine` | Buyer | The caller's orders (paginated) |
| `GET` | `/{id}` | Buyer/Admin | Detail (ownership enforced) |
| `GET` | `/public/{id}` | — | Public summary (payment result pages) |
| `GET` | `/public/lookup?orderNumber=&contact=` | — | Tracking by order number + matching email or WA (digit-suffix tolerant) |
| `GET` | `/` | Admin | Paginated list (`status`, `search` over name/number/email) |
| `PUT` | `/{id}/status` | Admin | `{status, midtransTransactionId?}`; PAID/COMPLETED publish events |
| `POST` | `/{id}/revisions` | Buyer | `{changes: JSON}`; only on PAID/IN_REVISION, quota-checked |
| `PUT` | `/revisions/{revisionId}/complete` | Admin | Complete revision → order back to PAID |
| `GET` | `/{id}/revisions` | Buyer/Admin | Revision history |

### Intake questionnaire

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/orders/{orderId}/intake/questions` | Buyer/Admin | Active questions with `minTier ≤ order.tier` |
| `GET` / `PUT` | `/api/v1/orders/{orderId}/intake` | Buyer/Admin | Read / save answers `{answers, submitted}` |
| `GET`/`POST`/`PUT /{id}`/`DELETE /{id}` | `/api/v1/admin/intake/questions` | Admin | Question CRUD `{section, label, fieldKey, inputType, options[], minTier, required, sortOrder, active}` |

### Payments — `/api/v1/payments`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/charge` | internal | Create Snap transaction for an order (normally driven by the `order.created` event) |
| `GET` | `/snap-token/{orderId}` | — | Snap token + redirect URL for an existing order |
| `POST` | `/webhook/midtrans` (+ `/recurring`, `/pay-account`) | — (signature-validated) | Midtrans notifications; `GIFT-` order ids routed to the gift handler |
| `POST` | `/gifts/charge` | — (public) | Digital gift: `{invitationId, senderName, message?, amount ≥ 20000}` → `{giftPaymentId, snapToken, paymentUrl}` |

### Invitations — public `/api/v1/invitations`

| Method | Path | Description |
|---|---|---|
| `POST` | `/{slug}/rsvp` | `{guestName, phone?, attendance: hadir\|tidak_hadir, guestCount ≥ 1, message?}` |
| `GET` / `POST` | `/{slug}/guestbook` | Approved entries / submit (max 500 chars, pending approval) |
| `GET` | `/{slug}/events` | Structured events with maps links |
| `GET` | `/{slug}/gift-accounts` | Bank/GoPay/OVO/QRIS registry info |
| `POST` | `/{slug}/gift-confirm` | Manual transfer confirmation `{senderName, amount, bankFrom?, proofUrl?, message?}` |
| `GET` / `POST` | `/{slug}/checkin/{code}` | Guest lookup / check-in (form field `actualCount`, defaults 1) |

### Invitations — client portal `/api/v1/invitations/my/{orderId}` (Buyer JWT; `buyerId` in invitation content must match)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Summary (slug, couple name, status) |
| `GET`/`POST` `/guests` · `DELETE /guests/{guestId}` | | Guest management (`{name, groupLabel?, tableNo?, allottedCount ≥ 1}`) |
| `GET` | `/rsvp` · `/attendance` · `/gifts` · `/guestbook` | RSVP list, check-in stats, amplop summary, full guestbook |
| `PUT` | `/guestbook/{entryId}/approve` | Approve an entry |

### Invitations — admin `/api/v1/admin/invitations` (Admin JWT)

| Method | Path | Description |
|---|---|---|
| `GET` | `/`, `/{id}` | List / detail |
| `PUT` | `/{id}/content` | **Merge-patch** invitation content JSON |
| `PUT` | `/{id}/status` · `/{id}/slug` | DRAFT/ACTIVE/EXPIRED; custom slug (validated + unique) |
| `GET`/`POST` `/{id}/guests` · `DELETE /{id}/guests/{guestId}` | | Guest management |
| `GET` | `/{id}/attendance` · `/{id}/rsvp` · `/{id}/guestbook` · `/{id}/gifts` | Stats & lists |
| `PUT` | `/{id}/approve-guestbook/{entryId}` · `/{id}/gift-accounts` | Moderation; registry setup |
| `GET` | `/active-phones` | WA numbers of all ACTIVE invitations (broadcast source) |
| `GET` | `/api/v1/invitations/expiring?days=` | Internal: invitations expiring within N days |

### Media — `/api/v1/media`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/upload/presign` | Buyer | `{filename, contentType, folder: couples/<slug>}` → presigned PUT (type whitelist, sanitized + UUID-prefixed key) |
| `GET` | `/download/**` | Buyer | Presigned GET (bucket resolved from key prefix) |
| `DELETE` | `/**` | Admin | Delete object |
| `POST` | `/template/upload` | Admin | Server-side multipart upload to the templates bucket (size-limited) |

### Notifications — `/api/v1/notifications` (Admin JWT)

| Method | Path | Description |
|---|---|---|
| `POST` | `/broadcast` | `{targetGroup: ALL_ACTIVE\|EXPIRING_7D, message}` — bulk WA |
| `POST` | `/test-wa` | `{phone, message}` — Fonnte connectivity test |

---

## Pages

### Storefront (public site)

| Path | Description |
|---|---|
| `/` | Landing (pricing tiers + featured templates) |
| `/templates` · `/templates/{slug}` | Catalogue (HTMX pagination) / detail |
| `/pesan` | Order flow (template preselect via `?template=`) |
| `/bayar/{orderId}` + `/bayar/selesai` · `/bayar/pending` · `/bayar/gagal` | Snap payment + result pages |
| `/lacak` | Public order tracker |
| `/masuk` · `/daftar` | Login / register (`?redirect=` supported; auth-aware nav) |
| `/pesanan-saya` | Buyer order dashboard |
| `/pesanan/{orderId}/intake` | Post-payment questionnaire wizard |
| `/pesanan/{orderId}/kelola` | Kelola Undangan portal (Tamu / RSVP / Kehadiran / Amplop / Buku Tamu) |
| `/tentang` · `/robots.txt` · `/sitemap.xml` | About + SEO |

### Invitation pages (invitation-service)

| Path | Description |
|---|---|
| `/i/{slug}` | Themed invitation (`?to=` greeting, PIN gate, floating amplop) |
| `/i/{slug}/gift` | Standalone digital gift page |
| `/i/{slug}/scan` | QR check-in scanner (camera + manual code) |
| `/i/{slug}/checkin/{code}` | Guest check-in page |

### Admin back office (`/admin`, admin JWT via cookie)

Dashboard · Orders (list/detail/status/notes/`export.csv`) · Templates (create/edit/toggle/delete) · Invitations (detail/status/**build**/slug/rsvp/guests/attendance/gifts/gift-account/guestbook + RSVP `export.csv`) · Intake questions · WA broadcast · Buyers · Revision complete.

---

## Pricing Tiers

Prices and names come from `app.pricing.tiers` config (order-service is the source of truth at checkout).

| Tier | Price | Highlights |
|---|---|---|
| **Dasar** (1) | Rp 119.000 | 1 template, event info, RSVP, unique link — 0 revisions |
| **Standar** (2) ⭐ | Rp 199.000 | All templates, guestbook, gallery, countdown, interactive map — 0 revisions |
| **Premium** (3) | Rp 249.000 | Everything + music, gift registry, priority support — **2 revisions** |

The intake questionnaire shows/hides questions via each question's `minTier`.

---

## Scheduled Jobs

| Job | Service | Schedule | What it does |
|---|---|---|---|
| Refresh-token purge | auth | 03:00 daily | Deletes expired/revoked refresh tokens |
| Expiry reminders (publish) | invitation | 08:00 WIB daily | Publishes `invitation.expiring` for invitations due within 7 days |
| Bulk expiry | invitation | 00:00 UTC daily | Flips overdue ACTIVE invitations to EXPIRED |
| Expiry reminders (send) | notification | 01:00 UTC daily | Fetches expiring invitations and WhatsApps the couples |

---

## CI / CD

### Continuous Integration (`.github/workflows/ci.yml`)

Runs on every pull request targeting `develop` or `main`:

1. **Build & Test** — `mvn clean verify` (Checkstyle at `validate`, then compile + every module's tests)
2. **Checkstyle** — `mvn checkstyle:check`
3. Surefire reports uploaded as artifacts

Controller tests use `@WebMvcTest` with `@AutoConfigureMockMvc(addFilters = false)` so requests hit real handler mappings; they cover routing, auth/ownership enforcement (401/404) and request-body validation (400).

### Continuous Deployment (`.github/workflows/cd.yml`)

`workflow_dispatch` with an action selector + per-service checkboxes; deploys are serialized via a `deploy` concurrency group (queued, not cancelled):

- **full-deploy** — git pull + `mvn package` + `podman compose up --build`
- **build-service** — rebuild + recreate selected containers (`podman rm -f` + `--force-recreate`)
- **restart-service** — restart selected containers without rebuild
- **restart-all** — remove every container and recreate from current images
- **deploy-config** — sync `config-repo/` and refresh the config server (restart the gateway afterwards when routes changed)
- **deploy-env** — push `.env` from the `ENV_FILE` secret

#### Required GitHub Secrets

`SSH_KEY`, `SSH_USER`, `SSH_HOST` (as `SSH_URL`), `SSH_PORT`, `SSH_PATH` (deploy dir), `ENV_FILE`.

---

## Project Structure

```
DigitalInvitationStore/
├── pom.xml                   # Parent POM — dependency management, Checkstyle wiring
├── common/                   # Shared auto-configured library (envelopes, exceptions, logging)
├── config-server/            # Spring Cloud Config Server
├── config-repo/              # All service YAML incl. gateway routes
├── gateway-service/          # Edge: routing, JWT filter, rate limiting, CORS
├── auth-service/             # Users, RS256 JWTs, refresh rotation, admin seeding
├── storefront-service/       # Public site: catalogue → order → pay → track → intake → kelola
├── template-service/         # Template catalogue, presets, features, Bible verses
├── invitation-service/       # Invitation rendering (4 themes), RSVP/guestbook/gifts/guests/check-in, portal API
├── order-service/            # Orders, revisions, public lookup, intake questionnaire
├── payment-service/          # Midtrans Snap (orders + gifts), webhook + signature validation
├── notification-service/     # Fonnte WA + SMTP email, event consumers, broadcast, expiry reminders
├── admin-service/            # Back office incl. Bangun Undangan and intake builder
├── media-service/            # MinIO façade (3 buckets, presign, validation)
├── checkstyle/checkstyle.xml # Google style, 120 cols, 4-space indent
├── docs/server-setup.sh      # Server provisioning script
├── podman-compose.yml        # Full stack orchestration (healthcheck-ordered)
└── .env.example              # Environment variable template
```

---

## Contributing

1. Fork the repository and create a feature branch off `develop`
2. Write code that passes `mvn clean verify` (build + tests + Checkstyle)
3. Open a pull request against `develop` — CI runs automatically
4. Merges to `main` trigger deployment to production

### Code Style

[Google Java Style](https://google.github.io/styleguide/javaguide.html) with two adjustments: **120-char lines** and **4-space indentation**. Run `mvn checkstyle:check` before pushing.

---

## License

[MIT](LICENSE)
