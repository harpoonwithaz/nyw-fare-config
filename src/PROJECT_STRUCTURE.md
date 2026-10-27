# NYW Fare Configuration source structure

This document defines the recommended organization for the Spring Boot MVC
application. The structure follows the five project epics while keeping shared
cross-cutting concerns (security, persistence, validation, and external API
integration) in dedicated packages.

## Recommended layout

```text
src/
├── main/
│   ├── java/ca/sfu/cmpt276/nywfareconfig/
│   │   ├── NywFareConfigApplication.java
│   │   │
│   │   ├── config/
│   │   │   ├── WebConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   ├── WebClientConfig.java
│   │   │   └── ThymeleafConfig.java
│   │   │
│   │   ├── security/
│   │   │   ├── controller/
│   │   │   │   └── AuthenticationController.java
│   │   │   ├── service/
│   │   │   │   ├── OperatorDetailsService.java
│   │   │   │   └── AuthenticationService.java
│   │   │   ├── model/
│   │   │   │   ├── Operator.java
│   │   │   │   └── Role.java
│   │   │   └── repository/
│   │   │       └── OperatorRepository.java
│   │   │
│   │   ├── fare/
│   │   │   ├── controller/
│   │   │   │   └── FareController.java
│   │   │   ├── service/
│   │   │   │   ├── FareService.java
│   │   │   │   └── FareDraftService.java
│   │   │   ├── model/
│   │   │   │   ├── FareRule.java
│   │   │   │   ├── FareProduct.java
│   │   │   │   ├── Terminal.java
│   │   │   │   └── Route.java
│   │   │   ├── dto/
│   │   │   │   ├── FareGridView.java
│   │   │   │   ├── FareChangeRequest.java
│   │   │   │   └── FareChangeSummary.java
│   │   │   ├── validation/
│   │   │   │   └── ValidFareChange.java
│   │   │   └── repository/
│   │   │       └── FareDraftRepository.java
│   │   │
│   │   ├── availability/
│   │   │   ├── controller/
│   │   │   │   └── AvailabilityController.java
│   │   │   ├── service/
│   │   │   │   └── AvailabilityService.java
│   │   │   ├── model/
│   │   │   │   ├── ProductAvailability.java
│   │   │   │   └── SalesWindow.java
│   │   │   ├── dto/
│   │   │   │   ├── AvailabilityView.java
│   │   │   │   └── AvailabilityChangeRequest.java
│   │   │   └── repository/
│   │   │       └── AvailabilityDraftRepository.java
│   │   │
│   │   ├── alert/
│   │   │   ├── controller/
│   │   │   │   └── AlertController.java
│   │   │   ├── service/
│   │   │   │   └── AlertService.java
│   │   │   ├── model/
│   │   │   │   ├── ServiceAlert.java
│   │   │   │   ├── RouteSchedule.java
│   │   │   │   ├── AlertAction.java
│   │   │   │   └── SupportedLanguage.java
│   │   │   ├── dto/
│   │   │   │   ├── AlertForm.java
│   │   │   │   └── TvmPreviewModel.java
│   │   │   └── repository/
│   │   │       ├── ServiceAlertRepository.java
│   │   │       └── RouteScheduleRepository.java
│   │   │
│   │   ├── publishing/
│   │   │   ├── controller/
│   │   │   │   └── PublishingController.java
│   │   │   ├── service/
│   │   │   │   ├── ValidationService.java
│   │   │   │   ├── StagingService.java
│   │   │   │   └── PublishingService.java
│   │   │   ├── model/
│   │   │   │   ├── ConfigurationDraft.java
│   │   │   │   ├── ValidationResult.java
│   │   │   │   └── AuditLogEntry.java
│   │   │   ├── dto/
│   │   │   │   ├── ReviewView.java
│   │   │   │   └── PublishRequest.java
│   │   │   └── repository/
│   │   │       ├── ConfigurationDraftRepository.java
│   │   │       └── AuditLogRepository.java
│   │   │
│   │   ├── integration/
│   │   │   └── caracal/
│   │   │       ├── CaracalClient.java
│   │   │       ├── CaracalClientException.java
│   │   │       ├── CaracalRequestMapper.java
│   │   │       ├── CaracalResponseMapper.java
│   │   │       └── dto/
│   │   │           ├── CatalogResponse.java
│   │   │           ├── RateUpdateRequest.java
│   │   │           ├── PublishRequest.java
│   │   │           └── AuditLogResponse.java
│   │   │
│   │   ├── common/
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   └── ConfigurationValidationException.java
│   │   │   ├── model/
│   │   │   │   └── ApiError.java
│   │   │   └── validation/
│   │   │       └── ValidationMessages.java
│   │   │
│   │   └── dashboard/
│   │       ├── controller/
│   │       │   └── DashboardController.java
│   │       └── dto/
│   │           └── DashboardView.java
│   │
│   ├── resources/
│   │   ├── application.properties
│   │   ├── application-local.properties
│   │   ├── db/migration/
│   │   │   ├── V1__create_operator_tables.sql
│   │   │   ├── V2__create_fare_draft_tables.sql
│   │   │   ├── V3__create_alert_and_schedule_tables.sql
│   │   │   └── V4__create_audit_log_tables.sql
│   │   ├── templates/
│   │   │   ├── layout/
│   │   │   │   ├── base.html
│   │   │   │   ├── fragments.html
│   │   │   │   └── navigation.html
│   │   │   ├── auth/
│   │   │   │   ├── login.html
│   │   │   │   └── access-denied.html
│   │   │   ├── dashboard/
│   │   │   │   └── index.html
│   │   │   ├── fare/
│   │   │   │   ├── grid.html
│   │   │   │   ├── change-summary.html
│   │   │   │   └── fragments/
│   │   │   │       └── fare-grid.html
│   │   │   ├── availability/
│   │   │   │   ├── index.html
│   │   │   │   └── sales-window-form.html
│   │   │   ├── alerts/
│   │   │   │   ├── index.html
│   │   │   │   ├── form.html
│   │   │   │   └── tvm-preview.html
│   │   │   └── publishing/
│   │   │       ├── review.html
│   │   │       ├── validation-results.html
│   │   │       └── publish-confirmation.html
│   │   └── static/
│   │       ├── css/
│   │       │   ├── application.css
│   │       │   ├── fare-grid.css
│   │       │   └── tvm-preview.css
│   │       ├── js/
│   │       │   ├── fare-grid.js
│   │       │   ├── alert-form.js
│   │       │   ├── tvm-preview.js
│   │       │   └── review.js
│   │       └── images/
│   │           └── nyw-logo.svg
│   │
│   └── test/
│       ├── java/ca/sfu/cmpt276/nywfareconfig/
│       │   ├── security/
│       │   ├── fare/
│       │   ├── availability/
│       │   ├── alert/
│       │   ├── publishing/
│       │   └── integration/
│       └── resources/
│           ├── application-test.properties
│           ├── fixtures/
│           │   ├── catalog-response.json
│           │   └── fare-grid.json
│           └── templates/
│               └── thymeleaf/
```

