## Epic 5: Configuration Review, Validation, and Publishing

### Story 5.1: View Pending Configuration Changes

- **GitHub Title:** `[Epic 5] View Pending Configuration Changes`

- **Iteration:** `3`

- **Story Points:** `3`

- **Story Description:** As a **NY Waterway Operator**, I want to **review pending ticket configuration changes alongside the currently published configurations**, so that **I can identify and verify proposed modifications before they are published to ticket vending machines**.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`).

- **Preconditions & Triggers:**
  - *Precondition:* The user is authenticated with either the `OPERATOR` or `CLIENT_SERVICES` role.
  - *Precondition:* The current `NYW_SANDBOX` configuration is accessible through the Caracal backend API.
  - *Precondition:* A reliable previously published configuration baseline is available through a supported publication endpoint or an application-maintained snapshot.
  - *Trigger:* The user navigates to the Configuration Review page (`GET /review`).

- **Actions / Postconditions:**
  - *Action:* The Spring Boot application retrieves the current sandbox configuration through the Java `CaracalApiClient`, using supported endpoints such as `price-simulate` and `availability-simulate`.
  - *Action:* The application retrieves the previously published configuration baseline through `CaracalApiClient` or an application-maintained snapshot.
  - *Action:* The Java `ValidationService` compares the published and proposed configurations to identify changes in ticket prices and availability.
  - *Action:* The application displays published and proposed values side by side, highlighting modified entries.
  - *Postcondition:* The user can review pending configuration changes without modifying or publishing any data.
  - *Postcondition:* If no differences exist, the application displays a message indicating that no changes are pending.

#### Acceptance Criteria

1. Both `OPERATOR` and `CLIENT_SERVICES` users can access the Configuration Review page. Unauthenticated users must log in before accessing it.
2. The application displays the current `NYW_SANDBOX` configuration alongside the previously published configuration.
3. Each identified change includes the affected origin terminal, destination route, ticket type, published value, and proposed value.
4. Changes in ticket prices and availability are visually highlighted, with unchanged entries clearly distinguishable.
5. If no differences exist, the application displays a message indicating that there are no pending changes.
6. If the Caracal API or published baseline cannot be retrieved, the application displays an appropriate error message rather than presenting incomplete comparison results.
7. Configuration retrieval and comparison are performed server-side through Java services. JavaScript is responsible only for frontend display and user interactions.
8. The comparison uses a reliable published baseline. The `publication-diff` endpoint is not used for direct draft-versus-published comparison because it compares the last two publications.
9. Viewing pending changes does not modify or publish configuration data.

#### UI Mockup

Example of the Configuration Review page:

    Configuration Review - NYW_SANDBOX

    Origin    Destination   Ticket Type   Published   Proposed   Status
    -------------------------------------------------------------------
    EDGEWTR   MIDTOWN       Adult         $13.25      $14.00     Changed
    EDGEWTR   MIDTOWN       Child         $7.00       $7.00      Unchanged
    MIDTOWN   EDGEWTR       Senior        Sellable    Not Sold   Changed

    [Changed entries are visually highlighted]

#### Test Scenarios

- **Manual (Success):** Log in as an `OPERATOR` and modify an adult ticket price in `NYW_SANDBOX` from $13.25 to $14.00. Open the Configuration Review page and verify that the published and proposed prices appear side by side, with the difference highlighted.
- **Manual (No Changes):** Open the Configuration Review page when the sandbox matches the published baseline. Verify that the application displays a message indicating that no changes are pending.
- **Manual (Failure / Edge Case):** Simulate an unavailable Caracal API or missing published baseline. Verify that the application displays an appropriate error message without crashing or showing misleading results.
- **Manual (Access Control):** Verify that both `OPERATOR` and `CLIENT_SERVICES` users can access the page. Attempt to access the page without authentication and verify that login is required.
- **Automated (Spring MVC Slice Test):** Use JUnit, `@WebMvcTest`, and `MockMvc` to verify that an authenticated `OPERATOR` or `CLIENT_SERVICES` request to `GET /review` returns HTTP 200 and includes the configuration comparison model attribute. Mock the comparison service with known published and proposed values. Verify that unauthenticated requests are rejected or redirected according to the Spring Security configuration.
- **Automated (JUnit Service Test):** Test `ValidationService` using known published and draft configurations. Verify that modified prices and availability statuses are identified correctly and matching values produce no pending differences.

#### Actionable Sub-tasks

- [ ] Create the Configuration Review page using HTML, CSS, and the application's frontend template system.
- [ ] Implement a Spring Boot controller endpoint for `GET /review`.
- [ ] Extend `CaracalApiClient` to retrieve current `NYW_SANDBOX` pricing and availability through `price-simulate` and `availability-simulate`.
- [ ] Retrieve the previously published configuration through a supported publication endpoint, such as `publication-payload-get` if its response contains the required data, or maintain a reliable application-side snapshot.
- [ ] Implement server-side Java comparison logic in `ValidationService` to identify differences between published and proposed configurations.
- [ ] Ensure comparisons use matching terminal, route, ticket type, and time contexts, including relevant weekday, weekend, and monthly availability conditions.
- [ ] Display published and proposed values side by side, with visual highlighting for modified entries.
- [ ] Enforce access for authenticated `OPERATOR` and `CLIENT_SERVICES` users through Spring Security.
- [ ] Handle missing baselines, empty comparisons, and backend retrieval failures.
- [ ] Implement automated JUnit and MockMvc tests for comparison results and access control.

---

### Story 5.2: Validate Configuration Discrepancies

- **GitHub Title:** `[Epic 5] Validate Configuration Discrepancies`

- **Iteration:** `3`

- **Story Points:** `3`

- **Story Description:** As a **NY Waterway Operator**, I want to **validate the sandbox ticket configurations against the provided reference dataset**, so that **I can identify incorrect settings and ensure all known discrepancies are resolved before publication**.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`).

