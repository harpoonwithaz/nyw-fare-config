## Epic 2: Interactive Origin-Destination Rate Grid & Fare Management

This document defines the formal user stories and sub-stories for Epic 2 of the NY Waterway Fare Configuration Tool. Each story adheres strictly to the CMPT 276 agile project specification, INVEST criteria, and standardized persona definitions. <span class="it2-added">In Iteration 2, Epic 2 was aligned with our local PostgreSQL `fare_drafts` staging architecture (`POST /fares/drafts`), reserving live Caracal API batch dispatch (`/functions/od-matrix-set` and `/functions/site-dimension-bind`) for Iteration 3.</span>

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

- [x] Implement `TenantContext` model and register tenant session resolver in Spring Boot.
- [x] Create Thymeleaf layout with responsive context toolbar (Tenant dropdown, Mode indicator, Terminal/Product secondary selector).
- [x] Build dynamic matrix table component in `rate-grid.html` supporting both destination and zone row/column iteration.
- [x] Define CSS styling for price cells, `NOT_SOLD` badges, and `N/A` muted pills.
- [x] Write controller slice tests (`@WebMvcTest`) verifying dual-mode model attributes.

---

### Story 2.2: O-D Mode Direct Inline Rate Editing & Numeric Input Masking

- **GitHub Issue Title:** `[Rate Grid] O-D Mode Direct Inline Rate Editing & Numeric Input Masking`
- **Iteration:** `2`
- **Story Points:** `3`
- **Story Description:** As an authenticated operator, I want to click directly into a price cell in the O-D grid to edit the rate inline with real-time numeric validation, so that I can update point-to-point fares quickly and prevent malformed or negative amounts from being entered.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* The application is in O-D Mode (`NYW_SANDBOX`), displaying an active terminal rate table.
  - *Trigger:* The user clicks on any active price cell <span class="it2-added">(or focuses it via keyboard and presses `Enter` or `Space`)</span>.
- **Actions / Postconditions:**
  - *Action:* The static currency text converts into a focused numeric input field (`<input type="number">`) <span class="it2-added">with spreadsheet-style full text selection</span>. Keystrokes <span class="it2-added">and clipboard pastes</span> are sanitized in real time.
  - *Postcondition:* Pressing `Enter`, `Tab`, or blurring the input commits the value to local draft state (`DraftStateManager`), while pressing `Escape` cancels the edit <span class="it2-added">(or reverts an already-drafted cell back to its original baseline price)</span>.

#### Acceptance Criteria

1. Clicking a price cell converts the cell text into an active input pre-populated with the unformatted numeric value.
2. Input masking restricts entries strictly to positive numbers with a maximum of two decimal places (`min="0.00"`, `step="0.01"`), <span class="it2-added">and strips leading `$` symbols on paste</span>.
3. Negative signs, alphabetic letters, scientific notation (`e`), and third decimal points are blocked on keystroke.
4. Entering a blank value or invalid string applies the `.is-invalid` visual class, disables the save trigger, and alerts the user via the grid feedback banner.
5. Cells flagged as `NOT_SOLD` or `N/A` cannot be edited directly via inline text and display an operator feedback alert when clicked.

#### Test Scenarios

- **Manual (Success):** Click `EDGEWTR -> MIDTOWN` Adult fare (`$13.25`), change to `14.00`, and press `Enter`. Verify the cell reflects `$14.00` and enters draft state.
- **Manual (Failure / Edge Case):** Click a price cell, attempt to type `-12.50`, `abc`, or `13.999`. Verify that invalid characters are blocked on keystroke and the input highlights in red (`.is-invalid`) if forced via paste.
- **Automated (Unit / Slice / Integration):** Frontend unit / browser tests verifying cell input event handlers sanitize raw strings, reject values failing `/^\d+(\.\d{1,2})?$/`, and lock `NOT_SOLD` cells.

#### Actionable Sub-tasks

