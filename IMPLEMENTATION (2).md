# ExpiryWatch — Implementation Plan

Build phases strictly in order. No phase starts before the previous phase's completion criteria (listed at the end of each) are met.

---

## Phase 1 — Core Backend: Auth + Document CRUD + Schema

**Goal:** A working Spring Boot app with login and manual document entry (no OCR yet), deployed and reachable on a URL, no VM.

### Step 1.1 — Scaffold the project
1. Generate a Spring Boot 3 project via Spring Initializr: Spring Web, Spring Data JPA, Spring Security, PostgreSQL Driver, Thymeleaf, Validation.
2. Set up `application.yml` with datasource config read from environment variables.
3. Confirm the app boots locally (`./mvnw spring-boot:run`) and serves a blank index page.

### Step 1.2 — Set up the database
1. Create a free Neon or Supabase Postgres project.
2. Run the core schema tables: `users`, `document_types`, `documents`, `reminders` (see ARCHITECTURE.md section 3 — skip `document_type_fields`, `document_fields`, `api_tokens` for now, add those in Phase 4).
3. Point local `application.yml` at it via environment variables, confirm Spring Boot connects and validates the schema on boot.

### Step 1.3 — Build entities and repositories
1. Create JPA entities matching the schema exactly (`@Table`, `@Column`) — don't let Hibernate auto-generate a different structure.
2. Create Spring Data repositories: `UserRepository`, `DocumentTypeRepository`, `DocumentRepository`, `ReminderRepository`.
3. Write a `CommandLineRunner` `DataInitializer` that seeds `document_types` with the standard set (vehicle_insurance, puc, passport, visa, gym_membership, domain, software_license, amc, rent_agreement, warranty, other) on first boot if empty.

### Step 1.4 — Implement authentication
1. Configure Spring Security with form login and `BCryptPasswordEncoder`.
2. Build `/register`: collects email + password, hashes, saves `User`.
3. Build `/login` using Spring Security's form login, customized template.
4. Confirm `/documents/**` requires an authenticated session; unauthenticated requests redirect to `/login`.

### Step 1.5 — Manual document entry
1. Build `/documents/new`: title, document type dropdown, expiry date picker, severity (pre-filled from document type default, editable).
2. On submit, save to `documents`, tied to the logged-in user.
3. Build `/documents`: list sorted by `extracted_expiry_date` ascending, color-coded by severity.
4. Build `/documents/{id}`: detail view with "Archive" / "Mark Renewed" actions updating `status`.

### Step 1.6 — Deploy (no VM)
1. Write the `Dockerfile` (see ARCHITECTURE.md section 7).
2. Push to GitHub.
3. Create a Render free Web Service, Docker environment, connect the repo.
4. Set environment variables in Render (datasource, Spring profile).
5. Deploy, confirm the live Render URL serves login and manual document entry end to end against the live Postgres instance.

**Phase 1 complete when:** register, log in, add a document manually, see it correctly sorted, all on a live Render URL.

---

## Phase 2 — OCR Extraction

**Goal:** Replace manual date entry with automatic extraction from an uploaded document image/PDF.

### Step 2.1 — Add Tesseract to the Docker image
1. Update `Dockerfile`: `RUN apt-get update && apt-get install -y tesseract-ocr` in the build stage.
2. Add Tess4J to `pom.xml`.
3. Confirm the Tesseract binary is reachable inside the built container before proceeding.

### Step 2.2 — File upload endpoint
1. Update `/documents/new` to accept a `MultipartFile` alongside the manual form.
2. Store the file (byte array in Postgres for simplicity in v1, or Supabase Storage free tier if you want it decoupled from the DB) — save the URL/reference to `file_url`.

### Step 2.3 — Run OCR on upload
1. Pass the uploaded file to Tess4J's `Tesseract.doOCR(file)`.
2. Store the raw result in `raw_ocr_text` regardless of downstream success, so re-parsing later doesn't require re-uploading.

### Step 2.4 — Extract the expiry date
1. Build `DateExtractionService`: regex for common date formats (`dd/mm/yyyy`, `dd-mm-yyyy`, `dd MMM yyyy`, `Month dd, yyyy`) combined with keyword-proximity scoring (dates near "expiry," "valid until," "renewal due," "expires on" score higher).
2. Rank multiple candidate dates by proximity score, select the top one, store a `confidence_score`.
3. If confidence is below a set threshold, don't auto-save — flag as needing review, show the raw OCR text plus a manual date field pre-filled with the best guess.
4. Wire into `/documents/new`: uploaded file → OCR → auto-filled date + confidence badge → user confirms before final save. Never silently auto-save an unconfirmed extraction.

### Step 2.5 — Real-document testing
1. Test with an actual insurance PDF, a photographed warranty card, and a scanned rent agreement.
2. Log failure patterns (bad angle, low-resolution photo, unusual date format) and adjust regex/keyword lists accordingly. Budget real iteration time here — this step is not a one-shot.

**Phase 2 complete when:** uploading a real photographed document produces a correct or clearly-flagged-for-review expiry date.

---

## Phase 3 — Reminders + Dashboard Polish

**Goal:** Fully automatic escalating reminders with zero manual triggering, and a dashboard sorted by real urgency.

### Step 3.1 — Gmail API setup
1. Create a Google Cloud project, enable Gmail API.
2. Configure OAuth consent screen in testing mode, add your own account as test user.
3. Implement the OAuth flow in Spring Boot, store the refresh token as an environment variable (never in source control).