- **Preconditions & Triggers:**
  - *Precondition:* The user is authenticated with either the `OPERATOR` or `CLIENT_SERVICES` role.
  - *Precondition:* The `NYW_SANDBOX` configuration is accessible through the Caracal backend API.
  - *Precondition:* The reference dataset (`student-kit/data/nyw-sheet-2026-09-25.json`) is available to the Spring Boot application.
  - *Trigger:* The user navigates to the Configuration Validation page and clicks the "Run Validation" button.

- **Actions / Postconditions:**
  - *Action:* The Spring Boot application retrieves current sandbox ticket prices and availability through the Java `CaracalApiClient`, using supported endpoints such as `price-simulate` and `availability-simulate`.
  - *Action:* The Java `ValidationService.validateAgainstSheet()` compares the sandbox configuration against the expected values in `nyw-sheet-2026-09-25.json`.
  - *Action:* The service identifies discrepancies in ticket prices, ticket availability, and monthly pass sale windows.
  - *Action:* The application displays the detected discrepancies, their expected and actual values, and the total number of remaining issues.
  - *Postcondition:* Users can identify incorrect configurations and use the appropriate editing features to correct them before publication.
  - *Postcondition:* Users can rerun validation after configuration changes to obtain updated results.
  - *Postcondition:* When all known discrepancies are resolved, the application displays a message indicating that no differences remain.

#### Acceptance Criteria

