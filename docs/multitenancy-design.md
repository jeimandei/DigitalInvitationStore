# baundang.id — Multi-Tenancy & Founding-Template Design

**Status:** proposal · **Date:** 2026-08-14
**Scope:** how `Wedding-Invitation` becomes the founding template inside `DigitalInvitationStore`, what has to change to support paying clients, and in what order to build it.

---

## 0. Headline finding — the premise needs adjusting

The request framed this as *"combine my single-tenant wedding site into a new multi-tenant platform."* That is not the situation on disk.

**`DigitalInvitationStore` is already the multi-tenant platform.** It is a Java 21 / Spring Boot 3.5 microservices system — 239 Java files across 10 services, plus Postgres, Redis, RabbitMQ and MinIO — and it already implements *most* of the feature list attributed to `Wedding-Invitation`, but properly multi-tenant: per-order invitations, server-rendered themes, real auth, isolated per-invitation data, Midtrans payments, and a working admin back office.

So the work is **not** "make the wedding site multi-tenant." It is three much smaller, much better-defined jobs:

| # | Job | Size |
|---|---|---|
| A | Fix four real defects that block onboarding paying clients | small, urgent |
| B | Build client-side **content editing** — the hybrid model is specified but not implemented | medium, the main gap |
| C | Close a four-item feature gap and land Wedding-Invitation's *visual design* as a theme | medium |

The most important correction: **`Wedding-Invitation` is a design and UX donor, not a code donor.** Its HTML/JS architecture is precisely the single-tenant pattern being escaped, and DIS already has a server-side equivalent of nearly every behaviour. What is genuinely worth harvesting is the 2,826 lines of proven CSS, the production-tested check-in ergonomics, and four features DIS lacks.

---

## 1. What `DigitalInvitationStore` is today

```
Browser ─▶ gateway :1080  (Spring Cloud Gateway, RS256 JWT filter, Redis rate limit)
             ├─▶ storefront   :1082   public site, order flow, client portal
             ├─▶ auth         :1081   BCrypt-12, RS256 JWT, rotating refresh tokens
             ├─▶ template     :1083   catalogue, style presets, Bible verses
             ├─▶ invitation   :1084   ★ renderer, RSVP, guestbook, guests, check-in, gifts
             ├─▶ order        :1085   orders, revisions, intake questionnaire
             ├─▶ payment      :1086   Midtrans Snap + webhook signature validation
             ├─▶ notification :1087   Fonnte WhatsApp + SMTP
             ├─▶ admin        :1088   back office (Bangun Undangan, intake builder)
             └─▶ media        :1089   MinIO façade, presigned upload/download
```

**Tenancy model.** One shared deployment; a tenant is a **row**, not a stack. `invitation.invitations` is keyed by unique `order_id` and unique `couple_slug`, with all per-couple content in a single JSONB `content` column. Guests, RSVPs, guestbook entries, gifts and gift accounts all hang off `invitation_id`.

This is the right model and it is worth stating plainly, because the brief said *"I handle the technical setup/deployment for each client site."* **Do not deploy a stack per client.** "Setup per client" here means admin actions in the back office — build the invitation, set the slug, upload photos — not per-client infrastructure. That is already how the system works, and it is what keeps a handful of clients operationally free.

**Theming.** Four presets — GRACE, COVENANT, EDEN, GLORIA — implemented as CSS custom properties keyed on `[data-theme="..."]`, all inline in a single 684-line `invitation/view.html`, selected by `content.stylePreset`.

---

## 2. Feature parity: `Wedding-Invitation` → `baundang.id`

| Wedding-Invitation feature | Status in DIS | Notes |
|---|---|---|
| RSVP form | ✅ Built | `POST /api/v1/invitations/{slug}/rsvp` → `rsvp.submitted` → WhatsApp to couple |
| Guestbook / wish wall | ✅ Built, better | Adds moderation (approve before publish); Redis-cached |
| Per-guest QR invite links | ✅ Built, better | Random 24-hex `invite_code` per guest; `?to=` personal greeting |
| Entrance-pass check-in scanner | ✅ Built | `/i/{slug}/scan`, html5-qrcode + manual-code fallback |
| QR card printing | ✅ Built | Admin print-all sheet |
| Gift / angpao tracking | ✅ Built, better | Real Midtrans Snap payments, not just a tally |
| QRIS payment | ✅ Built | Snap + manual QRIS registry image with confirmation flow |
| Live arrival dashboard | ⚠️ **Partial** | `AttendanceDTO` stats exist; poll-based, no live push |
| Admin: photo management | ⚠️ **Partial** | media-service + presigned uploads exist; no gallery manager UI |
| Admin: guest import | ❌ **Gap** | Guests are added one at a time; no CSV / bulk-paste import |
| Admin: livestream config | ❌ **Gap** | Zero occurrences anywhere in the codebase |
| Automatic tax calculation | ❌ **Gap** | No equivalent of the 0.7%-above-Rp 500.000 QRIS deduction |

