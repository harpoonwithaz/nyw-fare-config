## Epic 1: Authentication and Access Control

This epic provides the functionality for staff members to sign in to the fare configuration application with a username or email and a password. After login, each user sees a landing page tailored to their role (`OPERATOR` vs. `CLIENT_SERVICES`). Operators receive standard access and can change their own password. Caracal Administrators (`CLIENT_SERVICES`) can create accounts, modify roles, and view all registered users. Accounts are provisioned administratively with no public sign-up. Passwords are encrypted as BCrypt hashes, sessions are persisted in PostgreSQL, and all protected endpoints require authentication. Google Single Sign-On and the Caracal API identity header are slated for Iteration 2.

---

### Story 1.1: Log In with Username or Email

- **GitHub Title:** `[Auth] Log In with Username or Email`

- **Iteration:** `1`

- **Story Points:** `2`

- **Story Description:** As a registered staff member, I want to log in with either my username or my email plus my password, so that I can securely access the system.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* An account exists in the `app_users` table and the user is currently signed out.
  - *Trigger:* The user navigates to `/login` and submits the authentication form.

- **Actions / Postconditions:**

  - *Action:* The system looks up the account by username or email and verifies the submitted password against the stored BCrypt hash.
  - *Postcondition:* On success, an HTTP session is created and the user is redirected to `/dashboard`. On failure, the login page displays an error alert and no session is initialized.

#### Acceptance Criteria

1. The login page displays an "Email or username" field, a "Password" field, and a "Login" button within a centered card adhering to the application layout.
2. Username and email matching is case-insensitive, while password verification is strictly case-sensitive.
3. Submitting an incorrect password or an unknown username displays the same generic message: "Invalid email/username or password."
4. Empty form submissions are blocked client-side by browser validation.
5. Plaintext passwords are never logged, displayed, or stored; verification uses the database hash exclusively.

#### Test Scenarios

- **Manual (Success):** Open `/login`, enter `admin` and the admin password, and click Login. Verify the dashboard opens with "Welcome, Admin!". Repeat with `eldon@caracal.com` and its valid password.
- **Manual (Failure / Edge Case):** Enter `admin` with an incorrect password; verify the generic error alert appears. Enter a non-existent username; verify the identical message appears. Submit with an empty field and verify the browser blocks submission.
- **Automated (Unit / Slice / Integration):** JUnit 5 and Mockito test of `CustomUserDetailsService`: mocked repository returns a user for lookup `ADMIN` (case-insensitive), and an unknown lookup throws `UsernameNotFoundException`. A `MockMvc` test using `formLogin()` asserts redirect to `/dashboard` for valid credentials and to `/login?error=invalid` for invalid credentials.

#### Actionable Sub-tasks

- [ ] Create the `AppUser` JPA entity and `AppUserRepository` lookup methods by email or username (case-insensitive).
- [ ] Implement `CustomUserDetailsService` and `AppUserDetails`.
- [ ] Configure form login with success and failure handlers in `SecurityConfig`.
- [ ] Build `login.html` with error alert handling and shared styling.
- [ ] Write automated tests for `CustomUserDetailsService` and the login authentication flow.

---

### Story 1.2: Role-Based Landing Page

- **GitHub Title:** `[Auth] Role-Based Landing Page`

- **Iteration:** `1`

- **Story Points:** `3`

- **Story Description:** As a signed-in user, I want a landing page that shows only the actions my role permits, so that I see relevant tools and avoid unauthorized options.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* The user is authenticated.
  - *Trigger:* User logs in successfully or navigates directly to `/dashboard` or `/`.

- **Actions / Postconditions:**

  - *Action:* The application resolves the authenticated user's granted authorities and renders the view.
  - *Postcondition:* An `OPERATOR` sees a welcome message and "Change password". A `CLIENT_SERVICES` administrator also sees "Create account" and "Manage users".

#### Acceptance Criteria

1. Both roles see a welcome banner with their capitalized username and a top/sidebar bar containing a "Log out" trigger.
2. `OPERATOR` accounts see only the "Change password" quick-action button.
3. `CLIENT_SERVICES` accounts see "Create account", "Manage users", and "Change password".
4. The landing page never exposes internal database IDs, raw password hashes, or API header values.

#### Test Scenarios

- **Manual (Success):** Log in as `admin`; verify that all three action buttons ("Create account", "Manage users", "Change password") display. Log out, log in as `operator`, and verify only "Change password" displays.
- **Manual (Failure / Edge Case):** Log in as `operator`; inspect the DOM and verify that no "Create account" or "Manage users" links or buttons exist in the rendered markup.
- **Automated (Unit / Slice / Integration):** `@WebMvcTest` of `DashboardController` with a mocked `AppUserDetails` principal: assert model attribute `isAdmin=false` for `OPERATOR` and `isAdmin=true` for `CLIENT_SERVICES`, returning view name `dashboard` in both cases.

