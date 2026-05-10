# Tippspiel WM 2026 — Project Documentation

> Multi-competition football betting game.
> Rebuilt from scratch based on the original EM-Tippspiel (Diploma thesis HSR, 2007).

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Infrastructure & Costs](#3-infrastructure--costs)
4. [What Existed in 2007](#4-what-existed-in-2007)
5. [Key Changes vs 2007](#5-key-changes-vs-2007)
6. [Architecture](#6-architecture)
7. [Database Schema Summary](#7-database-schema-summary)
8. [Scoring Rules](#8-scoring-rules)
9. [Security Concepts](#9-security-concepts)
10. [Use Cases](#10-use-cases)
11. [Open Questions & Decisions](#11-open-questions--decisions)
12. [What To Do Next](#12-what-to-do-next)

---

## 1. Project Overview

A **multi-competition football betting game** for a private group of friends (primary scope),
designed to be reusable across any football tournament:

- **FIFA World Cup 2026** (USA / Canada / Mexico, June–July 2026) — first target
- **UEFA Euro 2028**, **WM 2030**, etc. — reusable without code changes

### Core Concept

Every user (Tipper) can:
- Submit score predictions for each match (**Resultat-Tipp**) up to 10 minutes before kick-off
- Submit tournament predictions before the competition starts (**Prognose-Tipp**): group rankings, who advances, who wins
- Be a member of one or more **Tipp-Groups** (e.g. friends group, colleagues group)
- Each Tipp-Group has its own leaderboard and optionally custom scoring rules

### Old Project Reference

Original source code:
```
C:\Werni\Ausbildungen\nds-swe\EmTippSpiel\03 Implementation\EmTippSpiel-code.7z
C:\Werni\Ausbildungen\nds-swe\EmTippSpiel\03 Implementation\emtippspiel.mdb
```
Extracted to: `C:\Temp\EmTippSpiel\`

Original requirements document:
```
C:\Werni\Ausbildungen\nds-swe\EmTippSpiel\01 Requirements\Dokumente\Requirements.doc
C:\Werni\Ausbildungen\nds-swe\EmTippSpiel\02 Analyse&Design\Dokumente\Software Architecture Document.doc
```

---

## 2. Technology Stack

| Layer | Technology | Reason |
|-------|-----------|--------|
| Language | **Kotlin** | Modern JVM, concise, null-safe, great Spring support |
| Backend Framework | **Spring Boot 3.x** | Industry standard, excellent Kotlin support |
| Frontend | **Vaadin Flow** | Pure Kotlin/Java UI, no JavaScript needed, component-based |
| Database | **PostgreSQL** | Reliable OLTP, Cloud SQL managed on GCP |
| ORM | **Spring Data JPA / Hibernate** | Natural fit with Kotlin entities |
| Auth | **Spring Security** | BCrypt passwords, session management, role-based access |
| Build | **Gradle (Kotlin DSL)** | Modern, Kotlin-native build tool |
| IDE | **IntelliJ IDEA** | With integrated Claude Code |
| Local Dev DB | **PostgreSQL in Docker** | Free, no cloud costs during development |
| Analytics / History | **Google BigQuery** | Already used in solar project; post-WM data archive |

### Why Vaadin over Vue.js?

- No context switch between Kotlin and JavaScript
- Server-side rendering, no REST API layer needed for MVP
- Component library covers all needed UI: grids, forms, charts
- Can always add a REST API later if a mobile app is needed

---

## 3. Infrastructure & Costs

### Lifecycle Plan

| Phase | When | Infrastructure | Estimated Cost |
|-------|------|---------------|----------------|
| Development | Now → May 2026 | PostgreSQL in Docker (local) | **$0** |
| WM Active | June–July 2026 (~6 weeks) | Cloud SQL `db-f1-micro` on GCP | **~$20 total** |
| Post-WM Archive | After July 2026 | Export to BigQuery, delete Cloud SQL | **~$0** |

### Cloud SQL Details (`db-f1-micro`)

- 0.6 GB RAM, shared CPU
- ~$10/month when running
- Storage: ~$0.17/GB/month (even when stopped)
- Only pay for compute when instance is running
- **Stop instance** when not needed → pay storage only (~$0.02/month for 1 GB)

### Post-WM Strategy

1. Export all PostgreSQL tables → BigQuery (already used for solar project)
2. Delete Cloud SQL instance entirely
3. Historical results viewable via BigQuery queries or Looker Studio dashboard
4. Cost: essentially $0 (first 1 TB/month BigQuery queries are free)

### Data Volume Estimates

For ~50 friends, WM 2026 (64 matches):
- Tips: 50 users × 64 matches = ~3,200 rows
- Tip points: ~3,200 × 3 rules = ~9,600 rows
- Total DB size: well under 50 MB → `db-f1-micro` is more than sufficient

---

## 4. What Existed in 2007

### Tech Stack (2007)

| Component | Technology |
|-----------|-----------|
| Language | Java |
| Framework | Struts 2 (WebWork) |
| ORM | Hibernate 3 |
| Database | MySQL (InnoDB) |
| Server | Apache Tomcat |
| Build | Apache Ant |
| Logging | Log4J |
| Testing | JUnit |

### Database Tables (2007, MySQL)

| Table | Description |
|-------|-------------|
| `TIPPER` | Registered user |
| `TIPP_TEAM` | A group of tippers with shared rules |
| `MITGLIEDSCHAFT` | Many-to-many: Tipper ↔ TippTeam with points & rank |
| `RESULTAT_TIPP` | A user's score prediction for one match |
| `RESULTAT_TIPP_PUNKTE` | Points earned per tip per team membership |
| `RESULTAT` | Match result (45min, 90min, 120min, penalty as separate rows) |
| `REGEL_WERK` | A scoring ruleset linked to a TippTeam |
| `REGEL` | One rule within a ruleset (type × stage × points) |

### Static Data in XML (2007)

Teams, groups, matches, venues, and the competition structure were **not in the database**.
They were loaded from an XML file (`FussballEm.xml`) at application startup and held in memory.
This meant no multi-competition support and no ability to query match history.

### Scoring (2007)

Two types of tips:
- **Prognose-Tipp** — pre-tournament group rankings and knockout picks (frozen at tournament start)
- **Resultat-Tipp** — match score predictions (frozen 10 min before kick-off)

Scoring rules were fully configurable per TippTeam, with min/max bounds.

---

## 5. Key Changes vs 2007

### ✅ Multi-Competition Support (biggest architectural change)

**2007:** Hard-coded for one specific tournament (EM 2008). New tournament = new deployment.

**2026:** `competition` table at the top level. Every entity (match, team assignment, tipp_group,
tip, scoring rule) is linked to a competition. The same application handles WM 2026, Euro 2028,
WM 2030 without any code changes.

---

### ✅ Tournament Data in Database (not XML)

**2007:** Teams, groups, matches, venues loaded from `FussballEm.xml` at startup.
Cannot query, no history, admin must restart server to update.

**2026:** All tournament data in PostgreSQL:
- `team`, `competition_team`, `venue`, `match` tables
- Admin can update match dates, scores, venues via UI
- Full query capability (e.g. "all matches in group A", "matches on date X")
- Match history preserved in BigQuery after tournament ends

---

### ✅ Password Security (critical fix)

**2007 problems:**
- "Passwort vergessen" function **sent the plain-text password by email** — meaning passwords were
  either stored plain or with reversible encryption. Both are insecure.
- No brute-force protection documented in DB schema.

**2026 fixes:**
- Passwords stored as **BCrypt hash only** — never reversible, never stored plain
- "Forgot password" sends a **time-limited reset token** (2h expiry) — user sets new password
- **Login lockout**: 3 failed attempts → account locked for 5 minutes (`locked_until` column)
- Login attempts counter reset on successful login

---

### ✅ Email Verification

**2007:** Registration process included email confirmation (48h) — good, kept.

**2026:** Same concept, improved:
- Verification token stored as SHA-256 hash (not plain token in DB)
- Scheduled cleanup job removes unverified accounts after 48h
- User cannot place bets until email is verified (`email_verified` flag)

---

### ✅ Joining a Tipp-Group via Invite Code

**2007:** Users searched for TippTeams by name and could join any unlocked team freely.
TippTeamChef had to manually accept or exclude members.

**2026:** Each Tipp-Group has a unique **invite code** (short alphanumeric string).
- TippTeamChef shares the code with friends (WhatsApp, email, etc.)
- Users enter the code to join — no search, no open discovery
- Simpler and more private for friend groups

---

### ✅ Match Result Model Simplified

**2007:** Each result type (45min, 90min, 120min, penalty) was a **separate row** in the
`RESULTAT` table, requiring complex lookups to find the "final" result.

**2026:** All scores in **one row** per match with dedicated columns:
```
score_home_ht, score_away_ht   → half-time
score_home_90, score_away_90   → after 90 min
score_home_et, score_away_et   → after extra time
score_home_pen, score_away_pen → after penalties
```
NULL = not yet played. Simpler queries, clearer data model.

---

### ✅ Member Status (REMOVED vs BANNED)

**2007:** `gesperrt` (boolean) — a member was either active or blocked.
Did not distinguish between "removed" (can rejoin) and "banned" (cannot rejoin).

**2026:** `status` enum: `ACTIVE | REMOVED | BANNED`
- `REMOVED` — left or removed by admin; can rejoin via invite code
- `BANNED` — excluded permanently from this group

---

### ✅ Points Trend Tracking

**2007:** `RESULTAT_TIPP_PUNKTE` and `RESULTAT_TIPP_PUNKTE_ALT` stored current and previous
total points in the `MITGLIEDSCHAFT` table for trend arrows.

**2026:** Same concept, clearer naming:
- `total_points` / `total_points_prev` — for score trend (↑ ↓ →)
- `rank` / `rank_prev` — for rank trend

---

### ✅ BigQuery Integration (new)

**2007:** No analytics layer. Data stayed in MySQL forever (or was lost).

**2026:** After the WM:
1. Export all tables to BigQuery
2. Delete Cloud SQL instance (no more cost)
3. Historical dashboards via Looker Studio
4. Integration with solar project's existing GCP setup

---

## 6. Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Browser / Client                   │
└───────────────────────┬─────────────────────────────┘
                        │ HTTP / WebSocket (Vaadin)
┌───────────────────────▼─────────────────────────────┐
│              Spring Boot Application                  │
│                                                       │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │ Vaadin Views│  │   Services   │  │  Scheduled  │ │
│  │  (UI Layer) │  │ (Biz Logic)  │  │    Jobs     │ │
│  └──────┬──────┘  └──────┬───────┘  └──────┬──────┘ │
│         │                │                  │        │
│  ┌──────▼──────────────────────────────────▼──────┐  │
│  │          Spring Data JPA / Hibernate            │  │
│  └──────────────────────┬──────────────────────────┘  │
│                         │                             │
│  ┌──────────────────────▼──────────────────────────┐  │
│  │           Spring Security                        │  │
│  │  (BCrypt, Sessions, Role-based Access)           │  │
│  └─────────────────────────────────────────────────┘  │
└───────────────────────┬─────────────────────────────┘
                        │ JDBC
┌───────────────────────▼─────────────────────────────┐
│         PostgreSQL (local Docker / Cloud SQL)        │
└─────────────────────────────────────────────────────┘
```

### Package Structure (planned)

```
ch.werni.tippspiel
├── competition/        # Tournament & match management
│   ├── entity/         # Competition, Team, Match, Venue, CompetitionTeam
│   ├── repository/
│   └── service/
├── user/               # User management & authentication
│   ├── entity/         # AppUser, EmailVerificationToken, PasswordResetToken
│   ├── repository/
│   └── service/
├── tipping/            # Core betting logic
│   ├── entity/         # TippGroup, TippGroupMember, Tip, TipPoints, ProgniosisTip
│   ├── repository/
│   └── service/
├── scoring/            # Points calculation
│   ├── entity/         # ScoringRule, DefaultScoringRule
│   ├── repository/
│   └── service/
├── ui/                 # Vaadin views
│   ├── view/           # HomeView, PersonalPageView, ResultsView, AdminView, etc.
│   └── component/      # Reusable UI components
└── scheduler/          # Scheduled jobs (cleanup, points recalculation)
```

---

## 7. Database Schema Summary

Full DDL: `db/schema.sql`

### Tables Overview

| Section | Table | Description |
|---------|-------|-------------|
| Tournament | `competition` | A football tournament (WM, Euro, etc.) |
| Tournament | `team` | A national team (global, reused) |
| Tournament | `competition_team` | Team in a specific competition + group |
| Tournament | `venue` | Stadium / city |
| Tournament | `match` | One match with all score columns |
| Users | `app_user` | Registered user (BCrypt password) |
| Users | `email_verification_token` | 48h email confirmation token |
| Users | `password_reset_token` | 2h password reset token |
| Tipping | `tipp_group` | A group of friends betting together |
| Tipping | `tipp_group_member` | User in a group + points + rank |
| Tipping | `tip` | Score prediction for one match |
| Tipping | `tip_points` | Points per rule per tip |
| Tipping | `prognosis_tip` | Pre-tournament predictions |
| Scoring | `scoring_rule` | Points per rule per group (customizable) |
| Scoring | `default_scoring_rule` | Template rules applied to new groups |

---

## 8. Scoring Rules

### Resultat-Tipp (match score predictions)

| Stage | Rule | Default Points |
|-------|------|:--------------:|
| Group | TOTO (1X2 correct) | 1 |
| Group | GOAL_DIFF (correct goal difference) | 1 |
| Group | EXACT (exact score) | 1 |
| R16 / QF | WINNER | 2 |
| R16 / QF | TIMING (90min / ET / Penalty) | 2 |
| R16 / QF | EXACT | 2 |
| SF | WINNER | 3 |
| SF | TIMING | 3 |
| SF | EXACT | 3 |
| Final | WINNER | 5 |
| Final | TIMING | 5 |
| Final | EXACT | 5 |

### Prognose-Tipp (pre-tournament predictions)

| Tip Type | Default Points |
|----------|:--------------:|
| GROUP_RANK_1 (correct 1st in group) | 4 |
| GROUP_RANK_2 (correct 2nd in group) | 4 |
| GROUP_RANK_3 (correct 3rd in group) | 2 |
| GROUP_RANK_4 (correct 4th in group) | 2 |
| ADVANCEMENT to R16 | 2 |
| ADVANCEMENT to QF | 5 |
| ADVANCEMENT to SF | 10 |
| WINNER (tournament champion) | 20 |

### Rule Configuration

- Each TippGroup inherits default rules on creation
- TippTeamChef can adjust points within `[points_min, points_max]` bounds
- Setting points = 0 disables that rule for the group
- Rules are frozen when the competition starts

---

## 9. Security Concepts

### Password Handling

- Passwords stored as **BCrypt** hash (Spring Security `BCryptPasswordEncoder`)
- Never stored or transmitted in plain text
- Never sent by email (not even hashed)
- Minimum password length: 8 characters (to be validated in UI)

### Registration Flow

```
1. User fills registration form (email, nickname, password, name, city)
2. Account created with email_verified = false
3. Verification email sent with unique token link (48h expiry)
4. User clicks link → email_verified = true → can now bet
5. Scheduled job: delete unverified accounts older than 48h
```

### Password Reset Flow (replaces 2007 "send plain password")

```
1. User enters email on "Forgot Password" page
2. If email exists: reset token generated, SHA-256 hash stored in DB
3. Email sent with link containing raw token (not stored, only hash in DB)
4. User clicks link → token validated → new password form shown
5. New BCrypt hash stored, token marked as used
6. Token expires after 2 hours regardless
```

### Login Protection

- 3 failed login attempts → account locked for 5 minutes (`locked_until`)
- Login attempt counter reset on successful login
- Session timeout: 30 minutes of inactivity → auto-logout
- HTTPS required in production (Cloud SQL accessible only from GCP internal network)

### Roles

| Role | Description |
|------|-------------|
| `ROLE_USER` | Standard registered user (Tipper) |
| `ROLE_ADMIN` | System administrator — can enter match results, manage competition data |
| TippTeamChef | Not a Spring Security role; tracked via `tipp_group_member.is_admin` |

---

## 10. Use Cases

Derived from original requirements document (25 UCs), adapted for new system:

| # | Use Case | Actor | Status |
|---|----------|-------|--------|
| UC01 | Submit prognosis tips (group rankings, knockout picks) | User | planned |
| UC02 | Submit/update match score tip (until 10 min before kick-off) | User | planned |
| UC03 | Register (with email verification, 48h) | Guest | planned |
| UC04 | Confirm registration via email link | Guest | planned |
| UC05 | Auto-delete unconfirmed registrations after 48h | Scheduler | planned |
| UC06 | Login (3 attempts → 5 min lockout) | User | planned |
| UC07 | Logout (manual or 30 min inactivity) | User / System | planned |
| UC08 | Edit user profile (except email and nickname) | User | planned |
| UC09 | Forgot password (token-based reset, 2h) | User | planned |
| UC10 | View competition results (groups, knockout, standings) | All | planned |
| UC11 | Share/recommend tipp group via invite code | User | planned |
| UC12 | View homepage (upcoming matches, leaderboard preview) | All | planned |
| UC13 | View overall leaderboard (default rules) | All | planned |
| UC14 | View tipp-group leaderboard | All | planned |
| UC15 | View personal page (my groups, my tips, my rank) | User | planned |
| UC16 | View detailed leaderboard within one tipp-group | User | planned |
| UC17 | Enter competition data (teams, matches, venues) | Admin | planned |
| UC18 | Enter match result (HT, 90min, ET, penalty) | Admin | planned |
| UC19 | Manually override group ranking (UEFA tiebreaker) | Admin | planned |
| UC20 | Create tipp-group (become TippTeamChef) | User | planned |
| UC21 | Add members to tipp-group | TippTeamChef | planned |
| UC22 | Remove or ban member from tipp-group | TippTeamChef | planned |
| UC23 | Lock tipp-group (no new members) | TippTeamChef | planned |
| UC24 | Configure scoring rules for tipp-group | TippTeamChef | planned |
| UC25 | Join tipp-group via invite code | User | planned |

---

## 11. Open Questions & Decisions

| # | Question | Options | Status |
|---|----------|---------|--------|
| 1 | **Prognose-Tipps for WM 2026?** | Yes (full) / Yes (simplified) / No | ❓ open |
| 2 | **Tip deadline configurable?** | Fixed 10 min / Configurable per group | ❓ open |
| 3 | **Joker / double points?** | Not in 2007 / Add as bonus feature | ❓ open |
| 4 | **Group rank manual override UI?** | Admin page / CSV import | ❓ open |
| 5 | **Match data import for WM 2026?** | Manual admin entry / official API / FIFA data file | ❓ open |
| 6 | **Email provider?** | Gmail SMTP / SendGrid / GCP Cloud Mailer | ❓ open |
| 7 | **Flyway or Liquibase for DB migrations?** | Flyway (simpler) / Liquibase (more powerful) | Flyway preferred |
| 8 | **Public leaderboard for guests?** | Yes (read-only) / Login required | ❓ open |
| 9 | **Mobile-friendly Vaadin theme?** | Lumo (default) / Custom | ❓ open |

---

## 12. What To Do Next

### Phase 1 — Project Setup

- [ ] Create Kotlin + Spring Boot + Vaadin project in IntelliJ
  - Spring Web, Spring Data JPA, Spring Security, Vaadin, PostgreSQL driver, Flyway
- [ ] Set up PostgreSQL in Docker for local development
  ```bash
  docker run --name tippspiel-db -e POSTGRES_PASSWORD=tippspiel \
    -e POSTGRES_DB=tippspiel -p 5432:5432 -d postgres:16
  ```
- [ ] Add `db/schema.sql` as Flyway migration `V1__initial_schema.sql`
- [ ] Configure `application.yml` (local + prod profiles)
- [ ] Verify schema applies cleanly with `./gradlew flywayMigrate`

### Phase 2 — Core Entities & Repositories

- [ ] Kotlin `@Entity` classes for all tables
- [ ] Spring Data JPA repositories
- [ ] Basic service layer (UserService, CompetitionService, TippingService, ScoringService)
- [ ] Unit tests for scoring logic (points calculation)

### Phase 3 — Security & Auth

- [ ] Spring Security config (BCrypt, session management)
- [ ] Registration flow (form → email verification → activation)
- [ ] Login / logout with lockout
- [ ] Password reset flow (token → email → new password)
- [ ] Scheduled job: cleanup unverified registrations after 48h

### Phase 4 — Admin UI

- [ ] Vaadin Admin view (login-protected, `ROLE_ADMIN` only)
- [ ] Enter/edit competition data (teams, venues, matches for WM 2026)
- [ ] Enter match results (HT, 90min, ET, penalty)
- [ ] Trigger points recalculation after result entry
- [ ] Manual group ranking override

### Phase 5 — User UI

- [ ] Home page (upcoming matches, top leaderboard)
- [ ] Registration / login / forgot password pages
- [ ] Personal page (my groups, my tips, my rank)
- [ ] Match tip entry (grid with all matches, inline editing)
- [ ] Prognosis tip entry (group rankings + knockout tree)
- [ ] Leaderboard views (overall + per group)
- [ ] Competition results view (groups, knockout bracket)
- [ ] Create / manage tipp-group
- [ ] Join tipp-group via invite code

### Phase 6 — Production Setup

- [ ] Create Cloud SQL instance on GCP (`db-f1-micro`, PostgreSQL 16)
- [ ] Configure production `application-prod.yml`
- [ ] Set up GCP Cloud Run or VM for application hosting
- [ ] Configure HTTPS / domain
- [ ] Set up automated Cloud SQL backup
- [ ] Load WM 2026 match schedule (64 matches)

### Phase 7 — Post-WM Archival

- [ ] Export all tables from PostgreSQL → BigQuery
- [ ] Create Looker Studio dashboard for historical results
- [ ] Delete Cloud SQL instance
- [ ] Document archival process for future tournaments

---

## File Locations

| File | Path |
|------|------|
| This document | `C:\Users\werne\tippspiel\CLAUDE.md` |
| Database schema (DDL) | `C:\Users\werne\tippspiel\db\schema.sql` |
| Old source code (7z) | `C:\Werni\Ausbildungen\nds-swe\EmTippSpiel\03 Implementation\EmTippSpiel-code.7z` |
| Old source (extracted) | `C:\Temp\EmTippSpiel\` |
| Old DB (MS Access) | `C:\Werni\Ausbildungen\nds-swe\EmTippSpiel\03 Implementation\emtippspiel.mdb` |
| Old requirements (doc) | `C:\Werni\Ausbildungen\nds-swe\EmTippSpiel\01 Requirements\Dokumente\Requirements.doc` |
| Old SAD (doc) | `C:\Werni\Ausbildungen\nds-swe\EmTippSpiel\02 Analyse&Design\Dokumente\Software Architecture Document.doc` |
| Memory index | `C:\Users\werne\.claude\projects\C--Users-werne\memory\MEMORY.md` |
| Memory detail | `C:\Users\werne\.claude\projects\C--Users-werne\memory\project_tippspiel.md` |