- [x] Implement client-side inline edit toggle (`click` converts span to input, `blur`/`Enter`/`Tab` commits back to span).
- [x] Add vanilla JS input masking and paste sanitization to reject negative numbers, letters, and excessive decimal places.
- [x] Add `.is-invalid` error styling and feedback banner alerts for malformed inputs.
- [x] Lock `NOT_SOLD` and `N/A` cells from inline text entry with explanatory feedback cards.
- [x] Write automated tests verifying keystroke and paste validation rules.

---

### Story 2.3: Zonal Cell Action Modal for Rate Tier Management (Parent Story)

- **GitHub Issue Title:** `[Rate Grid] Zonal Cell Action Modal for Rate Tier Management`
- **Iteration:** <span class="it2-modified">`3`</span>
- **Story Points:** `5` *(Split into sub-stories 2.3a and 2.3b)*
- **Story Description:** As a Caracal administrator in Zonal Mode, I want clicking a matrix cell to open a Rate Action Modal with options to update the shared tier globally, reassign the cell to an existing tier, or create a new tier, so that I can manage network-wide zonal fare structures accurately.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* The application is in Zonal Mode (`METRA`).
  - *Trigger:* The user clicks on any zone-to-zone cell in the grid.
- **Actions / Postconditions:**
  - *Action:* The operator selects one of three rate management actions or toggles availability.
  - *Postcondition:* The modal closes and stages the selected rate adjustment into <span class="it2-modified">PostgreSQL `fare_drafts` (via `POST /fares/drafts`) and updates the active grid view with staged indicators</span>.

#### Acceptance Criteria

1. The modal displays three distinct action options: Global Rate Tier Adjustment (Option 1), Reassign to Existing Tier (Option 2), and Create & Assign New Tier (Option 3).
2. Option 1 explicitly warns the user how many total network zone pairs share this tier and will be changed.
3. Option 2 provides a dropdown of existing active system tiers.
4. Option 3 validates currency input strictly (`> 0.00`, two decimal places).
5. The modal includes a toggle to switch the cell between active pricing and `NOT_SOLD`.

#### Test Scenarios

- **Manual (Success):** Click a `$4.90` cell $\to$ select Option 1 $\to$ enter `$5.00` $\to$ apply. Verify all cells sharing that tier in the matrix reflect `$5.00` and display staged draft highlights.
- **Manual (Failure / Edge Case):** Select Option 3 $\to$ leave the new rate field blank or enter a negative value (`-3.00`). Verify validation prevents applying the change and displays an inline error alert.
- **Automated (Unit / Slice / Integration):** `MockMvc` / service integration test verifying modal form submission persists `GLOBAL_TIER`, `TIER_REASSIGN`, or `TIER_CREATE` records into `fare_drafts` and rejects invalid currency values with HTTP 400.

#### Actionable Sub-tasks

- [x] Construct Bootstrap 5 modal markup for the Zonal Rate Action dialog.
- [x] Implement dynamic modal population showing current pair (`originZone`, `destZone`) and linked tier details.
- [x] Implement Option 1 form controls with dynamic warning text showing count of affected pairs.
- [x] Implement Option 2 dropdown populated with active rate tier entities.
- [x] Implement Option 3 form controls with currency validation for new tier creation.
- [x] Wire modal "Apply" submission to `FareDraftService` and refresh grid cell displays.

---

### Story 2.3a: Zonal Action Modal UI & Tier Selection Dropdown

- **GitHub Issue Title:** `[Rate Grid] Zonal Action Modal UI & Tier Selection Dropdown`
- **Iteration:** <span class="it2-modified">`3`</span>
- **Story Points:** `2`
- **Story Description:** As an authenticated operator in Zonal Mode, I want clicking a matrix cell to open an action modal displaying the active zone pair and a dropdown of existing rate tiers, so that I can reassign that route to a different existing price tier.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* The application is in Zonal Mode (`METRA`).
  - *Trigger:* The operator clicks any zone-to-zone cell in the grid.
