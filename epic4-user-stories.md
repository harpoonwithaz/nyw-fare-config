# Epic 4: Pre-Purchase Warnings and TVM Preview

When a rider picks a ticket that can't be used right now, the ticket machine (TVM) should show a short heads-up before they pay. The rider can still buy the ticket. The warning just makes sure they know when it will work.

Our client (Eldon, Caracal) described three cases in our Oct 16 and Oct 30 meetings:

- **Weekday-only routes:** On a Saturday, a rider picks Edgewater → Midtown and sees "This route doesn't run on weekends. Your ticket is valid Monday."
- **Rush-hour-only routes:** Some routes only run during weekday morning and evening rush hours, so riders outside those hours see a notice.
- **Advance-purchase promos:** A rider buys a December promo ticket on Oct 30 and sees "You can buy this now, but you can't use it until December 1."

These are **not** outage or maintenance messages. Every warning is shown in English and Spanish, like the client's current TVM screens. This epic stores the warnings in our own database. Epic 3 (Availability) decides when a ticket is sold, and Epic 4 decides what the rider is told.

**Note:** The character limits below (200 for messages, 20 for button labels) are our best guess. We'll confirm them with Eldon at a Friday sync.

---

## Story 4.1: View Warning Messages

**Iteration:** 1

As an ***operator***, I want to ***see every saved warning in one list***, so that ***I know what riders are being told before I change anything.***

### Actors / Personas

Operator (`ROLE_OPERATOR`), Client Services admin (`ROLE_CLIENT_SERVICES`).

### Preconditions & Triggers

The user is logged in and clicks **Service Alerts & TVM Canvas** in the sidebar, which opens `/fares/alerts`.

### Actions / Postconditions

The page shows a table of every warning with its route, ticket, when it shows, and the English and Spanish messages.

### Acceptance Criteria

1. Only logged-in users can open the page.
2. Each row shows the route (for example, Edgewater → Midtown), the ticket (for example, Adult One-Way), when the warning shows (Weekends, Outside service hours, or Before a start date), and both messages.
3. Warnings with no Spanish message show a **Missing Spanish** badge.
4. If there are no warnings, the page says "No warnings yet" and shows a **New warning** button.

### Test Scenarios

- Manual (Success): Log in as `operator`, open Service Alerts, and check that saved warnings are listed.
- Manual (Empty): With nothing saved, check that "No warnings yet" and the **New warning** button appear.
- Manual (Failure): Log out and go to `/fares/alerts`. Check that you are sent to the login page.
- Automated (`MockMvc`): A logged-out `GET /fares/alerts` redirects to `/login`. A logged-in request returns HTTP 200 and the `alerts/index` view.

### Actionable Sub-tasks

- [ ] Create the `ServiceAlert` entity and `ServiceAlertRepository`.
- [ ] Create `AlertController` with `GET /fares/alerts`.
- [ ] Build `templates/alerts/index.html` with the shared sidebar.
- [ ] Add the Missing Spanish badge.
- [ ] Write `MockMvc` tests for logged-in and logged-out users.

---

## Story 4.2: Create a Bilingual Warning

**Iteration:** 1

As an ***operator***, I want to ***write a warning for a ticket in English and Spanish and choose when it shows***, so that ***riders know when their ticket will work before they pay.***

### Actors / Personas

Operator (`ROLE_OPERATOR`), Client Services admin (`ROLE_CLIENT_SERVICES`).

### Preconditions & Triggers

The user is logged in and clicks **New warning**, which opens `/fares/alerts/new`.

### Actions / Postconditions

The user picks an origin terminal, a destination, and a ticket from dropdowns. Then they pick when the warning shows (**Weekends**, **Outside service hours**, or **Before a start date**, with a date) and write the English message, plus the Spanish one if it's ready. On **Save**, the warning is stored and the user goes back to the list with a "Warning saved" message. If something is wrong, the form comes back with the user's input kept and an error under each problem field.

### Acceptance Criteria

1. Origin, destination, ticket, "show when", and English message are required.
2. If "Before a start date" is picked, a start date is required.
3. The Spanish message is optional for now, but the list flags warnings without one.
4. Messages are 200 characters or fewer.
5. Nothing is saved when validation fails.

### Test Scenarios

- Manual (Success): Pick Edgewater → Midtown, Adult One-Way, and **Weekends**. Enter "SERVICE NOTICE: No ferry service on this route on weekends. Your ticket is valid Monday." and "AVISO DE SERVICIO: No hay servicio de ferry en esta ruta los fines de semana. Su boleto es válido el lunes." Save, then check that the warning appears with "Warning saved".
- Manual (Success): Pick a December promo ticket and **Before a start date** with Dec 1. Save, then check that the list shows "Before Dec 1".
- Manual (Failure): Leave the English message blank and save. Check that "English message is required" shows and nothing is saved.
- Manual (Failure): Pick **Before a start date** but leave the date empty. Check that "Start date is required" shows.
- Manual (Failure): Paste a 250-character message. Check that "Message must be 200 characters or fewer" shows.
- Automated (`MockMvc`): A valid `POST /fares/alerts` redirects to the list and saves one warning. A `POST` with a blank English message returns the form with a field error.