## Package responsibilities

- **`config`** contains application wiring only: Spring Security rules,
  `WebClient` defaults, MVC settings, and template configuration.
- **`security`** owns operator login, logout, session expiry, roles, and the
  `X-Actor: operator:<email>` identity used for Caracal requests.
- **`fare`** owns the origin-destination/product matrix, currency validation,
  pending fare drafts, and the save-change confirmation flow.
- **`availability`** owns product enablement, route-specific availability, and
  recurring sales windows such as monthly-pass dates.
- **`alert`** owns bilingual notices, route schedules, configurable actions,
  and the TVM preview model.
- **`publishing`** owns staging, baseline validation, comparison/review, catalog
  publishing, and immutable audit records.
- **`integration.caracal`** is the only package that should know Caracal REST
  endpoint paths or external JSON shapes. Feature services should call the
  client through typed request/response objects.
- **`common`** contains cross-cutting error handling and shared validation
  types; it should not contain feature-specific business logic.
- **`dashboard`** is a thin composition layer for the landing page and current
  draft/publishing status. It should delegate to feature services.

## MVC and naming conventions

1. Controllers return Thymeleaf view names for browser requests. Keep business
   rules out of controllers; bind and validate form objects, then delegate to a
   service.
2. Use `dto` classes at controller boundaries and `model` classes for persisted
   domain entities. Do not expose JPA entities directly to templates or the
   Caracal client.
3. Keep one public controller per feature area. Use explicit route prefixes,
   for example `/fares`, `/availability`, `/alerts`, and `/publishing`.
4. Put browser-only behavior in `static/js` and browser-only styling in
   `static/css`. Server-rendered HTML belongs in `templates`, not `static`.
5. Use Thymeleaf fragments for the shared navigation, flash messages, form
   errors, and table controls. Feature pages should extend the shared layout.
6. Validate fare amounts and schedule rules twice: with browser JavaScript for
   immediate feedback and with server-side Bean Validation/service validation
   before any draft or external API call is made.
7. Keep external API calls transactional from the application perspective:
   stage and validate local changes first, then publish through one service that
   records the result in the audit log.

## Test organization

Each feature package under `src/test/java` should mirror its production
package. Prefer:

- service unit tests for fare, availability, alert, and publishing rules;
- `@WebMvcTest` tests for controller validation, access control, and view names;
- repository tests against the test database for persistence mappings;
- `MockWebServer` or a similar HTTP test double for Caracal client mappings; and
- an end-to-end publishing test covering draft → validation → review → publish →
  audit log.

The existing `static/index.html` can remain as the temporary starter page while
the MVC shell is being implemented. Once `DashboardController` and the
Thymeleaf layout exist, the application entry route should render
`templates/dashboard/index.html` instead of using a static landing page.
