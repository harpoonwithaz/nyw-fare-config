## Epic 2: Interactive Origin-Destination Rate Grid & Fare Management

This document defines the formal user stories and sub-stories for Epic 2 of the NY Waterway Fare Configuration Tool. Each story adheres strictly to the CMPT 276 agile project specification, INVEST criteria, and standardized persona definitions.

---

### Story 2.1: Unified Tenant Context Switching & Dual-Matrix View Scaffolding

- **GitHub Issue Title:** `[Rate Grid] Unified Tenant Context Switching & Dual-Matrix View Scaffolding`

- **Iteration:** `1`

- **Story Points:** `3`

- **Story Description:** As a Caracal administrator, I want the matrix grid to automatically adapt its axes and data bindings based on the selected tenant context, so that I can manage point-to-point ferry routes (NY Waterway) or zone-based rail networks (Metra) within a single unified application.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* Operator is authenticated and navigates to the Rate Management view (`/fares`).
  - *Trigger:* The user selects a tenant from the global context dropdown (e.g., `NYW_SANDBOX` or `METRA`).

- **Actions / Postconditions:** 
  - *Action:* The user toggles between tenant contexts in the navigation bar.
  - *Postcondition:* Selecting `NYW_SANDBOX` binds the view to **Point-to-Point (O-D) Mode** (origin terminal selector, outbound destination rows, ticket product columns). Selecting `METRA` binds the view to **Zonal Matrix Mode** (fare product selector, origin zone rows, destination zone columns). Cells display formatted rates (`$XX.XX`) or distinct badges (`NOT_SOLD`, `N/A`).

#### Acceptance Criteria

1. Switching the tenant dropdown dynamically re-renders the grid structure to the corresponding axis mode without a full-page error reload.
2. In O-D Mode, changing the origin terminal dynamically updates outbound destination rows and fetches current route prices.
3. In Zonal Mode, changing the active ticket product dynamically updates the zone-to-zone fare intersections.
4. Every cell without an active price rule resolves to a muted `NOT_SOLD` or `N/A` badge; no cell renders as blank, `$0.00`, or `NaN`.

#### Test Scenarios

- **Manual (Success):** Select `NYW_SANDBOX` $\to$ select `EDGEWTR` terminal. Verify rows show destinations (`MIDTOWN`, `BROOKFD`) and columns show products (`ADULT_ONEWAY`). Switch tenant to `METRA` $\to$ select `ADULT_ONEWAY`. Verify the table renders a symmetric Zone $\times$ Zone grid with rates formatted as `$XX.XX`.
- **Manual (Failure / Edge Case):** Select a terminal with no outbound routes or an unconfigured zone pair. Verify the cell displays a gray `NOT_SOLD` badge rather than breaking table formatting or rendering blank.
- **Automated (Unit / Slice / Integration):** Execute a `MockMvc` `GET /fares?tenant=NYW_SANDBOX&origin=EDGEWTR` asserting HTTP 200, verifying model attribute `matrixMode=OD` and table cells in the Thymeleaf response; execute `GET /fares?tenant=METRA&product=ADULT_ONEWAY` asserting HTTP 200 and `matrixMode=ZONAL`.

#### Actionable Sub-tasks

- [ ] Implement `TenantContext` model and register tenant session resolver in Spring Boot.
- [ ] Create Thymeleaf layout with responsive context toolbar (Tenant dropdown, Mode indicator, Terminal/Product secondary selector).
- [ ] Build dynamic matrix table component in `rate-grid.html` supporting both destination and zone row/column iteration.
- [ ] Define CSS styling for price cells, `NOT_SOLD` badges, and `N/A` muted pills.
- [ ] Write controller slice tests (`@WebMvcTest`) verifying dual-mode model attributes.

---

### Story 2.2: O-D Mode Direct Inline Rate Editing & Numeric Input Masking

