# Project Roadmap: NY Waterway Fare Configuration Tool

This document outlines the development schedule, weekly meeting cadence, and
release milestones for the NY Waterway Fare Configuration Tool. It serves as
the primary tracking guide for the Caracal Technologies client (Eldon) and the
student engineering team to ensure alignment across all three course
iterations.

## Key Deadlines & Meeting Schedule

Our team operates on a 2-week iteration cycle, culminating in course
deliverables. Client check-ins are scheduled for 30 minutes on Friday mornings
at 11:00 AM, with a slight adjustment for the course midterm.

| Date | Event / Milestone | Focus / Deliverable |
| --- | --- | --- |
| Mon, Oct 19 | Proposal Submission | Project scope locked, epics assigned, and repository initialized. |
| Thu, Oct 22 | CMPT 276 Midterm | No client meetings or heavy development. |
| Mon, Oct 26 | Client Sync (11:00 AM) | Rescheduled sync to review Iteration 1 UI wireframes and database connection. |
| Fri, Oct 30 | Client Sync (11:00 AM) | Demo local login, session management, and Render deployment. |
| Mon, Nov 2 | ITERATION 1 DUE | Release 1: Working infrastructure, Render deployment, and Authentication. |
| Fri, Nov 6 | Client Sync (11:00 AM) | Review Rate Grid mockups and Alert banner database schemas. |
| Fri, Nov 13 | Client Sync (11:00 AM) | Demo local persistence of Alert messages and Rate Grid UI validation. |
| Mon, Nov 16 | ITERATION 2 DUE | Release 2: Core Interface, offline data management, and SSO integration. |
| Fri, Nov 20 | Client Sync (11:00 AM) | Demo live Caracal API connectivity (`price-simulate`, `od-matrix-set`). |
| Fri, Nov 27 | Client Sync (11:00 AM) | Final feature review: Discrepancy auditing and catalog publication pipeline. |
| Mon, Nov 30 | ITERATION 3 DUE | Release 3: Full Platform Integration and API completion. |
| Tue, Dec 1 & 3 | Final Demo Days | Presenting the completed system to course staff and stakeholders. |

## Iteration Breakdown & Epic Timelines

The project is divided into three major iterations to incrementally deliver
value while meeting course requirements.

### Iteration 1: Foundation & Security (Oct 19 – Nov 2)

**Goal:** Establish the application infrastructure, continuous deployment
pipeline, and user authentication to satisfy the first course milestone.

- **Infrastructure (All Hands):** Set up the GitHub repository, provision the
  PostgreSQL database, configure Spring Boot, and establish the automated
  deployment pipeline to Render.
- **Epic 1 - Local Authentication (Harendeep):** Build the secure login portal
  using Spring Security. Create local session management and role-based access
  control (RBAC) to ensure only authorized Caracal/NYW staff can access the
  tool.
- **UI Scaffolding (Oliver & Lakshya):** Set up the Thymeleaf templating engine
  and Bootstrap layout so the application has a unified navigation shell for
  future epics.

### Iteration 2: Core Interfaces & Local Persistence (Nov 3 – Nov 16)

**Goal:** Build the primary administrative interfaces and implement local
database storage for features that the Caracal platform cannot natively store.

- **Epic 1 Expansion - SSO (Harendeep):** Transition the local login system to
  Google Single Sign-On (SSO) to match Caracal's internal identity provider.
- **Epic 4 - Alert Designer & Preview (Tejas):** Implement the local PostgreSQL
  schema to save route warning messages, inactive schedules, and bilingual text
  (English/Spanish). Build the visual TVM simulator canvas.
- **Epic 2 - Rate Grid UI & Validation (Oliver):** Build the interactive
  origin-destination matrix frontend. Implement strict browser-side and
  server-side validation (enforcing 2 decimal places, rejecting
  blanks/negatives) before connecting it to the external API.
- **Epic 3 - Availability Scaffolding (Athan):** Design the frontend toggles for
  ticket availability and define the temporal conditions logic (e.g., monthly
  pass windows).

### Iteration 3: REST API Integration & Publication (Nov 17 – Nov 30)

**Goal:** Connect the application to Caracal's Dockerized Go backend via HTTP
REST endpoints, fulfilling the final course requirement and delivering the
complete configuration lifecycle.

- **Epic 2 API Integration (Oliver):** Wire the Rate Grid to the platform API.
  Use `price-simulate` to fetch current rates and `od-matrix-set` to push price
  adjustments.
- **Epic 3 API Integration (Athan):** Connect the availability toggles to the
  `rate-assignment-update` and `when-condition-create` endpoints to enforce live
  sales windows.
- **Epic 5 - Review & Publication (Lakshya):** Build the staging diff viewer.
  Implement automated validation against the baseline dataset
  (`check-against-sheet.mjs`) to resolve the 50 known catalog discrepancies.
  Hook up the `catalog-publish` and `audit-log-get` endpoints for final release
  execution.
