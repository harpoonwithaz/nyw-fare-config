document.addEventListener("DOMContentLoaded", () => {
    const reviewTab = document.getElementById("review-content");
    const runButton = document.getElementById("runValidationBtn");
    const status = document.getElementById("validationStatus");
    const results = document.getElementById("validationResults");
    const tableBody = document.getElementById("validationTableBody");

    const routesChecked = document.getElementById("routesChecked");
    const fareChecks = document.getElementById("fareChecks");
    const discrepancyCount = document.getElementById("discrepancyCount");

    if (!reviewTab || !runButton || !status || !results || !tableBody) {
        return;
    }

    const tenantCode = reviewTab.dataset.tenantCode;

    if (!tenantCode) {
        runButton.disabled = true;
        status.textContent = "Select a tenant before running validation.";
        return;
    }

    runButton.addEventListener("click", async () => {
        runButton.disabled = true;
        results.hidden = true;
        tableBody.replaceChildren();

        status.className = "mt-3 mb-0 text-primary";
        status.textContent = "Running validation against Caracal. Please wait...";

        try {
            const csrfToken =
                document.querySelector('meta[name="_csrf"]')?.content;

            const csrfHeader =
                document.querySelector('meta[name="_csrf_header"]')?.content;

            if (!csrfToken || !csrfHeader) {
                throw new Error("Missing CSRF token.");
            }

            const url =
                `/api/publishing/validate?tenantCode=${encodeURIComponent(tenantCode)}`;

            const response = await fetch(url, {
                method: "POST",
                headers: {
                    "Accept": "application/json",
                    [csrfHeader]: csrfToken
                },
                credentials: "same-origin"
            });

            if (!response.ok) {
                throw new Error(
                    `Validation request failed (HTTP ${response.status})`
                );
            }

            const data = await response.json();

            if (!Array.isArray(data.discrepancies)) {
                throw new Error("Invalid validation response from server.");
            }

            routesChecked.textContent = data.routesChecked;
            fareChecks.textContent = data.fareChecks;
            discrepancyCount.textContent = data.discrepancyCount;

            for (const discrepancy of data.discrepancies) {
                const row = document.createElement("tr");

                const fields = [
                    discrepancy.category,
                    discrepancy.route,
                    discrepancy.ticketType,
                    discrepancy.expected,
                    discrepancy.actual
                ];

                for (const value of fields) {
                    const cell = document.createElement("td");
                    cell.textContent = value ?? "\u2014";
                    row.appendChild(cell);
                }

                tableBody.appendChild(row);
            }

            if (data.discrepancies.length === 0) {
                const row = document.createElement("tr");
                const cell = document.createElement("td");

                cell.colSpan = 5;
                cell.className = "text-center text-success py-3";
                cell.textContent = "No discrepancies found.";

                row.appendChild(cell);
                tableBody.appendChild(row);
            }

            results.hidden = false;

            if (data.discrepancyCount > 0) {
                status.className = "mt-3 mb-0 text-warning";
                status.textContent =
                    `Validation completed: ${data.discrepancyCount} discrepancies found.`;
            } else {
                status.className = "mt-3 mb-0 text-success";
                status.textContent =
                    "Validation completed successfully. No discrepancies found.";
            }

        } catch (error) {
            console.error("Validation failed:", error);

            status.className = "mt-3 mb-0 text-danger";
            status.textContent =
                "Unable to complete validation. Check that Caracal is running and try again.";

            results.hidden = true;

        } finally {
            runButton.disabled = false;
        }
    });
});
