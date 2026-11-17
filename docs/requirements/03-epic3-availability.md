## Epic 3: Ticket Availability and Product Lifecycle Management

This document defines the formal user stories and technical tasks for Epic 3 of the NY Waterway Fare Configuration Tool. Each story adheres strictly to the CMPT 276 agile project specification, INVEST criteria, and standardized persona definitions. <span class="it2-added">In Iteration 2, Stories 3.1 and 3.3 were implemented with local PostgreSQL persistence (`availability_rules` and `temporal_conditions` tables), canonical product identifiers (`ADULT_ONEWAY`, `CHILD_ONEWAY`, `SENIOR_ONEWAY`, `FULL_10TRIP`, `FULL_MONTHLY`), CSRF-protected batch staging (`POST /availability/api/stage`), and automated MockMvc test coverage, while Story 3.2 (Permanent Phase-Outs) and live Caracal REST API synchronization (Stories 3.4–3.6) are scheduled for Iteration 3.</span>

---

### Story 3.1: Terminal Ticket Availability UI Controls

- **GitHub Issue Title:** `[Availability] Terminal Ticket Availability UI Controls`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As an authenticated operator, I want interactive toggle switches for individual ticket classes across specific terminal rate tables, so that I can easily manage product availability and control offerings at each station.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* An operator is logged into the application, <span class="it2-added">has selected an active tenant (`NYW_SANDBOX` or `METRA`),</span> and navigates to the Availability view (`/availability`) with a terminal selected <span class="it2-added">(`MIDTOWN`, `PIER11`, `BROOKFD`, or `EDGEWTR`)</span>.
  - *Trigger:* The operator flips an availability toggle switch for a ticket product.

- **Actions / Postconditions:**
  - *Action:* The operator clicks a toggle switch corresponding to a ticket class.
  - *Postcondition:* The switch updates its visual state, changes the row status label to `Available` or `Unavailable` with a `STAGED MODIFICATION` badge, and registers the modification in the client-side staging buffer (`stagedAvailabilityDrafts`) <span class="it2-added">until persisted via `POST /availability/api/stage`</span>.

#### Acceptance Criteria

1. Each canonical ticket class (`ADULT_ONEWAY`, `CHILD_ONEWAY`, `SENIOR_ONEWAY`, `FULL_10TRIP`, `FULL_MONTHLY`) per terminal displays an active/inactive toggle switch.
2. Disabling a ticket class instantly updates its visual status on the table (displaying `Unavailable` and a `STAGED MODIFICATION` badge).
3. Availability changes are captured as pending draft modifications before batch persistence to PostgreSQL (`availability_rules`), surviving page refreshes.
4. <span class="it2-modified">Accessing `/availability` without an active tenant displays the standard `"No Tenant Selected"` guidance card, and unauthenticated or CSRF-missing `POST /availability/api/stage` requests are rejected (`403 Forbidden`).</span>

#### Test Scenarios

- **Manual (Success):** Select tenant `NYW_SANDBOX`, navigate to `/availability?site=EDGEWTR`, locate `SENIOR_ONEWAY`, flip its toggle to "Off", and click **Save Availability Drafts**. Refresh the page and verify `SENIOR_ONEWAY` remains `Unavailable`.
- **Manual (Failure / Edge Case):** Navigate to `/availability` with no tenant selected in the global navbar. Verify the `"No Tenant Selected"` feedback card is displayed and product controls remain hidden.
- **Automated (Unit / Slice / Integration):** <span class="it2-modified">`AvailabilityControllerMockMvcTests` (`@SpringBootTest` + `@AutoConfigureMockMvc` with H2) asserting `GET /availability` shows `"No Tenant Selected"` when no tenant is bound, `POST /availability/api/stage` without CSRF returns `403 Forbidden`, and valid submissions persist to `AvailabilityRuleRepository` and hydrate on reload.</span>

#### Actionable Sub-tasks

