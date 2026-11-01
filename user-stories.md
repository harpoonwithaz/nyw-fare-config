# User Stories

---

## Story 2.1: Unified Tenant Context Switching & Dual-Matrix View Scaffolding

As a ***Caracal client services operator***, I ***want the matrix grid to automatically adapt its axes and data bindings based on the selected tenant context***, so that ***I can manage point-to-point ferry routes (NY Waterway) or zone-based rail networks (Metra) within a single unified application***.

### Actors / Personas

Authenticated Operator (`ROLE_OPERATOR`), Administrator (`ROLE_ADMIN`).

### Precondtions & Triggers

- Operator is authenticated and navigates to the Rate Management view (`/fares`).
- The global context selector allows toggling between tenants (e.g., `NYW_SANDBOX` and `METRA`).

### Actions / Postconditions:

- Selecting `NYW_SANDBOX` binds the view to **Point-to-Point (O-D) Mode:** the sub-selector displays Origin Terminals (e.g., `EDGEWTR`, `PORTIMP`), the grid rows display outbound destinations (`MIDTOWN`, `BROOKFD`, `PIER11`), and columns display ticket products (`ADULT_ONEWAY`, `CHILD_ONEWAY`, `FULL_10TRIP`, etc.).
- Selecting `METRA` binds the view to Zonal Matrix Mode: the sub-selector displays Fare Products (e.g., `ADULT_ONEWAY`), the grid rows display Origin Zones (`Zone 1`, `Zone 2`, etc.), and columns display Destination Zones (`Zone 1`, `Zone 2`, etc.).
- Cells display formatted rates (`$XX.XX`) or distinct badges (`NOT_SOLD`, `N/A`) for unserved/inactive pairs

### Acceptance Criteria

1. Switching the tenant dropdown instantly re-renders the grid structure to the corresponding axis mode without full-page error reloading.
2. In O-D Mode, changing the origin terminal dynamically updates outbound destination rows and fetches current prices.
3. In Zonal Mode, changing the active ticket product dynamically updates the zone-to-zone fare intersections.
4. Every cell without an active price rule resolves to a muted `NOT_SOLD` or `N/A` badge; no cell renders as blank, `$0.00`, or `NaN`.

### Test Scenarios

- Manual (Success): Select `NYW_SANDBOX` $\rightarrow$ select `EDGEWTR` terminal. Verify rows show destinations like `MIDTOWN` and columns show products like `ADULT_ONEWAY`. Switch tenant to `METRA` $\rightarrow$ select `ADULT_ONEWAY`. Verify table renders a symmetric Zone $\times$ Zone grid with rates formatted as `$XX.XX`.
- Manual (Failure / Edge Case): Select a terminal with no outbound routes or an unconfigured zone pair. Verify the cell displays a gray `NOT_SOLD` badge rather than breaking table formatting or rendering blank.
- Automated (`MockMvc`): Execute `GET /fares?tenant=NYW_SANDBOX&origin=EDGEWTR` asserting HTTP 200, checking model attributes for `matrixMode=OD` and verifying table cells in the Thymeleaf response; execute `GET /fares?tenant=METRA&product=ADULT_ONEWAY` asserting HTTP 200 and `matrixMode=ZONAL`.

### Actionable Sub-tasks

- [ ] Implement `TenantContext` model and register tenant session resolver in Spring Boot.
- [ ] Create Thymeleaf layout with responsive context toolbar (Tenant dropdown, Mode indicator, Terminal/Product secondary selector).
- [ ] Build dynamic matrix table component in `rate-grid.html` supporting both destination and zone row/column iteration.
- [ ] Define CSS styling for price cells, `NOT_SOLD` badges, and `N/A` muted pills.
- [ ] Write controller slice tests (`@WebMvcTest`) verifying dual-mode model attributes.

## Story 2.2: O-D Mode Direct Inline Rate Editing & Numeric Input Masking

As a ***NY Waterway fare manager***, I want to ***click directly into a price cell in the O-D grid to edit the rate inline with real-time numeric validation,*** so that ***I can update point-to-point fares quickly and prevent malformed or negative amounts from being entered.***