1. Both `OPERATOR` and `CLIENT_SERVICES` users can access and initiate configuration validation. Unauthenticated users must log in before accessing the page.
2. The application retrieves the current `NYW_SANDBOX` configuration through the Java `CaracalApiClient`, without making direct browser requests to the Caracal API.
3. The Java `ValidationService.validateAgainstSheet()` compares the sandbox configuration against the expected values in `student-kit/data/nyw-sheet-2026-09-25.json`.
4. Validation identifies discrepancies in ticket prices, ticket availability, and monthly pass sale windows.
5. Each discrepancy displays its category, affected origin terminal or destination route, ticket type, expected value, and actual value, where applicable.
6. The application displays the total number of remaining discrepancies and a breakdown by discrepancy category.
7. The initial seeded `NYW_SANDBOX` configuration produces the 50 known discrepancies: 30 incorrect prices, 15 tickets incorrectly available, 4 missing tickets, and 1 incorrect monthly sale window.
8. Users can rerun validation after configuration changes, and the displayed results reflect the latest sandbox configuration.
9. When all known discrepancies are resolved, the application displays a message indicating that no differences remain.
10. If the reference dataset is missing, malformed, or cannot be loaded, or if the Caracal API is unavailable, the application displays an appropriate error message rather than incomplete validation results.
11. Validation is read-only and does not modify or publish configuration data.
12. Configuration comparison and validation logic are implemented server-side in Java. JavaScript is used only for frontend interactions and displaying results.

#### Test Scenarios

- **Manual (Success):** Log in as an `OPERATOR`, open the Configuration Validation page, and click "Run Validation" against a freshly seeded `NYW_SANDBOX` configuration. Verify that the application reports 50 discrepancies: 30 incorrect prices, 15 tickets incorrectly available, 4 missing tickets, and 1 incorrect monthly sale window.

- **Manual (Partially Resolved):** Correct one or more known discrepancies using the configuration editing features, then rerun validation. Verify that the updated results reflect the corrected values and that the remaining discrepancy count decreases appropriately.

- **Manual (No Discrepancies):** Run validation after all known discrepancies have been corrected. Verify that the application displays a message indicating that no differences remain.

- **Manual (Failure / Edge Case):** Simulate an unavailable Caracal API, missing reference JSON file, or malformed reference data. Verify that the application displays an appropriate error message without crashing or presenting incomplete results as valid.

- **Manual (Access Control):** Verify that both `OPERATOR` and `CLIENT_SERVICES` users can initiate validation. Attempt to access the page without authentication and verify that login is required.

- **Automated (JUnit Service Test):** Use JUnit to test `ValidationService.validateAgainstSheet()` with a known reference dataset and mocked Caracal responses. Verify that an incorrect ticket price and an incorrectly available ticket are reported with the correct category, expected value, and actual value. Verify that matching values produce no discrepancies.

- **Automated (Spring MVC Slice Test):** Use `@WebMvcTest` and `MockMvc` to verify that authenticated `OPERATOR` and `CLIENT_SERVICES` users can access the validation page and initiate validation successfully. Verify that unauthenticated requests are rejected or redirected according to the Spring Security configuration.

#### Actionable Sub-tasks

- [ ] Create the Configuration Validation page using HTML, CSS, and the application's frontend template system.
- [ ] Implement Spring Boot controller endpoints to display the validation page and initiate validation.
- [ ] Extend `CaracalApiClient` to retrieve sandbox ticket prices and availability using `price-simulate` and `availability-simulate`.
- [ ] Load and parse `student-kit/data/nyw-sheet-2026-09-25.json` in the Spring Boot application.
- [ ] Implement `ValidationService.validateAgainstSheet()` in Java to compare the sandbox configuration against the reference dataset.
- [ ] Use `student-kit/tools/check-against-sheet.mjs` as a reference for the expected comparison behavior, including weekday/weekend ticket handling and monthly sale windows.
- [ ] Implement detection of pricing, availability, missing-ticket, and monthly sale-window discrepancies.
- [ ] Display detected discrepancies with their category, affected terminal or route, ticket type, expected value, and actual value.
- [ ] Display the total number of remaining discrepancies and counts for each category.
- [ ] Add a "Run Validation" button that allows users to rerun validation after configuration changes.
- [ ] Enforce access for authenticated `OPERATOR` and `CLIENT_SERVICES` users through Spring Security.
- [ ] Handle API failures, missing or malformed reference data, and configurations with no discrepancies.
- [ ] Implement automated JUnit tests for `ValidationService` and MockMvc tests for controller behavior and access control.
- [ ] Verify the initial 50 discrepancies and confirm that validation reports zero differences after all known issues are corrected.