- **GitHub Issue Title:** `[Rate Grid] O-D Mode Direct Inline Rate Editing & Numeric Input Masking`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As an authenticated operator, I want to click directly into a price cell in the O-D grid to edit the rate inline with real-time numeric validation, so that I can update point-to-point fares quickly and prevent malformed or negative amounts from being entered.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* The application is in O-D Mode (`NYW_SANDBOX`), displaying an active terminal rate table.
  - *Trigger:* The user clicks on any active price cell.

- **Actions / Postconditions:** 
  - *Action:* The static currency text converts into a focused numeric input field (`<input type="number">`). Keystrokes are sanitized in real time.
  - *Postcondition:* Pressing Enter or blurring the input commits the value to local draft state, while pressing Escape cancels the edit.

#### Acceptance Criteria

1. Clicking a price cell converts the cell text into an active input pre-populated with the unformatted numeric value.
2. Input masking restricts entries strictly to positive numbers with a maximum of two decimal places (`min="0.00"`, `step="0.01"`).
3. Negative signs, alphabetic letters, and third decimal points are blocked on keystroke.
4. Entering a blank value or invalid string applies the `.is-invalid` visual class, disables the save trigger, and alerts the user.
5. Cells flagged as `NOT_SOLD` cannot be edited directly via inline text without explicitly toggling their sold state.

#### Test Scenarios

- **Manual (Success):** Click `EDGEWTR -> MIDTOWN` Adult fare (`$13.25`), change to `14.00`, and press Enter. Verify the cell reflects `$14.00`.
- **Manual (Failure / Edge Case):** Click a price cell, attempt to type `-12.50`, `abc`, or `13.999`. Verify that invalid characters are blocked on keystroke and the input highlights in red if forced.
- **Automated (Unit / Slice / Integration):** Jest/frontend unit test verifying cell input event handlers sanitize raw strings and reject values failing `/^\d+(\.\d{1,2})?$/`.

#### Actionable Sub-tasks

- [ ] Implement client-side inline edit toggle (`click` converts span to input, `blur`/`Enter` converts back to span).
- [ ] Add vanilla JS input masking to reject negative numbers, letters, and excessive decimal places.
- [ ] Add `.is-invalid` error styling and accessible ARIA attributes for malformed inputs.
- [ ] Lock `NOT_SOLD` and unconfigured cells from inline text entry.
- [ ] Write automated browser tests verifying keystroke validation rules.

---

### Story 2.3: Zonal Cell Action Modal for Rate Tier Management (Parent Story)

- **GitHub Issue Title:** `[Rate Grid] Zonal Cell Action Modal for Rate Tier Management`

- **Iteration:** `2`

- **Story Points:** `5` *(Split into sub-stories 2.3a and 2.3b)*

- **Story Description:** As a Caracal administrator in Zonal Mode, I want clicking a matrix cell to open a Rate Action Modal with options to update the shared tier globally, reassign the cell to an existing tier, or create a new tier, so that I can manage network-wide zonal fare structures accurately.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* The application is in Zonal Mode (`METRA`).
  - *Trigger:* The user clicks on any zone-to-zone cell in the grid.

- **Actions / Postconditions:** 
  - *Action:* The operator selects one of three rate management actions or toggles availability.
  - *Postcondition:* The modal closes and stages the selected rate adjustment into local draft state with metadata indicating the update scope.

#### Acceptance Criteria

1. The modal displays three distinct action options: Global Rate Tier Adjustment (Option 1), Reassign to Existing Tier (Option 2), and Create & Assign New Tier (Option 3).
2. Option 1 explicitly warns the user how many total network zone pairs share this tier and will be changed.
3. Option 2 provides a dropdown of existing active system tiers.
4. Option 3 validates currency input strictly (`> 0.00`, two decimal places).
5. The modal includes a toggle to switch the cell between active pricing and `NOT_SOLD`.

#### Test Scenarios

- **Manual (Success):** Click a `$4.90` cell $\to$ select Option 1 $\to$ enter `$5.00` $\to$ apply. Verify all cells sharing that tier in the matrix reflect `$5.00` and display draft highlights.
- **Manual (Failure / Edge Case):** Select Option 3 $\to$ leave the new rate field blank or enter a negative value (`-3.00`). Verify validation prevents applying the change and displays an inline error alert.
- **Automated (Unit / Slice / Integration):** Trigger modal submission via JavaScript test fixture; verify that the emitted change event payload contains the correct `actionType` (`GLOBAL_UPDATE`, `REASSIGN_TIER`, or `NEW_TIER`) and target rate.

