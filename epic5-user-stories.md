# Epic 5: Configuration Review, Validation, and Publishing

## Story 5.1: View Pending Configuration Changes

As a **NY Waterway Operator**, I want to **review pending ticket configuration changes alongside the currently published configurations**, so that **I can identify and verify proposed modifications before they are published to ticket vending machines.**

### Actors/Personas

Authenticated Operator (`ROLE_OPERATOR`), Administrator (`ROLE_ADMIN`).

### Preconditions & Triggers

- The operator is logged into the application.
- The current NYW_SANDBOX configuration is available through the provided backend API.
- A reliable snapshot of the previously published configuration is available for comparison.
- The operator navigates to the Configuration Review page.

### Actions/Postconditions

The application retrieves the current sandbox configuration and compares it against the previously published configuration. Pending changes to ticket prices and availability are displayed alongside their published values, with differences visually highlighted.

The operator can review these changes before publication. If no differences exist, the application displays an appropriate message.

### Acceptance Criteria

1. Only authorized users can access the Configuration Review page.
2. The application displays the current sandbox configuration alongside the previously published configuration.
3. Each change includes the affected route, ticket type, current value, and proposed value.
4. Differences in ticket prices and availability are clearly highlighted.
5. If no differences exist, the application displays a message indicating that there are no pending changes.
6. If configuration data cannot be retrieved, the application displays an appropriate error message.

### Test Scenarios

- Manual (Success): Log in as an operator and modify an adult ticket price in NYW_SANDBOX from $13.25 to $14.00. Open the Configuration Review page and verify that the previously published price and the modified sandbox price are displayed side by side, with the change highlighted

- Manual (No Changes): Open the Configuration Review page when the sandbox configuration matches the published baseline. Verify that the application displays a message indicating that no changes are pending.

- Manual (Failure): Simulate an unavailable backend API or missing published configuration snapshot. Verify that the application displays an appropriate error message.

- Manual (Unauthorized Access): Attempt to access the Configuration Review page without logging in. Verify that access is denied or the user is redirected to the login page.

### Actionable Sub-tasks

- [ ] Create the Configuration Review page using HTML and CSS.
- [ ] Retrieve the currently published configuration values from the existing backend.
- [ ] Retrieve proposed configuration values from the application's draft state.
- [ ] Implement JavaScript logic to compare published and proposed configurations.
- [ ] Display differences in ticket prices and availability with visual highlighting.
- [ ] Handle cases where no changes exist or configuration data cannot be retrieved.
- [ ] Test successful comparisons, empty states, retrieval failures, and unauthorized access.

## Story 5.2: Validate Configuration Discrepancies

As a **NY Waterway administrator**, I want to **validate the sandbox ticket configurations against the provided reference dataset**, so that I can **identify incorrect settings and ensure all known discrepancies are resolved before publication.**

### Actors / Personas

Administrator (ROLE_ADMIN), Authenticated Operator (ROLE_OPERATOR).

### Preconditions & Triggers

- The operator is logged into the application.
- The NYW_SANDBOX configuration is available through the provided backend API.
- The reference dataset (nyw-sheet-2026-09-25.json) is available.
- The operator initiates validation from the Configuration Validation page.

### Actions/Postconditions

The application retrieves the current sandbox configuration through the backend API and compares it against the expected values in the reference dataset. It identifies discrepancies in ticket prices, availability, and monthly pass sale windows.

The application displays the detected discrepancies, their details, and the total number of remaining issues. Operators can use the appropriate configuration editing features to correct the issues and rerun validation before publication.

### Acceptance Criteria