#### Actionable Sub-tasks

- [ ] Create `DashboardController` passing the username and administrative status flags to the model.
- [ ] Build `dashboard.html` with role-conditional action buttons using Thymeleaf `sec:authorize`.
- [ ] Build shared navigation top/sidebar bar in `fragments.html`.
- [ ] Write `@WebMvcTest` slice tests covering model attributes for both roles.

---

### Story 1.3: Log Out

- **GitHub Title:** `[Auth] Log Out`

- **Iteration:** `1`

- **Story Points:** `1`

- **Story Description:** As a signed-in user, I want to log out, so that nobody else can use my session on a shared workstation.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* The user has an active authenticated session.
  - *Trigger:* The user clicks "Log out" in the navigation bar.

- **Actions / Postconditions:**

  - *Action:* The browser submits an HTTP POST request to `/logout`. The server invalidates the session and deletes session cookies.
  - *Postcondition:* The user is redirected to `/login` with the alert "You have been logged out." and can no longer access protected pages.

#### Acceptance Criteria

1. Log out functionality is accessible across all protected application pages.
2. Following logout, the user is redirected to `/login?logout` displaying "You have been logged out."
3. Following logout, navigating directly to `/dashboard` redirects to `/login`.
4. Logout requires an HTTP POST request carrying a valid CSRF token to prevent cross-site logout exploits.

#### Test Scenarios

- **Manual (Success):** Log in and click "Log out". Verify the login page opens displaying "You have been logged out."
- **Manual (Failure / Edge Case):** Immediately after logging out, click the browser "Back" button or open `/dashboard` in a new tab. Verify the login page appears and no cached user data is displayed.
- **Automated (Unit / Slice / Integration):** `MockMvc`: a POST to `/logout` with a valid CSRF token as an authenticated user redirects to `/login?logout`. A POST without a CSRF token returns HTTP 403 Forbidden.

#### Actionable Sub-tasks

- [ ] Configure Spring Security logout handling (session invalidation, cookie deletion, success URL).
- [ ] Implement the Log out trigger as an HTTP POST form with CSRF token in `fragments.html`.
- [ ] Add the logout confirmation banner to `login.html`.
- [ ] Write `MockMvc` tests verifying session termination and CSRF enforcement.

---

### Story 1.4: Automatic Logout After Inactivity

- **GitHub Title:** `[Auth] Automatic Logout After Inactivity`

- **Iteration:** `1`

- **Story Points:** `2`

- **Story Description:** As a Caracal administrator, I want user sessions to expire automatically after an inactivity period, so that unattended terminals cannot be misused.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* A user is authenticated and stops interacting with the application.
  - *Trigger:* The user's next HTTP request arrives after the inactivity window has elapsed.

- **Actions / Postconditions:**

  - *Action:* The servlet container/Spring Session detects an expired session.
  - *Postcondition:* The request is redirected to `/login` and the user must sign in again.

#### Acceptance Criteria

1. The maximum inactivity limit is set to 30 minutes (`spring.session.timeout=30m`).
2. Sessions are persisted in PostgreSQL via Spring Session JDBC.
3. User interaction within the timeout window resets the expiration timer.
4. Application service restarts do not immediately log out active users, as sessions reside in the database rather than in-memory storage.

#### Test Scenarios

- **Manual (Success):** Temporarily set `spring.session.timeout=1m`, log in, wait 65 seconds without clicking anything, and refresh the page. Verify the login page appears with a session expired notification.
- **Manual (Failure / Edge Case):** Log in and navigate between pages every 20 seconds for two minutes. Verify the session remains active and the user is not signed out.
- **Automated (Unit / Slice / Integration):** `@SpringBootTest` verifying that the application context loads the `spring.session.timeout` property set to 30 minutes and initializes the Spring Session JDBC repository.

#### Actionable Sub-tasks

- [ ] Add `spring-boot-starter-session-jdbc` dependency to `pom.xml`.
- [ ] Configure `spring.session.jdbc.initialize-schema=always` and `spring.session.timeout=30m` in `application.properties`.
- [ ] Verify database migration generates the `SPRING_SESSION` and `SPRING_SESSION_ATTRIBUTES` tables.
- [ ] Write an automated configuration test verifying session timeout properties.

---

### Story 1.5: Protected Pages and Login Redirects

- **GitHub Title:** `[Auth] Protected Pages and Login Redirects`

- **Iteration:** `1`

- **Story Points:** `2`

- **Story Description:** As an authenticated operator or administrator, I want unauthenticated visitors redirected to the login page and authenticated users redirected away from the login page, so that private transit data remains secure and authenticated users avoid redundant login forms.

- **Actors / Personas:** Unauthenticated Visitor, Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* None.
  - *Trigger:* A user accesses any application URL directly.