- **Actions / Postconditions:**
  - *Action:* A modal opens showing origin and destination zone labels alongside current pricing. The user selects an existing tier from the dropdown (Option 2) and clicks "Apply".
  - *Postcondition:* <span class="it2-modified">The clicked cell's reassignment (`TIER_REASSIGN`) is persisted to `fare_drafts`, and the grid reflects the new staged rate.</span>

#### Acceptance Criteria

1. Clicking any active zonal cell opens the modal without a page reload.
2. Modal displays the origin zone, destination zone, and active fare.
3. Dropdown lists all active system rate tiers with their dollar values.
4. Selecting a tier and clicking "Apply" updates only the clicked cell's staged rate.
5. Includes an availability toggle to flag the cell as `NOT_SOLD` <span class="it2-added">(`STATUS_TOGGLE`)</span>.

#### Test Scenarios

- **Manual (Success):** Click a `$5.90` cell $\to$ select `$7.40` tier from dropdown $\to$ apply. Verify only that single cell updates to `$7.40` in the grid.
- **Manual (Failure / Edge Case):** Open the Rate Action Modal for an active zone pair, leave the tier selection on the unselected placeholder (or choose an empty/invalid value), and click "Apply". Verify an inline error message ("Please select a valid rate tier") appears, the modal remains open, and the cell value in the matrix does not change.
- **Automated (Unit / Slice / Integration):** Unit/slice test verifying modal population and `TIER_REASSIGN` persistence; assert submitting with a null/empty `tierCode` blocks submission and returns a validation error.

#### Actionable Sub-tasks

- [x] Build modal shell and backdrop in `rate-grid.html`.
- [x] Wire cell click listener to pass selected `originZone` and `destZone` to modal view state.
- [x] Implement Option 2 select element populated with system rate tiers.
- [x] Add availability switch for toggling `NOT_SOLD`.
- [x] Write automated tests for modal form submission validation.

---

### Story 2.3b: Global Tier Adjustment & New Tier Creation Logic

- **GitHub Issue Title:** `[Rate Grid] Global Tier Adjustment & New Tier Creation Logic`
- **Iteration:** <span class="it2-modified">`3`</span>
- **Story Points:** `3`
- **Story Description:** As a Caracal administrator, I want the zonal rate modal to support modifying a tier's base price globally or creating a brand new tier, so that I can adjust shared network rates or define custom fare levels.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* The Rate Action Modal is open for a selected cell.
  - *Trigger:* The user selects either Option 1 (Global Rate Update) or Option 3 (Create New Tier).
- **Actions / Postconditions:**
  - *Action:* The user modifies a tier's base value or inputs a custom price for a new tier.
  - *Postcondition:* Choosing Option 1 updates the shared tier price, <span class="it2-modified">upserting `GLOBAL_TIER` draft rows in PostgreSQL for all linked network cells</span>. Choosing Option 3 registers a new tier and links the active cell to it <span class="it2-modified">via `TIER_CREATE`</span>.

#### Acceptance Criteria

1. Option 1 displays an impact warning stating the total count of network zone pairs sharing that tier.
2. Applying Option 1 updates all matrix cells associated with that tier in `fare_drafts`.
3. Option 3 validates currency input strictly (`> 0.00`, two decimal places).
4. Option 3 creates the new tier and assigns it exclusively to the active cell.

#### Test Scenarios

- **Manual (Success):** Select Option 1 for a `$4.90` tier (12 linked pairs) $\to$ enter `$5.00` $\to$ apply. Verify all 12 cells update to `$5.00`. Select Option 3 $\to$ enter `$6.50` $\to$ verify a new tier is created and applied to the cell.
- **Manual (Failure / Edge Case):** Select Option 1 (Global Update) or Option 3 (New Tier) and input a negative number (`-$1.00`), blank text, or a third decimal place (`$5.555`). Click "Apply" and verify an inline validation error displays, input border highlights red, and no matrix cells or global tier values are modified.
- **Automated (Unit / Slice / Integration):** Service and controller tests asserting `GLOBAL_TIER` and `TIER_CREATE` draft creation on valid input, and verifying negative or non-decimal inputs fail validation without mutating database state.