#### Actionable Sub-tasks

- [ ] Construct Bootstrap 5 modal markup for the Zonal Rate Action dialog.
- [ ] Implement dynamic modal population showing current pair (`originZone`, `destZone`) and linked tier details.
- [ ] Implement Option 1 form controls with dynamic warning text showing count of affected pairs.
- [ ] Implement Option 2 dropdown populated with active rate tier entities.
- [ ] Implement Option 3 form controls with currency validation for new tier creation.
- [ ] Wire modal "Apply" button to update grid cell display and dispatch draft state events.

---

### Story 2.3a: Zonal Action Modal UI & Tier Selection Dropdown

- **GitHub Issue Title:** `[Rate Grid] Zonal Action Modal UI & Tier Selection Dropdown`

- **Iteration:** `2`

- **Story Points:** `2`

- **Story Description:** As an authenticated operator in Zonal Mode, I want clicking a matrix cell to open an action modal displaying the active zone pair and a dropdown of existing rate tiers, so that I can reassign that route to a different existing price tier.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* The application is in Zonal Mode (`METRA`).
  - *Trigger:* The operator clicks any zone-to-zone cell in the grid.

- **Actions / Postconditions:** 
  - *Action:* A modal opens showing origin and destination zone labels alongside current pricing. The user selects an existing tier from the dropdown (Option 2) and clicks "Apply".
  - *Postcondition:* The clicked cell updates its staged rate in local draft state, and the modal closes without reloading the page.

#### Acceptance Criteria

1. Clicking any active zonal cell opens the modal without a page reload.
2. Modal displays the origin zone, destination zone, and active fare.
3. Dropdown lists all active system rate tiers with their dollar values.
4. Selecting a tier and clicking "Apply" updates only the clicked cell's staged rate.
5. Includes an availability toggle to flag the cell as `NOT_SOLD`.

#### Test Scenarios

- **Manual (Success):** Click a `$5.90` cell $\to$ select `$7.40` tier from dropdown $\to$ apply. Verify only that single cell updates to `$7.40` in the grid.
- **Manual (Failure / Edge Case):** Open the Rate Action Modal for an active zone pair, leave the tier selection on the unselected placeholder (or choose an empty/invalid value), and click "Apply". Verify an inline error message ("Please select a valid rate tier") appears, the modal remains open, and the cell value in the matrix does not change.
- **Automated (Unit / Slice / Integration):** JavaScript unit test verifying the modal open event populates tier options and emits a `REASSIGN_TIER` event on success; assert submitting with a null/empty `tierId` blocks submission, prevents event dispatch, and applies `.is-invalid` to the dropdown.

#### Actionable Sub-tasks

- [ ] Build modal shell and backdrop in `rate-grid.html`.
- [ ] Wire cell click listener to pass selected `originZone` and `destZone` to modal view state.
- [ ] Implement Option 2 select element populated with system rate tiers.
- [ ] Add availability switch for toggling `NOT_SOLD`.
- [ ] Write unit test for modal form submission validation.

---

### Story 2.3b: Global Tier Adjustment & New Tier Creation Logic

- **GitHub Issue Title:** `[Rate Grid] Global Tier Adjustment & New Tier Creation Logic`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As a Caracal administrator, I want the zonal rate modal to support modifying a tier's base price globally or creating a brand new tier, so that I can adjust shared network rates or define custom fare levels.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* The Rate Action Modal is open for a selected cell.
  - *Trigger:* The user selects either Option 1 (Global Rate Update) or Option 3 (Create New Tier).

- **Actions / Postconditions:** 
  - *Action:* The user modifies a tier's base value or inputs a custom price for a new tier.
  - *Postcondition:* Choosing Option 1 updates the shared tier price, flagging all linked network cells as modified. Choosing Option 3 registers a new tier and links the active cell to it.