- **Actions / Postconditions:**

  - *Action:* The Spring Security filter chain evaluates the authentication status of the incoming request.
  - *Postcondition:* An unauthenticated visitor is redirected to `/login`. An already authenticated user accessing `/login` is redirected to `/dashboard`.

#### Acceptance Criteria

1. Only `/login`, static web resources (`/css/**`, `/js/**`, `/images/**`), and error views are publicly accessible.
2. Unauthenticated attempts to access `/dashboard`, `/admin/**`, or `/account/**` result in a 302 redirect to `/login`.
3. An authenticated user attempting to open `/login` is redirected to `/dashboard`.
4. Navigating to the root path `/` redirects authenticated users to `/dashboard` and unauthenticated users to `/login`.

#### Test Scenarios

- **Manual (Success):** While logged in, attempt to navigate to `/login`. Verify the application redirects immediately to `/dashboard`.
- **Manual (Failure / Edge Case):** In a private/incognito window while logged out, navigate directly to `/dashboard` and `/admin/users`. Verify both URLs redirect to `/login` and expose no internal data.
- **Automated (Unit / Slice / Integration):** `MockMvc`: anonymous `GET /dashboard` asserts HTTP 302 redirecting to `http://localhost/login`. Authenticated `GET /login` asserts HTTP 302 redirecting to `/dashboard`.

#### Actionable Sub-tasks

- [ ] Define HTTP request authorization matchers in `SecurityConfig`.
- [ ] Add redirect logic in `LoginController` to bypass `/login` when the security context is already authenticated.
- [ ] Ensure root path `/` forwards to `/dashboard`.
- [ ] Write `MockMvc` tests for anonymous redirects and authenticated bypasses.

---

### Story 1.6: Restrict Administration Pages to Client Services

- **GitHub Title:** `[Auth] Restrict Administration Pages to Client Services`

- **Iteration:** `1`

- **Story Points:** `3`

- **Story Description:** As a Caracal administrator, I want account administration restricted to the `CLIENT_SERVICES` role, so that standard operators cannot provision users or elevate roles.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* The user has an active authenticated session.
  - *Trigger:* The user accesses an administrative URL under `/admin/**`.

- **Actions / Postconditions:**

  - *Action:* The security interceptor checks whether the session principal holds the `CLIENT_SERVICES` authority.
  - *Postcondition:* A `CLIENT_SERVICES` user receives the page. An `OPERATOR` user receives HTTP 403 Forbidden with a styled Access Denied page.

#### Acceptance Criteria

1. All endpoints under `/admin/**` strictly require `ROLE_CLIENT_SERVICES`.
2. Unauthorized access by an `OPERATOR` returns HTTP 403 and renders `error/403.html` stating "You do not have permission to view this page."
3. The Access Denied view includes a navigation button returning to `/dashboard` matching the application theme.
4. Role checks are enforced on both HTTP GET views and HTTP POST mutation endpoints.

#### Test Scenarios

- **Manual (Success):** Log in as a `CLIENT_SERVICES` administrator and navigate to `/admin/users`. Verify the user management view renders correctly.
- **Manual (Failure / Edge Case):** Log in as an `OPERATOR` and type `/admin/users` or `/admin/users/new` directly into the browser address bar. Verify HTTP 403 and the Access Denied page display.
- **Automated (Unit / Slice / Integration):** `@WebMvcTest` of `AdminController`: `GET /admin/users` as `OPERATOR` returns HTTP 403, while as `CLIENT_SERVICES` it returns HTTP 200. A `POST /admin/users/new` as `OPERATOR` returns HTTP 403.

#### Actionable Sub-tasks

- [ ] Add `requestMatchers("/admin/**").hasRole("CLIENT_SERVICES")` in `SecurityConfig`.
- [ ] Create `templates/error/403.html` with return links and shared styling.
- [ ] Ensure administrative navigation buttons are hidden from `OPERATOR` views.
- [ ] Write `MockMvc` tests verifying HTTP 403 enforcement on GET and POST requests.

---

### Story 1.7: View All Users

- **GitHub Title:** `[Auth] View All Users`

- **Iteration:** `1`

- **Story Points:** `2`

- **Story Description:** As a Caracal administrator, I want to view every account with its username, email, and assigned role, so that I have complete visibility over who has access to the configuration tool.

- **Actors / Personas:** Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* The administrator is authenticated with `CLIENT_SERVICES`.
  - *Trigger:* The administrator navigates to `/admin/users`.

- **Actions / Postconditions:**

  - *Action:* The application queries all user records from the `app_users` table.
  - *Postcondition:* A table displays all accounts sorted alphabetically by username without modifying data.

#### Acceptance Criteria