### Actionable Sub-tasks

- [ ] Create `AlertForm` with validation (`@NotBlank`, `@Size`).
- [ ] Add `GET /fares/alerts/new` and `POST /fares/alerts`.
- [ ] Create `AlertService` to save warnings.
- [ ] Build `templates/alerts/form.html` with terminal, destination, and ticket dropdowns.
- [ ] Write `MockMvc` tests for valid and invalid input.

---

## Story 4.3: Edit or Delete a Warning

**Iteration:** 2

As an ***operator***, I want to ***edit a warning***, so that ***I can fix wording without starting over.*** As a ***Client Services admin***, I want to ***delete old warnings***, so that ***the list stays clean.***

### Actors / Personas

Operator (`ROLE_OPERATOR`) can edit. Client Services admin (`ROLE_CLIENT_SERVICES`) can edit and delete.

### Preconditions & Triggers

At least one warning exists, and the user clicks **Edit** or **Delete** on it.

### Actions / Postconditions

**Edit** opens the Story 4.2 form with the current values filled in, and saving updates that warning. **Delete** asks for confirmation, then removes it.

### Acceptance Criteria

1. Every logged-in user sees **Edit**. Only admins see **Delete**.
2. Delete asks "Are you sure?" first.
3. Edits follow the same rules as Story 4.2.
4. Opening a warning that doesn't exist shows a "not found" page.

### Test Scenarios

- Manual (Success): As `operator`, change a warning's message and save. Check that the list shows the new text.
- Manual (Success): As `admin`, delete a warning and confirm. Check that it is gone.
- Manual (Failure): As `operator`, check that there's no Delete button, and that sending the delete request directly shows the 403 page.
- Manual (Failure): Open `/fares/alerts/9999/edit` and check that the not found page shows.
- Automated (`MockMvc`): A delete as an operator returns 403 and the warning still exists. A delete as an admin removes it.

### Actionable Sub-tasks

- [ ] Add `GET /fares/alerts/{id}/edit` and `POST /fares/alerts/{id}`.
- [ ] Add `POST /fares/alerts/{id}/delete`, limited to `CLIENT_SERVICES`.
- [ ] Hide the Delete button for operators.
- [ ] Write permission tests.

---

## Story 4.4: Show When the Ticket Becomes Valid

**Iteration:** 2

As a ***rider***, I want ***the warning to tell me exactly when my ticket will work***, so that ***I'm not confused about whether I can ride today.***

### Actors / Personas

Riders see the result. Operators (`ROLE_OPERATOR`) set it up. The route schedule comes from Epic 3 (Availability).

### Preconditions & Triggers

A rider picks a ticket that has a warning, at a time when the warning applies.

### Actions / Postconditions

The app works out the next valid time and adds it to the warning, for example "Valid Monday", "Next service 6:00 AM", or "Valid from December 1".

### Acceptance Criteria

1. **Weekends:** On Saturday or Sunday, the warning names the next weekday.
2. **Outside service hours:** Outside the route's service hours, the warning names the next service time.
3. **Before a start date:** Before the start date, the warning names that date.
4. When the ticket is usable right now, no warning shows.
5. All checks use the terminal's time zone (`America/New_York`), not the server's.
6. If a route has no schedule saved yet, the plain warning still shows, just without a time.

### Test Scenarios

- Manual (Success): Preview Edgewater → Midtown on Saturday at 10 AM. Check that the warning says "Valid Monday".
- Manual (Success): Preview the December promo ticket on Oct 30. Check that the warning says "Valid from December 1".
- Manual (Success): Preview a rush-hour-only route on Wednesday at 1 PM. Check that the warning gives the next service time.
- Manual (Failure): Preview a route that has no schedule saved yet. Check that the app shows the plain warning without a time instead of crashing.
- Manual (No Warning): Preview Edgewater → Midtown on Wednesday during rush hour. Check that no warning shows.
- Manual (Edge Case): Preview on Sunday at 11:59 PM New York time. Check that the warning still shows, even though it's already Monday on the server's UTC clock.
- Automated (JUnit): Test `nextValidTime()` with Friday night, Saturday, Sunday 11:59 PM, Monday morning, and a date before a promo start.

### Actionable Sub-tasks

- [ ] Agree with Athan (Epic 3) on where route schedules and service hours are stored.
- [ ] Create `RouteSchedule` if Epic 3 doesn't already provide it.
- [ ] Write `nextValidTime()` using the terminal's time zone.
- [ ] Write JUnit tests for the edge cases.

---

## Story 4.5: Clear Button Labels on the Warning Popup

**Iteration:** 2

