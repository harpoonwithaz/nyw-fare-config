## Epic 4: Pre-Purchase Service Alerts

When a rider picks a ticket that can't be used right now, the ticket machine (TVM) should show a short heads-up before they pay. The rider can still buy the ticket. The warning just makes sure they know when it will work.

Our client described three cases in our meetings so far:

- **Weekday-only routes:** On a Saturday, a rider picks Edgewater → Midtown and sees "This route doesn't run on weekends. Your ticket is valid Monday."
- **Rush-hour-only routes:** Some routes only run during weekday morning and evening rush hours, so riders outside those hours see a notice.
- **Advance-purchase promos:** A rider buys a December promo ticket on Oct 30 and sees "You can buy this now, but you can't use it until December 1."

These are **not** outage or maintenance messages. Every warning is shown in English and Spanish, like the client's current TVM screens. Epic 3 (Availability) decides when a ticket is sold, and Epic 4 decides what the rider is told.

**[Updated in Iteration 2]** Alerts are a proof of concept. We store them in our own PostgreSQL database (`service_notices` table) and do not send them to Caracal, so they are kept separate from Epic 5 (Review and Publish), which only covers changes to the Caracal catalog. Routes use the platform site codes (for example `PIER11:HOB14ST`).

**Note:** The character limits below (200 for messages, 20 for button labels) are still our best guess. We'll confirm them with the client.

### Changes Since Iteration 1

- **Removed Story 4.6 (Live TVM Preview).** The client asked us to drop the TVM preview. The preview code was removed in PR #100.
- **Alert lifecycle added.** Every save creates a **Draft**. Alerts can be **Draft**, **Active**, or **Archived**. Editing a live alert creates a draft copy instead of changing the live one. Since alerts are not published through Epic 5, a **Make active** button on draft rows turns a draft on, and a draft copy replaces (and archives) the alert it was copied from.
- **Story 4.3 changed** from "administrators delete warnings" to "administrators delete drafts and archive active alerts". Archived alerts can't be edited.
- **Story 4.2 changed:** one alert can cover several routes, there is no status dropdown, and errors show as a list at the top of the form.
- **Story 4.4 moved to Iteration 3.** The `RouteSchedule` table exists, but the next-valid-time logic isn't built yet.
- **Story 4.5 partly done.** Button labels are saved with a 20 character limit. Suggested and default labels are moved to Iteration 3.
- **Ownership:** Harendeep built the backend (`ServiceAlert`, `RouteSchedule`, `AlertService`, validation). Tejas built the pages, controller, and controller tests.

---

### Story 4.1: View Warning Messages