1. The table contains Username, Email, and Role columns, sorted by username ascending.
2. Password hashes and credentials are never exposed in the HTML markup or API payloads.
3. The page provides a "Create account" button and a navigation link returning to the dashboard.
4. `OPERATOR` users are blocked from viewing this page (enforced via Story 1.6).

#### Test Scenarios

- **Manual (Success):** Log in as an administrator and click "Manage users". Verify all registered accounts appear in alphabetical order with their usernames, emails, and roles.
- **Manual (Failure / Edge Case):** Log in as an `OPERATOR` and attempt to navigate to `/admin/users`. Verify access is denied and no user roster data is leaked.
- **Automated (Unit / Slice / Integration):** JUnit and Mockito test of `UserService.listUsers()` verifying the repository is queried with sorting by username ascending. A `MockMvc` test asserts that `GET /admin/users` as `CLIENT_SERVICES` populates the `users` model attribute and returns the `users` view.

#### Actionable Sub-tasks

- [ ] Implement `UserService.listUsers()` sorting by username ascending.
- [ ] Create controller route `GET /admin/users` in `AdminController`.
- [ ] Build `templates/admin/users.html` table adhering to Bootstrap styling.
- [ ] Write automated service and controller slice tests.

---

### Story 1.8: Change a User's Role

- **GitHub Title:** `[Auth] Change a User's Role`

- **Iteration:** `1`

- **Story Points:** `3`

- **Story Description:** As a Caracal administrator, I want to change another user's assigned role, so that staff members maintain only the permissions their current job function requires.

- **Actors / Personas:** Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* An administrator is signed in and the target user account exists in `app_users`.
  - *Trigger:* The administrator selects a new role in the user's table row and clicks "Save".

- **Actions / Postconditions:**

  - *Action:* The system validates the role selection, updates the user's database record, and terminates the target user's active sessions.
  - *Postcondition:* The table reflects the updated role. The target user must log in again to receive the new authority set.

#### Acceptance Criteria

1. Every user row except the currently logged-in administrator's contains a role selection dropdown and a "Save" button.
2. The admin's own row shows "Not available" under Change role, and "(you)" beside the username.
3. The server rejects self-role modification attempts with the error message "You cannot change your own role."
4. Submitting an invalid role value displays "Please choose a valid role." Submitting for a non-existent user displays "That user no longer exists."
5. On success, an alert displays "Role updated. The user is signed out and receives the new role at their next login."
6. The last remaining Client Services account cannot be changed to Operator: "At least one Client Services account must remain.

#### Test Scenarios

- **Manual (Success):** As an administrator, select a test operator, change their role to `CLIENT_SERVICES`, and click "Save". Verify the success banner appears and the table reflects the new role. If that user was signed in, verify their next page click forces them to log in again.
- **Manual (Failure / Edge Case):** Inspect the administrator's own row in `/admin/users`; verify there is no dropdown. Attempt to submit a role-change POST targeting the administrator's own ID; verify the server rejects the update with "You cannot change your own role."
- **Automated (Unit / Slice / Integration):** JUnit test of `UserService.changeRole()`: self-change returns a rejection status and does not save; invalid role returns an error; valid role saves and calls session invalidation. `MockMvc` POST to the role change endpoint as `CLIENT_SERVICES` with CSRF token asserts redirect to `/admin/users?roleChanged`.

#### Actionable Sub-tasks

- [ ] Implement `UserService.changeRole()` with self-modification prevention and input validation.
- [ ] Invalidate active sessions for the modified user via `FindByIndexNameSessionRepository`.
- [ ] Add `POST /admin/users/{id}/role` route in `AdminController`.
- [ ] Wire role dropdowns and forms in `templates/admin/users.html`.
- [ ] Write unit and slice tests for role modifications.

---

### Story 1.9: Admin Creates an Account

- **GitHub Title:** `[Auth] Admin Creates an Account`

- **Iteration:** `1`

- **Story Points:** `3`

- **Story Description:** As a Caracal administrator, I want to provision new accounts with an email, username, role, and initial password, so that only approved personnel gain access to the system.

- **Actors / Personas:** Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* The administrator is authenticated with `CLIENT_SERVICES`.
  - *Trigger:* The administrator clicks "Create account", navigates to `/admin/users/new`, and submits the form.

- **Actions / Postconditions:**

  - *Action:* The system validates all input fields, encodes the password with BCrypt, and persists the record.
  - *Postcondition:* The new user appears in the user list and can immediately authenticate using their username or email.

#### Acceptance Criteria

1. The creation form includes Email, Username, Role (defaulting to `OPERATOR`), Password, and Confirm Password fields.
2. Email must be a valid RFC 5322 format. Username must be 3–30 characters containing only alphanumeric characters, dots, underscores, or hyphens.
3. Password and confirmation must match and satisfy the security policy defined in Story 1.11.
4. Email and username are stored in lowercase. Passwords are saved strictly as BCrypt hashes.
5. On validation failure, the form re-renders displaying field error messages while preserving entered email, username, and role (clearing password fields).
6. Success redirects to `/admin/users` displaying "Account created."
7. Public user self-registration is disabled; accounts are created exclusively by administrators.