#### Acceptance Criteria

1. Option 1 displays an impact warning stating the total count of network zone pairs sharing that tier.
2. Applying Option 1 updates all matrix cells associated with that tier in draft state.
3. Option 3 validates currency input strictly (`> 0.00`, two decimal places).
4. Option 3 creates the new tier and assigns it exclusively to the active cell.

#### Test Scenarios

- **Manual (Success):** Select Option 1 for a `$4.90` tier (12 linked pairs) $\to$ enter `$5.00` $\to$ apply. Verify all 12 cells update to `$5.00`. Select Option 3 $\to$ enter `$6.50` $\to$ verify a new tier is created and applied to the cell.
- **Manual (Failure / Edge Case):** Select Option 1 (Global Update) or Option 3 (New Tier) and input a negative number (`-$1.00`), blank text, or a third decimal place (`$5.555`). Click "Apply" and verify an inline validation error displays ("Price must be a positive number with at most two decimal places"), input border highlights red, and no matrix cells or global tier values are modified.
- **Automated (Unit / Slice / Integration):** Frontend unit test asserting `GLOBAL_UPDATE` and `NEW_TIER` payload construction on valid input; assert that negative or non-decimal inputs fail currency regex validation and do not emit change events.

#### Actionable Sub-tasks

- [ ] Add Option 1 radio button, input field, and dynamic affected-pairs calculation.
- [ ] Add Option 3 radio button and validated new tier input field.
- [ ] Wire event handlers to broadcast global vs. single-cell draft updates.
- [ ] Write unit tests verifying input regex validation for new tier creation.

---

### Story 2.4: Global Rate Tier Catalog & Bulk Adjustment Tool (Parent Story)

- **GitHub Issue Title:** `[Rate Grid] Global Rate Tier Catalog & Bulk Adjustment Tool`

- **Iteration:** `2`

- **Story Points:** `5` *(Split into sub-stories 2.4a and 2.4b)*

- **Story Description:** As a Caracal administrator, I want a dedicated catalog view of all active rate tiers with a bulk adjustment tool, so that I can execute network-wide fare changes (such as an annual 10% increase) across all tiers simultaneously.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* Operator is in Zonal Mode (`METRA`).
  - *Trigger:* The user clicks the "Manage Rate Tiers" button in the header toolbar.

- **Actions / Postconditions:** 
  - *Action:* A management panel displays a summarized table of all active rate tiers, their values, and their assignment counts. The operator applies a percentage or fixed dollar increase across all tiers.
  - *Postcondition:* The operator previews recalculations and stages bulk adjustments across all affected matrix cells.

#### Acceptance Criteria

1. Displays every active rate tier with: Tier Code/Name, Base Rate ($), and Number of Linked Station/Zone Pairs.
2. The bulk adjustment tool supports percentage increases (rounded to the nearest cent) and flat increments.
3. Clicking "Preview Bulk Adjustment" shows side-by-side Old Price vs. New Price comparisons for every tier before applying.
4. Applying updates stages modifications for all affected tiers and marks corresponding matrix cells as drafted.

#### Test Scenarios

- **Manual (Success):** Open Rate Tier Manager $\to$ input `10%` increase $\to$ click Preview. Verify a `$4.90` tier becomes `$5.39` and a `$7.40` tier becomes `$8.14`. Click "Apply to Grid" and verify the active zonal matrix reflects the updated values.
- **Manual (Failure / Edge Case):** Input a negative percentage that would drop rates below `$0.00` (e.g., `-150%`) or non-numeric input. Verify validation blocks the calculation and displays an inline error alert ("Adjustment must result in fares greater than $0.00").
- **Automated (Unit / Slice / Integration):** JUnit service test for `RateTierService.calculateBulkAdjustment(List<RateTier> tiers, AdjustmentType type, double value)` asserting correct mathematical half-up rounding and boundary checks; assert `IllegalArgumentException` on negative-resulting rates.

#### Actionable Sub-tasks

