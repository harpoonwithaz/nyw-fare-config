document.addEventListener("DOMContentLoaded", function () {
    // client state manager so we don't spam requests on every click
    const stagedAvailabilityDrafts = {
        siteCode: document.querySelector("[data-site]")?.getAttribute("data-site") || "MNHTN",
        drafts: {},
        isDirty: false
    };

    const btnSave = document.getElementById("btnSaveAvailabilityDrafts");
    const alertBanner = document.getElementById("availabilityAlert");
    const alertText = document.getElementById("availabilityAlertText");

    // --- 1. handle toggle switches (story 3.1) ---
    document.querySelectorAll(".availability-toggle").forEach(toggle => {
        toggle.addEventListener("change", function () {
            const productCode = this.getAttribute("data-product");
            const isAvailable = this.checked;

            // update the row label visually
            const row = this.closest("tr");
            const statusLabel = row.querySelector(".status-label");
            if (statusLabel) {
                statusLabel.textContent = isAvailable ? "Available" : "Unavailable";
            }

            // add or update state in local object
            if (!stagedAvailabilityDrafts.drafts[productCode]) {
                stagedAvailabilityDrafts.drafts[productCode] = {
                    siteCode: stagedAvailabilityDrafts.siteCode,
                    productCode: productCode,
                    available: isAvailable
                };
            } else {
                stagedAvailabilityDrafts.drafts[productCode].available = isAvailable;
            }

            markRowStaged(row);
            enableSaveButton();
        });
    });

    // --- 2. modal condition builder stuff (story 3.3) ---
    const typeRecurring = document.getElementById("typeRecurring");
    const typePromo = document.getElementById("typePromo");
    const sectionDaysOfWeek = document.getElementById("sectionDaysOfWeek");
    const sectionTimeRange = document.getElementById("sectionTimeRange");
    const sectionDateRange = document.getElementById("sectionDateRange");

    // swap modal fields depending on selected radio button
    function updateModalVisibility() {
        if (typeRecurring && typeRecurring.checked) {
            sectionDaysOfWeek.classList.remove("d-none");
            sectionTimeRange.classList.remove("d-none");
            sectionDateRange.classList.add("d-none");
        } else if (typePromo && typePromo.checked) {
            sectionDaysOfWeek.classList.add("d-none");
            sectionTimeRange.classList.add("d-none");
            sectionDateRange.classList.remove("d-none");
        }
    }

    if (typeRecurring && typePromo) {
        typeRecurring.addEventListener("change", updateModalVisibility);
        typePromo.addEventListener("change", updateModalVisibility);
    }

    // open modal for specific product row
    document.querySelectorAll(".btn-configure-rule").forEach(btn => {
        btn.addEventListener("click", function () {
            const productCode = this.getAttribute("data-product");
            document.getElementById("modalProductCode").value = productCode;
            document.getElementById("modalTargetLabel").textContent = 
                `Product: ${productCode} | Site: ${stagedAvailabilityDrafts.siteCode}`;
        });
    });

    // grab values from modal and save into staged state object
    const btnApplyCondition = document.getElementById("btnApplyCondition");
    if (btnApplyCondition) {
        btnApplyCondition.addEventListener("click", function () {
            const productCode = document.getElementById("modalProductCode").value;
            const conditionType = document.querySelector('input[name="conditionType"]:checked')?.value || "RECURRING_WEEKLY";

            const selectedDays = Array.from(document.querySelectorAll(".day-checkbox:checked")).map(cb => parseInt(cb.value));
            const startTime = document.getElementById("inputStartTime")?.value;
            const endTime = document.getElementById("inputEndTime")?.value;
            const startDate = document.getElementById("inputStartDate")?.value;
            const endDate = document.getElementById("inputEndDate")?.value;

            if (!stagedAvailabilityDrafts.drafts[productCode]) {
                const toggleState = document.getElementById(`toggle-${productCode}`)?.checked ?? true;
                stagedAvailabilityDrafts.drafts[productCode] = {
                    siteCode: stagedAvailabilityDrafts.siteCode,
                    productCode: productCode,
                    available: toggleState
                };
            }

            const draft = stagedAvailabilityDrafts.drafts[productCode];
            draft.conditionType = conditionType;
            draft.daysOfWeek = selectedDays;
            draft.startTime = startTime ? `${startTime}:00` : null;
            draft.endTime = endTime ? `${endTime}:00` : null;
            draft.startDate = startDate || null;
            draft.endDate = endDate || null;

            // update table row badge text
            const row = document.querySelector(`tr[data-product="${productCode}"]`);
            if (row) {
                const badgeContainer = row.querySelector(".temporal-condition-badge-container");
                if (badgeContainer) {
                    const badgeText = conditionType === "RECURRING_WEEKLY" 
                        ? `${selectedDays.length} Days (${startTime}-${endTime})`
                        : `Promo (${startDate || 'Start'} to ${endDate || 'End'})`;
                    badgeContainer.innerHTML = `
                        <span class="badge bg-warning-subtle text-warning-emphasis border border-warning condition-badge">
                            <i class="bi bi-clock-history me-1"></i>${badgeText}
                        </span>
                    `;
                }
                markRowStaged(row);
            }

            enableSaveButton();

            // close modal
            const modalEl = document.getElementById("conditionBuilderModal");
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();
        });
    }

    // --- 3. quick helpers & batch save handler ---
    function markRowStaged(row) {
        if (!row) return;
        const stagedBadge = row.querySelector(".staged-badge");
        if (stagedBadge) {
            stagedBadge.className = "badge bg-warning-subtle text-warning-emphasis border border-warning staged-badge";
            stagedBadge.innerHTML = `<i class="bi bi-pencil-fill me-1"></i>STAGED MODIFICATION`;
        }
    }

    function enableSaveButton() {
        stagedAvailabilityDrafts.isDirty = true;
        if (btnSave) {
            btnSave.classList.remove("disabled");
            btnSave.removeAttribute("disabled");
        }
        if (alertBanner) {
            alertBanner.classList.remove("d-none");
        }
    }

    // post batch to spring boot when save button clicked
    if (btnSave) {
        btnSave.addEventListener("click", function () {
            const payload = Object.values(stagedAvailabilityDrafts.drafts);
            if (payload.length === 0) return;

            btnSave.classList.add("disabled");
            btnSave.setAttribute("disabled", "true");

            fetch("/availability/api/stage", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            })
            .then(response => {
                if (!response.ok) throw new Error("failed to stage availability drafts");
                return response.json();
            })
            .then(data => {
                stagedAvailabilityDrafts.isDirty = false;
                stagedAvailabilityDrafts.drafts = {};

                if (alertText) alertText.textContent = data.message || "availability drafts staged!";
                if (alertBanner) {
                    alertBanner.className = "alert alert-success alert-dismissible fade show mb-4 shadow-sm";
                }

                // update row badges so user knows DB has it
                document.querySelectorAll(".staged-badge").forEach(badge => {
                    if (badge.textContent.includes("STAGED")) {
                        badge.className = "badge bg-info-subtle text-info border staged-badge";
                        badge.innerHTML = `<i class="bi bi-clock-fill me-1"></i>STAGED IN DB`;
                    }
                });
            })
            .catch(error => {
                console.error("staging error:", error);
                btnSave.classList.remove("disabled");
                btnSave.removeAttribute("disabled");
                if (alertBanner) {
                    alertBanner.className = "alert alert-danger alert-dismissible fade show mb-4 shadow-sm";
                }
                if (alertText) alertText.textContent = "error staging availability changes. try again.";
            });
        });
    }
});