1. Only authorized users can access and initiate configuration validation.
2. The application retrieves the current sandbox configuration using the provided backend API.
3. The application compares the sandbox configuration against the expected values in the reference dataset.
4. Validation identifies discrepancies in ticket prices, ticket availability, and monthly pass sale windows.
5. Each discrepancy displays the affected route or terminal, ticket type, expected value, and actual value, where applicable.
6. The application displays the total number of remaining discrepancies.
7. Operators can rerun validation after configuration changes to view updated results.
8. When all known discrepancies are resolved, the application displays a message indicating that no differences remain.
9. If validation fails due to missing data or an API error, the application displays an appropriate error message.

### Test Scenarios

- Manual (Success): Run validation against the initial NYW_SANDBOX configuration. Verify that the application identifies the 50 known discrepancies: 30 incorrect prices, 15 tickets incorrectly available, 4 missing tickets, and 1 incorrect monthly sale window.

- Manual (Resolved Discrepancies): Correct one or more discrepancies using the configuration editing features and rerun validation. Verify that the updated results reflect the corrections.

Manual (No Discrepancies): Run validation after all known discrepancies have been corrected. Verify that the application displays a message indicating that no differences remain.

Manual (Failure): Simulate an unavailable backend API or missing reference dataset. Verify that the application displays an error message instead of incomplete validation results.

Manual (Unauthorized Access): Attempt to access the Configuration Validation page without logging in. Verify that access is denied or the user is redirected to the login page.

### Actionable Sub-taks

- [ ] Create a Configuration Validation page using HTML and CSS.
- [ ] Retrieve sandbox ticket prices and availability using the provided backend APIs (price-simulate and availability-simulate).
- [ ] Implement JavaScript logic to compare sandbox configurations against nyw-sheet-2026-09-25.json, using check-against-sheet.mjs as a reference.
- [ ] Display detected discrepancies with their expected and actual values.
- [ ] Display the total number of remaining discrepancies.
- [ ] Allow operators to rerun validation after configuration changes.
- [ ] Handle API failures, missing data, and configurations with no discrepancies.
- [ ] Restrict validation access to authorized users.
- [ ] Test initial, partially corrected, fully corrected, and failure scenarios.

## Story 5.3: Publish Verified Configurations

As **a NY Waterway administrator**, I want to **publish reviewed and validated ticket configurations** so that **updated ticket prices and availability settings can be made available to ticket vending machines safely and accurately.**

### Actors/Personas

Administrator (ROLE_ADMIN), Authenticated Operator (ROLE_OPERATOR) with publishing permission.

### Preconditions & Triggers

- The user is logged into the application and has permission to publish configurations.
- The current NYW_SANDBOX configuration is available through the provided backend API.
- The proposed configuration changes have been reviewed through the Configuration Review page.
- Configuration validation has completed successfully, with no remaining known discrepancies.
- The user initiates publication from the application.

### Actions / Postconditions

The application verifies that the configuration has been reviewed and successfully validated before allowing publication. The user is presented with a confirmation prompt to prevent accidental publication.

Upon confirmation, the application sends a request to the provided catalog-publish API for the NYW_SANDBOX tenant.

If publication succeeds, the application displays a success message and confirms the new publication through the backend. If publication fails, an appropriate error message is displayed without reporting a successful release.

### Accepetance Criteria

1. Only authenticated users with publishing permission can initiate publication.
2. Publication is blocked if the configuration has not been reviewed or has unresolved validation discrepancies.
3. The application requires explicit user confirmation before publishing.
4. Upon confirmation, the application sends a POST request to catalog-publish with tenantCode set to NYW_SANDBOX.
5. The application displays a success message only after the backend confirms successful publication.
6. The application allows the user to verify the publication through publication-list.
7. If the publication request fails, the application displays an appropriate error message.
8. The application prevents duplicate publication requests while a publication is in progress.

### Test Scenarios

- Manual (Success): Log in as an authorized administrator, review the proposed configuration, and complete validation with zero remaining discrepancies. Confirm publication and verify that the catalog-publish request succeeds and the new publication appears in publication-list.

- Manual (Validation Failure): Attempt to publish while known discrepancies remain unresolved. Verify that publication is blocked and the user is informed that validation must be completed.

