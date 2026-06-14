# WebScraper Pro

**WebScraper Pro** is a full-stack web scraping application built primarily in
**Java**. It lets a signed-in user paste any public URL and instantly extract its
links, images, headings, tables, contacts, metadata, text statistics and a page
screenshot — then export everything as JSON, CSV, TXT, PDF or Word (.docx).

The backend (Java + Spark + JSoup) does the heavy lifting: it fetches and parses
pages, de-duplicates results, **renders the result HTML server-side**, and
generates export files (PDF via OpenPDF, DOCX via Apache POI). The frontend is a
lightweight vanilla HTML/CSS/JS layer that mainly injects server-rendered markup
and wires up interactions. User accounts are stored in **MySQL** (XAMPP) with
BCrypt-hashed passwords and server-side sessions.

---

## Table of contents
1. [Features](#features)
2. [Tech stack](#tech-stack)
3. [Project structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Quick start](#quick-start)
6. [Running manually](#running-manually)
7. [Configuration](#configuration)
8. [Pages & routes](#pages--routes)
9. [API reference](#api-reference)
10. [How it works](#how-it-works)
11. [Troubleshooting](#troubleshooting)
12. [Responsible use](#responsible-use)
13. [Appendix — Full setup from scratch (Windows)](#appendix--full-setup-from-scratch-windows)

---

## Features

- **Accounts** — sign up / sign in, BCrypt-hashed passwords, server-side sessions.
- **Scraping (JSoup)** — title, description, keywords, language, favicon, Open Graph image.
- **De-duplicated results** — links (classified internal/external), images, headings and table rows are de-duplicated even if the page repeats them.
- **Tables** — HTML tables normalized into aligned rows/columns.
- **Contacts** — emails and phone numbers extracted from the page.
- **Text statistics** — word, character and paragraph counts plus reading time.
- **Screenshots** — page capture via a free screenshot service.
- **Progress stages** — a live progress bar while scraping.
- **Multi-format export** (generated in Java):
  | Format | Library |
  |--------|---------|
  | JSON / CSV / TXT | built-in Java |
  | **PDF**  | OpenPDF |
  | **DOCX** | Apache POI |
  | HTML source / Screenshot | client side |
- **Server-side rendering** — result panels are rendered in Java (`ResultView`).
- **Clean URLs** — `/`, `/signin`, `/signup`, `/dashboard`, `/terms`, `/privacy`, …
- **Light / dark theme** on every page (preference persisted).
- **Responsive, glassmorphism UI**.

---

## Tech stack

| Layer       | Technology                                          |
|-------------|-----------------------------------------------------|
| Language    | Java 17                                             |
| Web server  | Spark Java (embedded Jetty)                         |
| Scraping    | JSoup                                               |
| Database    | MySQL (XAMPP) via HikariCP connection pool          |
| Security    | jBCrypt password hashing                            |
| Export      | OpenPDF (PDF), Apache POI (DOCX), Gson (JSON)       |
| Frontend    | Vanilla HTML / CSS / JS (modular)                   |
| Build       | Maven (shade plugin → single runnable fat JAR)      |

---

## Project structure

```
.
├── pom.xml                      # Maven build + dependencies
├── README.md
├── run.bat                      # frees port 8080 if busy, builds if needed, runs
├── build-and-run.bat            # always rebuilds, then runs
├── database/
│   └── schema.sql               # MySQL schema (also auto-created on startup)
└── src/main/
    ├── java/com/webscraper/
    │   ├── Main.java             # ENTRY POINT — bootstraps DB, routes, server
    │   ├── PageController.java   # serves HTML pages at clean URLs
    │   ├── ScraperController.java# POST /api/scrape
    │   ├── ScraperService.java   # JSoup scraping + extraction logic
    │   ├── ScraperResult.java    # result data model
    │   ├── ResultView.java       # server-side HTML rendering of results
    │   ├── ScrapeResponse.java   # { data, statsHtml, resultHtml }
    │   ├── ExportController.java # POST /api/export/{json,csv,txt,pdf,docx}
    │   ├── auth/
    │   │   ├── AuthController.java
    │   │   ├── UserDao.java
    │   │   ├── User.java
    │   │   └── PasswordUtil.java
    │   └── db/Database.java      # HikariCP pool + auto schema creation
    └── resources/public/
        ├── css/style.css         # shared design system
        ├── js/                   # api.js, render.js, app.js, signin.js, signup.js
        └── pages/                # index, signin, signup, dashboard, terms,
                                  #   privacy, security, status, docs, contact
```

---

## Prerequisites

- **Java 17+** (Microsoft OpenJDK recommended)
- **Maven 3.9+**
- **XAMPP** with **MySQL running** on `localhost:3306`
  (default user `root`, empty password)

> The database `webscraper` and its tables are created **automatically** on first
> run. You can also import `database/schema.sql` via phpMyAdmin if you prefer.

---

## Quick start

1. Start **MySQL** from the XAMPP Control Panel.
2. Double-click **`run.bat`** (Windows).
   - It checks if port `8080` is busy and frees it,
   - builds the JAR if it doesn't exist,
   - then starts the server.
3. Open **http://localhost:8080** in your browser.
4. Click **Get Started**, create an account, and start scraping.

---

## Running manually

```bash
# Build the fat JAR
mvn package

# Run it (this is the Main entry point: com.webscraper.Main)
java -jar target/web-scraper-1.0.0.jar
```

Or run **`com.webscraper.Main`** directly from your IDE (Maven puts all
dependencies on the classpath).

Then open **http://localhost:8080**.

---

## Configuration

Database settings can be overridden with environment variables (defaults shown):

| Variable      | Default       | Description              |
|---------------|---------------|--------------------------|
| `DB_HOST`     | `localhost`   | MySQL host               |
| `DB_PORT`     | `3306`        | MySQL port               |
| `DB_NAME`     | `webscraper`  | Database name            |
| `DB_USER`     | `root`        | MySQL user               |
| `DB_PASSWORD` | *(empty)*     | MySQL password           |

The server listens on port **8080**.

---

## Pages & routes

| URL          | Page                                  |
|--------------|---------------------------------------|
| `/`          | Landing page (auth-aware CTAs)        |
| `/signin`    | Sign in                               |
| `/signup`    | Sign up                               |
| `/dashboard` | The scraper app (requires sign-in)    |
| `/terms`     | Terms of Service                      |
| `/privacy`   | Privacy Policy                        |
| `/security`  | Security                              |
| `/status`    | System status                         |
| `/docs`      | Documentation                         |
| `/contact`   | Contact                               |

---

## API reference

| Method | Route                | Auth | Description                       |
|--------|----------------------|------|-----------------------------------|
| POST   | `/api/auth/signup`   | —    | Create account                    |
| POST   | `/api/auth/signin`   | —    | Sign in                           |
| GET    | `/api/auth/me`       | —    | Current session info              |
| POST   | `/api/auth/logout`   | —    | Sign out                          |
| POST   | `/api/scrape`        | ✓    | Scrape a URL → data + rendered HTML |
| POST   | `/api/export/json`   | ✓    | Export JSON                       |
| POST   | `/api/export/csv`    | ✓    | Export links CSV                  |
| POST   | `/api/export/txt`    | ✓    | Export plain text                 |
| POST   | `/api/export/pdf`    | ✓    | Export PDF (OpenPDF)              |
| POST   | `/api/export/docx`   | ✓    | Export Word document (Apache POI) |

---

## How it works

1. The browser POSTs a URL to `/api/scrape`.
2. `ScraperService` fetches the page with JSoup, extracts and de-duplicates the
   content, and builds a `ScraperResult`.
3. `ResultView` renders that result into HTML **on the server**.
4. The response (`ScrapeResponse`) contains the raw data plus `statsHtml` and
   `resultHtml`; the frontend simply injects the markup.
5. For exports, the frontend posts the data back to `/api/export/{format}` and
   Java generates the file (PDF/DOCX/CSV/JSON/TXT) for download.

---

## Troubleshooting

- **`mvn` or `java` not recognized** — open a new terminal after installing, or
  use `run.bat` which sets the paths for you.
- **Database init failed / connection refused** — make sure **MySQL is running**
  in XAMPP on port 3306.
- **Port 8080 already in use** — `run.bat` frees it automatically; otherwise stop
  the other process or change the port in `Main.java`.
- **Blank page or old UI** — hard-refresh the browser (Ctrl+Shift+R) to clear
  cached JS/CSS.
- **Screenshot shows a placeholder** — the free screenshot service renders the
  page on demand; give it a few seconds or reopen the Screenshot tab.

---

## Responsible use

Only scrape pages you are permitted to access, and respect each site's
`robots.txt` and terms of service. This is an educational/demo project provided
without warranty.

---

## Appendix — Full setup from scratch (Windows)

If Java and Maven are **not yet installed**, follow these steps once. After this,
you can always run the app with `run.bat`.

### Step 1 — Install Java 17 (JDK)

**Option A — winget (Windows 10/11, easiest):**
```powershell
winget install Microsoft.OpenJDK.17
```

**Option B — manual download:**
1. Download the **Microsoft OpenJDK 17** (or Eclipse Temurin 17) MSI from
   <https://learn.microsoft.com/java/openjdk/download> or <https://adoptium.net>.
2. Run the installer and accept the defaults.
3. Note the install path, e.g. `C:\Program Files\Microsoft\jdk-17.x.x.x-hotspot`.

Verify (open a **new** terminal):
```powershell
java -version
```

### Step 2 — Install Maven 3.9+

Maven usually isn't on winget, so install it manually:
1. Download the **Binary zip** from <https://maven.apache.org/download.cgi>
   (e.g. `apache-maven-3.9.16-bin.zip`).
2. Extract it to a simple location, e.g. `C:\maven\apache-maven-3.9.16`.

Verify after Step 3:
```powershell
mvn -version
```

### Step 3 — Add Java & Maven to environment variables

You can do this with the GUI **or** PowerShell.

**GUI method:**
1. Press `Win` and search **"Edit the system environment variables"** → open it.
2. Click **Environment Variables…**
3. Under **User variables**, click **New** and add:
   - Variable name: `JAVA_HOME`
   - Variable value: your JDK path, e.g. `C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot`
4. Select the **Path** variable → **Edit** → **New**, then add these two entries:
   - `%JAVA_HOME%\bin`
   - `C:\maven\apache-maven-3.9.16\bin`  *(adjust to your Maven folder)*
5. Click **OK** on all dialogs.
6. **Close and reopen** any terminal so the changes take effect.

**PowerShell method (run once):**
```powershell
# Set JAVA_HOME (adjust the path to your installed JDK)
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot", "User")

# Append Java and Maven to the user PATH (adjust the Maven path)
$p = [Environment]::GetEnvironmentVariable("PATH", "User")
[Environment]::SetEnvironmentVariable("PATH", "$p;C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin;C:\maven\apache-maven-3.9.16\bin", "User")
```
Then **open a new terminal** and confirm both work:
```powershell
java -version
mvn -version
```

> Note: `run.bat` also sets these paths internally, so it works even if the
> global PATH isn't configured — but configuring it lets you run `mvn` and
> `java` from any terminal.

### Step 4 — Install & start MySQL (XAMPP)

1. Download and install **XAMPP** from <https://www.apachefriends.org>.
2. Open the **XAMPP Control Panel** and click **Start** next to **MySQL**.
3. (Optional) Open **phpMyAdmin** at <http://localhost/phpmyadmin> to inspect data.

The app auto-creates the `webscraper` database and tables on first run, so no
manual SQL is required. Default credentials are user `root` with an empty
password (the XAMPP default).

### Step 5 — Build & run the project

From the project folder:
```powershell
# Build (downloads dependencies the first time — needs internet)
mvn package

# Run
java -jar target\web-scraper-1.0.0.jar
```
Or simply double-click **`run.bat`**, which frees port 8080 if busy, builds when
needed, and starts the server.

### Step 6 — Open the app

Visit **<http://localhost:8080>**, create an account, and start scraping.

### Common setup issues

| Problem | Fix |
|---------|-----|
| `'java'`/`'mvn' is not recognized` | Open a **new** terminal after editing PATH, or use `run.bat`. |
| `Database init failed` / connection refused | Start **MySQL** in the XAMPP Control Panel. |
| `Port 8080 already in use` | `run.bat` frees it automatically; or stop the other process. |
| Build can't download dependencies | Check your internet connection / proxy and re-run `mvn package`. |
| Old UI after changes | Hard-refresh the browser with `Ctrl+Shift+R`. |