#### Actionable Sub-tasks

- [x] Add Option 1 radio button, input field, and dynamic affected-pairs calculation.
- [x] Add Option 3 radio button and validated new tier input field.
- [x] Wire submission handlers to persist global vs. single-cell draft updates in `FareDraftService`.
- [x] Write unit tests verifying input regex validation and tier expansion logic.

---

### Story 2.4: Global Rate Tier Catalog & Bulk Adjustment Tool (Parent Story)

- **GitHub Issue Title:** `[Rate Grid] Global Rate Tier Catalog & Bulk Adjustment Tool`
- **Iteration:** <span class="it2-modified">`3`</span>
- **Story Points:** `5` *(Split into sub-stories 2.4a and 2.4b)*
- **Story Description:** As a Caracal administrator, I want a dedicated catalog view of all active rate tiers with a bulk adjustment tool, so that I can execute network-wide fare changes (such as an annual 10% increase) across all tiers simultaneously.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* Operator is in Zonal Mode (`METRA`).
  - *Trigger:* The user clicks the "Manage Rate Tiers" button in the header toolbar.
- **Actions / Postconditions:**
  - *Action:* A management panel displays a summarized table of all active rate tiers, their values, and their assignment counts. The operator applies a percentage or fixed dollar increase across all tiers.
  - *Postcondition:* The operator previews recalculations and stages bulk adjustments across all affected matrix cells <span class="it2-modified">in PostgreSQL `fare_drafts`</span>.

#### Acceptance Criteria

1. Displays every active rate tier with: Tier Code/Name, Base Rate ($), and Number of Linked Station/Zone Pairs.
2. The bulk adjustment tool supports percentage increases (rounded to the nearest cent) and flat increments.
3. Clicking "Preview Bulk Adjustment" shows side-by-side Old Price vs. New Price comparisons for every tier before applying.
4. Applying updates stages modifications for all affected tiers in `fare_drafts` and marks corresponding matrix cells as staged.

#### Test Scenarios

- **Manual (Success):** Open Rate Tier Manager $\to$ input `10%` increase $\to$ click Preview. Verify a `$4.90` tier becomes `$5.39` and a `$7.40` tier becomes `$8.14`. Click "Apply to Grid" and verify the active zonal matrix reflects the updated values.
- **Manual (Failure / Edge Case):** Input a negative percentage that would drop rates below `$0.00` (e.g., `-150%`) or non-numeric input. Verify validation blocks the calculation and displays an inline error alert ("Adjustment must result in fares greater than $0.00").
- **Automated (Unit / Slice / Integration):** JUnit service test for `RateTierService.calculateBulkAdjustment(...)` asserting correct mathematical half-up rounding and boundary checks; assert `IllegalArgumentException` on negative-resulting rates.

#### Actionable Sub-tasks

- [x] Create `RateTier` DTO and tier aggregation service over dimensional pricing rules.
- [x] Build "Manage Rate Tiers" modal / view with tier summary table.
- [x] Implement bulk calculation engine supporting percentage and flat addition with `BigDecimal` half-up cent rounding.
- [x] Add side-by-side preview diff table inside the tier manager dialog.
- [x] Write unit tests verifying precision rounding and negative-rate prevention.

---

### Story 2.4a: Rate Tier Catalog View & Summary Table

- **GitHub Issue Title:** `[Rate Grid] Rate Tier Catalog View & Summary Table`
- **Iteration:** <span class="it2-modified">`3`</span>
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
3. Table dynamically reflects active staged draft changes made during the session.
4. If no tiers exist or if service retrieval fails, a clear fallback notification is rendered.

#### Test Scenarios

