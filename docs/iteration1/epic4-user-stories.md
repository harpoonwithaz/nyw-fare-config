## Epic 4: Pre-Purchase Warnings and TVM Preview

When a rider picks a ticket that can't be used right now, the ticket machine (TVM) should show a short heads-up before they pay. The rider can still buy the ticket. The warning just makes sure they know when it will work.

Our client described three cases in our meetings so far:

- **Weekday-only routes:** On a Saturday, a rider picks Edgewater → Midtown and sees "This route doesn't run on weekends. Your ticket is valid Monday."
- **Rush-hour-only routes:** Some routes only run during weekday morning and evening rush hours, so riders outside those hours see a notice.
- **Advance-purchase promos:** A rider buys a December promo ticket on Oct 30 and sees "You can buy this now, but you can't use it until December 1."

These are **not** outage or maintenance messages. Every warning is shown in English and Spanish, like the client's current TVM screens. Caracal's platform API has no endpoints for warnings or route service hours, so this epic stores both in our own database. Epic 3 (Availability) decides when a ticket is sold, and Epic 4 decides what the rider is told.

**Note:** The character limits below (200 for messages, 20 for button labels) are our best guess. We'll confirm the character limit at a client sync.

---

### Story 4.1: View Warning Messages

- **GitHub Issue Title:** `[Alerts] View Warning Messages`

- **Iteration:** `1`

- **Story Points:** `2`

- **Story Description:** As an operator, I want to see every saved warning for my tenant in one list, so that I know what riders are being told before I change anything.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* The user is logged in and a tenant is selected (`NYW_SANDBOX` for now).
  - *Trigger:* The user clicks **Service Alerts & TVM Canvas** in the sidebar, which opens `/fares/alerts`.

- **Actions / Postconditions:**
  - *Action:* The app loads the warnings saved for the active tenant.
  - *Postcondition:* A table shows each warning's route, which tickets it applies to, when it shows, and the English and Spanish messages.

#### Acceptance Criteria

1. Only logged-in users can open the page.
2. Only warnings for the active tenant (`tenantCode`) are listed.
3. Each row shows the route (for example, Edgewater → Midtown), whether it applies to **All tickets** or one specific ticket (for example, Adult One-Way), when it shows (Weekends, Outside service hours, or Before a start date), and both messages.
4. Warnings with no Spanish message show a **Missing Spanish** badge.
5. If there are no warnings, the page says "No warnings yet" and shows a **New warning** button.

#### Test Scenarios

- **Manual (Success):** Log in as `operator`, open Service Alerts, and check that saved warnings are listed with their route, ticket scope, and messages.
- **Manual (Failure / Edge Case):** Log out and go to `/fares/alerts`. Check that you are sent to the login page. Then log in with no warnings saved and check that "No warnings yet" shows.
- **Automated (Unit / Slice / Integration):** `@WebMvcTest` with `MockMvc`: a logged-out `GET /fares/alerts` redirects to `/login`, and a logged-in request returns HTTP 200, the `alerts/index` view, and only warnings for `NYW_SANDBOX`.

#### Actionable Sub-tasks

- [ ] Create the `ServiceAlert` entity (with `tenantCode`) and `ServiceAlertRepository`.
- [ ] Add `GET /fares/alerts` to `AlertController`, filtered by the active tenant.
- [ ] Replace the placeholder in `templates/alerts/index.html` with the real table.
- [ ] Add the ticket scope column and the Missing Spanish badge.
- [ ] Write `MockMvc` tests for logged-in, logged-out, and tenant filtering.

---

### Story 4.2: Create a Bilingual Warning

- **GitHub Issue Title:** `[Alerts] Create a Bilingual Warning`

- **Iteration:** `1`

- **Story Points:** `3`

- **Story Description:** As an operator, I want to write a warning for a route in English and Spanish and choose when it shows, so that riders know when their ticket will work before they pay.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* The user is logged in and on the alerts page.
  - *Trigger:* The user clicks **New warning**, which opens `/fares/alerts/new`.

- **Actions / Postconditions:**
  - *Action:* The user picks an origin terminal and a destination, optionally picks one ticket (it defaults to **All tickets on route**), picks when the warning shows (**Weekends**, **Outside service hours**, or **Before a start date**, with a date), and writes the English and Spanish messages.
  - *Postcondition:* On **Save**, the warning is stored for the active tenant and the user goes back to the list with a "Warning saved" message. If something is wrong, the form comes back with the user's input kept and an error under each problem field.

