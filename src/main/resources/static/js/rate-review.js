/*STORY 2.6b: Transaction Review Diff Modal
Displays staged fare changes before batch submission.
Currently using sample data until DraftStateManager is integrated.*/

(function () {
    "use strict";

    // STORY 2.6b: Temporary sample changes for UI testing.
    // Replace with DraftStateManager.getPendingChanges() later.
    const sampleChanges = [
        {
            origin: "EDGEWTR",
            destination: "MIDTOWN",
            product: "Adult One-Way",
            originalPrice: 13.25,
            newPrice: 14.00
        },
        {
            origin: "PORTIMP",
            destination: "MIDTOWN",
            product: "10-Trip Pass",
            originalPrice: 95.00,
            newPrice: 100.00
        }
    ];

    // Format prices consistently.
    function formatPrice(price) {
        return "$" + Number(price).toFixed(2);
    }

    // Populate the comparison table.
    function renderReviewTable(changes) {
        const tableBody = document.getElementById("rateReviewTableBody");
        const changeCount = document.getElementById("rateReviewChangeCount");

        if (!tableBody || !changeCount) {
            return;
        }

        //removes previously displayed rows
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

        //create a table row for every pending change.
        changes.forEach(function (change) {
            const row = document.createElement("tr");

            const values = [
                change.origin,
                change.destination,
                change.product,
                formatPrice(change.originalPrice),
                formatPrice(change.newPrice)
            ];

            values.forEach(function (value) {
                const cell = document.createElement("td");
                cell.textContent = value;
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

        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        modal.show();
    }

    // Temporary preview button for development.
    function initializeReviewPreview() {
        const saveButton = document.getElementById("saveChangesBtn");

        if (!saveButton) {
            return;
        }

        const previewButton = document.createElement("button");
        previewButton.type = "button";
        previewButton.id = "previewRateReviewBtn";
        previewButton.className = "btn btn-outline-primary btn-sm";
        previewButton.textContent = "Preview Review Modal (Demo)";

        previewButton.addEventListener("click", function () {
            openReviewModal(sampleChanges);
        });

        // Keep the existing Save button disabled and unchanged.
        saveButton.parentElement.appendChild(previewButton);
    }

    document.addEventListener("DOMContentLoaded", function () {
        // Prototype only: remove this preview when real draft integration is ready.
        initializeReviewPreview();
    });

})();
