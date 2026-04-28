<div align="center">

# ⚔️ CodeArena

**A full-stack competitive programming platform where developers battle, learn, and level up.**

[![Java](https://img.shields.io/badge/Backend-Java%2017%20%2F%20Spring%20Boot%203-orange?style=flat-square&logo=java)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Frontend-Angular-red?style=flat-square&logo=angular)](https://angular.io/)
[![MySQL](https://img.shields.io/badge/Database-MariaDB%2FMySQL-blue?style=flat-square&logo=mysql)](https://mariadb.org/)
[![Auth0](https://img.shields.io/badge/Auth-Auth0%20%2F%20Keycloak-black?style=flat-square&logo=auth0)](https://auth0.com/)
[![Docker](https://img.shields.io/badge/Deploy-Docker%20Compose-2496ED?style=flat-square&logo=docker)](https://www.docker.com/)

[Features](#-features) • [Architecture](#-architecture) • [Modules](#-modules) • [Database](#-database-schema) • [Getting Started](#-getting-started) • [Contributing](#-contributing)

</div>

---

## 📖 Overview

**CodeArena** is a competitive programming platform designed to help developers sharpen their coding skills through challenges, real-time battles, quizzes, coaching sessions, and community events. Built as a monorepo with a Spring Boot backend and an Angular frontend, it offers a gamified experience with XP, ranks, badges, and a merch shop.

---

## ✨ Features

| Feature | Description |
|---|---|
| 🧩 **Coding Challenges** | Solve algorithmic problems with multi-language support and automatic test case evaluation |
| ⚔️ **Battle Rooms** | Real-time competitive rooms where participants race to solve the same challenge |
| 🧠 **Quizzes** | Timed knowledge quizzes with scored attempts and difficulty levels |
| 🏆 **Leaderboards & Ranks** | XP-based ranking system with tiered ranks and visual badges |
| 🎓 **Coaching** | Book 1-on-1 coaching sessions with rated coaches and defined specializations |
| 📅 **Events** | Participate in or organize time-boxed programming events with registration management |
| 🛍️ **Shop** | Purchase branded merch (hoodies, mugs, keyboards, stickers, etc.) with full order lifecycle |
| 🏅 **Achievements & Badges** | Earn badges automatically based on platform activity criteria |
| 🚨 **Reports & Support** | Report content or users; admin support tickets for conflict resolution |
| 🔐 **Auth & Roles** | Auth0-based authentication with permission-level access control |

---

## 🏗️ Architecture

CodeArena is a **monorepo** composed of two main projects:

```
CodeArena/
├── code-arena-backend/        # Spring Boot 3 REST API (Java 17)
├── code-arena-frontend/       # Angular standalone SPA (TypeScript)
├── codearena.sql              # Full database schema + seed data
├── seed_challenges.sql        # Sample challenge data
├── AUTH0_SETUP.md             # Auth0 configuration guide
└── docker-compose.yml         # One-command local deployment
```

### Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3, Spring Security, JPA/Hibernate |
| **Frontend** | Angular (standalone components), TypeScript, CSS |
| **Database** | MariaDB / MySQL |
| **Authentication** | Auth0 (`keycloak_id` stored per user) |
| **Containerization** | Docker + Docker Compose |

---

## 📦 Modules

### 1. 👤 User & Profile Management

Manages user accounts and their public-facing profiles. Each user has a linked `user_profile` with avatar, bio, total XP, and their current rank.

**Entities:** `user`, `user_profile`

| Field | Description |
|---|---|
| `username`, `email` | Core identity fields |
| `keycloak_id` | Maps to the external auth provider |
| `active` | Soft-enable/disable accounts |
| `avatar_url`, `bio` | Public profile display |
| `total_xp`, `current_rank_id` | Gamification state |

---

### 2. 🔐 Authentication & Permissions

Authentication is handled externally via **Auth0**, with each user mapped by `keycloak_id`. Fine-grained access control is handled through a `permission` table, enabling role-based feature gating.

**Entities:** `permission`

For setup instructions, refer to [`AUTH0_SETUP.md`](./AUTH0_SETUP.md).

---

### 3. 🧩 Challenge & Submission System

The core of the platform. Challenges are authored with a title, description, difficulty level, and tags. Each challenge includes hidden and visible test cases. Users submit code in a chosen language, and results are evaluated against all test cases.

**Entities:** `challenge`, `test_case`, `submission`

| Entity | Key Fields |
|---|---|
| `challenge` | `title`, `description`, `difficulty`, `tags`, `author_id` |
| `test_case` | `input`, `expected_output`, `is_hidden`, `challenge_id` |
| `submission` | `code`, `language`, `status`, `xp_earned`, `submitted_at` |

---

### 4. ⚔️ Battle Rooms (Real-Time Competition)

Users can create or join **battle rooms** tied to a specific challenge. Participants compete simultaneously; scores and ranks are tracked per room, and a final battle result records the winner.

**Entities:** `battle_room`, `battle_participant`, `battle_result`

| Entity | Key Fields |
|---|---|
| `battle_room` | `room_key`, `host_id`, `challenge_id`, `status`, `created_at` |
| `battle_participant` | `user_id`, `room_id`, `score`, `rank`, `joined_at` |
| `battle_result` | `room_id`, `winner_id`, `ended_at` |

---

### 5. 🧠 Quiz System

A standalone quiz module with questions of configurable type and point value. Users take quizzes; each attempt records the score and completion timestamp.

**Entities:** `quiz`, `question`, `quiz_attempt`

| Entity | Key Fields |
|---|---|
| `quiz` | `title`, `description`, `difficulty`, `created_by` |
| `question` | `content`, `type`, `points`, `quiz_id` |
| `quiz_attempt` | `user_id`, `quiz_id`, `score`, `completed_at` |

---

### 6. 🏆 Ranks, XP & Gamification

Users accumulate XP through challenge submissions and quiz attempts. Each rank has an XP range (min/max), a name, and an icon. Badges are awarded based on defined criteria and tracked as achievements per user.

**Entities:** `rank`, `badge`, `achievement`

| Entity | Key Fields |
|---|---|
| `rank` | `name`, `min_xp`, `max_xp`, `icon_url` |
| `badge` | `name`, `description`, `criteria`, `icon_url` |
| `achievement` | `user_id`, `badge_id`, `earned_at` |

---

### 7. 🎓 Coaching System

Experienced developers can register as coaches with a bio, specializations, and a rating. Other users can book **coaching sessions** with a full status lifecycle and a virtual meeting URL.

**Entities:** `coach`, `coaching_session`

| Entity | Key Fields |
|---|---|
| `coach` | `user_id`, `bio`, `specializations`, `rating` |
| `coaching_session` | `coach_id`, `learner_id`, `scheduled_at`, `duration_minutes`, `meeting_url`, `status` |

---

### 8. 📅 Programming Events

Organizers can create time-boxed programming events with a maximum participant cap and a status (upcoming / live / ended). Users register for events they want to join.

**Entities:** `programming_event`, `event_registration`

| Entity | Key Fields |
|---|---|
| `programming_event` | `title`, `description`, `start_date`, `end_date`, `max_participants`, `organizer_id`, `status` |
| `event_registration` | `user_id`, `event_id`, `registered_at` |

---

### 9. 🛍️ Merch Shop

A full e-commerce module featuring branded developer merchandise. Supports multiple product categories and a complete order lifecycle.

**Entities:** `shop_items`, `purchases`, `purchase_items`

**Product Categories:** `TSHIRT` · `HOODIE` · `CAP` · `MUG` · `KEYBOARD` · `MOUSEPAD` · `STICKER` · `ACCESSORY` · `OTHER`

**Order Statuses:** `PENDING` → `CONFIRMED` → `SHIPPED` → `DELIVERED` (or `CANCELLED`)

**Sample items:**

| Item | Category | Price |
|---|---|---|
| CodeArena Zip Hoodie | HOODIE | $44.99 |
| Mechanical Keyboard 60% | KEYBOARD | $89.99 |
| ERROR 404 Mug | MUG | $14.99 |
| Developer Sticker Pack x10 | STICKER | $9.99 |
| CodeArena XL Mouse Pad | MOUSEPAD | $24.99 |

---

### 10. 🚨 Reports & Support Tickets

Users can report other users or specific content. Reports are routed to admins through a support ticket system with full resolution tracking.

**Entities:** `report`, `support_ticket`

| Entity | Key Fields |
|---|---|
| `report` | `reporter_id`, `target_id`, `target_type`, `reason`, `status` |
| `support_ticket` | `report_id`, `assigned_admin_id`, `resolution`, `resolved_at` |

---

## 🗄️ Database Schema

The full schema is available in [`codearena.sql`](./codearena.sql). Below is a high-level entity relationship overview:

```
user ─────────── user_profile ─── rank
  │
  ├── submission ──────── challenge ─── test_case
  │
  ├── battle_participant ─── battle_room ─── battle_result
  │
  ├── quiz_attempt ─── quiz ─── question
  │
  ├── achievement ─── badge
  │
  ├── coaching_session ─── coach
  │
  ├── event_registration ─── programming_event
  │
  ├── purchases ─── purchase_items ─── shop_items
  │
  └── report ─── support_ticket
```

---

## 🚀 Getting Started

### Prerequisites

- [Docker & Docker Compose](https://docs.docker.com/get-docker/)
- [Java 17+](https://adoptium.net/) (for local backend development)
- [Node.js 18+](https://nodejs.org/) (for local frontend development)
- An [Auth0](https://auth0.com/) (see `AUTH0_SETUP.md`)

### 1. Clone the Repository

```bash
git clone https://github.com/AdamZahi/CodeArena.git
cd CodeArena
```

### 2. Configure Environment Variables

Copy and fill in the required environment values for the backend (database URL, Auth0 credentials, etc.) as described in [`AUTH0_SETUP.md`](./AUTH0_SETUP.md).

### 3. Run with Docker Compose

```bash
docker-compose up --build
```

This starts:
- The **Spring Boot API** (backend)
- The **Angular SPA** (frontend)
- A **MariaDB** database instance

### 4. Initialize the Database

Import the schema and optional seed data:

```bash
mysql -u root -p codearena < codearena.sql
mysql -u root -p codearena < seed_challenges.sql
```

### 5. Access the Application

| Service | URL |
|---|---|
| Frontend | `http://localhost:4200` |
| Backend API | `http://localhost:8080` |

---

## 🗂️ Suggested GitHub Topics

Add these topics to the repository for better discoverability:

`competitive-programming` · `java` · `spring-boot` · `angular` · `typescript` · `coding-platform` · `gamification` · `auth0` · `docker` · `monorepo` · `rest-api` · `mysql` · `e-commerce`

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes with a clear, descriptive message
4. Open a Pull Request describing what you changed and why

---

## 📄 License

This project is open source. See the [LICENSE](LICENSE) file for details.

---