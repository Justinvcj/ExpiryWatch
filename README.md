# ExpiryWatch ⏳

ExpiryWatch is a comprehensive, multi-tenant Spring Boot web application designed to track and automatically monitor critical documents (Passports, Driver's Licenses, Car Insurance, Subscriptions). It features an integrated AI-powered OCR engine to automatically extract expiry dates from uploaded photos, background jobs to send Gmail reminders, and a companion Chrome Extension to warn you when browsing related websites.

## 🌟 Key Features

* **AI-Powered OCR Data Entry:** Built-in `Tess4J` (Tesseract) engine scans uploaded images (ID cards, receipts, insurance papers), finds all dates using Regex, and applies a Keyword Proximity Scoring algorithm to auto-detect the expiration date.
* **Automated Gmail Reminders:** Background `@Scheduled` Spring Cron jobs run hourly and daily to calculate warning thresholds based on Document Types (e.g., 180, 90, 30 days prior). Uses Google OAuth2 to silently dispatch formatted reminder emails.
* **Smart Chrome Extension:** A custom Chrome extension monitors your browsing locally. If you visit a website related to a document that needs your attention (e.g., `dmv.ca.gov` when your license is expiring), a bright red warning banner drops down from the top of your screen.
* **Dynamic Dashboard:** Documents are automatically classified into *Urgent/Expired* (Red), *Active & Monitored* (Green), and *Archived* (Grey). 

## 🛡️ Security Architecture

Security was prioritized from day one. The system is designed to be fully multi-tenant, ensuring strict data segregation:
* **Authentication:** Handled natively by `Spring Security` with robust, session-based Form Login.
* **Password Hashing:** All passwords are mathematically salted and hashed using `BCryptPasswordEncoder`. Plaintext passwords are never stored.
* **Data Isolation:** Every Database Query is bound to the `@AuthenticationPrincipal UserDetails`. The backend forcefully validates the `user_id` on every view, update, and file download. You simply cannot view or manipulate another user's document, even if you know its exact UUID.
* **Environment Secrets:** Hardcoded secrets are explicitly banned. Supabase keys and Google OAuth tokens are securely loaded from a local `.env` file that is completely `.gitignore`d.

## 🚀 Getting Started (Local Development)

Because this application relies on heavy native C++ binaries for OCR, **Local Development is highly recommended**.

### Prerequisites
* Java 17+
* A Supabase PostgreSQL Database (using the IPv4 Connection Pooler)
* A Google Cloud Project (for the Gmail API OAuth Refresh Token)

### Installation
1. Clone the repository: `git clone https://github.com/Justinvcj/ExpiryWatch.git`
2. Create a `.env` file in the root of the project with your secrets:
   ```env
   DB_URL=jdbc:postgresql://aws-0-[region].pooler.supabase.com:5432/postgres
   DB_USERNAME=postgres.[your-project-id]
   DB_PASSWORD=your_super_secret_db_password
   GMAIL_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
   GMAIL_CLIENT_SECRET=your_google_client_secret
   GMAIL_REFRESH_TOKEN=your_google_refresh_token
   ```
3. Run the application (Windows):
   Simply run the included helper script in PowerShell, which automatically injects the `.env` secrets into your system and boots the Spring Boot server:
   ```powershell
   .\run.ps1
   ```
4. Visit `http://localhost:8080`.

## 🏗️ Architecture Decisions & Deployment Notes

During development, several modern architectural pathways were evaluated. We purposely stuck to a **Spring Boot Monolith (Thymeleaf UI)** for the following reasons:

### 1. The React / Next.js Dilemma
We strongly considered decoupling the frontend and moving it to a Vercel-hosted Next.js application. While this would offer instantaneous edge-rendered UI, it would have required:
* Completely ripping out Spring Security's impenetrable session-cookie system in favor of complex cross-origin JSON Web Tokens (JWTs).
* Converting all backend controllers to pure Headless REST JSON endpoints.
To keep the application highly secure, tightly coupled, and easy to maintain by a single developer, we intentionally retained the Spring Boot Monolith pattern.

### 2. Render Deployment Limitations
You can deploy this codebase to a cloud provider like Render, but you must be aware of two severe limitations on free tiers:
1. **Network Unreachable (IPv6):** Supabase defaults to IPv6, but Render's free tier cannot route outbound IPv6 traffic. To fix this, you **must** use the Supabase IPv4 Connection Pooler (Port 5432 or 6543) in your `DB_URL`.
2. **OCR Out-Of-Memory Crashes:** The `Tess4J` library consumes massive amounts of RAM and CPU to scan images. Render's free tier (512MB RAM) will frequently crash with `HTTP 502 Bad Gateway` when processing images. **If you want OCR to work on the live internet, you must upgrade your cloud host to a tier that offers at least 1GB - 2GB of RAM.**
