/* STORY 2.6b: Transaction Review Diff Modal
   Displays staged in-memory fare changes from DraftStateManager before batch submission. */

(function () {
    "use strict";

    // Format prices consistently as US currency.
    function formatPrice(price) {
        const parsed = Number(price);
        return Number.isFinite(parsed) ? "$" + parsed.toFixed(2) : "$0.00";
    }

    // Populate the comparison table with in-memory drafts.
    function renderReviewTable(changes) {
        const tableBody = document.getElementById("rateReviewTableBody");
        const changeCount = document.getElementById("rateReviewChangeCount");

        if (!tableBody || !changeCount) {
            return;
        }

        tableBody.replaceChildren();
        changeCount.textContent = String(changes.length);

        if (changes.length === 0) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");

            cell.colSpan = 5;
            cell.className = "text-center text-muted py-4";
            cell.textContent = "No pending rate changes to display.";

            row.appendChild(cell);
            tableBody.appendChild(row);
            return;
        }

        changes.forEach(function (change) {
            const row = document.createElement("tr");
            const originalRate = change.originalValue ?? change.originalPrice;
            const newRate = change.newValue ?? change.newPrice;

            const values = [
                change.origin,
                change.destination,
                change.product,
                formatPrice(originalRate),
                formatPrice(newRate)
            ];

            values.forEach(function (value, index) {
                const cell = document.createElement("td");
                cell.textContent = value;

                // Make the New Rate column have the same visual properties as fare drafts in the fare grid
                if (index === 4) {
                    cell.classList.add(
                        "table-warning",
                        "cell-draft",
                        "fw-bold",
                        "font-monospace",
                        "text-dark"
                    );
                }

                row.appendChild(cell);
            });

            tableBody.appendChild(row);
        });
    }

    // Open the Bootstrap review modal.
    function openReviewModal(changes) {
        const modalElement = document.getElementById("rateReviewModal");

        if (!modalElement || !window.bootstrap) {
            return;
        }

        renderReviewTable(changes);

        const errorBanner = document.getElementById("rateReviewError");
        if (errorBanner) {
            errorBanner.hidden = true;
            errorBanner.textContent = "";
        }

        const modal = window.bootstrap.Modal.getOrCreateInstance(modalElement);
        modal.show();
    }

    // Wire the "Save Rate Changes" toolbar button to open the review modal.
    function initializeRateReview() {
        const saveButton = document.getElementById("saveChangesBtn");

        if (!saveButton) {
            return;
        }

        saveButton.addEventListener("click", function () {
            // Do not open the review modal if an inline input currently has a validation error.
            if (document.querySelector(".fare-inline-input.is-invalid")) {
                return;
            }

            const pendingChanges = window.DraftStateManager
                ? window.DraftStateManager.getPendingChanges()
                : [];

            if (pendingChanges.length === 0) {
                return;
            }

            openReviewModal(pendingChanges);
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeRateReview);
    } else {
        initializeRateReview();
    }
})();