- **Manual (Success):** Click "Manage Rate Tiers". Verify the table matches active system tiers and accurately totals mapped zone pairs.
- **Manual (Failure / Edge Case):** Click "Manage Rate Tiers" when no rate tiers exist in the system or when the service encounters a backend fetch failure. Verify the modal displays a clear alert banner and disables the bulk adjustment controls rather than crashing or rendering a broken table.
- **Automated (Unit / Slice / Integration):** Spring MVC test (`@WebMvcTest`) asserting the endpoint returns HTTP 200 with active `RateTier` DTO collections; simulate service failure asserting graceful error handling.

#### Actionable Sub-tasks

- [x] Define `RateTierDto` class with tier code, rate value, and pair count fields.
- [x] Create controller/service endpoint returning active tier summaries.
- [x] Build modal view with Bootstrap summary table.
- [x] Implement empty state and error state alert templates.
- [x] Write `@WebMvcTest` controller test for tier catalog retrieval.

---

### Story 2.4b: Bulk Percentage & Flat Fare Adjustment Engine

- **GitHub Issue Title:** `[Rate Grid] Bulk Percentage & Flat Fare Adjustment Engine`
- **Iteration:** <span class="it2-modified">`3`</span>
- **Story Points:** `3`
- **Story Description:** As a Caracal administrator, I want to calculate and apply bulk percentage or flat adjustments across all rate tiers simultaneously, so that I can execute system-wide fare updates in a single action.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* The "Manage Rate Tiers" dialog is open.
  - *Trigger:* The user inputs an adjustment value and clicks "Preview Bulk Adjustment".
- **Actions / Postconditions:**
  - *Action:* The user inputs a percentage (e.g., `+10%`) or flat increment (e.g., `+$0.25`), reviews Old vs. New comparisons, and clicks "Apply to Grid".
  - *Postcondition:* The application recalculates rates and stages `GLOBAL_TIER` draft modifications in PostgreSQL for all affected cells across the matrix.

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

- [x] Implement `calculateBulkAdjustment` service logic with `BigDecimal` half-up rounding.
- [x] Build preview diff table component showing Old Price, New Price, and Absolute Change.
- [x] Wire "Apply to Grid" button to persist bulk drafts and refresh matrix cells.
- [x] Add client-side validation blocking invalid or non-numeric adjustment inputs.
- [x] Write JUnit unit tests covering flat, percentage, and boundary rounding edge cases.

---

### Story 2.5: Station-to-Zone Topology Mapping Side Panel

- **GitHub Issue Title:** `[Rate Grid] Station-to-Zone Topology Mapping Side Panel`
- **Iteration:** <span class="it2-modified">3 *(Deferred from Iteration 2 — see Future Iterations Backlog)*</span>
- **Story Points:** `3`
- **Story Description:** As a Caracal administrator managing a zonal rail network, I want to view and reassign stations to zones using a slide-out drawer on the same screen as the matrix, so that I can maintain station zone assignments without navigating away from the fare table.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* User is in Zonal Mode (`METRA`).
  - *Trigger:* The user clicks the "Station Zone Mapping" button on the workspace toolbar.
- **Actions / Postconditions:**
  - *Action:* A collapsible offcanvas drawer slides out containing a searchable list of all network stations and their assigned zone dimensions (`site-dimension-bind`).
  - *Postcondition:* <span class="it2-modified">The operator can reassign a station to a new zone, staging the topology update in `station_zone_drafts` for Iteration 3 dispatch to `/functions/site-dimension-bind`.</span>

#### Acceptance Criteria

1. The drawer opens alongside the fare grid without reloading or clearing the current matrix state.
2. Displays all stations with: Station Code (`siteCode`), Station Name, and current Zone Assignment dropdown.
3. Includes a real-time search filter allowing quick lookup of stations by name or code.
4. Selecting a new zone from a station's dropdown highlights the row as modified.
5. Saving the drawer persists the station-to-zone binding draft and displays a confirmation badge.

#### Test Scenarios

