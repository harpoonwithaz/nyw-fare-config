## Future Iterations (Iteration 3 Backlog)

<span class="it2-modified">In Iteration 2, the team prioritized local PostgreSQL staging, input validation, and core operator workflows; live Caracal REST API integration and advanced zonal rate tier tooling (`Stories 2.3–2.5`) have been scheduled for Iteration 3.</span>

### Epic 1: Authentication & Platform Identity

* **Story 1.15 — Operator Identity on Caracal API Requests (`2 pts`):** Attach the authenticated user's `X-Actor: operator:<email>` HTTP header via `ActorService` on all mutating REST calls to the Caracal sandbox API.
* <span class="it2-removed">~~**Story 1.14 — Google OAuth2 SSO Login (`3 pts`):** Removed from scope per client feedback to keep focus on core fare, availability, and publishing workflows.~~</span>

### Epic 2: Interactive Origin-Destination Rate Grid & Fare Management

* <span class="it2-modified">**Story 2.3 (`2.3a` & `2.3b`) — Zonal Cell Action Modal for Rate Tier Management (`5 pts`):** *(Deferred from Iteration 2)* Open a 3-way action modal when clicking Zonal Mode (`METRA`) cells to update a shared rate tier globally, reassign the cell to an existing tier, or create a new rate tier in staging.</span>
* <span class="it2-modified">**Story 2.4 (`2.4a` & `2.4b`) — Global Rate Tier Catalog & Bulk Adjustment Tool (`5 pts`):** *(Deferred from Iteration 2)* Provide a consolidated catalog view of active rate tiers with a bulk percentage (`+%`) and flat (`+$`) adjustment engine using half-up cent rounding.</span>
* <span class="it2-modified">**Story 2.5 — Station-to-Zone Topology Mapping Side Panel (`3 pts`):** *(Deferred from Iteration 2)* Add a slide-out offcanvas drawer in Zonal Mode to search and reassign stations to zone dimensions (`site-dimension-bind`).</span>

### Epic 3: Ticket Availability & Product Lifecycle Management

### Epic 4: Service Alerts & Pre-Purchase Notices

### Epic 5: Configuration Review, Validation, and Publishing

* <span class="it2-modified">**Story 5.1 – Published Configuration Comparison (Deferred to Iteration 3):** Extend the existing fare draft review to compare staged changes against a reliable published Caracal baseline and support schedule and availability changes.</span>
* <span class="it2-modified">**Story 5.2 – Validate Configuration Discrepancies (`3 pts`):** Implement server-side validation against the NY Waterway reference dataset to detect incorrect ticket prices, availability settings, and monthly pass sale windows.</span>
* <span class="it2-modified">**Story 5.3 – Publish Verified Configurations (`2 pts`):** Enable authorized administrators to publish reviewed and validated configurations through the Caracal API, with confirmation and appropriate error handling.</span>
* <span class="it2-modified">**Story 5.4 – Configuration Change and Publication History (`2 pts`):** Display read-only configuration changes and publication records, including responsible actors, timestamps, and operation details.</span>