As an ***operator***, I want to ***replace the generic "Confirm" and "Dismiss" buttons with clearer labels***, so that ***riders know what each button does.***

### Actors / Personas

Operator (`ROLE_OPERATOR`), Client Services admin (`ROLE_CLIENT_SERVICES`).

### Preconditions & Triggers

The user is creating or editing a warning.

### Actions / Postconditions

The form has a "continue" label and a "go back" label, each in English and Spanish. They're saved with the warning and shown on the popup.

### Acceptance Criteria

1. Each warning can have two button labels in both languages.
2. Blank labels fall back to "Continue" and "Go back".
3. Labels are 20 characters or fewer so they fit on the TVM button.

### Test Scenarios

- Manual (Success): Set the labels to "Buy for Monday" and "Choose another". Check that the preview shows them.
- Manual (Default): Leave both labels blank. Check that the preview shows "Continue" and "Go back".
- Manual (Failure): Enter a 30-character label. Check that "Label must be 20 characters or fewer" shows.
- Automated (JUnit): Blank labels return the default text.

### Actionable Sub-tasks

- [ ] Create the `AlertAction` model for button labels.
- [ ] Add the label fields to `AlertForm` with `@Size(max = 20)`.
- [ ] Apply the default labels when blank.

---

## Story 4.6: Live TVM Preview

**Iteration:** 2

As an ***operator***, I want to ***see the warning popup as riders will see it, at any day and time I pick***, so that ***I catch wording, translation, or timing problems before anything is published.***

### Actors / Personas

Operator (`ROLE_OPERATOR`), Client Services admin (`ROLE_CLIENT_SERVICES`).

### Preconditions & Triggers

The user is on the create or edit warning form. The app targets desktop browsers, as agreed with the client.

### Actions / Postconditions

A panel next to the form shows a mock TVM popup. It updates as the user types, switches between English and Spanish, and has a date and time picker to check whether the warning would show at that moment.

### Acceptance Criteria

1. The preview updates without reloading the page.
2. An **EN / ES** toggle switches the language.
3. Picking a date and time shows or hides the warning based on Story 4.4.
4. Text that is too long for the screen turns red.
5. If **ES** is picked and there's no Spanish message, the preview shows "Missing Spanish translation".

### Test Scenarios

- Manual (Success): Type in the English message box. Check that the preview updates with each letter.
- Manual (Success): Set the preview time to Saturday 10 AM. Check that the weekend warning shows. Change it to Wednesday 8 AM and check that it disappears.
- Manual (Failure): Leave the Spanish message empty and click **ES**. Check that the missing translation warning shows.
- Automated (`MockMvc`): `GET /fares/alerts/new` includes the preview panel and loads `tvm-preview.js`.

### Actionable Sub-tasks

- [ ] Build `templates/alerts/tvm-preview.html` as a fragment, styled by `tvm-preview.css`.
- [ ] Write `tvm-preview.js` to update the preview on each `input` event.
- [ ] Add the EN / ES toggle, the date and time picker, and the warnings.
- [ ] Create `TvmPreviewModel` to pass warning data to the preview.

---

## UI Mockups

**Warnings list (Stories 4.1 and 4.3)**

```text
+------------------------------------------------------------------------------+
| Fare Config      |  Service Alerts & TVM Canvas              [+ New warning] |
|------------------|-----------------------------------------------------------|
| Fare Matrix      |  Route                Ticket       Shows when    Message  |
| Availability     |  ---------------------------------------------------------|
| > Service Alerts |  Edgewater → Midtown  Adult 1-Way  Weekends      SERVICE..|
| Publish          |                                           [Edit] [Delete*]|
|                  |  Port Imperial → P11  Dec Promo    Before Dec 1  You can..|
|                  |                       (Missing Spanish)   [Edit] [Delete*]|
|                  |                                                           |
|                  |  * Only Client Services admins see Delete                 |
+------------------------------------------------------------------------------+
```

**Warning form with live preview (Stories 4.2, 4.5, and 4.6)**

```text
+--------------------------------------+   +------------------------------+
| New warning                          |   | TVM preview        [EN] [ES] |
|                                      |   | Preview at [Sat 10:00 AM  ]  |
| Origin       [Edgewater          v]  |   | +--------------------------+ |
| Destination  [Midtown            v]  |   | |  ! SERVICE NOTICE        | |
| Ticket       [Adult One-Way      v]  |   | |                          | |
| Shows when   [Weekends           v]  |   | |  No ferry service on     | |
| Message (EN) [SERVICE NOTICE: No...] |   | |  weekends. Your ticket   | |
| Message (ES) [AVISO DE SERVICIO:...] |   | |  is valid Monday.        | |
| Button 1     [Buy for Monday       ] |   | |                          | |
| Button 2     [Choose another       ] |   | | [Buy for Monday]         | |
|                  [Cancel]  [Save]    |   | | [Choose another]         | |
+--------------------------------------+   | +--------------------------+ |
                                           +------------------------------+
```