**Four things to build, one to finish.** That is the entire functional gap.

---

## 3. Defects found — fix before onboarding paying clients

These are real, verified in code, and each one bites the hybrid model directly.

### 3.1 Anonymous checkout permanently orphans the order 🔴

`order-service/.../controller/OrderController.java:47`

```java
UUID buyerId = auth != null ? UUID.fromString(auth.getName()) : UUID.randomUUID();
```

An anonymous buyer is assigned a **random UUID** as owner — a value matching no user account, with no claim or link flow anywhere. That buyer can never open `/pesanan-saya` or `/pesanan/{id}/kelola`: `requireOwned` compares the JWT subject to this orphan UUID and always throws. The random UUID exists only to satisfy `buyer_id NOT NULL`.

Since the whole hybrid model rests on clients self-serving their content, this is a blocker.

**Fix:** make `buyer_id` nullable; on anonymous checkout store a claim token alongside the contact details already collected, and let the buyer bind the order to an account at first login (the `/lacak` public tracker already proves identity by order number + email/WA — reuse that check as the claim gate).

### 3.2 Tenant ownership lives inside a mutable JSONB blob 🔴

`MyInvitationApiController.requireOwned` resolves the tenant owner by reading `content.buyerId`. The `invitations` table has **no owner column** — ownership is a key inside the same JSON document the admin edits.

And the admin content update is an unfiltered merge-patch, `InvitationService.java:174`:

```java
ObjectNode merged = (ObjectNode) existing.deepCopy();
merged.setAll((ObjectNode) patch);      // any key, including buyerId
```

A patch carrying `buyerId` silently reassigns the tenant. A patch that arrives non-object replaces content wholesale (line 177) and drops ownership entirely.

**Fix:** promote ownership to a first-class `invitations.buyer_id` column with an index; have `requireOwned` read the column, never the JSON; and strip reserved keys (`buyerId`, `orderId`, `slug`) from every inbound patch. This is the single highest-value change in the document — it is what makes "isolated per-client data" true by construction rather than by convention.

### 3.3 The PIN gate is exactly as cosmetic as the one being replaced 🟠

The brief noted that Wedding-Invitation's PIN gates are "cosmetic/client-side only." **DIS has the same flaw**, which is worth knowing since the migration was expected to fix it.

`invitation/view.html:613` renders the plaintext PIN into the page:

```js
requiredPin: /*[[${accessPin}]]*/ '',
```

It is compared in Alpine on the client, and the full invitation is already in the DOM behind `x-show="!pinOk"`. View-source defeats it completely.

**Fix:** move the check server-side in `InvitationPageController` — on a PIN-protected slug, render a gate page only; on correct POST, set a short-lived signed cookie scoped to that slug and render the real page. Store the PIN hashed. Never send `accessPin` to the client.

### 3.4 Committed secret in `Wedding-Invitation` — do not carry it over 🟠

`apps-script.js:60`, `js/script.js:567` and `gifts.html:985` all contain:

```js
const SALT = 'ShadowRubyAsh120122';
```

Guest invite IDs are `SHA-256(normalizedName + SALT)`. With the salt public in client-side JS, **anyone can forge any guest's invite and check-in code** from their name alone. The same file also hard-codes the Apps Script deployment URL and the Spreadsheet ID, and identical client-side PIN hashes are duplicated across four HTML files.

**Action:** none of this scheme migrates. DIS's random 24-hex `invite_code` is already the correct design. Treat the salt, the script URL and the sheet ID as compromised and rotate them if that deployment stays live.

---

## 4. The template model — how Wedding-Invitation becomes reusable

