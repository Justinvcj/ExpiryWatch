# ExpiryWatch — Architecture

## 1. System Overview

Two clients, one backend:

```
                    ┌─────────────────────┐
                    │   Web Dashboard      │
                    │   (Thymeleaf, served │
                    │   by Spring Boot)    │
                    └──────────┬───────────┘
                               │ HTTP (session auth)
                               ▼
┌──────────────────┐   ┌──────────────────────────┐
│ Chrome Extension  │──▶│   Spring Boot Backend    │
│ (content script + │   │   (REST API + web app,   │
│  popup, token auth)│  │    single deployable)    │
└──────────────────┘   └──────────┬───────────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
             ┌───────────┐ ┌─────────────┐ ┌─────────────┐
             │ PostgreSQL│ │ Tess4J (OCR,│ │  Gmail API  │
             │ (Neon/    │ │ in-process, │ │  (reminder  │
             │ Supabase) │ │ local)      │ │  emails)    │
             └───────────┘ └─────────────┘ └─────────────┘
```

**Single deployable artifact.** The Spring Boot app serves both the Thymeleaf dashboard and the JSON REST API the extension calls. One Docker image, one Render service, one thing to deploy and debug. This is a deliberate simplicity choice — a separate frontend/backend split buys nothing here and adds a second free-tier account, CORS config, and deploy pipeline to maintain for zero benefit at this scale.

## 2. Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Backend framework | Spring Boot 3 (Java 17) | Resume requirement; also genuinely fits the transactional/scheduled-job nature of this app |
| Web frontend | Thymeleaf (server-rendered) | No separate deploy, no CORS, fastest to ship v1 |
| Database | PostgreSQL — Neon or Supabase free tier | Managed, free, no VM needed |
| OCR | Tess4J (Tesseract Java binding) | Free, runs in-process, no per-document API cost |
| Scheduling | Spring `@Scheduled` (Batch only if volume ever demands it — it won't, don't over-engineer) | Free, built into Spring Boot |
| Email | Gmail API (OAuth, your own account) | Free tier, generous quota for personal-scale reminder volume |
| Extension | Vanilla JS/HTML, Manifest V3 | No framework needed for this scope; smaller, easier to reason about |
| Hosting | Render free Web Service (Docker) | No VM; free; supports the Tesseract native binary via Dockerfile |
| Encryption (Phase 4) | AES via `javax.crypto`, or `jasypt` for simpler config | Sensitive structured fields (policy/passport numbers) require encryption at rest |

## 3. Database Schema (full, including Phase 4)

```sql
create table users (
  id uuid primary key default gen_random_uuid(),
  email text unique not null,
  password_hash text not null,
  created_at timestamp default now()
);

create table document_types (
  id serial primary key,
  name text unique not null,           -- 'vehicle_insurance', 'puc', 'passport', 'visa', 'gym_membership',
                                        -- 'domain', 'software_license', 'amc', 'rent_agreement', 'warranty', 'other'
  default_severity text not null,      -- 'fine' | 'financial_loss' | 'legal' | 'minor'
  reminder_schedule_days int[] not null default '{30,7,1}'
);

create table document_type_fields (
  id serial primary key,
  document_type_id int references document_types(id) not null,
  field_name text not null,            -- 'policy_number', 'passport_number', 'license_number', 'registration_number', 'member_id'
  extraction_pattern text not null,    -- regex used by FieldExtractionService for this field on this document type
  is_sensitive boolean default true    -- drives whether the value is encrypted at rest
);

create table documents (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references users(id) not null,
  document_type_id int references document_types(id) not null,
  title text not null,
  raw_ocr_text text,
  extracted_expiry_date date not null,
  confidence_score numeric,
  severity text not null,
  status text default 'active',        -- 'active' | 'expired' | 'renewed' | 'archived'
  file_url text,
  created_at timestamp default now()
);

create table document_fields (
  id uuid primary key default gen_random_uuid(),
  document_id uuid references documents(id) not null,
  field_name text not null,
  field_value_encrypted text,          -- AES-encrypted if is_sensitive, plaintext otherwise
  confidence_score numeric,
  confirmed boolean default false      -- user has reviewed/confirmed this value
);

create table reminders (
  id uuid primary key default gen_random_uuid(),
  document_id uuid references documents(id) not null,
  scheduled_for date not null,
  sent boolean default false,
  sent_at timestamp
);

create table api_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references users(id) not null,
  token_hash text not null,            -- hash the token, never store it plaintext, same principle as a password
  created_at timestamp default now(),
  last_used_at timestamp,
  revoked boolean default false
);
```

## 4. API Surface (used by the extension)

All extension-facing endpoints are under `/api/` and authenticated via bearer token (the `api_tokens` table), not session cookies — the extension isn't a browser session on your dashboard's origin.

- `POST /api/tokens` — generate a new personal access token (called from the dashboard UI, session-authenticated, shown once).
- `GET /api/fields/match?field_name=policy_number` — returns the user's stored, confirmed `document_fields` rows matching that field name (decrypted server-side, sent over HTTPS only). Never returns fields not explicitly requested.
- `POST /api/fields/usage` — optional: log when a field was actually used for autofill, for the user's own audit trail on the dashboard ("last autofilled on X site, on Y date").

## 5. Chrome Extension Architecture

- `manifest.json` — Manifest V3, `permissions: ["activeTab", "storage"]`, `host_permissions: ["<all_urls>"]` (needed since autofill has to work on any site, not a fixed list).
- `content-script.js` — runs on every page, scans `document.querySelectorAll('input')`, checks each field's `name`, `id`, `autocomplete`, and nearby `<label>` text against a known keyword list per field type (e.g. "policy," "policy no," "policy number" → `policy_number`).
- On a match, injects a small clickable icon inside/adjacent to the field (mirrors the password-manager key-icon pattern).
- On click, calls `GET /api/fields/match` with the stored token (kept in `chrome.storage.local`, entered once during setup), presents matching values in a small dropdown, fills the field on selection.
- No background scraping, no polling, no data sent anywhere until the user clicks. This is a deliberate minimal-footprint design — it should be inert until explicitly invoked.

## 6. Security Considerations

- Passwords: BCrypt, never plaintext, never logged.
- API tokens: stored as a hash (like passwords), shown to the user exactly once at generation time, revocable from the dashboard.
- Sensitive `document_fields` values (policy numbers, passport numbers, etc.): AES-encrypted at rest. The encryption key lives in an environment variable, never in source control.
- All API traffic over HTTPS only (Render provides this by default).
- The extension requests exactly one field type at a time and only on explicit user click — no bulk export endpoint exists that the extension (or a compromised copy of it) could call to exfiltrate everything at once.
- OCR raw text (`raw_ocr_text`) is retained for debugging/re-parsing but should be treated with the same sensitivity as the source document — it's not scrubbed of sensitive numbers automatically in v1; note this as a known limitation, not silently ignore it.

## 7. Deployment (no VM)

1. `Dockerfile` — base `eclipse-temurin:17-jdk`, `apt-get install tesseract-ocr` in the build stage, copy the built jar, `ENTRYPOINT ["java","-jar","app.jar"]`.
2. Render free Web Service, Docker environment, connected to GitHub repo.
3. Environment variables in Render: datasource URL/credentials, Gmail OAuth refresh token, AES encryption key, Spring profile.
4. Neon or Supabase for managed Postgres — no self-hosted database, no VM.
5. Optional: UptimeRobot free tier pinging the Render URL every 5 minutes to reduce cold-start delay for demos.