- [ ] Create `RateTier` DTO and in-memory/database catalog registry service.
- [ ] Build "Manage Rate Tiers" modal / view with tier summary table.
- [ ] Implement bulk calculation engine in JavaScript/Java supporting percentage and flat addition with half-up cent rounding.
- [ ] Add side-by-side preview diff table inside the tier manager dialog.
- [ ] Write unit tests verifying precision rounding and negative-rate prevention.

---

### Story 2.4a: Rate Tier Catalog View & Summary Table

- **GitHub Issue Title:** `[Rate Grid] Rate Tier Catalog View & Summary Table`

- **Iteration:** `2`

- **Story Points:** `2`

- **Story Description:** As a Caracal administrator, I want a dedicated catalog view displaying all active rate tiers and their usage counts, so that I have complete visibility over network fare groupings.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* Operator is in Zonal Mode (`METRA`).
  - *Trigger:* The user clicks the "Manage Rate Tiers" button in the toolbar.

- **Actions / Postconditions:** 
  - *Action:* The user opens the tier catalog view.
  - *Postcondition:* A management dialog displays a consolidated table of all active rate tiers, their base currency values, and the total count of mapped zone pairs.

#### Acceptance Criteria

1. Table lists Tier Code, Base Fare ($), and Number of Linked Station/Zone Pairs.
2. Unassigned or empty tiers are clearly labeled.
3. Table dynamically reflects in-memory draft changes made during the active session.
4. If no tiers exist or if service retrieval fails, a clear fallback notification is rendered.

#### Test Scenarios

- **Manual (Success):** Click "Manage Rate Tiers". Verify the table matches active system tiers and accurately totals mapped zone pairs.
- **Manual (Failure / Edge Case):** Click "Manage Rate Tiers" when no rate tiers exist in the system or when the service encounters a backend fetch failure. Verify the modal displays a clear alert banner ("No active rate tiers found; please configure a tier" or "Unable to load rate tiers; please refresh") and disables the bulk adjustment controls rather than crashing or rendering a broken table.
- **Automated (Unit / Slice / Integration):** Spring MVC test (`@WebMvcTest`) asserting the endpoint returns HTTP 200 with active `RateTier` DTO collections; simulate service failure asserting HTTP 500 or error view with a user-friendly error message.

#### Actionable Sub-tasks

- [ ] Define `RateTierDto` class with tier code, rate value, and pair count fields.
- [ ] Create controller endpoint `GET /api/rate-tiers` returning active tier summaries.
- [ ] Build modal view with Bootstrap summary table.
- [ ] Implement empty state and error state alert templates.
- [ ] Write `@WebMvcTest` controller test for tier catalog retrieval.

---

### Story 2.4b: Bulk Percentage & Flat Fare Adjustment Engine

- **GitHub Issue Title:** `[Rate Grid] Bulk Percentage & Flat Fare Adjustment Engine`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As a Caracal administrator, I want to calculate and apply bulk percentage or flat adjustments across all rate tiers simultaneously, so that I can execute system-wide fare updates in a single action.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* The "Manage Rate Tiers" dialog is open.
  - *Trigger:* The user inputs an adjustment value and clicks "Preview Bulk Adjustment".

- **Actions / Postconditions:** 
  - *Action:* The user inputs a percentage (e.g., `+10%`) or flat increment (e.g., `+$0.25`), reviews Old vs. New comparisons, and clicks "Apply to Grid".
  - *Postcondition:* The application recalculates rates and stages draft modifications for all affected tiers across the matrix.

#### Acceptance Criteria

1. Supports percentage (`%`) and fixed dollar (`$`) calculations.
2. Applies standard financial half-up rounding to the nearest cent (`$0.01`).
3. Displays a side-by-side Old Price vs. New Price diff preview before committing.
4. Blocks calculations resulting in rates $\le 0.00$.
5. Applying recalculates and stages draft changes for all affected matrix cells.

#### Test Scenarios