- **GitHub Issue Title:** `[Alerts] View Warning Messages` (#55)

- **Iteration:** `1`, finished in `2`

- **Story Points:** `2`

- **Status [Updated in Iteration 2]:** Done (PR #100)

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
2. **[Updated]** With no tenant selected, the page shows "No Tenant Selected: Choose an active tenant above to begin configuration." and no table.
3. Only warnings for the active tenant (`tenantCode`) are listed.
4. Each row shows the message, every route it covers (for example, `PIER11 → HOB14ST`), the ticket (**All tickets** if blank), the trigger (Weekends, Outside service hours, or Before the start date with the date), and the status.
5. Warnings with no Spanish message show a **Missing** badge.
6. **[Updated]** Status shows as a coloured badge (Draft, Active, Archived). A draft that replaces a live alert shows "Replaces #N".
7. **[Updated]** The list can be filtered by message text, status, and route. Filters use the URL, so a filtered view can be bookmarked.
8. If there are no warnings, the page says "No alerts yet".

#### Test Scenarios

- **Manual (Success):** Log in, pick NY Waterway in the navbar, open Service Alerts, and check that saved warnings show with their routes, ticket, trigger, and status. Filter by **Draft** and check that only drafts show.
- **Manual (Failure / Edge Case):** Log out and go to `/fares/alerts`. Check that you are sent to the login page. Log in without picking a tenant and check that the "No Tenant Selected" message shows instead of the table.
- **Automated [Updated]:** `AlertControllerTests` (`@SpringBootTest` with `MockMvc`, since the selected tenant lives in the session): `loggedOutUserIsSentToLogin`, `noTenantShowsSelectTenantPrompt`, and `selectedTenantListsItsAlerts`.

---

### Story 4.2: Create a Bilingual Warning

- **GitHub Issue Title:** `[Alerts] Create a Bilingual Warning` (#56)

- **Iteration:** `1`, finished in `2`

- **Story Points:** `3`

- **Status [Updated in Iteration 2]:** Done (PR #105)

- **Story Description:** As an operator, I want to write a warning for one or more routes in English and Spanish and choose when it shows, so that riders know when their ticket will work before they pay.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* The user is logged in, a tenant is selected, and the user is on the alerts page.
  - *Trigger:* The user clicks **New alert**, which opens `/fares/alerts/new`.

- **Actions / Postconditions:**
  - *Action:* **[Updated]** The user ticks one or more routes (grouped by origin terminal), optionally enters a ticket code (blank means all tickets), picks when the warning shows (**Weekends**, **Outside service hours**, or **Before the start date**, with a date), and writes the English and Spanish messages.
  - *Postcondition:* **[Updated]** On **Save draft**, the warning is stored as a **Draft** for the active tenant and the user goes back to the list with "Alert saved as a draft." If something is wrong, the form comes back with the user's input kept and a list of errors at the top.

#### Acceptance Criteria

1. At least one route, a trigger, and the English message are required. The ticket code is optional.
2. If "Before the start date" is picked, a start date is required.
3. Messages are 200 characters or fewer. A live counter under each box shows how many are used (for example, "145 / 200"), and the box stops accepting text at 200.
4. The Spanish message is optional, but the list flags warnings without one.
5. **[Updated]** Every save creates a Draft. There is no status dropdown.
6. Nothing is saved when validation fails.

#### Test Scenarios

- **Manual (Success):** Tick `PIER11 to HOB14ST`, leave the ticket blank, pick **Weekends**, and enter "This route does not run on weekends." and "Esta ruta no opera los fines de semana." Save and check that the alert shows in the list as a Draft with "Alert saved as a draft."
- **Manual (Failure / Edge Case):** Leave the English message blank and save. Check that "English message is required." shows and nothing is saved. Pick **Before the start date** with no date and check that "Start date is required." shows. Save with no route ticked and check that "Select at least one route." shows.
- **Automated [Updated]:** `AlertControllerTests`: `newFormRenders`, `validAlertIsSavedAsDraft`, `missingEnglishMessageShowsError`, and `messageOver200CharactersIsRejected`.

---

### Story 4.3: Edit, Delete, or Archive a Warning

- **GitHub Issue Title:** `[Alerts] Edit or Delete a Warning` (#57)

- **Iteration:** `2`

- **Story Points:** `2`

- **Status [Updated in Iteration 2]:** Done (PR #105)

- **Story Description:** As an operator, I want to edit a warning, so that I can fix wording without starting over. **[Updated]** As a Caracal administrator, I want to delete drafts and archive live warnings, so that the list stays clean and riders stop seeing old warnings.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`) can edit. Caracal Administrator (`CLIENT_SERVICES`) can edit, delete, and archive.

- **Preconditions & Triggers:**
  - *Precondition:* At least one warning exists for the selected tenant.
  - *Trigger:* The user clicks **Edit**, **Delete**, or **Archive** on a warning.

- **Actions / Postconditions:**
  - *Action:* **Edit** opens the Story 4.2 form with the current values filled in. **Delete** and **Archive** ask for confirmation.
  - *Postcondition:* **[Updated]** Saving an edit updates the draft. Editing a live alert creates a draft copy that replaces it later, and the live alert stays as it is. Deleting removes a draft from the database. Archiving moves a live alert to Archived.

#### Acceptance Criteria

1. Every logged-in user sees **Edit** on Draft and Active warnings. Archived warnings can't be edited.
2. **[Updated]** Only `CLIENT_SERVICES` users see **Delete** (on drafts) and **Archive** (on active alerts).
3. Both endpoints are restricted to `CLIENT_SERVICES` in `SecurityConfig`, so hiding the buttons isn't the only protection.
4. Delete and Archive ask "Are you sure?" first.
5. Edits follow the same rules as Story 4.2.
6. **[Updated]** If a live alert already has a pending draft, **Edit** opens that draft instead of the old live values.
7. **[Updated]** Opening a warning that doesn't exist, or an archived one, goes back to the list with an error message.

#### Test Scenarios

- **Manual (Success):** As `operator`, change a draft's message and save. Check that the list shows the new text. As `admin`, delete a draft and confirm. Check that it's gone.
- **Manual (Failure / Edge Case):** As `operator`, check that there are no Delete or Archive buttons, and that sending the delete request directly returns 403. Open `/fares/alerts/9999/edit` and check that you're sent back to the list with "That alert no longer exists."
- **Automated [Updated]:** `AlertControllerTests`: `operatorCannotDelete` (403, the alert still exists), `clientServicesCanDeleteDraft`, and `clientServicesCanArchiveActiveAlert`.

---

### Story 4.4: Show When the Ticket Becomes Valid

- **GitHub Issue Title:** `[Alerts] Show When the Ticket Becomes Valid` (#58)

- **Iteration:** **[Updated]** moved from `2` to `3`

- **Story Points:** `3`

- **Status [Updated in Iteration 2]:** Not started. The `RouteSchedule` table for service days exists. We still need to confirm service hours with the client, since the sheet lists 12-hour times without AM/PM.

- **Story Description:** As a rider, I want the warning to tell me exactly when my ticket will work, so that I'm not confused about whether I can ride today.

- **Actors / Personas:** Riders see the result. Authenticated Operator (`OPERATOR`) and Caracal Administrator (`CLIENT_SERVICES`) set up the route service hours.

- **Preconditions & Triggers:**
  - *Precondition:* The route's service days and AM/PM rush-hour windows are saved in our local database. Caracal's backend has no endpoint for service hours, so we store them alongside the warnings.
  - *Trigger:* A rider picks a ticket that has a warning, at a time when the warning applies.

- **Actions / Postconditions:**
  - *Action:* The app works out the next time the ticket is valid.
  - *Postcondition:* The warning text includes it, for example "Valid Monday", "Next service 4:30 PM", or "Valid from December 1".

#### Acceptance Criteria

1. **Weekends:** On Saturday or Sunday, the warning names the next weekday.
2. **Outside service hours:** Outside the route's AM/PM service windows, the warning names the next service time.
3. **Before the start date:** Before the start date, the warning names that date.
4. When the ticket is usable right now, no warning shows.
5. All checks use the terminal's time zone (`America/New_York`), not the server's.
6. If a route has no service hours saved yet, the plain warning still shows, just without a time.

#### Test Scenarios

- **Manual (Success):** Check Edgewater → Midtown on Saturday at 10 AM and check that the warning says "Valid Monday". Check the December promo ticket on Oct 30 and check that it says "Valid from December 1".
- **Manual (Failure / Edge Case):** Check a route with no service hours saved and check that the plain warning shows without a time. Check Sunday at 11:59 PM New York time and check that the warning still shows, even though it's already Monday on the server's UTC clock.
- **Automated:** JUnit service test for `nextValidTime()` with Friday night, Saturday, Sunday 11:59 PM, Monday morning, Wednesday 1 PM, a date before a promo start, and a route with no hours saved.

---

### Story 4.5: Clear Button Labels on the Warning Popup

- **GitHub Issue Title:** `[Alerts] Clear Button Labels on the Warning Popup` (#59)

- **Iteration:** `2`, finished in `3`

- **Story Points:** `2`

- **Status [Updated in Iteration 2]:** Partly done. The four label fields are on the form and saved with a 20 character limit. Suggested labels and blank-label defaults are moved to Iteration 3.

- **Story Description:** As an operator, I want to replace the generic "Confirm" and "Dismiss" buttons with clearer labels, so that riders know what each button does.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* The user is creating or editing a warning.
  - *Trigger:* The user types in the button label fields.

- **Actions / Postconditions:**
  - *Action:* The user sets a continue label and a cancel label, each in English and Spanish.
  - *Postcondition:* The labels are saved with the warning.

#### Acceptance Criteria

1. Each warning has a continue label and a cancel label, in English and Spanish. (Done)
2. Labels are 20 characters or fewer so they fit on the TVM button. (Done)
3. The form suggests a continue label based on the warning type: "Buy for [Day]" for weekend and service-hour warnings, and "Buy now" for promo warnings. (Iteration 3)
4. Blank labels fall back to "Continue" / "Continuar" and "Cancel" / "Cancelar". (Iteration 3)

#### Test Scenarios

- **Manual (Success):** Create an alert with "Buy for Monday" and "Comprar para el lunes" as the continue labels. Edit it and check that the labels are still filled in.
- **Manual (Failure / Edge Case):** Enter a label longer than 20 characters and check that the box stops at 20. Sending a longer label directly shows "Label must be 20 characters or fewer."
- **Automated:** Label length is checked in `AlertService.save()`. A JUnit test for the suggested and default labels will be added in Iteration 3.

---

### ~~Story 4.6: Live TVM Preview~~ [Removed in Iteration 2]

The client asked us to drop the TVM preview, so this story (#60, 3 points) is closed and the preview code was removed in PR #100.

---

### Iteration 2 Progress (Story Points)

| Story | Points | Planned for | Status |
|---|---|---|---|
| 4.1 View Warning Messages | 2 | Iteration 1 | Done in Iteration 2 |
| 4.2 Create a Bilingual Warning | 3 | Iteration 1 | Done in Iteration 2 |
| 4.3 Edit, Delete, or Archive a Warning | 2 | Iteration 2 | Done |
| 4.4 Show When the Ticket Becomes Valid | 3 | Iteration 2 | Moved to Iteration 3 |
| 4.5 Clear Button Labels | 2 | Iteration 2 | Partly done, rest in Iteration 3 |
| 4.6 Live TVM Preview | 3 | Iteration 2 | Removed at client request |

Iteration 1 planned 5 points (4.1 and 4.2) and finished 0, because the TVM preview took that iteration's time before the client dropped it. Iteration 2 finished all 7 points of 4.1, 4.2, and 4.3, plus part of 4.5. That leaves 5 points (4.4 and the rest of 4.5) for Iteration 3.

**Next iteration:**
- **Languages from the tenant.** Messages are fixed to English and Spanish right now. Caracal's tenant details list each tenant's supported locales (`en-US` and `es-US` for NY Waterway, and Metra also has `pl-PL` and `zh-CN`), so the form should offer one message box per supported locale.
- **Real route list.** The route picker still uses two sample routes. It should list every valid NY Waterway route by platform site code.
- **Open question for the client:** Alerts only live in our database for now. Caracal has an API function for updating alert messages, so we want to ask whether they'd like a later hookup to it.

---

#### UI Mockups [Updated in Iteration 2]

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
|                  |                                   Dec 1, 2026  [Edit] [Delete*]
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