### Actors / Personas

Fare Manager, Operator.

### Preconditions & Triggers

The application is in O-D Mode (`NYW_SANDBOX`), displaying an active terminal rate table. The user clicks on any active price cell.

### Actions / Postconditions

The static currency text converts into a focused numeric input field (`<input type="number">`). Keystrokes are sanitized in real time; blur or Enter commits the value to local draft state, while Escape cancels the edit.

### Acceptance Criteria

1. Clicking a price cell converts the cell text into an active input pre-populated with the unformatted numeric value.
2. Input masking restricts entries strictly to positive numbers with a maximum of two decimal places (`min="0.00"`, `step="0.01"`).
3. Negative signs, alphabetic letters, and third decimal points are blocked on keystroke.
4. Entering a blank value or invalid string applies the `.is-invalid` visual class, disables the save trigger, and alerts the user.
5. Cells flagged as `NOT_SOLD` cannot be edited directly via inline text without explicitly toggling their sold state.

### Test Scenarios

- *Manual (Success):* Click `EDGEWTR -> MIDTOWN` Adult fare (`$13.25`), change to `14.00`, and press Enter. Verify the cell reflects `$14.00`.
- *Manual (Failure):* Click a price cell, attempt to type `-12.50`, `abc`, or `13.999`. Verify that invalid characters are blocked and the input highlights in red if forced.
- `Automated (JavaScript Unit / Jest):` Test cell input event handlers to verify regex `/^\d+(\.\d{1,2})?$/` sanitizes string input and rejects non-conforming values.

### Actionable Sub-tasks

- [ ] Implement client-side inline edit toggle (`click` converts span to input, `blur`/`Enter` converts back to span).
- [ ] Add vanilla JS input masking to reject negative numbers, letters, and excessive decimal places. 
- [ ] Add `.is-invalid` error styling and accessible ARIA attributes for malformed inputs.
- [ ] Lock `NOT_SOLD` and unconfigured cells from inline text entry.
- [ ] Write automated browser tests verifying keystroke validation rules.

## Story 2.3: Zonal Cell Action Modal for Rate Tier Management

As a ***transit administrator in Zonal Mode,*** I want ***clicking a matrix cell to open a Rate Action Modal with options to update the shared tier globally, reassign the cell to an existing tier, or create a new tier,*** so that ***I can manage network-wide zonal fare structures accurately.***

### Actors / Personas

Transit Administrator, Client Services Operator.

### Preconditions & Triggers

The application is in Zonal Mode (`METRA`). The user clicks on any zone-to-zone cell in the grid.

### Actions / Postconditions

A modal window opens displaying the current route/zone pair (e.g., `Zone 1 -> Zone 2`), its active rate tier, and three radio-selected action paths plus an availability toggle.

### Acceptance Criteria

1. The modal displays three distinct action options:
    - **Option 1 (Global Rate Tier Adjustment):** Updates the price of the current rate tier itself. The modal explicitly warns the user how many total network zone pairs share this tier and will be changed.
    - **Option 2 (Reassign to Existing Tier):** Provides a dropdown of existing active system tiers (e.g., `$4.90`, `$5.90`, `$7.40`), allowing the cell to be linked to a different tier without altering the tier's base price.
    - **Option 3 (Create & Assign New Tier):** Allows inputting a new rate value (e.g., `$6.50`) and creating a new tier assigned to this zone pair.
2. The modal includes a toggle to switch the cell between active pricing and `NOT_SOLD`.
3. Clicking "Apply Action" stages the change into the local draft tracking state with metadata identifying the action type.

### Test Scenarios

- *Manual (Success - Global Update)*: Click a `$4.90` cell, select Option 1, enter `$5.00`, and click Apply. Verify all cells sharing that tier in the matrix reflect `$5.00` and display draft highlights.
- *Manual (Success - Reassign Tier):* Click a `$5.90` cell, select Option 2, choose the `$7.40` tier from the dropdown, and apply. Verify only this specific cell updates to `$7.40`.
- *Manual (Failure):* Select Option 3, leave the new rate field blank or enter a negative value (`-3.00`). Verify validation prevents applying the change and displays an error message.
- *Automated (Frontend Integration):* Trigger modal submission via JavaScript test fixture; verify that the emitted change event payload contains the correct `actionType` (`GLOBAL_UPDATE`, `REASSIGN_TIER`, or `NEW_TIER`) and target rate.