- **Manual (Success):** Input `10%` increase $\to$ click Preview. Verify `$4.90` becomes `$5.39` and `$7.40` becomes `$8.14`. Click Apply and verify matrix cells update.
- **Manual (Failure / Edge Case):** In the bulk adjustment tool, enter an extreme negative percentage that drops rates to or below `$0.00` (e.g., `-120%`) or enter non-numeric text (`abc`) and click "Preview". Verify an inline alert appears ("Adjustment must result in fares greater than $0.00"), preview diffs do not calculate, and the "Apply to Grid" button remains disabled.
- **Automated (Unit / Slice / Integration):** JUnit service test verifying `calculateBulkAdjustment()` rounds cents accurately (half-up) on valid input; assert that method throws `IllegalArgumentException` when an adjustment results in fares $\le 0.00$.

#### Actionable Sub-tasks

- [ ] Implement `calculateBulkAdjustment` service logic with `BigDecimal` half-up rounding.
- [ ] Build preview diff table component showing Old Price, New Price, and Absolute Change.
- [ ] Wire "Apply to Grid" button to update client-side matrix cells and trigger draft state.
- [ ] Add client-side validation blocking invalid or non-numeric adjustment inputs.
- [ ] Write JUnit unit tests covering flat, percentage, and boundary rounding edge cases.

---

### Story 2.5: Station-to-Zone Topology Mapping Side Panel

- **GitHub Issue Title:** `[Rate Grid] Station-to-Zone Topology Mapping Side Panel`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As a Caracal administrator managing a zonal rail network, I want to view and reassign stations to zones using a slide-out drawer on the same screen as the matrix, so that I can maintain station zone assignments without navigating away from the fare table.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* User is in Zonal Mode (`METRA`).
  - *Trigger:* The user clicks the "Station Zone Mapping" button on the workspace toolbar.

- **Actions / Postconditions:** 
  - *Action:* A collapsible offcanvas drawer slides out containing a searchable list of all network stations and their assigned zone dimensions (`site-dimension-bind`).
  - *Postcondition:* The operator can reassign a station to a new zone and save the mapping without reloading or clearing the active fare matrix.

#### Acceptance Criteria

1. The drawer opens alongside the fare grid without reloading or clearing the current matrix state.
2. Displays all stations with: Station Code (`siteCode`), Station Name, and current Zone Assignment dropdown.
3. Includes a real-time search filter allowing quick lookup of stations by name or code.
4. Selecting a new zone from a station's dropdown highlights the row as modified.
5. Saving the drawer persists the station-to-zone binding changes and displays a confirmation badge.

#### Test Scenarios

- **Manual (Success):** Open the drawer, search for "Ravenswood", and change its zone from `Zone B` to `Zone A`. Click "Update Mappings". Verify the drawer reflects the new zone and shows a success toast.
- **Manual (Failure / Edge Case):** Attempt to submit an invalid station binding request via a corrupted payload (e.g., missing station code or invalid target zone identifier) or simulate a network failure when clicking "Update Mappings". Verify the server rejects the request with an HTTP 400 error banner ("Failed to update station mapping: invalid zone identifier"), and the UI displays an error alert without closing the drawer or losing entered changes.
- **Automated (Unit / Slice / Integration):** `MockMvc` POST request to `/api/topology/bind-station` with valid payload asserting HTTP 200; execute request with blank/invalid zone parameter asserting HTTP 400 Bad Request and verifying that database mappings remain unmutated.

#### Actionable Sub-tasks

- [ ] Implement Bootstrap 5 Offcanvas drawer component in `rate-grid.html`.
- [ ] Build station mapping table with instant JavaScript search/filter bar.
- [ ] Populate station rows with zone selection dropdowns.
- [ ] Add change-tracking listeners to flag modified station-zone pairs.
- [ ] Create backend controller endpoint handling station dimension bindings.

---

### Story 2.6: Visual Draft Tracking, Diff Confirmation, & Platform Batch API Dispatch (Parent Story)

- **GitHub Issue Title:** `[Rate Grid] Visual Draft Tracking, Diff Confirmation, & Platform Batch API Dispatch`

- **Iteration:** `3`

