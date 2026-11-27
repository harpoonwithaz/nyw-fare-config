
/* Handle publishing history includes:
1. Requesting publication and audit records after opening the History tab.
2. Populate the tables with the records.
3. Display an error if the backend isn't ready.
*/

document.addEventListener("DOMContentLoaded", () => {
    const historyTab = document.getElementById("history-tab");

    if (!historyTab) return;

    // Run loadHistory when History tab is opened.
    historyTab.addEventListener("shown.bs.tab", loadHistory);

    // Handle publication and audit history.
    async function loadHistory() {
        await Promise.all([
            loadTable(
                "/api/publishing/history/publications",
                "publicationHistoryTableBody",
                renderPublication
            ),
            loadTable(
                "/api/publishing/history/audit",
                "auditLogTableBody",
                renderAudit
            )
        ]);
    }

    // Load the 2 tables.
    async function loadTable(endpoint, tableId, renderRow) {
        const tbody = document.getElementById(tableId);
        if (!tbody) return;

        tbody.replaceChildren();
        addMessage(tbody, "Loading history...");

        try {
            const tenantCode = document
                .getElementById("history-content")
                .dataset.tenantCode;

            if (!tenantCode) {
                throw new Error("No tenant selected.");
            }

            const response = await fetch(
                `${endpoint}?tenantCode=${encodeURIComponent(tenantCode)}`
            );

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();

            // CHANGED: Support both publication and audit responses.
            const records = Array.isArray(data)
                ? data
                : (data.publications ?? data.entries);

            if (!Array.isArray(records)) {
                throw new Error("Unexpected history response.");
            }

            tbody.replaceChildren();

            // Display the records.
            if (records.length === 0) {
                addMessage(tbody, "No history records found.");
                return;
            }

            records.forEach(record => {
                tbody.appendChild(renderRow(record));
            });

        } catch (error) {
            tbody.replaceChildren();
            addMessage(tbody, "Unable to load history.");

            // CHANGED: Identify which table failed.
            console.error(`Failed to load ${tableId}:`, error);
        }
    }

    function addMessage(tbody, message) {
        const row = tbody.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 4;
        cell.textContent = message;
    }

    function renderAudit(record) {
        return createRow([
            record.at,

            // CHANGED: Use actorType when actorRef is empty.
            record.actorRef || record.actorType || "Unknown",

            record.function,
            JSON.stringify(record.after ?? {})
        ]);
    }

    function renderPublication(record) {
        return createRow([
            record.id,
            record.publishedAt,
            record.version,
            record.kind
        ]);
    }

    function createRow(values) {
        const row = document.createElement("tr");

        values.forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value ?? "—";
            row.appendChild(cell);
        });

        return row;
    }
});
