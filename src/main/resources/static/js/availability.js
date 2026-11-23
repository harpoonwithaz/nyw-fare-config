document.addEventListener("DOMContentLoaded", function () {
    const tableEl = document.getElementById("availabilityTable");
    const stagedAvailabilityDrafts = {
        siteCode: tableEl?.getAttribute("data-site") || document.getElementById("siteSelect")?.value || "MIDTOWN",
        routeCode: tableEl?.getAttribute("data-route") || document.getElementById("routeSelect")?.value || "ALL",
        drafts: {},
        isDirty: false
    };

    const btnSave = document.getElementById("btnSaveAvailabilityDrafts");
    const btnDiscard = document.getElementById("discardDraftsBtn");
    const btnSyncCaracal = document.getElementById("btnSyncCaracal");
    const changesCounter = document.getElementById("changesCounter");
    const alertBanner = document.getElementById("availabilityAlert");
    const alertText = document.getElementById("availabilityAlertText");

    // --- 0. Station & Nested Route Dropdown Navigation ---
    const filterForm = document.getElementById("stationRouteFilterForm");
    const siteSelect = document.getElementById("siteSelect");
    const routeSelect = document.getElementById("routeSelect");

    if (siteSelect && filterForm) {
        siteSelect.addEventListener("change", function () {
            // Reset route to ALL when changing origin station
            if (routeSelect) {
                routeSelect.value = "ALL";
            }
            filterForm.submit();
        });
    }

    if (routeSelect && filterForm) {
        routeSelect.addEventListener("change", function () {
            filterForm.submit();
        });
    }

    // --- 1. Handle Availability Toggle Switches (Story 3.1) ---
    document.querySelectorAll(".availability-toggle").forEach(toggle => {
        toggle.addEventListener("change", function () {
            const productCode = this.getAttribute("data-product");
            const isAvailable = this.checked;
            const row = this.closest("tr");
            const wasPhasedOut = row?.getAttribute("data-phased-out") === "true";

            const statusLabel = row?.querySelector(".status-label");
            if (statusLabel) {
                statusLabel.textContent = isAvailable ? "Available" : "Unavailable";
                statusLabel.classList.remove("text-danger");
                statusLabel.classList.add("text-dark");
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

            if (wasPhasedOut && isAvailable) {
                stagedAvailabilityDrafts.drafts[productCode].phasedOut = false;
            }

            markRowStaged(row);
            updateStagingButtons();
        });
    });

    // --- 2. Permanent Phase-Out Confirmation Modal (Story 3.2) ---
    document.querySelectorAll(".btn-phase-out").forEach(btn => {
        btn.addEventListener("click", function () {
            const productCode = this.getAttribute("data-product");
            const hiddenInput = document.getElementById("phaseOutProductCode");
            const labelEl = document.getElementById("phaseOutProductLabel");
            if (hiddenInput) hiddenInput.value = productCode;
            if (labelEl) labelEl.textContent = productCode;
        });
    });

    const btnConfirmPhaseOut = document.getElementById("btnConfirmPhaseOut");
    if (btnConfirmPhaseOut) {
        btnConfirmPhaseOut.addEventListener("click", function () {
            const productCode = document.getElementById("phaseOutProductCode")?.value;
            if (!productCode) return;

            stagedAvailabilityDrafts.drafts[productCode] = {
                siteCode: stagedAvailabilityDrafts.siteCode,
                routeCode: stagedAvailabilityDrafts.routeCode,
                productCode: productCode,
                available: false,
                phasedOut: true
            };

            const row = document.querySelector(`tr[data-product="${productCode}"]`);
            if (row) {
                row.setAttribute("data-phased-out", "true");
                const toggle = row.querySelector(".availability-toggle");
                if (toggle) {
                    toggle.checked = false;
                    toggle.disabled = true;
                }
                const statusLabel = row.querySelector(".status-label");
                if (statusLabel) {
                    statusLabel.textContent = "Phased Out";
                    statusLabel.classList.remove("text-dark");
                    statusLabel.classList.add("text-danger");
                }
                markRowStaged(row);
            }

            updateStagingButtons();

            const modalEl = document.getElementById("phaseOutConfirmModal");
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();
        });
    }

    // --- 3. Modal Temporal Condition & Discount Builder (Story 3.3 + Start/End Dates & Hours) ---
    const typeRecurring = document.getElementById("typeRecurring");
    const typeMonthly = document.getElementById("typeMonthly");
    const typePromo = document.getElementById("typePromo");
    const sectionDaysOfWeek = document.getElementById("sectionDaysOfWeek");
    const sectionDayOfMonth = document.getElementById("sectionDayOfMonth");
    const sectionTimeRange = document.getElementById("sectionTimeRange");
    const sectionDateRange = document.getElementById("sectionDateRange");

    function updateModalVisibility() {
        if (!sectionDaysOfWeek || !sectionTimeRange || !sectionDateRange) return;

        // Start/End Date and Start/End Hour are visible for BOTH recurring and one-time discounts
        sectionDateRange.classList.remove("d-none");
        sectionTimeRange.classList.remove("d-none");

        if (typeRecurring && typeRecurring.checked) {
            sectionDaysOfWeek.classList.remove("d-none");
            if (sectionDayOfMonth) sectionDayOfMonth.classList.add("d-none");
        } else if (typeMonthly && typeMonthly.checked) {
            sectionDaysOfWeek.classList.add("d-none");
            if (sectionDayOfMonth) sectionDayOfMonth.classList.remove("d-none");
        } else if (typePromo && typePromo.checked) {
            sectionDaysOfWeek.classList.add("d-none");
            if (sectionDayOfMonth) sectionDayOfMonth.classList.add("d-none");
        }
    }

    [typeRecurring, typeMonthly, typePromo].forEach(radio => {
        if (radio) radio.addEventListener("change", updateModalVisibility);
    });
    updateModalVisibility();

    function setModalTarget(productCode) {
        const prodInput = document.getElementById("modalProductCode");
        const targetLabel = document.getElementById("modalTargetLabel");
        if (prodInput) prodInput.value = productCode;
        if (targetLabel) {
            targetLabel.textContent =
                `Product: ${productCode} | Site: ${stagedAvailabilityDrafts.siteCode} | Route: ${stagedAvailabilityDrafts.routeCode}`;
        }
    }

    const btnOpenConditionModal = document.getElementById("btnOpenConditionModal");
    if (btnOpenConditionModal) {
        btnOpenConditionModal.addEventListener("click", function () {
            const firstProd = document.querySelector("#availabilityTable tbody tr")?.getAttribute("data-product") || "ADULT_ONEWAY";
            setModalTarget(firstProd);
        });
    }

    document.querySelectorAll(".btn-configure-rule").forEach(btn => {
        btn.addEventListener("click", function () {
            setModalTarget(this.getAttribute("data-product"));
        });
    });

    const btnApplyCondition = document.getElementById("btnApplyCondition");
    if (btnApplyCondition) {
        btnApplyCondition.addEventListener("click", function () {
            const productCode = document.getElementById("modalProductCode").value;
            const conditionType = document.querySelector('input[name="conditionType"]:checked')?.value || "RECURRING_WEEKLY";
            const discountLabel = document.getElementById("inputDiscountLabel")?.value?.trim() || null;

            const selectedDays = Array.from(document.querySelectorAll(".day-checkbox:checked")).map(cb => parseInt(cb.value, 10));
            const dayOfMonthStart = parseInt(document.getElementById("inputDayOfMonthStart")?.value, 10) || null;
            const dayOfMonthEnd = parseInt(document.getElementById("inputDayOfMonthEnd")?.value, 10) || null;
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
            draft.discountLabel = discountLabel;
            draft.daysOfWeek = conditionType === "RECURRING_WEEKLY" ? selectedDays : [];
            draft.dayOfMonthStart = conditionType === "RECURRING_MONTHLY" ? dayOfMonthStart : null;
            draft.dayOfMonthEnd = conditionType === "RECURRING_MONTHLY" ? dayOfMonthEnd : null;
            draft.startTime = startTime ? `${startTime}:00` : null;
            draft.endTime = endTime ? `${endTime}:00` : null;
            draft.startDate = startDate || null;
            draft.endDate = endDate || null;

            const row = document.querySelector(`tr[data-product="${productCode}"]`);
            if (row) {
                const badgeContainer = row.querySelector(".temporal-condition-badge-container");
                if (badgeContainer) {
                    const prefix = discountLabel ? `${discountLabel}: ` : "";
                    const timeStr = (startTime || endTime) ? `${startTime || "00:00"}-${endTime || "23:59"}` : "";
                    const dateStr = (startDate || endDate) ? `${startDate || "Start"} to ${endDate || "End"}` : "";

                    let badgeText;
                    if (conditionType === "TEMPORARY_PROMO") {
                        badgeText = `${prefix}One-Time (${dateStr || "Active Window"})${timeStr ? ` [${timeStr}]` : ""}`;
                    } else if (conditionType === "RECURRING_MONTHLY") {
                        badgeText = `${prefix}Monthly Days ${dayOfMonthStart}-${dayOfMonthEnd}${timeStr ? ` (${timeStr})` : ""}${dateStr ? ` [${dateStr}]` : ""}`;
                    } else {
                        badgeText = `${prefix}${selectedDays.length} Days (${timeStr || "00:00-23:59"})${dateStr ? ` [${dateStr}]` : ""}`;
                    }

                    badgeContainer.innerHTML = `
                        <span class="badge bg-warning-subtle text-warning-emphasis border border-warning condition-badge">
                            <i class="bi bi-clock-history me-1"></i>${badgeText}
                        </span>
                    `;
                }
                markRowStaged(row);
            }

            updateStagingButtons();

            const modalEl = document.getElementById("conditionBuilderModal");
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();
        });
    }

    // --- 4. Staging Helpers, Discard, Save to DB, and Sync to Caracal ---
    function markRowStaged(row) {
        if (!row) return;
        const stagedBadge = row.querySelector(".staged-badge");
        if (stagedBadge) {
            stagedBadge.className = "badge bg-warning-subtle text-warning-emphasis border border-warning staged-badge";
            stagedBadge.innerHTML = `<i class="bi bi-pencil-fill me-1"></i>STAGED MODIFICATION`;
        }
    }

    function updateStagingButtons() {
        const count = Object.keys(stagedAvailabilityDrafts.drafts).length;
        stagedAvailabilityDrafts.isDirty = count > 0;
        if (changesCounter) {
            changesCounter.textContent = String(count);
        }
        if (btnSave) {
            btnSave.disabled = count === 0;
            btnSave.classList.toggle("disabled", count === 0);
        }
        if (btnDiscard) {
            btnDiscard.disabled = count === 0;
            btnDiscard.classList.toggle("disabled", count === 0);
        }
        if (alertBanner && count > 0) {
            alertBanner.className = "alert alert-info alert-dismissible fade show mb-3 shadow-sm";
            if (alertText) {
                alertText.textContent = `${count} modification(s) staged locally. Click "Save Availability Drafts" to persist to DB.`;
            }
        }
    }

    if (btnDiscard) {
        btnDiscard.addEventListener("click", function () {
            window.location.reload();
        });
    }

    function getCsrfHeaders() {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
        const headers = { "Content-Type": "application/json" };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        return headers;
    }

    if (btnSave) {
        btnSave.addEventListener("click", function () {
            const payload = Object.values(stagedAvailabilityDrafts.drafts);
            if (payload.length === 0) return;

            btnSave.disabled = true;
            btnSave.classList.add("disabled");

            fetch("/availability/api/stage", {
                method: "POST",
                headers: getCsrfHeaders(),
                body: JSON.stringify(payload)
            })
            .then(async response => {
                const body = await response.json().catch(() => ({}));
                if (!response.ok) {
                    throw new Error(body.message || "Failed to stage availability drafts.");
                }
                return body;
            })
            .then(data => {
                stagedAvailabilityDrafts.isDirty = false;
                stagedAvailabilityDrafts.drafts = {};
                if (changesCounter) changesCounter.textContent = "0";
                if (btnDiscard) btnDiscard.disabled = true;

                if (alertText) alertText.textContent = data.message || "Availability rules saved!";
                if (alertBanner) {
                    alertBanner.className = "alert alert-success alert-dismissible fade show mb-3 shadow-sm";
                }

                document.querySelectorAll(".staged-badge").forEach(badge => {
                    if (badge.textContent.includes("STAGED")) {
                        badge.className = "badge bg-info-subtle text-info-emphasis border border-info-subtle staged-badge";
                        badge.innerHTML = `<i class="bi bi-database-check me-1"></i>SAVED IN DB`;
                    }
                });
            })
            .catch(error => {
                btnSave.disabled = false;
                btnSave.classList.remove("disabled");
                if (alertBanner) {
                    alertBanner.className = "alert alert-danger alert-dismissible fade show mb-3 shadow-sm";
                }
                if (alertText) alertText.textContent = error.message || "Error saving availability changes.";
            });
        });
    }

    if (btnSyncCaracal) {
        btnSyncCaracal.addEventListener("click", function () {
            btnSyncCaracal.disabled = true;

            fetch("/availability/api/sync", {
                method: "POST",
                headers: getCsrfHeaders()
            })
            .then(async response => {
                const body = await response.json().catch(() => ({}));
                if (!response.ok) {
                    throw new Error(body.message || "Failed to synchronize availability with platform.");
                }
                return body;
            })
            .then(data => {
                btnSyncCaracal.disabled = false;
                if (alertBanner) {
                    alertBanner.className = "alert alert-success alert-dismissible fade show mb-3 shadow-sm";
                }
                if (alertText) {
                    alertText.textContent = data.message || "Synchronized availability rules with Caracal.";
                }
                document.querySelectorAll(".staged-badge").forEach(badge => {
                    if (badge.textContent.includes("SAVED IN DB")) {
                        badge.className = "badge bg-success-subtle text-success border border-success-subtle staged-badge";
                        badge.innerHTML = `<i class="bi bi-check-circle-fill me-1"></i>Live/Synced`;
                    }
                });
            })
            .catch(error => {
                btnSyncCaracal.disabled = false;
                if (alertBanner) {
                    alertBanner.className = "alert alert-danger alert-dismissible fade show mb-3 shadow-sm";
                }
                if (alertText) {
                    alertText.textContent = error.message || "Failed to synchronize availability with platform.";
                }
            });
        });
    }
});