#### Test Scenarios

- **Manual (Success):** As an administrator, create account `testoperator` with email `test.operator@nywaterway.com`, role `OPERATOR`, and password `Ferry@2026`. Log out, then log in using the username and then the email; verify both authentications succeed.
- **Manual (Failure / Edge Case):** Submit mismatched passwords, a username containing spaces, and an email without a domain. Verify individual error alerts display ("Passwords do not match.") and the typed username and email are preserved.
- **Automated (Unit / Slice / Integration):** JUnit test of `UserService.createUser()`: valid input saves an `AppUser` with normalized lowercase username/email and hashed password; invalid input throws validation exceptions. `MockMvc` POST with invalid data asserts view `admin/create-account` is returned with an `errors` model attribute.

#### Actionable Sub-tasks

- [ ] Implement `CreateUserForm` DTO with Bean Validation constraints.
- [ ] Implement `UserService.createUser()` with BCrypt hashing and normalization.
- [ ] Add `GET /admin/users/new` and `POST /admin/users/new` in `AdminController`.
- [ ] Build `templates/admin/create-account.html` with field error feedback.
- [ ] Write automated tests for user provisioning and validation rejection.

---

### Story 1.10: Prevent Duplicate Usernames and Emails

- **GitHub Title:** `[Auth] Prevent Duplicate Usernames and Emails`

- **Iteration:** `1`

- **Story Points:** `2`

- **Story Description:** As a Caracal administrator, I want the system to reject duplicate usernames and emails, so that every account uniquely identifies exactly one staff member.

- **Actors / Personas:** Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* At least one account exists in `app_users`.
  - *Trigger:* An administrator submits `/admin/users/new` with a username or email that already exists.

- **Actions / Postconditions:**

  - *Action:* The system checks uniqueness case-insensitively, and database unique constraints enforce uniqueness at the storage layer.
  - *Postcondition:* The transaction is aborted, no account is inserted, and the form informs the user which field is duplicated.

#### Acceptance Criteria

1. A duplicate username displays "That username is already taken."
2. A duplicate email displays "That email is already registered."
3. Comparisons are case-insensitive (e.g., `admin` and `ADMIN` are treated as identical).
4. Concurrent submissions with identical values are caught by database constraints, presenting a safe error message ("That username or email is already in use.") without exposing SQL stack traces.
5. Usernames cannot contain the `@` character, preventing collisions between usernames and emails.

#### Test Scenarios

- **Manual (Success):** Create an account with a unique username and email. Verify the account is created successfully.
- **Manual (Failure / Edge Case):** Attempt to create an account with username `admin`, then `ADMIN`, then email `admin@caracal.com`. Verify each submission is rejected with its corresponding duplicate notification.
- **Automated (Unit / Slice / Integration):** JUnit test: when `existsByUsernameIgnoreCase` or `existsByEmailIgnoreCase` returns true, `createUser` returns a duplicate error and aborts persistence. When `saveAndFlush` throws `DataIntegrityViolationException`, the service safely catches it and returns "That username or email is already in use."

#### Actionable Sub-tasks

- [ ] Add `existsByUsernameIgnoreCase` and `existsByEmailIgnoreCase` to `AppUserRepository`.
- [ ] Apply unique constraints to `username` and `email` columns in PostgreSQL schema.
- [ ] Add duplicate pre-checks and `DataIntegrityViolationException` handling in `UserService`.
- [ ] Write unit tests verifying duplicate rejection for both usernames and emails.

---

### Story 1.11: Password Rules

- **GitHub Title:** `[Auth] Enforce Password Rules`

- **Iteration:** `1`

- **Story Points:** `2`

- **Story Description:** As an administrator, I want system passwords to meet strength requirements, so that accounts are protected from brute-force and dictionary guessing.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* A user password is being established.
  - *Trigger:* An administrator submits "Create account", or any user submits "Change password".

- **Actions / Postconditions:**

  - *Action:* The server validates the candidate password against the complexity policy.
  - *Postcondition:* A password satisfying all rules is accepted. An invalid password is rejected and all failing criteria are presented together.

#### Acceptance Criteria

1. Length must be between 8 and 72 characters inclusive.
2. Must contain at least one uppercase letter.
3. Must contain at least one lowercase letter.
4. Must contain at least one special symbol (e.g., `! @ # $ % ^ & *`).
5. All violated rules are returned simultaneously in a structured feedback list.
6. Complexity is enforced server-side, supplemented by client-side form hint text.

#### Test Scenarios