### Step 3.2 — Reminder generation
1. On document save/update, generate `Reminder` rows per `document_types.reminder_schedule_days` (default 30/7/1 days before `extracted_expiry_date`).
2. Write a daily `@Scheduled` job: find `reminders` where `scheduled_for <= today AND sent = false`, send via Gmail API, mark `sent = true`.
3. Vary email tone by severity + days remaining — a 1-day `legal`-severity reminder should read urgent, a 30-day informational one should not.

### Step 3.3 — Auto-expire stale documents
1. Daily scheduled check: `extracted_expiry_date < today AND status = 'active'` → set `status = 'expired'`.
2. Surface expired-and-unrenewed documents at the top of the dashboard, visually distinct (red).

### Step 3.4 — Dashboard polish
1. Group `/documents` by urgency tier: Expired / Due this week / Due this month / Everything else.
2. Add a summary header: count expiring in next 30 days, and how many are `legal`/`financial_loss` severity.
3. Add filter/search by document type.

### Step 3.5 — End-to-end test
1. Create a test document with `extracted_expiry_date` set to tomorrow.
2. Trigger (or wait for) the scheduled job, confirm a real email lands in your inbox.
3. Confirm auto-expire flips status correctly the day after expiry if unrenewed.

**Phase 3 complete when:** reminders fire automatically with a real email delivered, and the dashboard clearly surfaces what's urgent without manual scanning.

---

## Phase 4 — Structured Field Extraction + Autofill (the differentiator)

**Goal:** Extract identifying document numbers (not just dates) and autofill them into web forms via a Chrome extension, click-to-fill, like a password manager.

### Step 4.1 — Extend the schema
1. Add `document_type_fields`, `document_fields`, `api_tokens` tables (see ARCHITECTURE.md section 3).
2. Seed `document_type_fields` per document type: e.g. `vehicle_insurance` → `policy_number` (regex for typical alphanumeric policy formats); `passport` → `passport_number`; `visa` → `visa_number`; `gym_membership` → `member_id`; `software_license` → `license_key`.

### Step 4.2 — Build the field extraction service
1. Build `FieldExtractionService`, generalizing `DateExtractionService`'s approach: for each `document_type_fields` row matching the document's type, run its `extraction_pattern` regex against `raw_ocr_text`, score by keyword proximity same as dates.
2. Store results in `document_fields` with `confidence_score`, `confirmed = false`.
3. On the document detail page, show extracted fields for user confirmation (same principle as date extraction — never silently trust OCR).

### Step 4.3 — Encrypt sensitive fields at rest
1. Implement an `AES` encrypt/decrypt utility (or use `jasypt` for simpler config-driven encryption).
2. On save, encrypt `field_value` before persisting to `field_value_encrypted` for any field where `document_type_fields.is_sensitive = true`.
3. Decrypt only server-side, on demand, when serving `/api/fields/match` — never send the encryption key or raw encrypted blob to any client.

### Step 4.4 — Build the token system
1. Add `/api/tokens` (session-authenticated): generates a random token, stores its hash in `api_tokens`, returns the plaintext token to the user exactly once.
2. Add a "Revoke" action on the dashboard that sets `revoked = true`.
3. Build a Spring Security filter or interceptor that authenticates `/api/**` requests via bearer token, checking the hash against `api_tokens` and rejecting revoked/unknown tokens.

### Step 4.5 — Build the field-match endpoint
1. `GET /api/fields/match?field_name=policy_number` — authenticated via the bearer token, looks up the token's `user_id`, returns that user's `document_fields` rows where `field_name` matches and `confirmed = true`, decrypted.
2. Return only field name, value, and the source document's title (for disambiguation if multiple matches exist) — nothing else.

### Step 4.6 — Build the Chrome extension
1. Scaffold `manifest.json` (Manifest V3), `permissions: ["activeTab","storage"]`, `host_permissions: ["<all_urls>"]`.
2. Build a setup popup: paste in your ExpiryWatch API token, stored via `chrome.storage.local`.
3. Build `content-script.js`: on page load, scan all `<input>` elements; for each, check `name`/`id`/`autocomplete`/nearby `<label>` text against a keyword map (e.g. "policy" → `policy_number`, "passport" → `passport_number`).
4. On a match, inject a small clickable icon adjacent to the field.
5. On click, call `/api/fields/match?field_name=<matched_type>` with the stored token; show returned value(s) in a small dropdown; fill the field on selection.
6. Load unpacked in Chrome (`chrome://extensions` → Developer mode) for testing.

### Step 4.7 — End-to-end test
1. Upload a real (or realistic test) insurance document, confirm the extracted policy number in the dashboard.
2. Generate an API token, paste into the extension.
3. Visit any form with a field labeled "Policy Number" (a test HTML form is fine if you don't have a live site to test against), confirm the extension detects it and offers to autofill.
4. Click, confirm the correct value fills in.
5. Confirm a request for an unrelated field type (e.g. `passport_number` when only insurance data exists) returns nothing, not an error leaking unrelated data.

**Phase 4 complete when:** a document you've uploaded has its identifying number extracted, confirmed, and successfully autofilled into a real web form via the extension, with sensitive data encrypted at rest and access scoped per-field via a revocable token.

---

## Overall Completion Criteria

- Phase 1: auth + manual tracking, live on Render, no VM.
- Phase 2: OCR replaces manual date entry, with confidence-scored human review for uncertain extractions.
- Phase 3: fully automatic escalating reminders and an urgency-sorted dashboard, tested with a real delivered email.
- Phase 4: structured field extraction + Chrome extension autofill, encrypted at rest, token-scoped — the feature that differentiates this from every existing competitor found.