- [x] Build availability table view with Bootstrap toggle switches (`form-check-input`).
- [x] Connect toggle change listeners to client-side `stagedAvailabilityDrafts` buffer.
- [x] Add visual badge rendering (`Available` vs `Unavailable` and `STAGED MODIFICATION` / `SAVED IN DB`).
- [x] Implement controller endpoints `GET /availability` and `POST /availability/api/stage` with `AvailabilityRuleRepository` persistence and `ActorService` audit tracking.
- [x] Write `AvailabilityControllerMockMvcTests` covering view rendering, tenant guardrails, CSRF enforcement, and H2 persistence.

---

### Story 3.2: Permanent Phase-Out Handling & Local Persistence

- **GitHub Issue Title:** `[Availability] Permanent Phase-Out Handling & Local Persistence`

- **Iteration:** <span class="it2-modified">`3` *(Deferred from Iteration 2)*</span>

- **Story Points:** `2`

- **Story Description:** As a Caracal administrator, I want permanent product phase-outs (such as network-wide Senior fare discontinuations) to persist across session reloads, so that discontinued tickets are permanently blocked without breaking underlying database schemas.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* An operator or administrator is logged in with access to product lifecycle settings.
  - *Trigger:* The user flags a ticket product for permanent phase-out.

- **Actions / Postconditions:**
  - *Action:* The user confirms the permanent phase-out in a confirmation dialog.
  - *Postcondition:* The application updates the local PostgreSQL database to permanently disable the product class across all relevant regional rate tables, persisting across browser restarts.

#### Acceptance Criteria

1. Permanent phase-outs completely disable purchasing permissions (`NOT_SOLD`) for specified products across all terminals.
2. Phased-out states persist in PostgreSQL across session reloads and browser restarts.
3. Phased-out products cannot be re-enabled by standard `OPERATOR` users without `CLIENT_SERVICES` clearance.
4. The system prompts the user with an explicit confirmation dialog before applying a permanent phase-out.

#### Test Scenarios

- **Manual (Success):** Log in as `CLIENT_SERVICES`, flag Senior fares as permanently phased out, and refresh the browser. Verify the product remains disabled and labeled as phased out across all terminals.
- **Manual (Failure / Edge Case):** Log in as `OPERATOR` and attempt to re-enable a permanently phased-out ticket product. Verify action is blocked and a 403 Forbidden or permission alert displays.
- **Automated (Unit / Slice / Integration):** `@DataJpaTest` verifying phased-out entity flags persist in the local PostgreSQL repository and prevent unauthorized status mutations.

#### Actionable Sub-tasks

- [ ] Add `isPhasedOut` attribute to `AvailabilityRule` schema.
- [ ] Build confirmation modal dialog for permanent phase-outs.
- [ ] Enforce `CLIENT_SERVICES` authorization checks for reactivating phased-out products.
- [ ] Write persistence tests using `@DataJpaTest`.

---

### Story 3.3: Temporal Sales Window & Condition Builder Modal

- **GitHub Issue Title:** `[Availability] Temporal Sales Window & Condition Builder Modal`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As an authenticated operator, I want a dedicated modal to define and attach temporal availability conditions (such as <span class="it2-modified">recurring weekly day/time schedules or promotional date ranges</span>) to specific ticket products, so that automated calendar rules and recurring sales windows are properly enforced.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* An operator is on the availability matrix with an active tenant and terminal selected.
  - *Trigger:* The operator clicks the "Configure Condition" gear button (`btn-configure-rule`) on a ticket product row.

- **Actions / Postconditions:**
  - *Action:* The user selects a condition type (<span class="it2-modified">`RECURRING_WEEKLY` with day-of-week checkboxes and start/end times, or `TEMPORARY_PROMO` with start/end calendar dates</span>) and clicks **Apply Condition**.
  - *Postcondition:* The condition is staged in `stagedAvailabilityDrafts`, updates the row's constraint badge immediately, and persists to the `temporal_conditions` table upon clicking **Save Availability Drafts**.

#### Acceptance Criteria

