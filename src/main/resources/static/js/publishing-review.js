/**
Retrieves configuration comparisons from Spring Boot.
 Supports O-D and zonal fare configurations.
*/
(function () {
    "use strict";

    const tableBody = document.getElementById("fareChangesTableBody");

    // Summary counter elements.
    const totalCountElement = document.getElementById("totalItemsCount");
    const changedCountElement = document.getElementById("changedItemsCount");
    const unchangedCountElement = document.getElementById("unchangedItemsCount");

    // Format currency values.
    function formatPrice(price) {
        if (price === null || price === undefined) {
            return "N/A";
        }

        return "$" + Number(price).toFixed(2);
    }

    // Safely create a table cell.
    function createCell(value, className = "") {
        const cell = document.createElement("td");
        cell.textContent = value ?? "";

        if (className) {
            cell.className = className;
        }

        return cell;
    }

    // Update summary counters using actual pending fare changes.
    function updateSummaryCounters(fareChanges) {
        const changedFares = fareChanges.filter(
            change => change.changed === true
        ).length;

        const availabilityCount = Number(
            totalCountElement?.dataset.availabilityCount ?? 0
        );

        const total = changedFares + availabilityCount;

        if (totalCountElement) {
            totalCountElement.textContent = total;
        }

        if (changedCountElement) {
            changedCountElement.textContent = changedFares;
        }

        if (unchangedCountElement) {
            unchangedCountElement.textContent = 0;
        }
    }

    // Render only changed fare drafts.
    function renderFareChanges(changes) {
        if (!tableBody) {
            return;
        }

        tableBody.replaceChildren();

        const changedFares = changes.filter(
            change => change.changed === true
        );

        if (changedFares.length === 0) {
            const row = document.createElement("tr");
            const cell = createCell(
                "No pending fare changes.",
                "text-center text-muted py-3"
            );

            cell.colSpan = 8;
            row.appendChild(cell);
            tableBody.appendChild(row);

            updateSummaryCounters(changedFares);
            return;
        }

        changedFares.forEach(function (change) {
            const row = document.createElement("tr");
            row.dataset.changed = "true";

            row.appendChild(createCell(change.tenantCode));
            row.appendChild(createCell(change.gridMode));
            row.appendChild(createCell(change.selectedContext));

            const location = change.gridMode === "ZONAL"
                ? change.rowKey + " → " + change.colKey
                : change.rowKey;

            row.appendChild(createCell(location));
            row.appendChild(createCell(change.product));

            row.appendChild(createCell(
                formatPrice(change.publishedPrice)
            ));

            row.appendChild(createCell(
                formatPrice(change.stagedPrice),
                "table-warning"
            ));

            const statusCell = document.createElement("td");
            const badge = document.createElement("span");

            badge.className = "badge bg-warning text-dark";
            badge.textContent = "Changed";

            statusCell.appendChild(badge);
            row.appendChild(statusCell);
            tableBody.appendChild(row);
        });

        updateSummaryCounters(changedFares);
    }

    // Retrieve tenant-scoped pending changes from Spring Boot.
    async function loadFareChanges() {
        if (!tableBody) {
            return;
        }

        try {
            const response = await fetch(
                "/api/publishing/pending-changes"
            );

            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }

            const changes = await response.json();
            renderFareChanges(changes);

        } catch (error) {
            console.error(
                "Failed to load configuration changes:",
                error
            );

            tableBody.replaceChildren();

            const row = document.createElement("tr");
            const cell = createCell(
                "Unable to load configuration changes. Please refresh.",
                "text-center text-danger py-3"
            );

            cell.colSpan = 8;
            row.appendChild(cell);
            tableBody.appendChild(row);

            // Avoid displaying stale counters after an API failure.
            if (totalCountElement) {
                totalCountElement.textContent = "—";
            }

            if (changedCountElement) {
                changedCountElement.textContent = "—";
            }

            if (unchangedCountElement) {
                unchangedCountElement.textContent = "—";
            }
        }
    }

    // Initialize the review page.
    document.addEventListener("DOMContentLoaded", function () {
        loadFareChanges();
    });

})();


