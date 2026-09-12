# ExpiryWatch — Product Requirements Document

**One-line pitch:** ExpiryWatch scans your insurance, licenses, warranties, and renewal documents with OCR to extract hidden expiry dates, then sends escalating reminders so a forgotten deadline never costs you a fine or a denied claim — and autofills the document numbers buried in them straight into web forms, like a password manager for your paperwork.

**Owner:** Justin
**Constraint:** $0 cost, no VM, no paid APIs beyond generous free tiers.

---

## 1. Problem Statement

Insurance, PUC certificates, passports, visas, gym memberships, domain renewals, software licenses, AMC contracts, rent agreements, laptop warranties — all carry silent expiry dates and buried identifying numbers (policy numbers, passport numbers, registration numbers). Missing an expiry means a fine, a denied claim, or a legal problem. Needing to find one of those buried numbers later — to fill a form, make a claim, renew something — usually means digging through email or a drawer of paper.

## 2. Competitive Landscape (be honest about it)

This category already exists: RenewalKit, Document Expiry Reminder, Docly, ExpiryTrack (consumer), Expiration Reminder, Remindax, Contracko (business/SaaS). All of them do OCR → expiry date → reminder. **None of them do anything with the data besides remind you.** They're all read-only trackers.

**What makes ExpiryWatch different:** it also autofills the extracted document numbers directly into web forms — the same interaction pattern as a password manager, but for policy numbers, passport numbers, license numbers, and membership IDs instead of passwords. No competitor found does this. This is the actual point of differentiation and should be the center of any pitch or interview conversation about this project — not the reminder feature, which is table stakes.

## 3. Non-Goals

- No LLM dependency for core extraction — deterministic OCR + regex/keyword matching, not a paid API call per document.
- No mobile app in v1 — web dashboard + Chrome extension only.
- No payment processing or auto-renewal execution — the tool reminds and autofills, it does not act on your behalf financially or legally.
- No auto-fill without an explicit click — never silently populate a form field.

## 4. Users

- Primary: an individual managing their own documents (you, initially — dogfood it).
- Secondary (not v1, but architecture should allow it later): a family/household managing shared documents across members (e.g. a parent tracking a whole family's insurance and IDs).

## 5. Feature Set

### Core (Phases 1-3)
1. Account creation and login.
2. Upload a document (photo or PDF) or enter one manually.
3. OCR extracts the expiry date automatically; low-confidence extractions are flagged for manual confirmation, never silently guessed.
4. Dashboard shows all documents grouped by urgency (expired / due this week / due this month / everything else).
5. Automatic escalating email reminders (30/7/1 days before expiry by default, configurable per document type) sent via your own Gmail account.
6. Documents past expiry with no renewal logged auto-flip to "expired" status and surface at the top of the dashboard.

### Differentiator (Phase 4)
7. On document upload, extract structured fields beyond just the date — policy number, passport number, license number, registration number, membership ID — per document type, with the same confidence-scoring/human-review principle as the date extraction.
8. Chrome extension detects relevant form fields on any website (by field name/label/placeholder matching) and shows a click-to-fill icon, exactly like a password manager's autofill prompt.
9. Extension authenticates to the backend via a personal access token generated from the dashboard, and only ever requests the specific field type needed for the field it detected — never a bulk data dump.
10. Sensitive extracted fields (policy numbers, passport numbers, etc.) are encrypted at rest.

## 6. Success Criteria

- Phase 1: register, log in, manually track a document with an expiry date, see it sorted correctly on a live URL — no VM used.
- Phase 2: upload a real photographed document, correct or flagged-for-review expiry date extracted automatically.
- Phase 3: a real reminder email lands in your inbox with zero manual triggering; expired documents auto-flag.
- Phase 4: visiting a real insurance-renewal-style form, the extension detects a policy number field and offers to fill it from a document you already uploaded — demoable live in an interview.

## 7. Interview Talking Points

- The concept category is not new — say so upfront, don't oversell novelty on the base idea.
- The autofill layer is the genuinely differentiated piece — no found competitor does this; frame it as "I looked at what exists, all of it is read-only, so I built the missing write-back interaction."
- OCR reliability is handled honestly: confidence scoring + human-in-the-loop confirmation, not blind trust in extracted text — a deliberate engineering decision, not an afterthought.
- Security posture changed once structured identifying fields were added (Phase 4) — encryption at rest was added specifically because the data got more sensitive, and that's a decision explicitly called out, not missed.
- Fully free-tier, self-hosted architecture, no VM, no paid APIs — proof of shipping under real constraints.