- Manual (Cancellation): Initiate publication but cancel the confirmation prompt. Verify that no publication request is sent.

- Manual (API Failure): Simulate an unavailable backend or failed publication request. Verify that an error message is displayed and publication is not reported as successful.

- Manual (Duplicate Request): Click the Publish button multiple times while a request is processing. Verify that only one publication request is submitted.

- Manual (Unauthorized Access): Attempt to publish without logging in or without publishing permission. Verify that publication is denied.

### Actionable Sub-tasks

- [ ] Create a publication interface with a Publish Configuration button using HTML and CSS.
- [ ] Integrate the review and validation results from Stories 5.1 and 5.2.
- [ ] Implement JavaScript checks to prevent publication when required review or validation is incomplete.
- [ ] Add a confirmation prompt before publishing.
- [ ] Implement the catalog-publish API request for NYW_SANDBOX.
- [ ] Retrieve publication records using publication-list to verify successful publication.
- [ ] Display appropriate success and error messages.
- [ ] Prevent duplicate publication requests while processing.
- [ ] Restrict publication functionality to authorized users.
- [ ] Test successful publication, unresolved discrepancies, cancellation, API failures, duplicate requests, and unauthorized access.

## Story 5.4: View Configuration Change and Publication History

As a **NY Waterway Operator**, I want to **view the history of ticket configuration changes and publications, so that I can track who made changes, when they were made, and maintain accountability for configuration updates.**

### Actors/Personas

Administrator (ROLE_ADMIN), Authenticated Operator (ROLE_OPERATOR) with permission to view configuration history.

### Preconditions & Triggers

- The user is logged into the application.
- The NYW_SANDBOX environment is accessible through the provided backend API.
- Configuration changes or publications have previously occurred.
- The user navigates to the Configuration History page.

### Actions/Postconditions

The application retrieves configuration change records using the audit-log-get API and publication records using publication-list.

The application displays a history of configuration activity, including the operation performed, the responsible actor, and the timestamp. Where available, the application also displays the configuration details associated with each operation.

Users can review past configuration changes and publication records without modifying the existing configuration.

### Acceptance Criteria

1. Only authorized users can access the Configuration History page.
2. The application retrieves configuration change records from audit-log-get for the NYW_SANDBOX tenant.
3. Each audit record displays the operation performed, responsible actor, and timestamp.
4. The application displays additional change details when available in the audit record.
5. The application retrieves and displays publication records using publication-list.
6. The application displays an appropriate message when no history records are available.
7. Viewing configuration history does not modify any configuration data.

### Test Scenarios

- Manual (Success): Log in as an authorized administrator after modifying a ticket price in NYW_SANDBOX. Open the Configuration History page and verify that the corresponding audit record displays the operation, responsible actor, and timestamp.

- Manual (Publication History): Publish a verified configuration using catalog-publish. Open the Configuration History page and verify that the new publication appears in the publication history.

- Manual (No History): Open the Configuration History page for a tenant with no recorded configuration activity. Verify that an appropriate message is displayed.

- Manual (API Failure): Simulate an unavailable backend API and attempt to retrieve configuration history. Verify that the application displays an error message instead of incomplete or misleading records.

- Manual (Unauthorized Access): Attempt to access the Configuration History page without logging in. Verify that access is denied or the user is redirected to the login page.

### Actionable Sub-tasks

- [ ] Create a Configuration History page using HTML and CSS.
- [ ] Retrieve configuration change records using the audit-log-get API.
- [ ] Retrieve publication records using the publication-list API.
- [ ] Implement JavaScript logic to display audit records, including operation, actor, timestamp, and available change details.
- [ ] Display publication history with the information returned by the backend.
- [ ] Handle empty history records and API retrieval failures.
- [ ] Restrict access to authorized users.
- [ ] Test configuration history, publication history, empty states, API failures, and unauthorized access.