---

### Story 5.3: Publish Verified Configurations

- **GitHub Title:** `[Epic 5] Publish Verified Configurations`

- **Iteration:** `3`

- **Story Points:** `2`

- **Story Description:** As a **Caracal Administrator**, I want to **publish reviewed and validated NY Waterway ticket configurations**, so that **updated ticket prices and availability settings can be safely released for use by ticket vending machines**.

- **Actors / Personas:** Caracal Administrator (`CLIENT_SERVICES`), Authenticated Operator (`OPERATOR`) with view-only access.

- **Preconditions & Triggers:**
  - *Precondition:* The user is authenticated with the `CLIENT_SERVICES` role to publish configurations.
  - *Precondition:* The current `NYW_SANDBOX` configuration is accessible through the Caracal backend API.
  - *Precondition:* The proposed configuration changes have been reviewed through the Configuration Review page from Story 5.1.
  - *Precondition:* Configuration validation from Story 5.2 has completed successfully with zero remaining discrepancies, and the validated configuration has not changed since validation.
  - *Trigger:* An authorized administrator clicks the "Publish Configuration" button and confirms the publication.

- **Actions / Postconditions:**
  - *Action:* The Spring Boot application verifies that the current configuration has been reviewed and successfully validated before allowing publication.
  - *Action:* The application presents a confirmation prompt describing the configuration being published.
  - *Action:* After confirmation, the Spring Boot backend sends a POST request to `/functions/catalog-publish` through the Java `CaracalApiClient`, with `tenantCode` set to `NYW_SANDBOX` and the required `X-Actor` header.
  - *Action:* The application retrieves publication records using `/functions/publication-list` to verify the new publication.
  - *Postcondition:* If publication succeeds and the new record is verified, the application displays a success message with the available publication details.
  - *Postcondition:* If publication fails or its result cannot be verified, the application displays an appropriate error or unverified-status message without falsely reporting a successful release.
  - *Postcondition:* The published configuration is recorded in the backend publication history.

#### Acceptance Criteria

1. Only authenticated users with the `CLIENT_SERVICES` role can initiate publication. `OPERATOR` users can view the publication interface but cannot publish.
2. The Publish Configuration button is disabled for `OPERATOR` users. Direct publication requests from an `OPERATOR` must return HTTP 403.
3. Publication is blocked unless the proposed configuration has been reviewed and validation has completed successfully with zero remaining discrepancies.
4. The application ensures that validation still applies to the current sandbox configuration before publishing, preventing publication of changes introduced after validation.
5. The application requires explicit confirmation before submitting a publication request. Cancelling the confirmation does not trigger publication.
6. Upon confirmation, the Spring Boot backend sends a POST request to `/functions/catalog-publish` through `CaracalApiClient` with the request body `{"tenantCode":"NYW_SANDBOX"}` and an appropriate `X-Actor` header.
7. The application checks `/functions/publication-list?tenant=NYW_SANDBOX` to verify that a new publication record exists.
8. A publication is reported as successful only when the backend confirms the operation and the expected publication record is verified.
9. If the publication request fails, the application displays an appropriate error message without reporting success.
10. The application prevents duplicate publication requests while a publication is in progress.
11. Publishing authorization, review requirements, and validation checks are enforced server-side through Spring Boot. JavaScript handles the confirmation prompt and frontend button state.
12. Publication requests are associated with the initiating actor through the `X-Actor` header, supporting backend traceability.

#### Test Scenarios

- **Manual (Success):** Log in as a `CLIENT_SERVICES` administrator, review the proposed configuration, and complete validation with zero remaining discrepancies. Click "Publish Configuration", confirm the action, and verify that publication succeeds and a new record appears in the publication history.

- **Manual (Validation Failure):** Attempt to publish while unresolved configuration discrepancies remain. Verify that the application blocks publication and displays a message requiring successful validation.