- **Manual (Success):** Open the drawer, search for "Ravenswood", and change its zone from `Zone B` to `Zone A`. Click "Update Mappings". Verify the drawer reflects the new zone and shows a success toast.
- **Manual (Failure / Edge Case):** Attempt to submit an invalid station binding request via a corrupted payload (e.g., missing station code or invalid target zone identifier) or simulate a network failure when clicking "Update Mappings". Verify the server rejects the request with an HTTP 400 error banner, and the UI displays an error alert without closing the drawer or losing entered changes.
- **Automated (Unit / Slice / Integration):** `MockMvc` POST request with valid payload asserting HTTP 200; execute request with blank/invalid zone parameter asserting HTTP 400 Bad Request and verifying that mappings remain unmutated.

#### Actionable Sub-tasks

- [ ] Implement Bootstrap 5 Offcanvas drawer component in `rate-grid.html`.
- [ ] Build station mapping table with instant JavaScript search/filter bar.
- [ ] Populate station rows with zone selection dropdowns.
- [ ] Add change-tracking listeners to flag modified station-zone pairs.
- [ ] Create backend controller endpoint handling station dimension bindings.

---

### Story 2.6: Visual Draft Tracking, Diff Confirmation, & PostgreSQL Staging Persistence (Parent Story)

- **GitHub Issue Title:** <span class="it2-modified">`[Rate Grid] Visual Draft Tracking, Diff Confirmation, & PostgreSQL Staging Persistence`</span>
- **Iteration:** <span class="it2-modified">`2` *(Split into Iteration 2 stories 2.6a & 2.6b; live Caracal API dispatch moved to Epic 5 in Iteration 3)*</span>
- **Story Points:** `5` *(Split into sub-stories 2.6a and 2.6b)*
- **Story Description:** As an authenticated operator, I want all unsaved rate changes visually highlighted in draft state, protected against accidental navigation, and summarized in a confirmation diff modal before persisting to local PostgreSQL staging (`POST /fares/drafts`), so that I can safely review and stage updates across terminals before final publication.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* The user has modified one or more cells (via inline editing, tier reassignment, or global tier adjustments).
  - *Trigger:* The user clicks "Save Rate Changes".
- **Actions / Postconditions:**
  - *Action:* The user reviews the diff modal (`#rateReviewModal`) detailing Origin, Destination, Product, Old Price, and New Price, and clicks "Confirm & Stage Drafts".
  - *Postcondition:* <span class="it2-modified">The Spring Boot backend validates the JSON batch payload (`POST /fares/drafts`) and upserts `PENDING` rows into PostgreSQL `fare_drafts` under the operator's identity. On success, in-memory dirty state clears, the grid refreshes with subtle staged indicators, and changes appear in Epic 5's `/publishing/review` diff screen.</span>

#### Acceptance Criteria

1. Any cell modified in browser memory during the active session is styled with an amber draft highlight (`.table-warning`, `.cell-draft`).
2. Each drafted cell provides an inline revert trigger (or `Escape` key) restoring its initial value, and a global "Discard All" button resets all unpersisted edits.
3. <span class="it2-added">Attempting to refresh or close the browser tab with unpersisted in-memory edits triggers a `beforeunload` confirmation warning.</span>
4. The "Save Rate Changes" confirmation modal summarizes all pending modifications with explicit old vs. new values.
5. <span class="it2-modified">The backend validates the payload (`@DecimalMin("0.00")`, `@Digits(fraction=2)`) and persists `PENDING` `FareDraft` records into PostgreSQL via `POST /fares/drafts` with CSRF protection.</span>
6. <span class="it2-added">Previously saved PostgreSQL `FareDraft` records overlay the matrix on reload with a subtle staged indicator badge/border so operators can distinguish persisted drafts from unsaved local edits.</span>

#### Test Scenarios