#### Acceptance Criteria

1. Origin, destination, "show when", and English message are required. Ticket is optional and defaults to **All tickets on route**.
2. If "Before a start date" is picked, a start date is required.
3. Messages are 200 characters or fewer, and a live counter under each box shows how many are used (for example, "145/200").
4. The Spanish message is optional for now, but the list flags warnings without one.
5. Nothing is saved when validation fails.

#### Test Scenarios

- **Manual (Success):** Pick Edgewater → Midtown, leave ticket as **All tickets on route**, and pick **Weekends**. Enter "SERVICE NOTICE: No ferry service on this route on weekends. Your ticket is valid Monday." and "AVISO DE SERVICIO: No hay servicio de ferry en esta ruta los fines de semana. Su boleto es válido el lunes." Save and check that the warning appears with "Warning saved".
- **Manual (Success):** Pick a December promo ticket and **Before a start date** with Dec 1. Save and check that the list shows "Before Dec 1".
- **Manual (Failure / Edge Case):** Leave the English message blank and save. Check that "English message is required" shows and nothing is saved. Pick **Before a start date** with no date and check that "Start date is required" shows.
- **Manual (Failure / Edge Case):** Type past 200 characters. Check that the counter turns red and "Message must be 200 characters or fewer" shows on save.
- **Automated (Unit / Slice / Integration):** `@WebMvcTest` with `MockMvc`: a valid `POST /fares/alerts` redirects to the list and saves one warning, and a `POST` with a blank English message returns the form with a field error on `messageEn`.

#### Actionable Sub-tasks

- [ ] Create `AlertForm` with Bean Validation (`@NotBlank`, `@Size(max = 200)`).
- [ ] Add `GET /fares/alerts/new` and `POST /fares/alerts`.
- [ ] Create `AlertService` to save warnings with the active `tenantCode`.
- [ ] Build `templates/alerts/form.html` with terminal, destination, and optional ticket dropdowns.
- [ ] Write `alert-form.js` for the live character counters.
- [ ] Write `MockMvc` tests for valid and invalid input.

---

### Story 4.3: Edit or Delete a Warning

- **GitHub Issue Title:** `[Alerts] Edit or Delete a Warning`

- **Iteration:** `2`

- **Story Points:** `2`

