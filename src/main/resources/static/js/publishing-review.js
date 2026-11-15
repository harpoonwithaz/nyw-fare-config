/**
Retrieves configuration comparisons from Spring Boot.
 Supports O-D and zonal fare configurations.
*/
(function () {
    "use strict";

    const tableBody = document.getElementById("fareChangesTableBody");
    const changedOnlyCheckbox = document.getElementById("showChangedOnly");

    // PR COMMENT #5: Summary counter elements.
    const totalCountElement = document.getElementById("totalItemsCount");
    const changedCountElement = document.getElementById("changedItemsCount");
    const unchangedCountElement = document.getElementById("unchangedItemsCount");

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

    // PR COMMENT #5:
    // Count the API fare records and server-rendered schedule records.
    function updateSummaryCounters(fareChanges) {
        const scheduleRows = document.querySelectorAll(
            "#scheduleChangesTableBody tr[data-changed]"
        );

        const totalFares = fareChanges.length;
        const changedFares = fareChanges.filter(
            change => change.changed === true
        ).length;

        const totalSchedules = scheduleRows.length;
        const changedSchedules = Array.from(scheduleRows).filter(
            row => row.dataset.changed === "true"
        ).length;

        const total = totalFares + totalSchedules;
        const changed = changedFares + changedSchedules;
        const unchanged = total - changed;

        if (totalCountElement) {
            totalCountElement.textContent = total;
        }

        if (changedCountElement) {
            changedCountElement.textContent = changed;
        }

        if (unchangedCountElement) {
            unchangedCountElement.textContent = unchanged;
        }
    }

    // PR COMMENT #6:
    // Hide unchanged fare and schedule rows when the filter is checked.
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

            // PR COMMENT #5: Still count schedule records.
            updateSummaryCounters(changes);
            return;
        }

        changes.forEach(function (change) {
            const row = document.createElement("tr");
            row.dataset.changed = String(change.changed);

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
                change.changed ? "table-warning" : ""
            ));

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

        // PR COMMENT #5: Synchronize counters after fetching fares.
        updateSummaryCounters(changes);

        // PR COMMENT #6: Apply the default filter.
        applyChangedOnlyFilter();
    }

    // STORY 5.1: Retrieve tenant-scoped changes from Spring Boot.
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

            // Do not display stale counters if the API fails.
            if (totalCountElement) totalCountElement.textContent = "—";
            if (changedCountElement) changedCountElement.textContent = "—";
            if (unchangedCountElement) unchangedCountElement.textContent = "—";
        }
    }

    // STORY 5.1: Initialize the review page.
    document.addEventListener("DOMContentLoaded", function () {

        // PR COMMENT #6: Changed-only is the default.
        if (changedOnlyCheckbox) {
            changedOnlyCheckbox.checked = true;

            changedOnlyCheckbox.addEventListener(
                "change",
                applyChangedOnlyFilter
            );
        }

        applyChangedOnlyFilter();
        loadFareChanges();
    });

})();