- **Manual (Configuration Changed):** Complete validation successfully, then modify the sandbox configuration before publishing. Verify that publication is blocked until the current configuration is validated again.

- **Manual (Cancellation):** Click "Publish Configuration" but cancel the confirmation prompt. Verify that no publication request is submitted.

- **Manual (Failure / Edge Case):** Simulate an unavailable Caracal API, a failed publication request, or an inability to verify the publication record. Verify that the application displays an appropriate error or unverified-status message without falsely reporting success.

- **Manual (Duplicate Request):** Click the Publish Configuration button multiple times while publication is processing. Verify that only one request is submitted.

- **Manual (Access Control):** Log in as an `OPERATOR` and verify that the Publish Configuration button is disabled. Attempt to submit a direct publication request and verify HTTP 403. Verify that unauthenticated users cannot publish.

- **Automated (WireMock Integration Test):** Use JUnit and WireMock to mock the Caracal backend. Verify that an authorized publication request sends `POST /functions/catalog-publish` with `{"tenantCode":"NYW_SANDBOX"}` and the required `X-Actor` header. Mock a successful publication response and a matching record from `GET /functions/publication-list?tenant=NYW_SANDBOX`, then verify that the application's publication endpoint returns HTTP 200.

- **Automated (Spring Security / MockMvc Test):** Use `MockMvc` to verify that `CLIENT_SERVICES` can initiate publication after review and validation, while `OPERATOR` receives HTTP 403. Verify that failed validation prevents the publishing service from being called.

#### Actionable Sub-tasks

- [ ] Create a publication interface with a Publish Configuration button using HTML, CSS, and the application's frontend template system.
- [ ] Implement a Spring Boot controller endpoint for initiating publication.
- [ ] Integrate review and validation results from Stories 5.1 and 5.2 into the publication workflow.
- [ ] Implement server-side checks to ensure that the current configuration has been reviewed and validated with zero remaining discrepancies.
- [ ] Ensure publication is blocked if the sandbox configuration changes after successful validation, requiring validation of the updated configuration.
- [ ] Add a JavaScript confirmation prompt before submitting a publication request.
- [ ] Extend `CaracalApiClient` to send `POST /functions/catalog-publish` with the correct tenant code and `X-Actor` header.
- [ ] Retrieve publication records using `GET /functions/publication-list?tenant=NYW_SANDBOX` to verify the publication result.
- [ ] Display publication success, failure, and unverified-status messages appropriately.
- [ ] Disable the Publish Configuration button while a request is processing and prevent duplicate submissions server-side.
- [ ] Enforce `CLIENT_SERVICES` publishing authorization through Spring Security and return HTTP 403 for unauthorized `OPERATOR` requests.
- [ ] Implement automated JUnit, WireMock, and MockMvc tests for publication requests, validation requirements, and access control.

---

### Story 5.4: View Configuration Change and Publication History

- **GitHub Title:** `[Epic 5] View Configuration Change and Publication History`

- **Iteration:** `3`

- **Story Points:** `2`