- **Story Description:** As an operator, I want to edit a warning, so that I can fix wording without starting over. As a Caracal administrator, I want to delete old warnings, so that the list stays clean.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`) can edit. Caracal Administrator (`CLIENT_SERVICES`) can edit and delete.

- **Preconditions & Triggers:**
  - *Precondition:* At least one warning exists.
  - *Trigger:* The user clicks **Edit** or **Delete** on a warning.

- **Actions / Postconditions:**
  - *Action:* **Edit** opens the Story 4.2 form with the current values filled in. **Delete** asks for confirmation.
  - *Postcondition:* Saving updates that warning. Confirming a delete removes it from the database.

#### Acceptance Criteria

1. Every logged-in user sees **Edit**. Only `CLIENT_SERVICES` users see **Delete**.
2. The delete endpoint is restricted to `CLIENT_SERVICES` in `SecurityConfig` (the same way `/admin/**` is today), so hiding the button isn't the only protection.
3. Delete asks "Are you sure?" first.
4. Edits follow the same rules as Story 4.2.
5. Opening a warning that doesn't exist shows the "not found" page.

#### Test Scenarios

- **Manual (Success):** As `operator`, change a warning's message and save. Check that the list shows the new text. As `admin`, delete a warning and confirm. Check that it's gone.
- **Manual (Failure / Edge Case):** As `operator`, check that there's no Delete button, and that sending the delete request directly shows the 403 page. Open `/fares/alerts/9999/edit` and check that the not found page shows.
- **Automated (Unit / Slice / Integration):** `@WebMvcTest` with `MockMvc`: a delete as `OPERATOR` returns 403 and the warning still exists, and a delete as `CLIENT_SERVICES` removes it.

#### Actionable Sub-tasks

- [ ] Add `GET /fares/alerts/{id}/edit` and `POST /fares/alerts/{id}`.
- [ ] Add `POST /fares/alerts/{id}/delete` and a `hasRole("CLIENT_SERVICES")` rule for it in `SecurityConfig`.
- [ ] Hide the Delete button for operators in the template.
- [ ] Return the not found page for missing warnings.
- [ ] Write permission tests for both roles.

---

### Story 4.4: Show When the Ticket Becomes Valid

- **GitHub Issue Title:** `[Alerts] Show When the Ticket Becomes Valid`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As a rider, I want the warning to tell me exactly when my ticket will work, so that I'm not confused about whether I can ride today.

- **Actors / Personas:** Riders see the result. Authenticated Operator (`OPERATOR`) and Caracal Administrator (`CLIENT_SERVICES`) set up the route service hours.

- **Preconditions & Triggers:**
  - *Precondition:* The route's service days and AM/PM rush-hour windows are saved in our local database. Caracal's backend has no endpoint for service hours, so we store them alongside the warnings.
  - *Trigger:* A rider picks a ticket that has a warning, at a time when the warning applies (or an operator previews that time in Story 4.6).

- **Actions / Postconditions:**
  - *Action:* The app works out the next time the ticket is valid.
  - *Postcondition:* The warning text includes it, for example "Valid Monday", "Next service 4:30 PM", or "Valid from December 1".

#### Acceptance Criteria

1. **Weekends:** On Saturday or Sunday, the warning names the next weekday.
2. **Outside service hours:** Outside the route's AM/PM service windows, the warning names the next service time.
3. **Before a start date:** Before the start date, the warning names that date.
4. When the ticket is usable right now, no warning shows.
5. All checks use the terminal's time zone (`America/New_York`), not the server's.
6. If a route has no service hours saved yet, the plain warning still shows, just without a time.

#### Test Scenarios

- **Manual (Success):** Preview Edgewater → Midtown on Saturday at 10 AM and check that the warning says "Valid Monday". Preview a rush-hour-only route on Wednesday at 1 PM and check that it gives the next service time. Preview the December promo ticket on Oct 30 and check that it says "Valid from December 1".
- **Manual (Failure / Edge Case):** Preview a route with no service hours saved and check that the plain warning shows without a time instead of crashing. Preview on Sunday at 11:59 PM New York time and check that the warning still shows, even though it's already Monday on the server's UTC clock.
- **Automated (Unit / Slice / Integration):** JUnit service test for `nextValidTime()` with Friday night, Saturday, Sunday 11:59 PM, Monday morning, Wednesday 1 PM, a date before a promo start, and a route with no hours saved.

#### Actionable Sub-tasks

- [ ] Create the `RouteSchedule` entity (service days, AM and PM windows) and `RouteScheduleRepository`.
- [ ] Agree with Athan (Epic 3) so availability rules and service hours don't overlap.
- [ ] Write `nextValidTime()` in `AlertService` using the terminal's time zone.
- [ ] Add the computed time to the warning text.
- [ ] Write JUnit tests for the edge cases.

---

### Story 4.5: Clear Button Labels on the Warning Popup

- **GitHub Issue Title:** `[Alerts] Clear Button Labels on the Warning Popup`

- **Iteration:** `2`

- **Story Points:** `2`

- **Story Description:** As an operator, I want to replace the generic "Confirm" and "Dismiss" buttons with clearer labels, so that riders know what each button does.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* The user is creating or editing a warning.
  - *Trigger:* The user types in the button label fields.

- **Actions / Postconditions:**
  - *Action:* The user sets a continue label and a go-back label, each in English and Spanish.
  - *Postcondition:* The labels are saved with the warning and shown on the TVM popup and in the preview.

#### Acceptance Criteria

1. Each warning has a continue label and a go-back label, in English and Spanish.
2. The form suggests a continue label based on the warning type: "Buy for [Day]" for weekend and service-hour warnings (for example, "Buy for Monday"), and "Buy now" for promo warnings.
3. Blank labels fall back to "Continue" / "Continuar" and "Go back" / "Regresar".
4. Labels are 20 characters or fewer so they fit on the TVM button.

#### Test Scenarios

- **Manual (Success):** Create a weekend warning and check that the continue label is pre-filled with "Buy for Monday". Change the go-back label to "Choose another" and check that the preview shows both.
- **Manual (Failure / Edge Case):** Clear both labels and check that the preview shows "Continue" and "Go back". Enter a 30-character label and check that "Label must be 20 characters or fewer" shows.
- **Automated (Unit / Slice / Integration):** JUnit test that blank labels return the default English and Spanish text, and that the suggested label matches the warning type.

#### Actionable Sub-tasks

- [ ] Create the `AlertAction` model for button labels.
- [ ] Add the label fields to `AlertForm` with `@Size(max = 20)`.
- [ ] Pre-fill the suggested continue label based on the warning type.
- [ ] Apply the default labels when blank.
- [ ] Write JUnit tests for defaults and suggestions.

---

### Story 4.6: Live TVM Preview

- **GitHub Issue Title:** `[Alerts] Live TVM Preview`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As an operator, I want to see the warning popup as riders will see it, at any day and time I pick, so that I catch wording, translation, or timing problems before anything is published.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**
  - *Precondition:* The user is on the create or edit warning form in a desktop browser.
  - *Trigger:* The user types in a field, switches language, or changes the preview date and time.

- **Actions / Postconditions:**
  - *Action:* A panel next to the form redraws a mock TVM popup.
  - *Postcondition:* The preview shows the current text and labels in the chosen language, and shows or hides the warning based on the picked time.

#### Acceptance Criteria

1. The preview box has a fixed width and aspect ratio that matches the physical TVM touchscreen, so it doesn't stretch with the browser window.
2. The preview updates on each keystroke without reloading the page.
3. An **EN / ES** toggle switches the language.
4. A date and time picker shows or hides the warning using the Story 4.4 rules.
5. Text that won't fit on the screen turns red.
6. If **ES** is picked and there's no Spanish message, the preview shows "Missing Spanish translation".

#### Test Scenarios

- **Manual (Success):** Type in the English message box and check that the preview updates with each letter. Click **ES** and check that the Spanish text and labels show.
- **Manual (Success):** Change the preview time to cover all three cases: Saturday 10 AM (weekend warning shows), Wednesday 1 PM on a rush-hour-only route (service hours warning shows), and Oct 30 for the December promo (promo warning shows). Then pick Wednesday 8 AM on a weekday route and check that no warning shows.
- **Manual (Failure / Edge Case):** Leave the Spanish message empty and click **ES**. Check that the missing translation warning shows. Resize the browser and check that the preview box keeps its shape.
- **Automated (Unit / Slice / Integration):** `@WebMvcTest` with `MockMvc`: `GET /fares/alerts/new` returns HTTP 200, includes the preview fragment, and loads `tvm-preview.js`.

#### Actionable Sub-tasks

- [ ] Build `templates/alerts/tvm-preview.html` as a fragment with a fixed-size screen in `tvm-preview.css`.
- [ ] Write `tvm-preview.js` to update the preview on each `input` event.
- [ ] Add the EN / ES toggle and the date and time picker.
- [ ] Create `TvmPreviewModel` to pass warning data to the preview.
- [ ] Write the `MockMvc` test for the form page.

---

#### UI Mockups

**Warnings list (Stories 4.1 and 4.3)**

```text
+------------------------------------------------------------------------------+
| Fare Config      |  Service Alerts & TVM Canvas              [+ New warning] |
|------------------|-----------------------------------------------------------|
| Fare Matrix      |  Route                Ticket       Shows when    Message  |
| Availability     |  ---------------------------------------------------------|
| > Service Alerts |  Edgewater → Midtown  All tickets  Weekends      SERVICE..|
| Publish          |                                           [Edit] [Delete*]|
|                  |  Port Imperial → P11  Dec Promo    Before Dec 1  You can..|
|                  |                       (Missing Spanish)   [Edit] [Delete*]|
|                  |                                                           |
|                  |  * Only Caracal administrators see Delete                 |
+------------------------------------------------------------------------------+
```

**Warning form with live preview (Stories 4.2, 4.5, and 4.6)**

```text
+--------------------------------------+   +------------------------------+
| New warning                          |   | TVM preview        [EN] [ES] |
|                                      |   | Preview at [Sat 10:00 AM  ]  |
| Origin       [Edgewater          v]  |   | +--------------------------+ |
| Destination  [Midtown            v]  |   | |  ! SERVICE NOTICE        | |
| Ticket       [All tickets        v]  |   | |                          | |
| Shows when   [Weekends           v]  |   | |  No ferry service on     | |
| Message (EN) [SERVICE NOTICE: No...] |   | |  weekends. Your ticket   | |
|              84/200                  |   | |  is valid Monday.        | |
| Message (ES) [AVISO DE SERVICIO:...] |   | |                          | |
|              97/200                  |   | | [Buy for Monday]         | |
| Button 1     [Buy for Monday       ] |   | | [Choose another]         | |
| Button 2     [Choose another       ] |   | +--------------------------+ |
|                  [Cancel]  [Save]    |   +------------------------------+
+--------------------------------------+
```