- **Story Points:** `5` *(Split into sub-stories 2.6a and 2.6b)*

- **Story Description:** As an authenticated operator, I want all unsaved rate changes visually highlighted in draft state and summarized in a confirmation diff modal before committing, so that I can review old vs. new prices and batch-dispatch valid updates to Caracal's platform API.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* The user has modified one or more cells (via inline editing, tier reassignment, or global tier adjustments).
  - *Trigger:* The user clicks "Save Rate Changes".

- **Actions / Postconditions:** 
  - *Action:* The user reviews the diff modal detailing Pair/Route, Product/Tier, Old Price, New Price, and Scope. The user clicks "Confirm & Publish".
  - *Postcondition:* The Spring Boot backend validates the payload and dispatches a batch request to `/functions/od-matrix-set` with the operator's `X-Actor` header. On success, draft styling clears from the grid.

#### Acceptance Criteria

1. Any cell modified during the active session is styled with an amber draft highlight.
2. Each drafted cell provides an inline revert trigger (or Escape key) restoring its initial value.
3. A global "Discard Changes" button resets all drafted cells back to their loaded platform values.
4. The "Save Rate Changes" confirmation modal summarizes all pending modifications with explicit old vs. new values.
5. The backend validates the payload using Spring Bean Validation (`@DecimalMin("0.00")`, `@Digits(fraction=2)`) before invoking Caracal's API.
6. Outgoing mutating requests attach the operator's identity via `X-Actor: operator:<email>` and return a success banner showing the count of rules written.

#### Test Scenarios

- **Manual (Success):** Edit two fares, verify the floating action bar reads "2 Pending Changes". Click "Save Rate Changes", confirm the diff modal lists both modifications accurately, click "Confirm & Publish". Verify an alert confirms "2 price rules updated" and cells return to normal styling.
- **Manual (Failure / Edge Case):** Submit a payload containing a negative price (`-5.00`) via developer tools or corrupted state, or simulate platform API failure (HTTP 500). Assert the Spring backend blocks negative prices with HTTP 400, or displays an error alert ("Failed to persist changes to platform") while preserving draft highlights on the grid.
- **Automated (Unit / Slice / Integration):** WireMock integration test asserting `/functions/od-matrix-set` receives the expected payload and `X-Actor` header, returning HTTP 200; simulate HTTP 500 from upstream API asserting controller catches error and preserves client draft state.

#### Actionable Sub-tasks

- [ ] Implement client-side `DraftStateManager` tracking changed cells, initial values, and change counts.
- [ ] Add amber visual draft styling (`.table-warning`, `.cell-draft`) and single-cell revert buttons.
- [ ] Build "Save Changes" review modal dynamically rendering the diff table.
- [ ] Implement Spring Boot `RateBatchUpdateDto` with strict Bean Validation constraints (`@DecimalMin`, `@Digits`, `@NotNull`).
- [ ] Implement `CaracalApiClient` method calling `/functions/od-matrix-set` with active session `X-Actor` header.
- [ ] Write slice tests (`@WebMvcTest`) and WireMock integration tests covering batch dispatch and validation failure handling.

---

### Story 2.6a: Visual Draft State Engine & Cell Revert Controls

- **GitHub Issue Title:** `[Rate Grid] Visual Draft State Engine & Cell Revert Controls`

- **Iteration:** `2`

- **Story Points:** `2`

- **Story Description:** As an authenticated operator, I want unsaved rate modifications visually highlighted on the grid with individual revert options, so that I can track pending edits and undo mistakes before saving.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* An operator edits any price cell via inline editing or modal action.
  - *Trigger:* A rate value is modified or an availability state is toggled.

- **Actions / Postconditions:** 
  - *Action:* The operator modifies cell values or clicks an inline revert button.
  - *Postcondition:* Modified cells receive an amber highlight (`.table-warning`), a toolbar badge displays the pending changes count, and the user can revert single cells or discard all edits.

#### Acceptance Criteria

