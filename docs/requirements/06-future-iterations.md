## Future Iterations (Iteration 3 Backlog)

<span class="it2-modified">In Iteration 2, the team prioritized local PostgreSQL staging, input validation, and core operator workflows; live Caracal REST API integration and advanced zonal rate tier tooling (`Stories 2.3–2.5`) have been scheduled for Iteration 3.</span>

### Epic 1: Authentication & Platform Identity

- **Story 1.15 — Operator Identity on Caracal API Requests (`2 pts`):** Attach the authenticated user's `X-Actor: operator:<email>` HTTP header via `ActorService` on all mutating REST calls to the Caracal sandbox API.
- <span class="it2-removed">~~**Story 1.14 — Google OAuth2 SSO Login (`3 pts`):** Removed from scope per client feedback to keep focus on core fare, availability, and publishing workflows.~~</span>

### Epic 2: Interactive Origin-Destination Rate Grid & Fare Management

- <span class="it2-modified">**Story 2.3 (`2.3a` & `2.3b`) — Zonal Cell Action Modal for Rate Tier Management (`5 pts`):** *(Deferred from Iteration 2)* Open a 3-way action modal when clicking Zonal Mode (`METRA`) cells to update a shared rate tier globally, reassign the cell to an existing tier, or create a new rate tier in staging.</span>
- <span class="it2-modified">**Story 2.4 (`2.4a` & `2.4b`) — Global Rate Tier Catalog & Bulk Adjustment Tool (`5 pts`):** *(Deferred from Iteration 2)* Provide a consolidated catalog view of active rate tiers with a bulk percentage (`+%`) and flat (`+$`) adjustment engine using half-up cent rounding.</span>
- <span class="it2-modified">**Story 2.5 — Station-to-Zone Topology Mapping Side Panel (`3 pts`):** *(Deferred from Iteration 2)* Add a slide-out offcanvas drawer in Zonal Mode to search and reassign stations to zone dimensions (`site-dimension-bind`).</span>

### Epic 3: Ticket Availability & Product Lifecycle Management

- <span class="it2-modified">**Story 3.2 — Permanent Phase-Out Handling & Local Persistence (`2 pts`):** *(Deferred from Iteration 2)* Add `isPhasedOut` lifecycle flags with confirmation modals and `CLIENT_SERVICES`-only reactivation guardrails.</span>
- **Story 3.4 — Backend REST API Integration for Availability & When-Conditions (`3 pts`):** Dispatch persisted `AvailabilityRule` and `TemporalCondition` records to Caracal's `/functions/rate-assignment-update` and `/functions/when-condition-create` endpoints with `X-Actor` headers.
- **Story 3.5 — Automated Unit & Integration Testing for Epic 3 (`2 pts`):** Add boundary unit tests for `TemporalCondition.isActiveAt()` and WireMock integration tests for Caracal `rate-assignment-update` and `when-condition-create` payload serialization.
- **Story 3.6 — Documentation for Availability & Temporal Rules Management (`1 pt`):** Author `docs/availability-guide.md` covering terminal availability workflows, temporal rule syntax, and Caracal API mappings.

### Epic 4: Service Alerts & Pre-Purchase Notices

- <span class="it2-modified">**Story 4.4: Configurable Alert Schedule (`5 pts`).** *(Moved from Iteration 2, rewritten after client feedback)* Replace the Weekends trigger with day-of-week, service-hour, and holiday rules. The alert names the next valid time, and the rider can still buy.</span>
- <span class="it2-modified">**Story 4.5: Suggested and Default Button Labels (`1 pt`).** *(Rest of Iteration 2 story)* Suggest "Buy for [Day]" labels and fall back to Continue / Cancel when blank.</span>
- <span class="it2-added">**Story 4.7: Tenant Holiday Calendar (`2 pts`).** Define one-time and recurring holidays once per tenant, reused by every alert.</span>
- <span class="it2-added">**Story 4.8: "When Will This Show?" Preview (`2 pts`).** Show the next 14 days on the form and let the operator test a date and time before making an alert active.</span>
- <span class="it2-added">**Story 4.9: Archived Alerts and Copy as Draft (`2 pts`).** Archived tab with who archived each alert, when, and what replaced it, plus a button to reuse an old alert as a new draft.</span>
- <span class="it2-added">**Story 4.10: Change Review and History (`2 pts`).** Show old vs new text, routes, and schedule before a replacement goes live, plus a per-alert history (created, edited, made active, archived).</span>
- <span class="it2-added">**Story 4.11: End Date and "Showing Now" View (`2 pts`).** Alerts can expire on their own, and one page lists which alerts riders see right now on each route.</span>
- <span class="it2-added">**Real Routes and Languages:** Load each tenant's real route list and one message box per supported locale (for example Polish and Chinese for Metra).</span>

### Epic 5: Configuration Review, Validation, and Publishing

- <span class="it2-modified">**Story 5.1 – Published Configuration Comparison (Deferred to Iteration 3):** Extend the existing fare draft review to compare staged changes against a reliable published Caracal baseline and support schedule and availability changes.</span>
- <span class="it2-modified">**Story 5.2 – Validate Configuration Discrepancies (`3 pts`):** Implement server-side validation against the NY Waterway reference dataset to detect incorrect ticket prices, availability settings, and monthly pass sale windows.</span>
- <span class="it2-modified">**Story 5.3 – Publish Verified Configurations (`2 pts`):** Enable authorized administrators to publish reviewed and validated configurations through the Caracal API, with confirmation and appropriate error handling.</span>
- <span class="it2-modified">**Story 5.4 – Configuration Change and Publication History (`2 pts`):** Display read-only configuration changes and publication records, including responsible actors, timestamps, and operation details.</span>