1. <span class="it2-modified">A dedicated modal interface (`#conditionBuilderModal`) allows configuration of recurring weekly windows (days of week + `LocalTime` start/end range) and temporary promotional windows (`LocalDate` start/end range).</span>
2. Day-of-week filtering (`1 = Mon` through `7 = Sun`) isolates weekday commuter products from weekend schedules and evaluates active status via `TemporalCondition.isActiveAt(LocalDateTime)`.
3. Client-side modal controls dynamically toggle visible inputs based on the selected condition type (`RECURRING_WEEKLY` vs `TEMPORARY_PROMO`).
4. <span class="it2-added">Saved temporal conditions eagerly load (`FetchType.EAGER`) and render human-readable constraint summaries on the availability table across page refreshes.</span>

#### Test Scenarios

- **Manual (Success):** Open the condition modal for `ADULT_ONEWAY`, select `Recurring Weekly`, check Mon–Fri (`1–5`), set `06:00` to `22:00`, apply, and save. Refresh `/availability` and verify the row badge displays `5 Days (06:00-22:00)`.
- **Manual (Failure / Edge Case):** Switch the condition radio button to `Temporary Promo` and verify day-of-week/time inputs hide while calendar date pickers appear cleanly.
- **Automated (Unit / Slice / Integration):** <span class="it2-modified">`AvailabilityControllerMockMvcTests` verifying `POST /availability/api/stage` with a `RECURRING_WEEKLY` payload persists `TemporalCondition` (`daysOfWeek = [1, 2, 3, 4, 5]`) and renders the saved constraint summary on subsequent `GET /availability` requests.</span>

#### Actionable Sub-tasks

- [x] Build Bootstrap condition builder modal (`group-edit-modal.html`) in Thymeleaf template.
- [x] Implement `RECURRING_WEEKLY` (day checkboxes + time range) and `TEMPORARY_PROMO` (date range) dynamic form inputs in `availability.js`.
- [x] Persist `TemporalCondition` via `@OneToOne(cascade = CascadeType.ALL)` on `AvailabilityRule` and display active condition badges on the availability grid.
- [x] Write `AvailabilityControllerMockMvcTests` verifying temporal rule persistence and reload hydration.

---

### Story 3.4: Backend REST API Integration for Availability & When-Conditions

- **GitHub Issue Title:** `[Availability] Backend REST API Integration for Availability & When-Conditions`

- **Iteration:** `3`

- **Story Points:** `3`

- **Story Description:** As an authenticated operator, I want local availability toggles and temporal sales rules to synchronize with Caracal's backend via REST API endpoints, so that live ticket vending machines enforce the configured sales restrictions and remote rate assignments.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* Availability edits or temporal conditions are persisted locally in PostgreSQL.
  - *Trigger:* The operator publishes reviewed configurations in Epic 5.

- **Actions / Postconditions:**
  - *Action:* The Spring Boot backend serializes the saved rules and dispatches HTTP calls to `/functions/rate-assignment-update` and `/functions/when-condition-create`.
  - *Postcondition:* Caracal platform updates rate assignments and displays a success alert with the operator's `X-Actor` header recorded.

#### Acceptance Criteria

1. Spring WebClient successfully packages availability updates into requests targeting `/functions/rate-assignment-update`.
2. Temporal rules are transmitted to the `/functions/when-condition-create` endpoint using correct JSON schemas.
3. Authentication headers (`X-Actor: operator:<email>`) captured in `AvailabilityRule.updatedBy` are attached to all outgoing API requests.
4. If Caracal's backend returns an error (4xx/5xx), local rule state is retained and an error banner is displayed.

#### Test Scenarios

- **Manual (Success):** Toggle a ticket's availability, save, and publish. Verify `/functions/rate-table-get` reflects the new assignment status.
- **Manual (Failure / Edge Case):** Simulate Caracal API server downtime (HTTP 500). Verify an error alert displays ("Failed to synchronize availability with platform").
- **Automated (Unit / Slice / Integration):** WireMock integration test asserting `POST /functions/rate-assignment-update` and `POST /functions/when-condition-create` receive correct JSON bodies and `X-Actor` headers, returning HTTP 200.

