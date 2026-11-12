/**
Retrieves configuration comparisons from Spring Boot.
 Supports O-D and zonal fare configurations.
*/

(function () {
    "use strict";

    const tableBody = document.getElementById("fareChangesTableBody");
    const changedOnlyCheckbox = document.getElementById("showChangedOnly");

    // STORY 5.1: Format currency values.
    function formatPrice(price) {
        if (price === null || price === undefined) {
            return "N/A";
        }

        return "$" + Number(price).toFixed(2);
    }

    // STORY 5.1: Safely create a table cell.
    function createCell(value, className = "") {
        const cell = document.createElement("td");
        cell.textContent = value ?? "";

        if (className) {
            cell.className = className;
        }

        return cell;
    }

    // STORY 5.1: Filter unchanged rows in both tables.
    function applyChangedOnlyFilter() {
        const rows = document.querySelectorAll("tr[data-changed]");

        rows.forEach(function (row) {
            const isChanged = row.dataset.changed === "true";

            row.style.display =
                changedOnlyCheckbox?.checked && !isChanged
                    ? "none"
                    : "";
        });
    }

    // STORY 5.1: Render configuration changes from the API.
    function renderFareChanges(changes) {
        if (!tableBody) {
            return;
        }

        tableBody.replaceChildren();

        if (changes.length === 0) {
            const row = document.createElement("tr");
            const cell = createCell(
                "No fare configurations available for review.",
                "text-center text-muted py-3"
            );

            cell.colSpan = 8;
            row.appendChild(cell);
            tableBody.appendChild(row);
            return;
        }

        changes.forEach(function (change) {
            const row = document.createElement("tr");
            row.dataset.changed = String(change.changed);

            // Tenant and grid mode
            row.appendChild(createCell(change.tenantCode));
            row.appendChild(createCell(change.gridMode));

            // Context: origin terminal for O-D, product for zonal
            row.appendChild(createCell(change.selectedContext));

            // O-D: destination; Zonal: origin zone → destination zone
            const location = change.gridMode === "ZONAL"
                ? change.rowKey + " → " + change.colKey
                : change.rowKey;

            row.appendChild(createCell(location));

            // Ticket product or tier
            row.appendChild(createCell(change.product));

            // Published baseline
            row.appendChild(createCell(
                formatPrice(change.publishedPrice)
            ));

            // Staged draft, highlighted if changed
            row.appendChild(createCell(
                formatPrice(change.stagedPrice),
                change.changed ? "table-warning" : ""
            ));

            // Changed / Unchanged badge
            const statusCell = document.createElement("td");
            const badge = document.createElement("span");

            badge.className = change.changed
                ? "badge bg-warning text-dark"
                : "badge bg-success";

            badge.textContent = change.changed
                ? "Changed"
                : "Unchanged";

            statusCell.appendChild(badge);
            row.appendChild(statusCell);

            tableBody.appendChild(row);
        });

        applyChangedOnlyFilter();
    }

    // STORY 5.1: Retrieve comparisons from Spring Boot.
    async function loadFareChanges() {
        if (!tableBody) {
            return;
        }

        try {
            const response = await fetch("/api/publishing/pending-changes");

            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }

            const changes = await response.json();

            renderFareChanges(changes);

        } catch (error) {
            console.error("Failed to load configuration changes:", error);

            tableBody.replaceChildren();

            const row = document.createElement("tr");
            const cell = createCell(
                "Unable to load configuration changes. Please refresh.",
                "text-center text-danger py-3"
            );

            cell.colSpan = 8;
            row.appendChild(cell);
            tableBody.appendChild(row);
        }
    }

    // STORY 5.1: Initialize the page.
    document.addEventListener("DOMContentLoaded", function () {
        loadFareChanges();

        if (changedOnlyCheckbox) {
            changedOnlyCheckbox.addEventListener(
                "change",
                applyChangedOnlyFilter
            );
        }
    });

})();

