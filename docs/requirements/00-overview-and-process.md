# NYW Fare Configuration & Management System

## Requirements and Specification Document

**Date:** 11/16/2026 | **Version:** v2.0 — Iteration 2

---

### Iteration 2 Change-Tracking Legend

To clearly indicate modifications since Iteration 1 for project markers, all text and structural changes in this document follow this color-coding convention:

* <span class="it2-added">**Blue Text / Border:**</span> Brand-new features, sections, or user stories added specifically for Iteration 2.
* <span class="it2-modified">**Amber Highlight:**</span> Modifications, re-taggings (e.g., shifted from Iteration 1 to Iteration 2), or scope adjustments made based on client meetings.
* <span class="it2-removed">~~Red Strikethrough:~~</span> Deprecated or explicitly removed features (such as Google Login and the TVM Kiosk canvas mockup) per client feedback.

---

## Preface & Version History

### Expected Readership

This document is prepared for CMPT 276 course instructors, teaching assistants, project markers, and system stakeholders (including client Eldon Wong). It defines the software requirements, system architecture, user stories, and development process for the NYW Fare Configuration & Management System.

### Version History & Rationale

* **v1.0 (Iteration 1):** Initial baseline proposal and foundational implementation. Established user authentication, basic role-based access control (`OPERATOR` and `CLIENT_SERVICES`), database connection, application shell, and initial static UI mockups.
* <span class="it2-added">**v2.0 (Iteration 2):** Current production release. Introduces interactive Point-to-Point (O-D) and Zonal rate editing matrices, local PostgreSQL draft persistence, client-side currency input masking, real-time draft status counters, single-cell and global revert workflows, bilingual (EN/ES) service alert management, temporal sales window configuration, and staging review diff interfaces. Deprecates Google Login to concentrate resources on core backend and frontend validation.</span>

---

## Introduction & Project Abstract

The NYW Fare Configuration & Management System is a centralized administrative web application designed to replace error-prone, manual JSON file manipulation with a robust, structured interface. It empowers transit operators and administrators to manage complex fare grids, schedule availability rules, and pre-purchase service alerts across multi-tenant environments (such as NY Waterway and Metra). The system interfaces with the underlying Caracal platform API while maintaining local staging workflows to prevent malformed configurations from reaching physical Ticket Vending Machines (TVMs).

### Customer

* **Primary Stakeholder:** Eldon Wong (Transit Operations & Systems Architect).
* **Operational Goals:** Eliminate manual JSON syntax errors, streamline rate updates across complex terminal topologies, provide immediate visual feedback on draft changes, and enable safe pre-staging review before platform dispatch.

### Competitive Landscape

Traditional transit configuration relies on raw spreadsheet editing and direct JSON file commits via Git repositories, which lack built-in validation constraints, role-based guardrails, or diff auditing. Our system introduces a dedicated administrative dashboard featuring strict input sanitization, multi-role access control, visual draft highlighting, and automated batch API validation.

---

## Team Process, Retrospective & Velocity

### Iteration 1 Retrospective & Process Improvements

Following Iteration 1, the team conducted a retrospective and identified several areas for workflow optimization:

1. **Work Distribution:** In Iteration 1, certain tasks were front-loaded or concentrated near deadlines. For Iteration 2, the team established strict vertical slicing and distributed tasks evenly across all five members.
2. **Branching & PRs:** All development strictly uses feature branches (`feature/epicX-...`) with mandatory Pull Requests and peer code reviews before m

::: {.it2-added}

### Iteration 2 Velocity & Sprint Progress

In accordance with CMPT 276 agile guidelines (targeting 1–2 story points per team member per week across a 5-person team, or 10–20+ points per 2-week sprint), the team tracked story points in GitHub Projects strictly for completed, tested, and merged user stories:

| Iteration | Completed Stories | Completed Story Points | Sprint Velocity (Pts / Week) | Per-Member Velocity (Pts / Member / Week) |
| :--- | :--- | :---: | :---: | :---: |
| **Iteration 1** *(Oct 19 – Nov 2)* | Stories `1.1–1.13`, `2.1` *(14 stories)* | **32 pts** | **16.0 pts/wk** | **1.60 pts/member/wk** |
| **Iteration 2** *(Nov 3 – Nov 16)* | Stories `1.15–1.17`, `2.2`, `2.6a`, `2.6b`, `3.1`, `3.3`, `4.1–4.3`, `4.5`*, `5.1` *(13 stories)* | **33 pts** *(31 pts fully closed + 2 pts in 4.5)* | **16.5 pts/wk** | **1.65 pts/member/wk** |

* **Scope Refinements & Client-Driven Adjustments:**
  * **Removed per Client Feedback (6 pts excluded from velocity):** Story `1.14` (Google Login, `3 pts`) and Story `4.6` (Live TVM Kiosk Preview, `3 pts`) were removed following the Nov 6 client sync with Eldon Wong to focus engineering capacity on core local staging and validation workflows.
  * **Deferred to Iteration 3 Backlog (20 pts):** Zonal tier management and station topology stories (`2.3`, `2.4`, `2.5` — `13 pts`), permanent product phase-outs (`3.2` — `2 pts`), and expanded multi-day/holiday alert schedules (`4.4` — `5 pts`, expanded after the Nov 16 client meeting) were moved to Iteration 3 alongside external Caracal REST API integration (`3.4–3.6`, `5.2–5.4`).
:::

### Work Distribution

* **Harendeep Singh:** Epic 1 Lead & Epic 4 Backend (`ActorService` identity tracking, user search and account deletion, RBAC enforcement, and `ServiceAlert` / `AlertService` backend persistence and validation).
* **Oliver Tadaniewicz:** Epic 2 Lead & DevOps (Interactive O-D Rate Grid inline editing, numeric input masking, client-side `DraftStateManager`, diff confirmation modal, PostgreSQL `FareDraft` persistence, GitHub Actions CI, Render deployment, and document compilation).
* **Athan Liu:** Epic 3 Lead (Terminal ticket availability toggles, batch staging persistence in `availability_rules`, and temporal sales window condition builder modal in `temporal_conditions`).
* **Tejas Dutt:** Epic 4 Frontend & Controller Lead (Bilingual `EN/ES` service alert views, multi-route assignment, draft/active/archive lifecycle controllers, removal of deprecated TVM preview, and `AlertControllerTests`).
* **Lakshya Joshi:** Epic 5 Lead (Configuration staging review diff interface at `/publishing/review`, `StagingService` integration with PostgreSQL fare drafts, and controller/service unit tests).

### Application Access

To facilitate rapid grading and testing without troubleshooting registration errors, pre-seeded test accounts for both `CLIENT_SERVICES` (Administrator) and `OPERATOR` roles are active on the production deployment:

* **Web Application URL:** `https://nyw-fare-config.onrender.com`
* **Git Repository:** `https://github.com/harpoonwithaz/nyw-fare-config`
* **Login Credentials:** Provided in the accompanying `info.txt` file included in the Canvas submission archive.

---
