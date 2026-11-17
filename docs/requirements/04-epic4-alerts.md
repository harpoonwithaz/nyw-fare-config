## Epic 4: Pre-Purchase Service Alerts

When a rider picks a ticket that can't be used right now, the ticket machine (TVM) should show a short heads-up before they pay. The rider can still buy the ticket. The warning just makes sure they know when it will work.

Our client described three cases in our meetings so far:

- **Weekday-only routes:** On a Saturday, a rider picks Edgewater → Midtown and sees "This route doesn't run on weekends. Your ticket is valid Monday."
- **Rush-hour-only routes:** Some routes only run during weekday morning and evening rush hours, so riders outside those hours see a notice.
- **Advance-purchase promos:** A rider buys a December promo ticket on Oct 30 and sees "You can buy this now, but you can't use it until December 1."

These are **not** outage or maintenance messages. Every warning is shown in English and Spanish, like the client's current TVM screens. Epic 3 (Availability) decides when a ticket is sold, and Epic 4 decides what the rider is told.

<span class="it2-added">Alerts are a proof of concept. We store them in our own PostgreSQL database (`service_notices` table) and do not send them to Caracal, so they are kept separate from Epic 5 (Review and Publish), which only covers changes to the Caracal catalog. Routes use the platform site codes (for example `PIER11:HOB14ST`).</span>

**Note:** The character limits below (200 for messages, 20 for button labels) are still our best guess. We'll confirm them with the client.

::: {.it2-added}
### Changes Since Iteration 1