### Actionable Sub-tasks

- [ ] Construct Bootstrap 5 modal markup for the Zonal Rate Action dialog.
- [ ] Implement dynamic modal population showing current pair (`originZone`, `destZone`) and linked tier details.
- [ ] Implement Option 1 form controls with dynamic warning text showing count of affected pairs.
- [ ] Implement Option 2 dropdown populated with active rate tier entities.
- [ ] Implement Option 3 form controls with currency validation for new tier creation.
- [ ] Wire modal "Apply" button to update grid cell display and dispatch draft state events.

### Story 2.3a: Zonal Action Modal UI & Tier Selection Dropdown

As a ***transit administrator in Zonal Mode,*** I want ***clicking a matrix cell to open an action modal displaying the active zone pair and a dropdown of existing rate tiers,*** so that ***I can reassign that route to a different existing price tier.***

#### Actors

Transit Administrator, Operator (`ROLE_OPERATOR`)

#### Preconditions & Triggers

The application is in Zonal Mode (`METRA`). The operator clicks any zone-to-zone cell in the grid.

#### Actions & Postconditions

A modal opens showing the origin and destination zone labels alongside current pricing. The user selects an existing tier from the dropdown (Option 2) and clicks "Apply", which stages the reassigned rate into the local draft state and closes the dialog.

#### Acceptance Criteria

1. Clicking any active zonal cell opens the modal without page reload.
2. Modal displays the origin zone, destination zone, and active fare.
3. Dropdown lists all active system rate tiers with their dollar values.
4.  Selecting a tier and clicking "Apply" updates only the clicked cell's staged rate.
5. Includes an availability toggle to flag the cell as `NOT_SOLD`.

#### Test Scenarios

- _Manual:_ Click a `$5.90` cell $\to$ select `$7.40` tier from dropdown $\to$ apply. Verify only that single cell updates to `$7.40` in the grid.
- _Automated:_ JavaScript unit test verifying the modal open event populates tier options and emits a `REASSIGN_TIER` event on submission.

### Story 2.3b: Global Tier Adjustment & New Tier Creation Logic

As a ***transit revenue manager,*** I want ***the zonal rate modal to support modifying a tier's base price globally or creating a brand new tier,*** so that ***I can adjust shared network rates or define custom fare levels.***

#### Actors

Transit Administrator, Revenue Manager.

#### Preconditions & Triggers

The Rate Action Modal is open for a selected cell.

#### Actions & Postconditions

Choosing Option 1 updates the shared tier price, flagging all linked network cells as modified. Choosing Option 3 validates a new dollar amount, registers a new tier, and links the active cell to it.   

#### Acceptance Criteria

1. Option 1 displays an impact warning stating the total count of network zone pairs sharing that tier.   
2. Applying Option 1 updates all matrix cells associated with that tier.   Option 3 validates currency input strictly (> 0.00, two decimal places).
3. Option 3 creates the new tier and assigns it exclusively to the active cell. 
4. Option 3 creates the new tier and assigns it exclusively to the active cell.

#### Test Scenarios

- _Manual:_ Select Option 1 for a `$4.90` tier (12 linked pairs) $\to$ enter `$5.00` $\to$ apply. Verify all 12 cells update to `$5.00`. Select Option 3 $\to$ enter `-2.00` $\to$ verify validation error blocks submission.
- _Automated_: Frontend unit test asserting `GLOBAL_UPDATE` and `NEW_TIER` payload construction and currency regex validation.

## Story 2.4: Global Rate Tier Catalog & Bulk Adjustment Tool

As a ***transit revenue manager,*** I want ***a dedicated catalog view of all active rate tiers with a bulk adjustment tool,*** so that ***I can execute network-wide fare changes (such as an annual 10% increase) across all tiers simultaneously.***