### 4.1 Name the two different things called "template"

DIS currently overloads the word, which is the root of the confusion:

| Today's name | What it really is | Rename to |
|---|---|---|
| `template-service` `templates` row | catalogue/marketing entry — name, thumbnail, price level, category | **Product** |
| `content.stylePreset` | the actual visual rendering | **Theme** |

They are only loosely coupled: the catalogue can list twenty Products while there are only four Themes. Making that split explicit prevents the trap below.

### 4.2 The trap to avoid

The obvious move — port `Wedding-Invitation`'s HTML/CSS/JS in as its own renderer — recreates the exact problem being escaped, one level up. Instead of duplicated constants across five files, you get duplicated *renderers* across N templates: every bug fix, every new section, every RSVP change has to be applied N times. At three clients it feels fine; at fifteen it is the same copy-paste swamp with more surface area.

### 4.3 Recommended: one renderer, N themes, composable sections

Keep the single `view.html` renderer. Add two things:

**1. Extract themes to their own files.** All four palettes currently sit inline in `view.html`. Move each to `static/themes/{THEME}.css` and load one per request. Wedding-Invitation's CSS becomes the fifth file, `SIGNATURE.css` — its aesthetic lands with zero renderer changes, and adding a sixth theme becomes a CSS-only task any designer can do.

**2. Add a section manifest to the Product's existing JSONB `config`.** A Product declares which sections render and in what order:

```jsonc
{
  "theme": "SIGNATURE",
  "sections": [
    "cover", "couple", "story", "events", "gallery",
    "livestream", "rsvp", "guestbook", "gift"
  ]
}
```

`view.html` becomes a loop over that manifest, each section a Thymeleaf fragment in `templates/sections/`. That yields real structural variety between products — not just recoloured clones — while every section has exactly one implementation.

**Net effect:** a new template = one CSS file + a JSON array. No Java, no renderer fork, no duplicated logic. This is the concrete answer to "config-driven theming/content instead of hardcoded values."

### 4.4 What actually migrates from Wedding-Invitation

| From | To | How |
|---|---|---|
| `css/styles.css` (2,826 lines) | `static/themes/SIGNATURE.css` | Rewrite selectors against the existing custom-property contract |
| Check-in UX (table numbers, "Next Guest") | `invitation/scanner.html`, `checkin.html` | Port the ergonomics; the backend already exists |
| Guestbook card layout | `sections/guestbook.html` | Presentation only |
| Livestream config | new content key + section | New capability (§2) |
| QRIS tax calculation | invitation-service gift summary | New capability — server-side, not client-side |
| Bulk guest import | client portal + admin | New capability |
| `apps-script.js` backend | — | **Discard.** Fully superseded by invitation-service |
| PIN hashes, `SALT`, script URL | — | **Discard.** See §3.4 |

---

## 5. Client management dashboard — recommended features

### 5.1 The gap that matters most

The brief specifies: *"clients edit their own content — names, date, photos, venue, RSVP settings — through a management dashboard."*

**That does not exist yet.** Today content editing is 100% admin-side: the client fills a one-time intake questionnaire, then the owner builds everything through *Bangun Undangan* (`PUT /api/v1/admin/invitations/{id}/content`, admin JWT required). The client portal at `/pesanan/{orderId}/kelola` is **read-and-operate only** — Tamu, RSVP, Kehadiran, Amplop, Buku Tamu. There is no path for a couple to fix their own venue typo.

Building this is the single biggest piece of new work, and it is what makes the hybrid model real.

### 5.2 Recommended dashboard feature set

Built on the existing `/api/v1/invitations/my/{orderId}` controller, which already has the ownership pattern (once §3.2 lands).

**Tier 1 — required for the hybrid model**

| Feature | Why | Build note |
|---|---|---|
| **Content editor** | The stated core of the model | Reuse *Bangun Undangan*'s form against a **field allowlist** — client may edit `coupleName`, dates, venues, `loveStory`, photos; may **not** touch `stylePreset`, `accessPin`, `buyerId`, slug, status |
| **Photo / gallery manager** | Most-requested edit; media-service already supports it | Presigned upload to `couples/{slug}`, reorder, delete, set cover |
| **Bulk guest import** | Feature gap; guest lists arrive as spreadsheets | CSV + paste-a-list, with a preview-and-confirm step |
| **Preview / publish** | Clients must see edits before guests do | Draft content beside live; explicit Publish |
| **Share kit** | Every couple needs this on day one | Per-guest link copy, QR download, WhatsApp broadcast text |