- **Removed Story 4.6 (Live TVM Preview).** The client asked us to drop the TVM preview. The preview code was removed in PR #100.
- **Alert lifecycle added.** Every save creates a **Draft**. Alerts can be **Draft**, **Active**, or **Archived**. Editing a live alert creates a draft copy instead of changing the live one. Since alerts are not published through Epic 5, a **Make active** button on draft rows turns a draft on, and a draft copy replaces (and archives) the alert it was copied from (PR #109).
- **Story 4.3 changed** from "administrators delete warnings" to "administrators delete drafts and archive active alerts". Archived alerts can't be edited.
- **Story 4.2 changed:** one alert can cover several routes, there is no status dropdown, and errors show as a list at the top of the form.
- **Story 4.4 moved to Iteration 3** and rewritten after the Nov 16 client meeting (see below).
- **Story 4.5 partly done.** Button labels are saved with a 20 character limit. Suggested and default labels are moved to Iteration 3.
- **Ownership:** Harendeep built the backend (`ServiceAlert`, `RouteSchedule`, `AlertService`, validation). Tejas built the pages, controller, and controller tests.

### Client Feedback (Nov 16 Meeting)

We demoed the full alert flow (create, validate, make active, safe edit, archive) to the client. Their feedback for Iteration 3:

- **Better time rules.** A single "Weekends" trigger is too limited. Operators should pick the **days of the week**, the **hours in a day** a route runs (service hours), and **holidays**, both one-time (Nov 26, 2026) and recurring (Dec 25 every year).
- **Warn, never block.** Riders must still be able to buy a ticket for a day or time it can't be used (saving it for later), but an alert must always show first. Example: a route that only runs Monday, Wednesday, and Friday shows "Not scheduled today. Your ticket is valid Monday." on a Tuesday.
- **Archived alerts and changes.** The client wants to see archived alerts and clearer information about what changed when a live alert is replaced.
- **Database visibility.** The client asked what happens in the database when alerts are created, made active, and archived. The table under "How Alerts Are Stored" answers this.
:::

---

### Story 4.1: View Warning Messages

- **GitHub Issue Title:** `[Alerts] View Warning Messages` (#55)

- **Iteration:** <span class="it2-modified">`2` (Moved from Iteration 1)</span>

- **Story Points:** `2`

- **Status:** <span class="it2-added">Done (PR #100)</span>

- **Story Description:** As an operator, I want to see every saved warning for my tenant in one list, so that I know what riders are being told before I change anything.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* The user is logged in.
  - *Trigger:* The user clicks **Service Alerts** in the sidebar, which opens `/fares/alerts`.

- **Actions / Postconditions:**
  - *Action:* If no tenant is selected, the page asks the user to pick one in the top navbar, the same way the fare grid does. Once a tenant is selected, the app loads that tenant's warnings.
  - *Postcondition:* A table shows each warning's message, routes, ticket, trigger, Spanish status, and alert status.

#### Acceptance Criteria

1. Only logged-in users can open the page.
2. <span class="it2-modified">With no tenant selected, the page shows "No Tenant Selected: Choose an active tenant above to begin configuration." and no table.</span>
3. Only warnings for the active tenant (`tenantCode`) are listed.
4. Each row shows the message, every route it covers (for example, `PIER11 → HOB14ST`), the ticket (**All tickets** if blank), the trigger (Weekends, Outside service hours, or Before the start date with the date), and the status.
5. Warnings with no Spanish message show a **Missing** badge.
6. <span class="it2-added">Status shows as a coloured badge (Draft, Active, Archived). A draft that replaces a live alert shows "Replaces #N".</span>
7. <span class="it2-added">The list can be filtered by message text, status, and route. Filters use the URL, so a filtered view can be bookmarked.</span>
8. If there are no warnings, the page says "No alerts yet".

#### Test Scenarios

- **Manual (Success):** Log in, pick NY Waterway in the navbar, open Service Alerts, and check that saved warnings show with their routes, ticket, trigger, and status. <span class="it2-added">Filter by **Draft** and check that only drafts show.</span>
- **Manual (Failure / Edge Case):** Log out and go to `/fares/alerts`. Check that you are sent to the login page. Log in without picking a tenant and check that the "No Tenant Selected" message shows instead of the table.
- **Automated:** <span class="it2-modified">`AlertControllerTests` (`@SpringBootTest` with `MockMvc`, since the selected tenant lives in the session): `loggedOutUserIsSentToLogin`, `noTenantShowsSelectTenantPrompt`, and `selectedTenantListsItsAlerts`.</span>

---

### Story 4.2: Create a Bilingual Warning

- **GitHub Issue Title:** `[Alerts] Create a Bilingual Warning` (#56)

- **Iteration:** <span class="it2-modified">`2` (Moved from Iteration 1)</span>

- **Story Points:** `3`

- **Status:** <span class="it2-added">Done (PR #105)</span>

- **Story Description:** As an operator, I want to write a warning for one or more routes in English and Spanish and choose when it shows, so that riders know when their ticket will work before they pay.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* The user is logged in, a tenant is selected, and the user is on the alerts page.
  - *Trigger:* The user clicks **New alert**, which opens `/fares/alerts/new`.

- **Actions / Postconditions:**
  - *Action:* <span class="it2-modified">The user ticks one or more routes (grouped by origin terminal), optionally enters a ticket code (blank means all tickets), picks when the warning shows (**Weekends**, **Outside service hours**, or **Before the start date**, with a date), and writes the English and Spanish messages.</span>
  - *Postcondition:* <span class="it2-modified">On **Save draft**, the warning is stored as a **Draft** for the active tenant and the user goes back to the list with "Alert saved as a draft." If something is wrong, the form comes back with the user's input kept and a list of errors at the top.</span>

#### Acceptance Criteria

1. At least one route, a trigger, and the English message are required. The ticket code is optional.
2. If "Before the start date" is picked, a start date is required.
3. Messages are 200 characters or fewer. A live counter under each box shows how many are used (for example, "145 / 200"), and the box stops accepting text at 200.
4. The Spanish message is optional, but the list flags warnings without one.
5. <span class="it2-added">Every save creates a Draft. There is no status dropdown.</span>
6. Nothing is saved when validation fails.

#### Test Scenarios

- **Manual (Success):** Tick `PIER11 to HOB14ST`, leave the ticket blank, pick **Weekends**, and enter "This route does not run on weekends." and "Esta ruta no opera los fines de semana." Save and check that the alert shows in the list as a Draft with "Alert saved as a draft."
- **Manual (Failure / Edge Case):** Leave the English message blank and save. Check that "English message is required." shows and nothing is saved. Pick **Before the start date** with no date and check that "Start date is required." shows. Save with no route ticked and check that "Select at least one route." shows.
- **Automated:** <span class="it2-modified">`AlertControllerTests`: `newFormRenders`, `validAlertIsSavedAsDraft`, `missingEnglishMessageShowsError`, and `messageOver200CharactersIsRejected`.</span>

---

### Story 4.3: Edit, Delete, or Archive a Warning

- **GitHub Issue Title:** `[Alerts] Edit or Delete a Warning` (#57)

- **Iteration:** `2`

- **Story Points:** `2`

- **Status:** <span class="it2-added">Done (PR #105 and PR #109)</span>

- **Story Description:** As an operator, I want to edit a warning, so that I can fix wording without starting over. <span class="it2-modified">As a Caracal administrator, I want to delete drafts and archive live warnings, so that the list stays clean and riders stop seeing old warnings.</span>

- **Actors / Personas:** Authenticated Operator (`OPERATOR`) can edit <span class="it2-added">and make a draft active</span>. Caracal Administrator (`CLIENT_SERVICES`) can edit, delete, <span class="it2-added">and archive</span>.

- **Preconditions & Triggers:**
  - *Precondition:* At least one warning exists for the selected tenant.
  - *Trigger:* The user clicks **Edit**, **Delete**, <span class="it2-added">**Archive**, or **Make active**</span> on a warning.

- **Actions / Postconditions:**
  - *Action:* **Edit** opens the Story 4.2 form with the current values filled in. **Delete**, <span class="it2-added">**Archive**, and **Make active**</span> ask for confirmation.
  - *Postcondition:* <span class="it2-modified">Saving an edit updates the draft. Editing a live alert creates a draft copy that replaces it later, and the live alert stays as it is. Deleting removes a draft from the database. Archiving moves a live alert to Archived. Making a draft active turns it on, and if it is a copy, the alert it replaces is archived in the same step.</span>

#### Acceptance Criteria

1. Every logged-in user sees **Edit** on Draft and Active warnings. <span class="it2-added">Archived warnings can't be edited.</span>
2. <span class="it2-modified">Only `CLIENT_SERVICES` users see **Delete** (on drafts) and **Archive** (on active alerts).</span>
3. <span class="it2-modified">Both endpoints (`/fares/alerts/*/delete` and `/fares/alerts/*/archive`) are restricted to `CLIENT_SERVICES` in `SecurityConfig`, so hiding the buttons isn't the only protection. An `OPERATOR` who sends the request directly gets 403.</span>
4. Delete and Archive ask "Are you sure?" first.
5. Edits follow the same rules as Story 4.2.
6. <span class="it2-added">If a live alert already has a pending draft, **Edit** opens that draft instead of the old live values.</span>
7. <span class="it2-added">Opening a warning that doesn't exist, or an archived one, goes back to the list with an error message.</span>
8. <span class="it2-added">**Make active** only works on drafts. Trying it on an active alert shows an error and changes nothing.</span>

#### Test Scenarios

- **Manual (Success):** As `operator`, change a draft's message and save. Check that the list shows the new text. As `admin`, delete a draft and confirm. Check that it's gone. <span class="it2-added">Edit an active alert, save, then click **Make active** on the copy. Check that the copy is Active and the old alert is Archived.</span>
- **Manual (Failure / Edge Case):** As `operator`, check that there are no Delete or Archive buttons, and that sending the delete request directly returns 403. Open `/fares/alerts/9999/edit` and check that you're sent back to the list with "That alert no longer exists."
- **Automated:** <span class="it2-modified">`AlertControllerTests`: `operatorCannotDelete` (403, the alert still exists), `clientServicesCanDeleteDraft`, `clientServicesCanArchiveActiveAlert`, `makeActiveTurnsDraftActive`, `activatingReplacementDraftArchivesOldAlert`, and `activatingActiveAlertShowsError`.</span>

---

### Story 4.4: Configurable Alert Schedule (Days, Hours, Holidays)

- **GitHub Issue Title:** `[Alerts] Show When the Ticket Becomes Valid` (#58)

- **Iteration:** <span class="it2-modified">`3` (Moved from Iteration 2)</span>

- **Story Points:** <span class="it2-modified">`5` (was 3, scope grew after client feedback)</span>

- **Status:** <span class="it2-modified">Not started. Rewritten after the Nov 16 client meeting. The `RouteSchedule` table for inactive days exists. We still need the confirmed service hours from the client, since the sheet lists 12-hour times without AM/PM.</span>

- **Story Description:** <span class="it2-modified">As an operator, I want to choose exactly when a warning shows (days of the week, service hours, and holidays), so that riders who buy a ticket they can't use right now are always told when it will work, without the sale being blocked.</span>

- **Actors / Personas:** Riders see the result. Authenticated Operator (`OPERATOR`) and Caracal Administrator (`CLIENT_SERVICES`) set up the schedule.

- **Preconditions & Triggers:**
  - *Precondition:* The user is creating or editing a warning (Story 4.2 form).
  - *Trigger:* A rider picks a ticket on a day or at a time that matches the warning's schedule.

- **Actions / Postconditions:**
  - *Action:* <span class="it2-modified">The "Weekends" trigger is replaced by a **When to show** section: day-of-week checkboxes (with Weekends and Weekdays presets), a service-hours range, a "show on holidays" option, and the existing start date.</span>
  - *Postcondition:* The warning shows whenever any rule matches. The rider can still buy. The message names the next time the ticket is valid, for example "Valid Monday", "Next ferry 6:00 AM", or "Valid from December 1".

#### Acceptance Criteria

1. <span class="it2-modified">**Days:** The operator ticks the days a route does not run. A route that runs Monday, Wednesday, and Friday shows the warning on Tuesday, Thursday, Saturday, and Sunday.</span>
2. **Service hours:** Outside the route's service hours, the warning names the next departure time.
3. <span class="it2-added">**Holidays:** The warning shows on holidays from the tenant's holiday list (one-time and recurring dates).</span>
4. **Before the start date:** Before the start date, the warning names that date.
5. <span class="it2-added">The warning never blocks the purchase. The rider can always continue and buy the ticket for later.</span>
6. When the ticket is usable right now, no warning shows.
7. All checks use the terminal's time zone (`America/New_York`), not the server's.
8. If a route has no service hours saved yet, the plain warning still shows, just without a time.

#### Test Scenarios

- **Manual (Success):** Set a route to run Monday, Wednesday, and Friday. Check on a Tuesday that the warning says "Not scheduled today. Your ticket is valid Wednesday." Check the December promo ticket on Oct 30 and check that it says "Valid from December 1".
- **Manual (Failure / Edge Case):** Check a route with no service hours saved and check that the plain warning shows without a time. Check Sunday at 11:59 PM New York time and check that the warning still shows, even though it's already Monday on the server's UTC clock. <span class="it2-added">Save a schedule with no days, hours, holidays, or start date and check that "Choose at least one rule for when this alert shows." appears.</span>
- **Automated:** JUnit service test for `nextValidTime()` with Friday night, Saturday, Sunday 11:59 PM, Monday morning, Wednesday 1 PM, <span class="it2-added">a Tuesday on a Mon/Wed/Fri route, Dec 25 as a recurring holiday,</span> a date before a promo start, and a route with no hours saved.

---

### Story 4.5: Clear Button Labels on the Warning Popup

- **GitHub Issue Title:** `[Alerts] Clear Button Labels on the Warning Popup` (#59)

- **Iteration:** <span class="it2-modified">`2`, finished in `3`</span>

- **Story Points:** `2`

- **Status:** <span class="it2-added">Partly done. The four label fields are on the form and saved with a 20 character limit. Suggested labels and blank-label defaults are moved to Iteration 3.</span>

- **Story Description:** As an operator, I want to replace the generic "Confirm" and "Dismiss" buttons with clearer labels, so that riders know what each button does.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* The user is creating or editing a warning.
  - *Trigger:* The user types in the button label fields.

- **Actions / Postconditions:**
  - *Action:* The user sets a continue label and a cancel label, each in English and Spanish.
  - *Postcondition:* The labels are saved with the warning.

#### Acceptance Criteria

1. Each warning has a continue label and a cancel label, in English and Spanish. <span class="it2-added">(Done)</span>
2. Labels are 20 characters or fewer so they fit on the TVM button. <span class="it2-added">(Done)</span>
3. The form suggests a continue label based on the warning type: "Buy for [Day]" for day and service-hour warnings, and "Buy now" for promo warnings. <span class="it2-modified">(Iteration 3)</span>
4. Blank labels fall back to "Continue" / "Continuar" and "Cancel" / "Cancelar". <span class="it2-modified">(Iteration 3)</span>

#### Test Scenarios

- **Manual (Success):** Create an alert with "Buy for Monday" and "Comprar para el lunes" as the continue labels. Edit it and check that the labels are still filled in.
- **Manual (Failure / Edge Case):** Enter a label longer than 20 characters and check that the box stops at 20. Sending a longer label directly shows "Label must be 20 characters or fewer."
- **Automated:** Label length is checked in `AlertService.save()`. A JUnit test for the suggested and default labels will be added in Iteration 3.

---

<span class="it2-removed">~~Story 4.6: Live TVM Preview (Removed per client feedback. The client asked us to drop the TVM kiosk canvas, so issue #60 (3 points) is closed and the preview code was removed in PR #100.)~~</span>

---

::: {.it2-added}
### How Alerts Are Stored

Every alert is one row in `service_notices`, and its routes are rows in `service_notice_routes`. Archived is a value in the `status` column, so archived alerts keep their full text and history.

| Action in the app | What changes in the database |
|---|---|
| Save draft | New row with `status = DRAFT`, `created_by`, `created_at`, plus one route row per ticked route |
| Edit a draft | Same row updated (text, routes, `updated_by`, `updated_at`) |
| Edit an active alert | New `DRAFT` row with `replaces_alert_id` pointing at the active one. The active row is not touched. |
| Make active | Row set to `ACTIVE` with `published_by` and `published_at`. If it replaces an alert, the old row is set to `ARCHIVED` (`archived_by`, `archived_at`) in the same transaction. |
| Archive | Row set to `ARCHIVED` with `archived_by` and `archived_at`. The row is kept. |
| Delete (drafts only) | Row and its route rows are removed |
:::

---

### Iteration 2 Progress (Story Points)

| Story | Points | Planned for | Status |
|---|---|---|---|
| 4.1 View Warning Messages | 2 | Iteration 1 | <span class="it2-modified">Done in Iteration 2</span> |
| 4.2 Create a Bilingual Warning | 3 | Iteration 1 | <span class="it2-modified">Done in Iteration 2</span> |
| 4.3 Edit, Delete, or Archive a Warning | 2 | Iteration 2 | <span class="it2-added">Done</span> |
| 4.4 Configurable Alert Schedule | <span class="it2-modified">5</span> | Iteration 2 | <span class="it2-modified">Moved to Iteration 3</span> |
| 4.5 Clear Button Labels | 2 | Iteration 2 | <span class="it2-modified">Partly done, rest in Iteration 3</span> |
| <span class="it2-removed">~~4.6 Live TVM Preview~~</span> | <span class="it2-removed">~~3~~</span> | Iteration 2 | <span class="it2-removed">Removed at client request</span> |

<span class="it2-added">Iteration 1 planned 5 points (4.1 and 4.2) and finished 0, because the TVM preview took that iteration's time before the client dropped it. Iteration 2 finished all 7 points of 4.1, 4.2, and 4.3, plus part of 4.5. Story 4.4 (5 points) and the rest of 4.5 move to Iteration 3, along with the new stories from client feedback listed in Future Iterations.</span>

---

#### UI Mockups <span class="it2-modified">(Updated for Iteration 2)</span>

**Alerts list (Stories 4.1 and 4.3)**

```text
+------------------------------------------------------------------------------------+
| Fare Config      |  Tenant: [NY Waterway v]                                        |
|------------------|-----------------------------------------------------------------|
| Fare Grid        |  Service Alerts                                    [+ New alert]|
| Availability     |  [Search message text   ] [All statuses v] [All routes v] [Filter]
| > Service Alerts |                                                                 |
| Review & Publish |  Message           Routes             Ticket   Trigger  ES  Status
|                  |  ---------------------------------------------------------------|
|                  |  No weekend serv.. PIER11 → HOB14ST   All      Weekends ok  Active
|                  |                                                  [Edit] [Archive*]
|                  |  Promo valid fr..  EDGEWTR → MIDTOWN  PROMO_D  Before   Missing Draft
|                  |                         Dec 1, 2026 [Edit] [Make active] [Delete*]
|                  |                                                                 |
|                  |  * Only Client Services sees Delete and Archive                 |
+------------------------------------------------------------------------------------+
```

**Alert form (Stories 4.2 and 4.5)**

```text
+-------------------------------------------------+  +------------------------------+
| New alert                                       |  | When it shows                |
| Saving stores a draft.                          |  | Trigger    [Weekends       v]|
|                                                 |  | Start date [mm/dd/yyyy     ] |
| English message *                               |  | Ticket code [blank = all   ] |
| [This route does not run on weekends.       ]   |  +------------------------------+
|                                     36 / 200    |  | Routes *                     |
| Spanish message                                 |  | PIER11                       |
| [Esta ruta no opera los fines de semana.    ]   |  |  [x] PIER11 to HOB14ST       |
|                                     39 / 200    |  | HOB14ST                      |
| Continue (EN) [Buy for Monday] (ES) [Comprar..] |  |  [ ] HOB14ST to PIER11       |
| Cancel   (EN) [Cancel        ] (ES) [Cancelar] |  +------------------------------+
|                                                 |
| [Save draft] [Cancel]                           |
+-------------------------------------------------+
```

::: {.it2-added}
**Planned "When to show" section (Story 4.4, Iteration 3)**

```text
+--------------------------------------------------+
| When to show                                     |
| Route does not run on:                           |
|  [ ] Mon [x] Tue [ ] Wed [x] Thu [ ] Fri          |
|  [x] Sat [x] Sun     (Weekends) (Weekdays)        |
| Service hours: [06:00 AM] to [09:30 PM]          |
| [x] Show on holidays (from the holiday list)     |
| [ ] Before start date [mm/dd/yyyy]               |
|                                                  |
| Next 7 days: Tue ■  Wed □  Thu ■  Fri □  Sat ■   |
|  ■ = alert shows   □ = no alert                  |
+--------------------------------------------------+
```
:::