### Actors / Personas

Transit Administrator, Revenue Manager.

### Preconditions & Triggers

Operator is in Zonal Mode and clicks the "Manage Rate Tiers" button in the header toolbar.

### Actions / Postconditions

A management panel displays a summarized table of all active rate tiers, their values, and their assignment counts. The operator can apply a percentage or fixed dollar increase across all tiers, preview recalculations, and stage the bulk adjustment.

### Acceptance Criteria

1. Displays every active rate tier with: Tier Code/Name, Base Rate ($), and Number of Linked Station/Zone Pairs.
2. The bulk adjustment tool supports two mathematical operations:
   - **Percentage Increase:** e.g., `+10%`, rounding results to the nearest cent (`$0.01`).
   - **Flat Increment:** e.g., `+$0.25` across all tiers.
3. Clicking "Preview Bulk Adjustment" shows side-by-side Old Price vs. New Price comparisons for every tier before applying.
4. Applying updates stages modifications for all affected tiers and marks corresponding matrix cells as drafted.

### Test Scenarios

- *Manual (Success):* Open Rate Tier Manager, input `10%` increase, and click Preview. Verify a `$4.90` tier becomes `$5.39` and a `$7.40` tier becomes `$8.14`. Click "Apply to Grid" and verify the active zonal matrix reflects the updated values.
- Manual (Failure): Input a negative percentage that would drop rates below `$0.00` (e.g., `-150%`). Verify validation blocks the calculation and displays an error alert.
- Automated (JUnit Service Test): Create a unit test for `RateTierService.calculateBulkAdjustment(List<RateTier> tiers, AdjustmentType type, double value)` asserting correct mathematical rounding and boundary checks.

### Actionable Sub-tasks

- [ ] Create `RateTier` DTO and in-memory/database catalog registry service.
- [ ] Build "Manage Rate Tiers" modal / view with tier summary table.
- [ ] Implement bulk calculation engine in JavaScript/Java supporting percentage and flat addition with half-up cent rounding.
- [ ] Add side-by-side preview diff table inside the tier manager dialog.
- [ ] Write unit tests verifying precision rounding and negative-rate prevention.

### Story 2.4a: Rate Tier Catalog View & Summary Table

As a **_transit administrator,_** I want **_a dedicated catalog view displaying all active rate tiers and their usage counts,_** so that **_I have complete visibility over network fare groupings._**

#### Actors

Transit Administrator, Operator.

#### Preconditions & Triggers

Operator is in Zonal Mode and clicks the "Manage Rate Tiers" button in the toolbar.

#### Actions & Postconditions

A management dialog opens displaying a consolidated table of all active rate tiers, their base currency values, and the total count of mapped zone pairs.

#### Acceptance Criteria

1. Table lists Tier Code, Base Fare ($), and Number of Linked Station/Zone Pairs.
2. Unassigned or empty tiers are clearly labeled.
3. Table dynamically reflects in-memory draft changes made during the session. 

#### Test Scenarios

- _Manual:_ Click "Manage Rate Tiers". Verify the table matches active system tiers and accurately totals mapped zone pairs.
- _Automated_: Spring MVC test (`@WebMvcTest`) asserting the endpoint returns the active `RateTier` DTO collection with correct counts.  

### Story 2.4b: Bulk Percentage & Flat Fare Adjustment Engine

As a **_revenue manager_**, I want **_to calculate and apply bulk percentage or flat adjustments across all rate tiers simultaneously,_** so that **_I can execute system-wide fare updates in a single action._**

#### Actors

Revenue Manager, Transit Administrator.

#### Preconditions & Triggers

The "Manage Rate Tiers" dialog is open.

#### Actions & Postconditions

Operator inputs a percentage (e.g., `+10%`) or flat increment (e.g., `+$0.25`), previews Old vs. New comparisons, and applies the calculation to all tiers across the matrix.   

#### Acceptance Criteria:

1. Supports percentage (`%`) and fixed dollar (`$`) calculations.
2. Applies standard financial half-up rounding to the nearest cent (`$0.01`).
3. Displays a side-by-side Old Price vs. New Price diff preview before committing.
4. Blocks calculations resulting in rates $\le 0.00$.
5. Applying recalculates and stages draft changes for all affected matrix cells.  