- **Manual (Success):** Edit two fares, verify the floating action bar reads "2 Pending Changes". Click "Save Rate Changes", confirm the diff modal lists both modifications accurately, click confirm. Verify a success alert appears, cells transition from amber unsaved draft styling to subtle persisted-draft indicators, and the edits appear on `/publishing/review`.
- **Manual (Failure / Edge Case):** Submit a payload containing a negative price (`-5.00`) or missing CSRF token, or simulate a backend database error (HTTP 500). Assert the Spring backend blocks invalid prices with HTTP 400, or displays an error alert inside `#rateReviewError` while preserving amber draft highlights on the grid.
- **Automated (Unit / Slice / Integration):** `MockMvc` / `@SpringBootTest` integration test asserting `POST /fares/drafts` persists `FareDraft` rows in PostgreSQL and returns HTTP 200; assert negative prices or invalid decimals return HTTP 400 without saving.

#### Actionable Sub-tasks

- [x] Implement client-side `DraftStateManager` tracking changed cells, initial values, change counts, and `beforeunload` navigation guard.
- [x] Add amber visual draft styling (`.table-warning`, `.cell-draft`) and single-cell revert buttons.
- [x] Build "Save Rate Changes" review modal (`rateReviewModal`) dynamically rendering the diff table in `rate-review.js`.
- [x] Implement Spring Boot `FareDraftController` (`POST /fares/drafts`) and `FareDraftService` with Bean Validation constraints.
- [x] Overlay persisted `PENDING` `FareDraft` rows onto `FareGridView` with subtle staged visual indicators.
- [x] Write `@WebMvcTest` and service integration tests covering draft persistence and validation failure handling.

---

### Story 2.6a: Visual Draft State Engine & Cell Revert Controls

- **GitHub Issue Title:** `[Rate Grid] Visual Draft State Engine & Cell Revert Controls`
- **Iteration:** `2`
- **Story Points:** `2`
- **Story Description:** As an authenticated operator, I want unsaved rate modifications visually highlighted on the grid with individual revert options and accidental-exit protection, so that I can track pending edits and undo mistakes before staging.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* An operator edits any price cell via inline editing or modal action.
  - *Trigger:* A rate value is modified to differ from `data-original-price`.
- **Actions / Postconditions:**
  - *Action:* The operator modifies cell values, clicks an inline revert button, or clicks "Discard All".
  - *Postcondition:* Modified cells receive an amber highlight (`.table-warning`, `.cell-draft`), a toolbar badge displays the pending changes count, <span class="it2-added">`beforeunload` guards against accidental page unload while dirty,</span> and the user can revert single cells or discard all edits.

#### Acceptance Criteria

1. Modified cells display an amber draft border/background class (`.table-warning`, `.cell-draft`), <span class="it2-added">while identity edits (entering the exact same price) do not mark the cell dirty</span>.
2. Hovering or focusing a drafted cell reveals a revert icon (or `Escape` key) to restore its initial value.
3. A toolbar status bar displays an active count of pending changes and enables/disables the "Save Rate Changes" button.
4. "Discard All" restores the entire grid back to loaded baseline values.
5. <span class="it2-added">A `beforeunload` listener warns the user if they attempt to leave or reload the page while unpersisted drafts exist in `DraftStateManager`.</span>

#### Test Scenarios

- **Manual (Success):** Modify two cells. Verify both turn amber and the toolbar reads "2 Pending Changes". Click revert on one cell; verify it resets to its loaded baseline value and the badge updates to "1 Pending Change".
- **Manual (Failure / Edge Case):** Attempt to trigger a revert action (or press `Escape`) on an unmodified cell that has no staged changes, or enter the identical existing fare into a cell. Verify the cell styling remains normal, no draft events fire, and the pending changes counter remains at `0`.
- **Automated (Unit / Slice / Integration):** JavaScript unit test verifying `DraftStateManager` tracks modifications, ignores identity edits, decrements on revert, and empties on discard.

#### Actionable Sub-tasks

- [x] Create `draft-manager.js` module tracking modified cell key-value pairs in a `Map`.
- [x] Add CSS classes `.table-warning` and `.cell-draft` for modified table cells.
- [x] Implement toolbar status badge showing pending changes count and enabling `#saveChangesBtn`.
- [x] Add individual revert button icon and `Escape` key shortcut to cell templates.
- [x] Implement "Discard All" button and `beforeunload` dirty-state guard.

