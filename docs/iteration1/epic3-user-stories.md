## Epic 3: Ticket Availability and Product Lifecycle Management

This document defines the formal user stories and technical tasks for Epic 3 of the NY Waterway Fare Configuration Tool. Each story adheres strictly to the CMPT 276 agile project specification, INVEST criteria, and standardized persona definitions.

---

### Story 3.1: Terminal Ticket Availability UI Controls

- **GitHub Issue Title:** `[Availability] Terminal Ticket Availability UI Controls`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As an authenticated operator, I want interactive toggle switches for individual ticket classes across specific terminal rate tables, so that I can easily manage product availability and control offerings at each station.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* An operator is logged into the application and navigates to the Availability view (`/availability`) with a terminal selected.
  - *Trigger:* The operator flips an availability toggle switch for a ticket product.

- **Actions / Postconditions:**
  - *Action:* The operator clicks a toggle switch corresponding to a ticket class.
  - *Postcondition:* The switch updates its visual state, changes the cell badge to `NOT_SOLD` or `SELLABLE`, and registers the modification in the client-side draft state manager.

#### Acceptance Criteria

1. Each ticket class per terminal displays an active/inactive toggle switch.
2. Disabling a ticket class instantly updates its visual status on the grid (e.g., displaying a muted `NOT_SOLD` badge).
3. Availability changes are captured as pending draft modifications before final commit.
4. Toggling an unconfigured route or invalid state displays an error notification and prevents toggling.

#### Test Scenarios

- **Manual (Success):** Select terminal `EDGEWTR`, locate `SENIOR_ONEWAY`, and flip its toggle to "Off". Verify the cell displays `NOT_SOLD` and the pending changes counter increments by 1.
- **Manual (Failure / Edge Case):** Attempt to toggle an availability switch while offline or when page state is stale. Verify an error alert appears ("Unable to stage change: stale state") and the toggle reverts to its original position.
- **Automated (Unit / Slice / Integration):** `@WebMvcTest` asserting `GET /availability?site=EDGEWTR` returns HTTP 200 with terminal products and toggle models; JavaScript unit test verifying toggle events update `DraftStateManager`.

#### Actionable Sub-tasks

- [ ] Build availability table view with Bootstrap toggle switches (`form-check-input`).
- [ ] Connect toggle change listeners to client-side `DraftStateManager`.
- [ ] Add visual badge rendering (`SELLABLE` vs `NOT_SOLD`).
- [ ] Implement controller endpoint `GET /availability` in Spring Boot.
- [ ] Write MockMvc slice tests for view rendering.

---

### Story 3.2: Permanent Phase-Out Handling & Local Persistence

- **GitHub Issue Title:** `[Availability] Permanent Phase-Out Handling & Local Persistence`

- **Iteration:** `2`

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

- [ ] Add `isPhasedOut` attribute to local product/availability schema.
- [ ] Build confirmation modal dialog for permanent phase-outs.
- [ ] Enforce `CLIENT_SERVICES` authorization checks for reactivating phased-out products.
- [ ] Write persistence tests using `@DataJpaTest`.

---

### Story 3.3: Temporal Sales Window & Condition Builder Modal

- **GitHub Issue Title:** `[Availability] Temporal Sales Window & Condition Builder Modal`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As an authenticated operator, I want a dedicated modal to define and attach temporal availability conditions (such as 
monthly pass windows or weekday/weekend segregation) to specific ticket products, so that automated calendar rules and recurring sales windows are properly enforced.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* An operator is on the availability matrix.
  - *Trigger:* The operator clicks the "Configure Condition" icon on a ticket product.
- **Actions / Postconditions:**
  - *Action:* The user selects a condition type (e.g., `DAY_OF_MONTH` for monthly passes, `WEEKDAY`, or `WEEKEND`) and sets date/time constraints.
  - *Postcondition:* The condition is validated and staged in draft state for that ticket assignment.

#### Acceptance Criteria

1. A dedicated modal interface allows configuration of date ranges and recurring monthly pass sales windows (e.g., 20th of the previous month to 10th of the current month).
2. Day-of-week filtering correctly isolates weekday commuter products from weekend fares.
3. Client-side and server-side validation ensures start dates/days are valid and prevents malformed inputs.
4. Modal shows current active conditions attached to the selected product.

#### Test Scenarios

- **Manual (Success):** Open condition modal for `FULL_MONTHLY`, select `DAY_OF_MONTH`, set start to 20 and end to 10, and click Save. Verify the modal closes and the product displays the monthly window badge.
- **Manual (Failure / Edge Case):** Enter an invalid day of month (e.g., start day 35 or negative values). Verify validation displays an error ("Day must be between 1 and 31") and blocks saving.
- **Automated (Unit / Slice / Integration):** JUnit service test verifying temporal condition validator correctly parses recurring monthly windows (20 to 10) and flags out-of-range days.

#### Actionable Sub-tasks

- [ ] Build Bootstrap condition builder modal in Thymeleaf template.
- [ ] Implement date/day-of-month form inputs with validation constraints.
- [ ] Add visual badge on availability grid indicating active temporal rules.
- [ ] Write JUnit unit tests for temporal window boundary parsing.

