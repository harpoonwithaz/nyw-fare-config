document.addEventListener("DOMContentLoaded", function () {
    const tableEl = document.getElementById("availabilityTable");
    const stagedAvailabilityDrafts = {
        siteCode: tableEl?.getAttribute("data-site") || "MIDTOWN",
        destCode: tableEl?.getAttribute("data-dest") || "ALL",
        routeCode: tableEl?.getAttribute("data-route") || "ALL",
        drafts: {},
        isDirty: false
    };

    const btnSave = document.getElementById("btnSaveAvailabilityDrafts");
    const btnDiscard = document.getElementById("discardDraftsBtn");
    const changesCounter = document.getElementById("changesCounter");
    const alertBanner = document.getElementById("availabilityAlert");
    const alertText = document.getElementById("availabilityAlertText");

    function formatTargetLabel(productCode) {
        const destDisplay = stagedAvailabilityDrafts.destCode === "ALL"
            ? "All Destinations"
            : stagedAvailabilityDrafts.destCode;
        return `Product: ${productCode} | Origin: ${stagedAvailabilityDrafts.siteCode} | Dest: ${destDisplay}`;
    }

    // --- 1. handle toggle switches (story 3.1) ---
    document.querySelectorAll(".availability-toggle").forEach(toggle => {
        toggle.addEventListener("change", function () {
            const productCode = this.getAttribute("data-product");
            const isAvailable = this.checked;

            const row = this.closest("tr");
            const statusLabel = row.querySelector(".status-label");
            if (statusLabel) {
                statusLabel.textContent = isAvailable ? "Available" : "Unavailable";
            }

            if (!stagedAvailabilityDrafts.drafts[productCode]) {
                stagedAvailabilityDrafts.drafts[productCode] = {
                    siteCode: stagedAvailabilityDrafts.siteCode,
                    routeCode: stagedAvailabilityDrafts.routeCode,
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

    // --- 2. modal condition builder (story 3.3) ---
    const typeRecurring = document.getElementById("typeRecurring");
    const typePromo = document.getElementById("typePromo");
    const sectionDaysOfWeek = document.getElementById("sectionDaysOfWeek");
    const sectionTimeRange = document.getElementById("sectionTimeRange");
    const sectionDateRange = document.getElementById("sectionDateRange");

    function updateModalVisibility() {
        if (typeRecurring && typeRecurring.checked) {
            sectionDaysOfWeek.classList.remove("d-none");
            sectionDateRange.classList.add("d-none");
            sectionTimeRange.classList.remove("d-none");
        } else if (typePromo && typePromo.checked) {
            sectionDaysOfWeek.classList.add("d-none");
            sectionDateRange.classList.remove("d-none");
            sectionTimeRange.classList.remove("d-none");
        }
    }

    if (typeRecurring && typePromo) {
        typeRecurring.addEventListener("change", updateModalVisibility);
        typePromo.addEventListener("change", updateModalVisibility);
    }

    const btnOpenConditionModal = document.getElementById("btnOpenConditionModal");
    if (btnOpenConditionModal) {
        btnOpenConditionModal.addEventListener("click", function () {
            const firstProduct = document.querySelector("[data-product]")?.getAttribute("data-product") || "ADULT_ONEWAY";
            document.getElementById("modalProductCode").value = firstProduct;
            document.getElementById("modalTargetLabel").textContent = formatTargetLabel(firstProduct);
        });
    }

    document.querySelectorAll(".btn-configure-rule").forEach(btn => {
        btn.addEventListener("click", function () {
            const productCode = this.getAttribute("data-product");
            document.getElementById("modalProductCode").value = productCode;
            document.getElementById("modalTargetLabel").textContent = formatTargetLabel(productCode);
        });
    });

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
                    routeCode: stagedAvailabilityDrafts.routeCode,
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

            const row = document.querySelector(`tr[data-product="${productCode}"]`);
            if (row) {
                const badgeContainer = row.querySelector(".temporal-condition-badge-container");
                if (badgeContainer) {
                    const timeSuffix = (startTime && endTime) ? ` (${startTime}-${endTime})` : "";
                    const badgeText = conditionType === "RECURRING_WEEKLY"
                        ? `${selectedDays.length} Days${timeSuffix}`
                        : `Promo (${startDate || 'Start'} to ${endDate || 'End'})${timeSuffix}`;
                    badgeContainer.innerHTML = `
                        <span class="badge bg-warning-subtle text-warning-emphasis border border-warning condition-badge">
                            <i class="bi bi-clock-history me-1"></i>${badgeText}
                        </span>
                    `;
                }
                markRowStaged(row);
            }

            enableSaveButton();

            const modalEl = document.getElementById("conditionBuilderModal");
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();
        });
    }

    // --- 3. helpers, discard & batch save handler ---
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
        const count = Object.keys(stagedAvailabilityDrafts.drafts).length;
        if (changesCounter) {
            changesCounter.textContent = String(count);
        }
        if (btnSave) {
            btnSave.classList.remove("disabled");
            btnSave.removeAttribute("disabled");
        }
        if (btnDiscard) {
            btnDiscard.classList.remove("disabled");
            btnDiscard.removeAttribute("disabled");
        }
        if (alertBanner) {
            alertBanner.classList.remove("d-none");
        }
    }

    if (btnDiscard) {
        btnDiscard.addEventListener("click", function () {
            window.location.reload();
        });
    }

    if (btnSave) {
        btnSave.addEventListener("click", function () {
            const payload = Object.values(stagedAvailabilityDrafts.drafts);
            if (payload.length === 0) return;

            btnSave.classList.add("disabled");
            btnSave.setAttribute("disabled", "true");

            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
            const headers = {
                "Content-Type": "application/json"
            };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            fetch("/availability/api/stage", {
                method: "POST",
                headers: headers,
                body: JSON.stringify(payload)
            })
            .then(response => {
                if (!response.ok) throw new Error("failed to stage availability drafts");
                return response.json();
            })
            .then(data => {
                stagedAvailabilityDrafts.isDirty = false;
                stagedAvailabilityDrafts.drafts = {};
                if (changesCounter) changesCounter.textContent = "0";
                if (btnDiscard) btnDiscard.setAttribute("disabled", "true");

                if (alertText) alertText.textContent = data.message || "availability rules saved!";
                if (alertBanner) {
                    alertBanner.className = "alert alert-success alert-dismissible fade show mb-3 shadow-sm";
                }

                document.querySelectorAll(".staged-badge").forEach(badge => {
                    if (badge.textContent.includes("STAGED")) {
                        badge.className = "badge bg-info-subtle text-info-emphasis border border-info-subtle staged-badge";
                        badge.innerHTML = `<i class="bi bi-database-check me-1"></i>Saved in DB`;
                    }
                });
            })
            .catch(error => {
                console.error("staging error:", error);
                btnSave.classList.remove("disabled");
                btnSave.removeAttribute("disabled");
                if (alertBanner) {
                    alertBanner.className = "alert alert-danger alert-dismissible fade show mb-3 shadow-sm";
                }
                if (alertText) alertText.textContent = "error saving availability changes. try again.";
            });
        });
    }
});