- **Manual (Success):** Enter `Ferry@2026` as a new password. Verify it is accepted without errors.
- **Manual (Failure / Edge Case):** Submit `ferry@2026` (missing uppercase), `FERRY@2026` (missing lowercase), `Ferry2026x` (missing symbol), and `abcdefgh` (multiple failures). Verify each attempt displays the exact list of missing requirements.
- **Automated (Unit / Slice / Integration):** JUnit 5 parameterized test for `PasswordPolicy.validate()` testing a matrix of valid, weak, short, and 73+ character passwords against expected error outputs.

#### Actionable Sub-tasks

- [ ] Implement `PasswordPolicy` validation utility class.
- [ ] Integrate password policy checks into `CreateUserForm` and `ChangePasswordForm`.
- [ ] Add policy hint text and live client-side validation hints to password views.
- [ ] Write parameterized JUnit tests covering all policy branches.

---

### Story 1.12: Change My Own Password

- **GitHub Title:** `[Auth] Change My Own Password`

- **Iteration:** `1`

- **Story Points:** `3`

- **Story Description:** As an authenticated user, I want to change my own password by verifying my current one, so that I can maintain my credentials independently without administrative intervention.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* The user is logged in.
  - *Trigger:* The user clicks "Change password", navigates to `/account/password`, and submits the form.

- **Actions / Postconditions:**

  - *Action:* The system verifies the current password hash, validates the new password against Story 1.11 rules, and stores the new BCrypt hash.
  - *Postcondition:* The old password is invalidated, and the new password is required for subsequent logins.

#### Acceptance Criteria

1. Form requires Current Password, New Password, and Confirm New Password.
2. Entering an incorrect current password displays "Current password is incorrect." and leaves credentials unchanged.
3. Mismatched new passwords display "New passwords do not match."
4. Entering a new password identical to the current password displays "New password must be different from the current password."
5. New passwords must satisfy all rules from Story 1.11.
6. Success redirects to `/dashboard` displaying "Your password has been updated."
7. Users can only modify their own credentials; target user ID cannot be overridden via request tampering.

#### Test Scenarios

- **Manual (Success):** Log in with a test account, navigate to `/account/password`, enter current credentials, and set new password to `Harbor#2026`. Verify success notification appears, log out, and log in with the new password.
- **Manual (Failure / Edge Case):** Enter an invalid current password, then mismatched new passwords, then reuse the current password. Verify each scenario displays its respective error message and leaves stored credentials intact.
- **Automated (Unit / Slice / Integration):** JUnit test of `UserService.changePassword()` with `BCryptPasswordEncoder`: valid current password replaces the hash; incorrect current password returns an error status and aborts save. `MockMvc` POST as an authenticated user asserts redirect to `/dashboard?passwordChanged`.

#### Actionable Sub-tasks

- [ ] Implement `ChangePasswordForm` DTO with validation.
- [ ] Add `UserService.changePassword()` verifying existing hash and hashing new password.
- [ ] Create `GET /account/password` and `POST /account/password` in `AccountController`.
- [ ] Build `templates/account/change-password.html` with field error indicators.
- [ ] Write unit and controller slice tests for password updates.

---

### Story 1.13: Store Passwords as Hashes

- **GitHub Title:** `[Auth] Hash Stored Passwords`

- **Iteration:** `1`

- **Story Points:** `1`

- **Story Description:** As a Caracal administrator, I want passwords stored strictly as salted one-way hashes, so that a database breach does not expose user credentials.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* A password is created or updated.
  - *Trigger:* Account provisioning, self-service password update, or initial database seeding.

- **Actions / Postconditions:**

  - *Action:* The application passes the plaintext password through `BCryptPasswordEncoder` prior to writing to storage.
  - *Postcondition:* The `password_hash` database column stores a BCrypt string starting with `$2a$` or `$2b$`. Plaintext passwords are never persisted.

#### Acceptance Criteria

1. All stored passwords in `app_users.password_hash` are formatted as valid BCrypt hashes.
2. Plaintext passwords never appear in application log files, database dumps, or UI templates.
3. Authentication evaluates hashes via `PasswordEncoder.matches()`. Entering a pre-computed hash into the password input does not authenticate.

#### Test Scenarios

- **Manual (Success):** Query the `app_users` table in PostgreSQL. Verify all `password_hash` values begin with `$2` and contain no readable text.
- **Manual (Failure / Edge Case):** Copy a valid BCrypt hash from the database and paste it directly into the password field on `/login`. Verify authentication fails with "Invalid email/username or password."
- **Automated (Unit / Slice / Integration):** JUnit test: asserting that after `createUser`, the persisted `passwordHash` does not equal the raw input, starts with `$2`, and evaluates to `true` when tested with `passwordEncoder.matches(raw, hash)`.

#### Actionable Sub-tasks