#### Actionable Sub-tasks

- [ ] Implement `CaracalApiClient.updateRateAssignment()` and `createWhenCondition()`.
- [ ] Propagate `X-Actor` header from `AvailabilityRule.updatedBy` / `ActorService`.
- [ ] Build batch sync service to coordinate assignment and condition updates with Epic 5 publishing.
- [ ] Write WireMock integration tests covering API success and failure paths.

---

### Story 3.5: Automated Unit & Integration Testing for Epic 3

- **GitHub Issue Title:** `[Availability] Automated Unit & Integration Testing for Epic 3`

- **Iteration:** `3`

- **Story Points:** `2`

- **Story Description:** As a developer, I want expanded automated unit and integration tests covering temporal date evaluators and Caracal API payload serialization, so that regressions are prevented before merging code into main releases.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* Caracal REST client integration for Epic 3 is implemented.
  - *Trigger:* A developer runs local test suites or triggers GitHub Actions CI.

- **Actions / Postconditions:**
  - *Action:* JUnit, MockMvc, and WireMock execute test routines across availability components.
  - *Postcondition:* All tests pass, verifying temporal condition evaluations and external Caracal API payload serialization.

#### Acceptance Criteria

1. Unit tests validate that `TemporalCondition.isActiveAt()` correctly evaluates recurring weekly and temporary promotional windows across boundary timestamps.
2. Spring Boot integration tests mock external Caracal API endpoints to confirm payload accuracy.
3. CI test suite executes in under 60 seconds with 100% pass rate.

#### Test Scenarios

- **Manual (Success):** Run `./mvnw test -Dtest="!NywFareConfigApplicationTests"` locally; verify all Epic 3 unit and integration tests execute with 0 failures and 0 errors.
- **Manual (Failure / Edge Case):** Introduce a failing assertion in `AvailabilityControllerMockMvcTests`; run Maven tests and verify the build fails with a descriptive report.
- **Automated (Unit / Slice / Integration):** Maven CI build pipeline runs unit, MockMvc, and WireMock integration tests on pull requests.

#### Actionable Sub-tasks

- [ ] Write unit tests for `TemporalCondition.isActiveAt()` boundary cases.
- [ ] Add WireMock test suite for Caracal `rate-assignment-update` and `when-condition-create` serialization.

---

### Story 3.6: Documentation for Availability & Temporal Rules Management

- **GitHub Issue Title:** `[Availability] Documentation for Availability & Temporal Rules Management`

- **Iteration:** `3`

- **Story Points:** `1`

- **Story Description:** As an authenticated operator or developer, I want clear internal documentation detailing how to configure ticket availability toggles and temporal sales windows, so that team members and future maintainers understand the workflow and API mappings.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* Epic 3 features are implemented.
  - *Trigger:* A developer or operator reviews repository documentation.

- **Actions / Postconditions:**
  - *Action:* Developer authors a markdown guide in the repository.
  - *Postcondition:* A complete documentation file outlines UI workflows, date constraint syntax, and backend API integration steps.

#### Acceptance Criteria

1. Documentation explains how operators interact with terminal availability toggles and condition modals.
2. API endpoints (`rate-assignment-update` and `when-condition-create`) are documented with expected JSON payloads.
3. Instructions for running Epic 3 tests are clearly outlined for the engineering team.

#### Test Scenarios

- **Manual (Success):** Open `docs/availability-guide.md` and follow the steps to configure a temporal sales window; verify all documented UI controls and API schemas match the codebase.
- **Manual (Failure / Edge Case):** Verify that missing API fields or undocumented endpoints are flagged during peer review before merging the documentation PR.
- **Automated (Unit / Slice / Integration):** Markdown formatting and internal links verified during documentation PR review.

#### Actionable Sub-tasks

- [ ] Author `docs/availability-guide.md` with UI workflow walkthrough.
- [ ] Document Caracal API request/response contracts for Epic 3.
- [ ] Add testing instructions and troubleshooting tips for developers.