- **Story Description:** As a **NY Waterway Operator**, I want to **view the history of ticket configuration changes and publications**, so that **I can track who made changes, when they were made, and maintain accountability for configuration updates**.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`).

- **Preconditions & Triggers:**
  - *Precondition:* The user is authenticated with either the `OPERATOR` or `CLIENT_SERVICES` role.
  - *Precondition:* The `NYW_SANDBOX` environment is accessible through the Caracal backend API.
  - *Trigger:* The user navigates to the Configuration History page (`GET /history`).

- **Actions / Postconditions:**
  - *Action:* The Spring Boot application retrieves configuration change records using `/functions/audit-log-get` through the Java `CaracalApiClient`.
  - *Action:* The application retrieves publication records using `/functions/publication-list` through `CaracalApiClient`.
  - *Action:* The application displays configuration activity, including the operation performed, responsible actor, timestamp, and additional details where available.
  - *Action:* The application displays publication history using the information returned by the backend.
  - *Postcondition:* Users can review previous configuration changes and publication records without modifying existing configuration data.
  - *Postcondition:* If no history records are available, the application displays an appropriate empty-state message.

#### Acceptance Criteria

1. Both `OPERATOR` and `CLIENT_SERVICES` users can access the Configuration History page. Unauthenticated users must log in before accessing it.
2. The application retrieves configuration change records for `NYW_SANDBOX` through `/functions/audit-log-get` using the Java `CaracalApiClient`.
3. Each audit record displays the operation performed, responsible actor, and timestamp, where those fields are provided by the backend.
4. The application displays additional configuration change details when available in the audit record.
5. The application retrieves and displays publication records through `/functions/publication-list?tenant=NYW_SANDBOX`.
6. Publication records display available information such as publication identifier, timestamp, and associated actor when provided by the backend.
7. Configuration changes and publications are clearly distinguishable in the history interface.
8. If no audit or publication records exist, the application displays a message indicating that no history is available.
9. If the Caracal API is unavailable or a history request fails, the application displays an appropriate error message rather than misleading or incomplete results.
10. Configuration history retrieval is performed server-side through Java services. JavaScript is responsible only for frontend display and interactions.
11. Viewing configuration history is read-only and does not modify or publish configuration data.

#### Test Scenarios

- **Manual (Success):** Log in as an `OPERATOR` or `CLIENT_SERVICES` user after a ticket price has been modified in `NYW_SANDBOX`. Open the Configuration History page and verify that the corresponding audit record displays the operation, responsible actor, timestamp, and available change details.

- **Manual (Publication History):** Publish a verified configuration through `catalog-publish`. Open the Configuration History page and verify that the new publication appears in the publication history with the available publication details.

- **Manual (No History):** Open the Configuration History page when the backend returns empty audit and publication record collections. Verify that the application displays a message indicating that no history is available.

- **Manual (Failure / Edge Case):** Simulate an unavailable Caracal API or a failed history retrieval request. Verify that the application displays an appropriate error message without crashing or presenting incomplete results as complete history.

- **Manual (Access Control):** Verify that both `OPERATOR` and `CLIENT_SERVICES` users can access the Configuration History page. Attempt to access the page without authentication and verify that login is required.

- **Automated (Spring MVC Slice Test):** Use JUnit, `@WebMvcTest`, and `MockMvc` to verify that authenticated `OPERATOR` and `CLIENT_SERVICES` requests to `GET /history` return HTTP 200 and include the expected audit and publication history model attributes. Mock the Java history service to return sample records originating from `audit-log-get` and `publication-list`. Verify that unauthenticated requests are redirected or rejected according to the Spring Security configuration.

- **Automated (JUnit Service Test):** Test the Java history service using mocked `CaracalApiClient` responses. Verify that audit and publication records are mapped correctly, empty responses produce an empty history state, and backend failures are handled appropriately.

#### Actionable Sub-tasks

- [ ] Create the Configuration History page using HTML, CSS, and the application's frontend template system.
- [ ] Implement a Spring Boot controller endpoint for `GET /history`.
- [ ] Extend `CaracalApiClient` to retrieve configuration change records through `/functions/audit-log-get`.
- [ ] Extend `CaracalApiClient` to retrieve publication records through `/functions/publication-list?tenant=NYW_SANDBOX`.
- [ ] Implement a Java history service to retrieve and organize audit and publication records for display.
- [ ] Display audit records with operation, responsible actor, timestamp, and available configuration change details.
- [ ] Display publication records with available publication identifiers, timestamps, and actor information.
- [ ] Clearly distinguish configuration changes from publication events in the history interface.
- [ ] Handle empty audit and publication records with an appropriate message.
- [ ] Handle Caracal API failures and missing optional record fields without crashing or displaying misleading results.
- [ ] Enforce access for authenticated `OPERATOR` and `CLIENT_SERVICES` users through Spring Security.
- [ ] Implement automated JUnit and MockMvc tests for history retrieval, empty states, backend failures, and access control.