- [ ] Declare `BCryptPasswordEncoder` bean in `SecurityConfig`.
- [ ] Verify hashing is applied in user provisioning, password updates, and test data seeding.
- [ ] Write automated tests asserting hash formatting and matching behavior.

---

### Story 1.14: Google Login

- **GitHub Title:** `[Auth] Google Login`

- **Iteration:** `2`

- **Story Points:** `3`

- **Story Description:** As a staff member, I want to sign in with my authorized Google account, so that I can access the configuration tool quickly without managing a separate local password.

- **Actors / Personas:** Authenticated Operator (`OPERATOR`), Caracal Administrator (`CLIENT_SERVICES`)

- **Preconditions & Triggers:**

  - *Precondition:* The user's Google email exists in `app_users` and a Google OAuth2 client is registered.
  - *Trigger:* The user clicks "Sign in with Google" on the login page.

- **Actions / Postconditions:**

  - *Action:* The user authenticates with Google, and the application matches the verified email claim to an existing `app_users` record.
  - *Postcondition:* The user is redirected to the role-based dashboard matching their database permissions.

Story 1.14: Google Login (Removed: the client does not require it, confirmed at the Nov 6 sync)

**Reason:** At the Nov 6 client sync, Eldon confirmed that Google login is not required. Staff keep signing in with a username or email and password, using accounts created by a Client Services admin (Stories 1.1 and 1.9).
**Impact:** Nothing else changes. Roles, sessions, automatic logout and the X-Actor header (Story 1.15) do not depend on Google login.
**GitHub issue:** Closed as not planned. It does not count toward velocity.



### Story 1.15: Operator Identity on Caracal API Requests

**GitHub Title:** `[Epic 1] Send operator identity in X-Actor header`
**Iteration:** <span class="it2-modified">2 (implemented in Iteration 2)</span>
**Story Points:** `2`
**Story Description:** As a Client Services Admin, I want <span class="it2-modified">every action taken in the tool to carry the signed-in operator's email, including requests sent to the Caracal platform in the X-Actor header</span>, so that each configuration change can be traced to a person.
**Actors / Personas:** Operator (`OPERATOR`), Caracal Client Services Admin (`CLIENT_SERVICES`)
**Preconditions & Triggers:**
*Precondition:* <span class="it2-modified">The user is signed in and uses a feature that changes data, such as saving a fare change or a service alert.</span>
*Trigger:* <span class="it2-modified">The user saves, publishes or archives something.</span>
**Actions / Postconditions:**
*Action:* <span class="it2-modified">ActorService reads the signed-in user's account from the server-side session and builds the value: the word operator, a colon, then the user's email. The feature passes this value on.</span>
*Postcondition:* <span class="it2-modified">The change is traceable to the person. Service alerts store the actor in their created, updated, published and archived fields, and Caracal API requests carry the same value in the X-Actor header.</span>

### Acceptance Criteria

1. The value is the word operator, a colon, and the email stored on the signed-in account, for example `operator:admin@caracal.com`.
2. The value always comes from the server-side session and never from user input.
3. <span class="it2-modified">ActorService refuses to produce a value when nobody is signed in, so no anonymous change is attributed to anyone and no Caracal API call is made.</span>
4. <span class="it2-added">Service alerts keep who changed them: AlertService stores the actor it is given in the created-by, updated-by, published-by and archived-by fields.</span>
5. <span class="it2-added">Both roles produce the same format. The role does not change the header.</span>

### Test Scenarios

* <span class="it2-modified">**Manual (Success):** Log in as admin and save a service alert, then check that its updated-by value names the admin. Log in as operator, edit the same alert and check that the value now names the operator. For a Caracal API change, check that the outgoing request shows operator: followed by the signed-in email.</span>
* <span class="it2-modified">**Manual (Failure / Edge Case):** Log out and open a save address directly. The login page appears and no record is created or changed.</span>
* <span class="it2-modified">**Automated (Unit / Slice / Integration):** A JUnit test of ActorService.currentActorHeader() with a security context holding an AppUserDetails returns operator: plus the email, and throws IllegalStateException with no authentication. A JUnit and Mockito test of AlertService.save asserts that created-by and updated-by equal the actor passed in. A WireMock test asserts the X-Actor header on the outgoing Caracal API request.</span>

### Actionable Sub-tasks

- [x] ActorService.currentActorHeader() builds the header value
- [x] <span class="it2-added">AlertService stores the actor in the created-by, updated-by, published-by and archived-by fields</span>
- [ ] <span class="it2-added">Alert controllers pass the ActorService value as the actor</span>
- [ ] Attach the header to every Caracal API request in the REST client
- [ ] Automated ActorService, alert audit and WireMock tests


### Story 1.16: Search the User List