**Tier 2 — high value, low cost**

| Feature | Why |
|---|---|
| **Live arrival dashboard** | Finish the partial: existing `/attendance` + polling, big-screen mode for the venue |
| **RSVP settings** | Deadline, on/off, max guests per invite, custom question |
| **Guestbook moderation** | Already built — surface it properly |
| **Gift/amplop summary + QRIS tax** | Reconciliation view with the 0.7% deduction |
| **Livestream config** | Feature gap; URL + go-live window |

**Tier 3 — defer past v1**

Custom domains, multi-language, self-serve theme switching (keep this owner-side deliberately — it protects the visual quality that is the product), analytics beyond the view counter.

**Deliberately owner-only, matching the hybrid model:** theme selection, slug, PIN, order status, revision completion, template/product management.

---

## 6. User stories

### 6.1 Platform owner (you)

- As the owner, I want a new client's invitation created automatically when their payment settles, so onboarding needs no manual database work. *(built)*
- As the owner, I want to build a client's invitation from their questionnaire answers in one click, so setup takes minutes. *(built — "Isi dari Kuesioner")*
- As the owner, I want to set each client's theme, slug and PIN, so the visual quality and URL structure stay under my control. *(built)*
- As the owner, I want clients to edit their own content within a bounded set of fields, so I am not the bottleneck for a venue typo. **(gap — §5.1)**
- As the owner, I want a client's edits to be impossible to apply to another client's invitation, so tenancy is guaranteed by schema and not by convention. **(gap — §3.2)**
- As the owner, I want one deployment serving all clients, so a handful of clients costs one server. *(built)*
- As the owner, I want to see every order's status and payment in one dashboard, so I know what needs work. *(built)*

### 6.2 Client (the couple)

- As a couple, I want to order and pay without creating an account first, so checkout is frictionless. *(built)*
- As a couple, I want to claim that order into my account afterwards, so I can manage it. **(gap — §3.1, currently impossible)**
- As a couple, I want a guided questionnaire after payment, so I know exactly what to supply. *(built)*
- As a couple, I want to fix our venue, date or story myself, so small corrections do not need a support message. **(gap)**
- As a couple, I want to upload and reorder our photos, so the gallery is ours. **(gap)**
- As a couple, I want to import our guest list from a spreadsheet, so I am not typing 200 names. **(gap)**
- As a couple, I want a personal link and QR per guest, so each is greeted by name and check-in works. *(built)*
- As a couple, I want to preview changes before guests see them, so we never publish a half-edit. **(gap)**
- As a couple, I want a WhatsApp alert on each RSVP and gift, so we can track responses live. *(built)*
- As a couple, I want to approve wishes before they appear, so the wall stays clean. *(built)*
- As a couple, I want to watch arrivals live on the day, so we know who is in the room. **(partial)**
- As a couple, I want gift totals net of QRIS fees, so reconciliation matches the bank. **(gap)**

### 6.3 Wedding guest

- As a guest, I want the invitation to open fast on my phone and greet me by name. *(built)*
- As a guest, I want the event details with a working map link. *(built)*
- As a guest, I want to RSVP in a few taps without an account. *(built)*
- As a guest, I want to leave a wish for the couple. *(built)*
- As a guest, I want to send a gift digitally without signing up. *(built)*
- As a guest, I want my QR to check me in at the door in one scan, showing my table. *(built)*
- As a remote guest, I want to watch the ceremony stream from the invitation. **(gap)**
- As a guest, I want a PIN-protected invitation to actually be protected. **(gap — §3.3)**

---

## 7. Recommended architecture

**Keep what exists. Add no new services.**

Ten microservices plus four infrastructure components is, candidly, more architecture than a handful of clients needs — a monolith with Postgres would serve this load on one small VM. But it is built, tested, deployed and working, and rewriting it would burn the entire runway for zero customer-visible gain. The brief asks for *simple, able to grow later, not over-built up front*; the correct reading here is **stop expanding the topology and grow inside the current boundaries.**

Three standing rules:

1. **Every new capability lands in an existing service.** Livestream, gallery, bulk import and tax all belong in invitation-service. Resist a `gallery-service`.
2. **Tenancy is enforced by column, not by document.** `buyer_id` on `invitations`, indexed, read directly by `requireOwned`. Every `/my/**` endpoint goes through one ownership helper — no exceptions, so a missed check is impossible rather than unlikely.
3. **One renderer, N themes.** Per §4.3, so template count never multiplies maintenance.

**What to leave alone:** JWT/gateway auth, the RabbitMQ event flow, Midtrans integration, MinIO media, the Flyway-per-service schema split. All sound.

**Watch items, not action items:** RabbitMQ for a handful of clients is heavy but harmless; Redis caching earns its place at the invitation-render path. Revisit only if operational pain shows up.

---

## 8. Phased rollout

### Phase 0 — Make it safe to sell (1–2 weeks)

Nothing else ships until these land.

1. `invitations.buyer_id` column + index; `requireOwned` reads the column (§3.2)
2. Reserved-key filter on the admin content merge-patch (§3.2)
3. Anonymous-order claim flow; `buyer_id` nullable (§3.1)
4. Server-side PIN gate, hashed, signed cookie (§3.3)
5. Rotate the exposed Apps Script salt / URL / sheet ID if that deployment stays live (§3.4)

**Exit:** a client can buy anonymously, claim the order, and reach a portal that only ever shows their own data.

### Phase 1 — Make the hybrid model real (2–3 weeks)

6. Client content editor with field allowlist (§5.1)
7. Photo/gallery manager on the existing media-service
8. Preview/publish split — draft content beside live
9. Share kit: per-guest links, QR download, WhatsApp broadcast text

**Exit:** a couple corrects their own venue typo and republishes without contacting you. This is the phase that removes you as the bottleneck.

### Phase 2 — Close the feature gap (1–2 weeks)

10. Bulk guest import (CSV + paste, preview-and-confirm)
11. Live arrival dashboard — finish the partial, big-screen venue mode
12. Livestream config: content key, section, client setting
13. QRIS tax calculation server-side in the gift summary

**Exit:** full parity with the production wedding site, multi-tenant.

### Phase 3 — Land the founding template (1–2 weeks, parallelisable with Phase 2)

14. Extract the four inline themes to `static/themes/{THEME}.css`
15. Decompose `view.html` into `sections/` fragments
16. Section manifest in the Product's JSONB `config`
17. Wedding-Invitation's CSS lands as `SIGNATURE.css` — the founding template

**Exit:** a new template is one CSS file plus a JSON array. Phase 3 touches templates and CSS while Phase 2 touches Java, so the two rarely collide.

### Phase 4 — Only when demand proves it out

Self-serve theme switching, custom domains, multi-language, analytics, subscription billing. None of it belongs in v1, and the one-time-fee model means no billing infrastructure is needed at all.

---

## 9. Summary

| Question asked | Answer |
|---|---|
| What's in `digitalinvitationstore`? | A working 10-service multi-tenant platform, not a greenfield repo — §1 |
| How do the two combine? | Wedding-Invitation is a **design donor**: its CSS becomes a fifth theme, four features become backend capabilities, its HTML/JS architecture is discarded — §4 |
| Config-driven theming? | Theme files + a section manifest in the Product's JSONB config; one renderer — §4.3 |
| Centralise duplicated constants/PIN/hash? | They do not migrate. The salt is a live vulnerability; DIS's random invite codes already supersede it — §3.4 |
| Real server-side auth? | Mostly built (RS256 JWT, BCrypt-12), but the **PIN gate is still client-side** and ownership is not enforced by schema — §3.2, §3.3 |
| Isolated per-client data? | Correct by design, but ownership sits in a mutable JSON blob. Promote it to a column — §3.2 |
| Client dashboard features? | Tiered list in §5.2. The content editor is the missing core |
| User stories? | §6 — with each story marked built / partial / gap |
| Architecture + rollout? | Keep the topology, add no services, grow inside current boundaries — §7; four phases, ~6–9 weeks — §8 |

**If only one thing gets done:** the ownership column and its patch filter (§3.2). Every client-facing feature in Phases 1–2 is built on top of that check, and it is the difference between multi-tenancy that is enforced and multi-tenancy that is merely intended.