1. Modified cells display an amber draft border/background class (`.table-warning`).
2. Hovering or clicking a drafted cell reveals a revert icon (or Escape key) to restore its initial value.
3. A floating status bar displays an active count of pending changes.
4. "Discard All" restores the entire grid back to loaded platform values.

#### Test Scenarios

- **Manual (Success):** Modify two cells. Verify both turn amber and the toolbar reads "2 Pending Changes". Click revert on one cell; verify it resets to its loaded platform value and the badge updates to "1 Pending Change".
- **Manual (Failure / Edge Case):** Attempt to trigger a revert action (or press Escape) on an unmodified cell that has no staged changes, or click "Discard All" when no edits are pending. Verify the cell styling remains normal, no draft events fire, and the pending changes counter remains at `0` without decrementing into negative numbers.
- **Automated (Unit / Slice / Integration):** JavaScript unit test verifying `DraftStateManager` tracks modifications, decrements on revert, and empties on discard; assert that calling `revertChange()` on an untracked cell returns `false` and does not decrement the counter below zero.

#### Actionable Sub-tasks

- [ ] Create `DraftStateManager.js` module tracking modified cell key-value pairs.
- [ ] Add CSS classes `.table-warning` and `.cell-draft` for modified table cells.
- [ ] Implement floating action bar showing pending changes count.
- [ ] Add individual revert button icon to cell templates.
- [ ] Implement "Discard All" button clearing the draft dictionary.

---

### Story 2.6b: Transaction Review Diff Modal & Batch API Dispatch

- **GitHub Issue Title:** `[Rate Grid] Transaction Review Diff Modal & Batch API Dispatch`

- **Iteration:** `3`

- **Story Points:** `3`

- **Story Description:** As an authenticated operator, I want to review an itemized diff of all staged changes before batch-submitting them, so that I can verify accuracy and persist valid updates to the platform API under my audit identity.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:** 
  - *Precondition:* One or more cells are in draft state.
  - *Trigger:* The operator clicks the "Save Rate Changes" button.

- **Actions / Postconditions:** 
  - *Action:* A confirmation diff modal lists each change (Pair, Product/Tier, Old Rate, New Rate). Clicking "Confirm & Publish" initiates batch persistence.
  - *Postcondition:* The Spring Boot backend dispatches a batch request to `/functions/od-matrix-set` with the operator's `X-Actor` header and clears the draft state.

#### Acceptance Criteria

1. Diff modal displays an itemized list of all pending changes with Old vs. New rates.
2. Backend DTO validates currency fields with `@DecimalMin("0.00")` and `@Digits(fraction=2)`.
3. Outgoing platform request carries `X-Actor: operator:<email>`.
4. On HTTP 200 from the platform, draft state clears and a success banner displays the rules written count.
5. If the platform is unreachable or returns an error, draft state is retained and an error banner is displayed.

#### Test Scenarios

- **Manual (Success):** Click "Save Rate Changes" $\to$ review diff modal $\to$ confirm. Verify success toast appears ("X price rules updated") and amber styling clears from cells.
- **Manual (Failure / Edge Case):** In the diff review modal, click "Confirm & Publish" while the platform backend is offline or returns an error (HTTP 500 / 400). Verify an error banner displays ("Failed to persist changes to platform: upstream service error"), the modal remains open or preserves the staged list, and all amber draft borders remain on the grid so unsaved edits are not lost.
- **Automated (Unit / Slice / Integration):** WireMock integration test asserting `/functions/od-matrix-set` receives the expected payload and `X-Actor` header, returning HTTP 200; simulate HTTP 500 from `/functions/od-matrix-set` asserting the controller catches the failure, returns an error status to the client, and retains draft state.

#### Actionable Sub-tasks

- [ ] Build Bootstrap diff review modal displaying staged item changes.
- [ ] Implement Spring Boot `RateBatchUpdateDto` with Bean Validation annotations.
- [ ] Implement `CaracalApiClient.setOdMatrix()` passing `X-Actor` header.
- [ ] Connect frontend "Confirm & Publish" button to controller batch POST endpoint.
- [ ] Write WireMock tests covering success and HTTP 500 error handling.