---

### Story 2.6b: Transaction Review Diff Modal & PostgreSQL Draft Persistence

- **GitHub Issue Title:** <span class="it2-modified">`[Rate Grid] Transaction Review Diff Modal & PostgreSQL Draft Persistence`</span>
- **Iteration:** <span class="it2-modified">`2` *(Updated from Iteration 3 API dispatch to Iteration 2 PostgreSQL staging; live Caracal API dispatch moved to Iteration 3 and Epic 5)*</span>
- **Story Points:** `3`
- **Story Description:** As an authenticated operator, I want to review an itemized diff of all in-memory rate changes in a confirmation modal and persist them to local PostgreSQL staging (`POST /fares/drafts`), so that I can verify accuracy and save staged drafts across sessions before final Caracal publication.
- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)
- **Preconditions & Triggers:**
  - *Precondition:* One or more cells are in browser draft state (`DraftStateManager.getPendingChanges().length > 0`).
  - *Trigger:* The operator clicks the "Save Rate Changes" button (`#saveChangesBtn`).
- **Actions / Postconditions:**
  - *Action:* A Bootstrap confirmation diff modal (`#rateReviewModal`) lists each change (Origin, Destination, Product, Old Rate, New Rate). Clicking confirm sends a CSRF-protected JSON `POST` request to `/fares/drafts`.
  - *Postcondition:* <span class="it2-modified">The Spring Boot backend validates the batch payload, upserts `PENDING` `FareDraft` records into PostgreSQL associated with the logged-in user, refreshes the rate grid to display subtle staged indicators, and makes the changes visible in `/publishing/review`.</span>

#### Acceptance Criteria

1. Diff modal displays an itemized list of all pending changes with Old vs. New rates and highlights proposed new prices.
2. Backend DTO (`FareChangeRequest`) validates currency fields with `@DecimalMin("0.00")` and `@Digits(fraction=2)`.
3. <span class="it2-modified">Frontend `rate-review.js` attaches Spring Security CSRF headers and dispatches all pending edits in a single atomic `POST /fares/drafts` request.</span>
4. <span class="it2-modified">On HTTP 200, in-memory draft state clears, the rate grid refreshes to render persisted `FareDraft` rows with subtle staged badges/borders, and a success feedback banner displays.</span>
5. If validation fails or the backend returns an error, `#rateReviewError` displays a clear message, the modal remains open, and in-memory amber draft highlights are retained.

#### Test Scenarios

- **Manual (Success):** Click "Save Rate Changes" $\to$ review diff modal $\to$ confirm. Verify success banner appears, the grid refreshes showing the updated fares with subtle staged draft indicators, and navigating to `/publishing/review` lists the staged fare diffs.
- **Manual (Failure / Edge Case):** Submit a malformed payload (e.g., negative price `-5.00` or missing CSRF header) or simulate a database exception on `POST /fares/drafts`. Verify an error banner displays in `#rateReviewError`, the modal stays open, and all amber draft borders remain on the grid so unsaved edits are not lost.
- **Automated (Unit / Slice / Integration):** `MockMvc` / `@SpringBootTest` test asserting `POST /fares/drafts` persists `FareDraft` entities into PostgreSQL with `status = PENDING` and returns HTTP 200; assert invalid currency values return HTTP 400 without modifying database records.

#### Actionable Sub-tasks

- [x] Build Bootstrap diff review modal (`rateReviewModal`) displaying staged item changes.
- [x] Implement `rate-review.js` reading `DraftStateManager.getPendingChanges()`, attaching CSRF headers, and posting to `/fares/drafts`.
- [x] Implement Spring Boot `FareController` (`POST /fares/drafts`) and `FareDraftService` with Bean Validation annotations.
- [x] Refresh the rate grid on successful `POST` and render subtle staged indicators for persisted `FareDraft` cells.
- [x] Write `@WebMvcTest` and integration tests covering valid draft staging, CSRF/validation rejection, and error retention.

---