**GitHub Title:** `[Epic 1] Search the user list`
**Iteration:** `2`
**Story Points:** `2`
**Story Description:** As a Client Services Admin, I want to search the user list by username or email and see how many users there are, so that I can find an account quickly.
**Actors / Personas:** Caracal Client Services Admin (`CLIENT_SERVICES`)
**Preconditions & Triggers:**
  * *Precondition:* The admin is signed in and at least one account exists.
  * *Trigger:* The admin types in the search box on Manage users and clicks Search.
**Actions / Postconditions:**
  * *Action:* The system finds accounts whose username or email contains the typed text, ignoring capital letters.
  * *Postcondition:* The table shows only the matching accounts, sorted by username, with a summary line of counts. Clear returns to the full list.

### Acceptance Criteria

1. The Manage users page has a search box labeled "Search by username or email" and a Search button. A Clear button appears only while a search is active.
2. A search matches any part of the username or email, ignoring capital letters. Spaces at the start and end are ignored, and an empty search shows everyone.
3. Results are sorted by username.
4. A summary line reads "Showing X of Y users. Client Services: A. Operators: B."
5. When nothing matches, the table shows "No users match your search."
6. Operators cannot open this page (see Story 1.6).

### Test Scenarios

**Manual (Success):** Log in as admin, open Manage users and search for "ope". Only the operator accounts appear and the summary line shows the smaller count. Click Clear and the full list returns.
**Manual (Failure / Edge Case):** Search for "zzz" and check that "No users match your search." appears while the summary still shows the full total. Log in as operator and type the Manage users address into the browser: Access denied appears.
**Automated (Unit / Slice / Integration):** A JUnit and Mockito test of UserService.listUsers: a blank query returns all users sorted by username, and a query with spaces around it is trimmed before the repository search. A MockMvc test of a search request as an admin asserts the users, shownCount, totalUsers, adminCount and operatorCount model attributes.

### Actionable Sub-tasks

- [x] Repository search by username or email, ignoring case, with sorting
- [x] UserService.listUsers(query) and the count methods
- [x] Search form, summary line and empty-result row on the Manage users page
- [ ] Automated service and controller tests
:::

##### Story 1.17: Delete a User Account

* **GitHub Title:** `[Epic 1] Delete a user account`
* **Iteration:** `2`
* **Story Points:** `3`
* **Story Description:** As a Client Services Admin, I want to delete an account that should no longer have access, after a confirmation step, so that former staff cannot log in and the user list stays accurate.
* **Actors / Personas:** Caracal Client Services Admin (`CLIENT_SERVICES`)
* **Preconditions & Triggers:**
  * *Precondition:* The admin is signed in and the target account exists and is not the admin's own account.
  * *Trigger:* The admin clicks Delete on a user's row in Manage users.
* **Actions / Postconditions:**
  * *Action:* A confirmation page shows the username and email. The admin clicks Delete account to confirm or Cancel to go back. The system deletes the account and ends its active sessions.
  * *Postcondition:* The user is removed from the list with the message "User deleted. Any active sessions for that account have ended." The person can no longer log in, and if they were signed in elsewhere, their next click returns them to the login page.

###### Acceptance Criteria

1. Every row has a Delete button except the admin's own row, which shows "Not available".
2. The confirmation page is titled "Delete account?", shows the username and email, and says the deletion is permanent. Cancel returns to Manage users without deleting.
3. Opening the delete address only shows the confirmation. The deletion itself is a form submission with a CSRF token.
4. An admin cannot delete their own account, and the server enforces this too: "You cannot change or delete your own account."
5. The last remaining Client Services account cannot be deleted: "At least one Client Services account must remain."
6. Deleting an account that no longer exists shows "That user no longer exists."
7. Operators cannot reach the delete pages (see Story 1.6).

###### Test Scenarios

* **Manual (Success):** Create a throwaway account, click Delete on its row, then click Delete account on the confirmation page. The list shows the success message and the account is gone. Logging in as that account fails with "Invalid email/username or password."
* **Manual (Failure / Edge Case):** Look at your own row: there is no Delete button. Click Cancel on a confirmation page: nothing is deleted. Keep a throwaway account signed in in another browser window, delete it, then click a link in that window: it goes to the login page. As operator, type a delete address into the browser: Access denied appears.
* **Automated (Unit / Slice / Integration):** JUnit and Mockito tests of UserService.deleteUser: deleting yourself returns "self" and never deletes, an unknown id returns "notfound", the only remaining Client Services account returns "lastadmin", and a valid delete removes the account. A MockMvc test asserts that a delete as OPERATOR returns 403 and as CLIENT_SERVICES redirects to the user list with the deleted message.

###### Actionable Sub-tasks

- [x] UserService.deleteUser with self-delete and last-admin guards
- [x] End the deleted user's active sessions through the session repository
- [x] AdminController confirmation page and delete routes
- [x] Confirmation page and Delete column on the Manage users page
- [ ] Automated service and controller tests
:::