---

### Story 3.4: Backend REST API Integration for Availability & When-Conditions

- **GitHub Issue Title:** `[Availability] Backend REST API Integration for Availability & When-Conditions`

- **Iteration:** `3`

- **Story Points:** `3`

- **Story Description:** As an authenticated operator, I want local availability toggles and temporal sales rules to synchronize with Caracal's backend via REST API endpoints, so that live ticket vending machines enforce the configured sales restrictions and remote rate assignments.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* Availability edits or temporal conditions are in draft state.
  - *Trigger:* The operator clicks "Save Availability & Conditions".

- **Actions / Postconditions:**
  - *Action:* The Spring Boot backend serializes the staged changes and dispatches HTTP calls to `/functions/rate-assignment-update` and `/functions/when-condition-create`.
  - *Postcondition:* Caracal platform updates rate assignments, clears local draft state, and displays a success alert with the operator's `X-Actor` header recorded.

#### Acceptance Criteria

1. Spring WebClient successfully packages availability updates into requests targeting `/functions/rate-assignment-update`.
2. Temporal rules are transmitted to the `/functions/when-condition-create` endpoint using correct JSON schemas.
3. Authentication headers (`X-Actor: operator:<email>`) are properly attached to all outgoing API requests.
4. If Caracal's backend returns an error (4xx/5xx), draft state is retained and an error banner is displayed.

#### Test Scenarios

- **Manual (Success):** Toggle a ticket's availability, click Save, and verify a success banner appears. Check that `/functions/rate-table-get` reflects the new assignment status.
- **Manual (Failure / Edge Case):** Simulate Caracal API server downtime (HTTP 500). Verify an error alert displays ("Failed to synchronize availability with platform") and staged toggles remain in draft state.
- **Automated (Unit / Slice / Integration):** WireMock integration test asserting `POST /functions/rate-assignment-update` and `POST /functions/when-condition-create` receive correct JSON bodies and `X-Actor` headers, returning HTTP 200.

#### Actionable Sub-tasks

- [ ] Implement `CaracalApiClient.updateRateAssignment()` and `createWhenCondition()`.
- [ ] Add `X-Actor` header propagation from authenticated session.
- [ ] Build batch persistence service to coordinate assignment and condition updates.
- [ ] Write WireMock integration tests covering API success and failure paths.

---

### Story 3.5: Automated Unit & Integration Testing for Epic 3

- **GitHub Issue Title:** `[Availability] Automated Unit & Integration Testing for Epic 3`

- **Iteration:** `3`

- **Story Points:** `2`

- **Story Description:** As a developer, I want automated unit and integration tests covering ticket availability toggles, temporal date parsers, and API payload serialization, so that regressions are prevented before merging code into main releases.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* Availability modules and services are implemented.
  - *Trigger:* A developer runs local test suites or triggers GitHub Actions CI.

- **Actions / Postconditions:**
  - *Action:* JUnit, Mockito, and MockMvc execute test routines across availability components.
  - *Postcondition:* All tests pass, verifying draft state management, temporal condition evaluations, and mock endpoint responses.

#### Acceptance Criteria

1. Unit tests verify that toggling ticket availability updates the internal draft state model properly.
2. Test suites validate that the temporal condition parser correctly evaluates active sales windows.
3. Spring Boot integration tests mock external Caracal API endpoints to confirm payload accuracy.
4. CI test suite executes in under 60 seconds with 100% pass rate.

#### Test Scenarios

- **Manual (Success):** Run `./mvnw test` locally; verify all Epic 3 unit and slice tests execute with 0 failures and 0 errors.
- **Manual (Failure / Edge Case):** Introduce an intentional syntax error or failing assertion in `AvailabilityServiceTest`; run `./mvnw test` and verify Maven build fails with a descriptive test failure report.
- **Automated (Unit / Slice / Integration):** Maven CI build pipeline runs unit, slice (`@WebMvcTest`), and integration tests on pull requests.

#### Actionable Sub-tasks

- [ ] Write unit tests for `AvailabilityDraftManager`.
- [ ] Write unit tests for temporal date condition evaluator.
- [ ] Create MockMvc slice tests for availability controller endpoints.
- [ ] Add WireMock test suite for availability payload serialization.

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
3. Instructions for running Epic 3 unit and integration tests are clearly outlined for the engineering team.

#### Test Scenarios

- **Manual (Success):** Open `docs/availability-guide.md` and follow the steps to configure a monthly pass window; verify all documented UI controls and API schemas match the codebase.
- **Manual (Failure / Edge Case):** Verify that missing API fields or undocumented endpoints are flagged during peer review before merging the documentation PR.
- **Automated (Unit / Slice / Integration):** Markdown linter / CI check verifies document formatting, valid headers, and working internal file links.

#### Actionable Sub-tasks

- [ ] Author `docs/availability-guide.md` with UI workflow walkthrough.
- [ ] Document Caracal API request/response contracts for Epic 3.
- [ ] Add testing instructions and troubleshooting tips for developers.