#### Test Scenarios:

- _Manual:_ Input `10%` increase $\to$ click Preview. Verify `$4.90` becomes `$5.39` and `$7.40` becomes `$8.14`. Click Apply and verify matrix cells update.
- _Automated:_ JUnit service test verifying `calculateBulkAdjustment()` rounds cents accurately and rejects negative adjustments.

## Story 2.5: Station-to-Zone Topology Mapping Side Panel

As an ***administrator managing a zonal rail network,*** I want to ***view and reassign stations to zones using a slide-out drawer on the same screen as the matrix,*** so that ***I can maintain station zone assignments without navigating away from the fare table.***

### Actors / Personas

Transit Administrator, Station Operations Manager.

### Preconditions & Triggers

User is in Zonal Mode (`METRA`) and clicks the **"Station Zone Mapping"** button on the workspace toolbar.

### Actions / Postconditions

A collapsible offcanvas drawer slides out from the right side of the screen containing a searchable list of all network stations and their assigned zone dimensions (`site-dimension-bind`).

### Acceptance criteria

1. The drawer opens alongside the fare grid without reloading or clearing the current matrix state.
2. Displays all stations with: Station Code (`siteCode`), Station Name, and current Zone Assignment dropdown.
3. Includes a real-time search filter allowing quick lookup of stations by name or code.
4. Selecting a new zone from a station's dropdown highlights the row as modified.
5. Saving the drawer persists the station-to-zone binding changes and displays a confirmation badge.

### Test Scenarios

- *Manual (Success):* Open the drawer, search for "Ravenswood", and change its zone from `Zone B` to `Zone A`. Click "Update Mappings". Verify the drawer reflects the new zone and shows a success toast.
- *Manual (Failure):* Attempt to assign a station to a non-existent or unconfigured zone. Verify the input is rejected by the select dropdown.
- *Automated (`MockMvc`):* Post a zone reassignment payload to `/api/topology/bind-station`; assert HTTP 200 and verify the updated mapping is returned in subsequent station lookups.

### Actionable Sub-tasks

- [ ] Implement Bootstrap 5 Offcanvas drawer component in rate-grid.html.
- [ ] Build station mapping table with instant JavaScript search/filter bar.
- [ ] Populate station rows with zone selection dropdowns.
- [ ] Add change-tracking listeners to flag modified station-zone pairs.
- [ ] Create backend controller endpoint handling station dimension bindings.

## Story 2.6: Visual Draft Tracking, Diff Confirmation, & Platform Batch API Dispatch

As an ***operator,*** I want ***all unsaved rate changes visually highlighted in draft state and summarized in a confirmation diff modal before committing,*** so that ***I can review old vs. new prices and batch-dispatch valid updates to Caracal's platform API.***

### Actors / Personas

Authenticated Operator, Administrator.

### Preconditions & Triggers

The user has modified one or more cells (via inline editing, tier reassignment, or global tier adjustments). The user clicks **"Save Rate Changes".**

### Actions / Postconditions

Modified cells display an amber draft border (`.table-warning`), the pending changes badge increments, and clicking Save opens a diff modal detailing: Pair/Route, Product/Tier, Old Price, New Price, and Scope (Single vs Global). Confirming dispatches a batch request to `/functions/od-matrix-set` with the operator's `X-Actor` header.

### Acceptance Criteria

1. Any cell modified during the active session is styled with an amber draft highlight.
2. Each drafted cell provides an inline revert trigger (or Escape key) restoring its initial value.
3. A global "Discard Changes" button resets all drafted cells back to their loaded platform values.
4. The "Save Rate Changes" confirmation modal summarizes all pending modifications with explicit old vs. new values.
5. The backend validates the payload using Spring Bean Validation (`@DecimalMin("0.00")`, `@Digits(fraction=2)`) before invoking Caracal's API.
6. Outgoing mutating requests attach the operator's identity via `X-Actor: operator:<email>` and return a success banner showing the count of rules written.

### Test Scenarios

- _Manual (Success):_ Edit two fares, verify the floating action bar reads "2 Pending Changes". Click "Save Rate Changes", confirm the diff modal lists both modifications accurately, click "Confirm & Publish". Verify an alert confirms "2 price rules updated" and cells return to normal styling.
- _Manual (Failure / Server Rejection):_ Submit a payload containing a negative price (`-5.00`) via developer tools or corrupted state. Assert the Spring backend blocks the request with HTTP 400 and displays a field validation error alert.
- _Automated (Integration Test / WireMock):_ Mock Caracal's `/functions/od-matrix-set` endpoint. Dispatch a valid batch update from the frontend controller, verify WireMock receives the payload with the expected JSON structure and `X-Actor` header, and assert HTTP 200.

### Actionable Sub-tasks

- [ ] Implement client-side `DraftStateManager` tracking changed cells, initial values, and change counts.
- [ ] Add amber visual draft styling (`.table-warning`, `.cell-draft)` and single-cell revert buttons.
- [ ] Build "Save Changes" review modal dynamically rendering the diff table.
- [ ] Implement Spring Boot `RateBatchUpdateDto` with strict Bean Validation constraints (`@DecimalMin`, `@Digits,` `@NotNull`).
- [ ] Implement `CaracalApiClient` method calling `/functions/od-matrix-set` with active session `X-Actor` header.
- [ ] Write slice tests (`@WebMvcTest`) and WireMock integration tests covering batch dispatch and validation failure handling.

### Story 2.6a: Visual Draft State Engine & Cell Revert Controls

As an **_operator_**, I want **_unsaved rate modifications visually highlighted on the grid with individual revert options,_** so that **_I can track pending edits and undo mistakes before saving._**

#### Actors

Operator, Fare Manager.

#### Preconditions & Triggers

An operator edits any price cell via inline editing or modal action. 

#### Actions & Postconditions

Modified cells receive an amber highlight (`.table-warning`), a toolbar badge displays the pending changes count, and the user can revert single cells or discard all edits.   

#### Acceptance Criteria

1. Modified cells display an amber draft border/background class (`.table-warning`). 
2. Hovering or clicking a drafted cell reveals a revert icon (or Escape key) to restore its initial value. 
3. A floating status bar displays an active count of pending changes.
4. "Discard All" restores the entire grid back to loaded platform values.  

#### Test Scenarios

- _Manual_: Modify two cells. Verify both turn amber and the toolbar reads "2 Pending Changes". Click revert on one cell; verify it resets and the badge updates to "1 Pending Change".   
- _Automated:_ JavaScript unit test verifying `DraftStateManager` tracks modifications, decrements on revert, and empties on discard.

### Story 2.6b: Transaction Review Diff Modal & Batch API Dispatch

As an **_operator,_** I want **_to review an itemized diff of all staged changes before batch-submitting them,_** so that **_I can verify accuracy and persist valid updates to the platform API under my audit identity._**   

#### Actors

Authenticated Operator, Administrator.   

#### Preconditions & Triggers

One or more cells are in draft state and the operator clicks "Save Rate Changes". 

#### Actions & Postconditions

A confirmation diff modal lists each change (Pair, Product/Tier, Old Rate, New Rate). Clicking "Confirm & Publish" sends a batch request to `/functions/od-matrix-set` with the operator's `X-Actor` header and clears the draft state.   

#### Acceptance Criteria

1. Diff modal displays an itemized list of all pending changes with Old vs. New rates.   
2. Backend DTO validates currency fields with `@DecimalMin("0.00")` and `@Digits(fraction=2)`.   
3. Outgoing platform request carries `X-Actor: operator:<email>`.   
4. On HTTP 200 from the platform, draft state clears and a success banner displays the rules written count.   

#### Test Scenarios

- _Manual:_ Click "Save Rate Changes" $\to$ review diff modal $\to$ confirm. Verify success toast appears and amber styling clears from cells.   
- _Automated:_ WireMock integration test asserting `/functions/od-matrix-set` receives the expected payload and `X-Actor` header, returning HTTP 200.   