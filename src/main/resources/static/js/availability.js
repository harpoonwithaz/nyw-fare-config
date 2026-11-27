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

    function showBanner(message, variant = "info") {
        if (!alertBanner || !alertText) return;
        alertText.textContent = message;
        alertBanner.className = `alert alert-${variant} alert-dismissible fade show mb-3 shadow-sm`;
    }

    // --- 1. Handle toggle switches ---
    document.querySelectorAll(".availability-toggle").forEach(toggle => {
        toggle.addEventListener("change", function () {
            const productCode = this.getAttribute("data-product");
            const isAvailable = this.checked;

            const row = this.closest("tr");
            const statusLabel = row?.querySelector(".status-label");
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

    // --- 2. Modal condition builder with per-product preload & clean reset ---
    const typeRecurring = document.getElementById("typeRecurring");
    const typePromo = document.getElementById("typePromo");
    const sectionDaysOfWeek = document.getElementById("sectionDaysOfWeek");
    const sectionTimeRange = document.getElementById("sectionTimeRange");
    const sectionDateRange = document.getElementById("sectionDateRange");

    function updateModalVisibility() {
        if (typeRecurring && typeRecurring.checked) {
            sectionDaysOfWeek?.classList.remove("d-none");
            sectionDateRange?.classList.add("d-none");
            sectionTimeRange?.classList.remove("d-none");
        } else if (typePromo && typePromo.checked) {
            sectionDaysOfWeek?.classList.add("d-none");
            sectionDateRange?.classList.remove("d-none");
            sectionTimeRange?.classList.remove("d-none");
        }
    }

    if (typeRecurring && typePromo) {
        typeRecurring.addEventListener("change", updateModalVisibility);
        typePromo.addEventListener("change", updateModalVisibility);
    }

    function openModalForProduct(productCode) {
        document.getElementById("modalProductCode").value = productCode;
        document.getElementById("modalTargetLabel").textContent = formatTargetLabel(productCode);

        const staged = stagedAvailabilityDrafts.drafts[productCode];
        const row = document.querySelector(`tr[data-product="${productCode}"]`);

        const conditionType = staged?.conditionType
            || row?.getAttribute("data-condition-type")
            || "RECURRING_WEEKLY";

        const daysList = staged?.daysOfWeek
            ?? (row?.getAttribute("data-days")
                ? row.getAttribute("data-days").split(",").map(v => parseInt(v, 10)).filter(n => !isNaN(n))
                : [1, 2, 3, 4, 5]);

        const startTimeRaw = staged?.startTime
            ? staged.startTime.slice(0, 5)
            : (row?.getAttribute("data-start-time") || "06:00");
        const endTimeRaw = staged?.endTime
            ? staged.endTime.slice(0, 5)
            : (row?.getAttribute("data-end-time") || "22:00");

        const startDateRaw = staged?.startDate ?? (row?.getAttribute("data-start-date") || "");
        const endDateRaw = staged?.endDate ?? (row?.getAttribute("data-end-date") || "");

        if (conditionType === "TEMPORARY_PROMO" && typePromo) {
            typePromo.checked = true;
        } else if (typeRecurring) {
            typeRecurring.checked = true;
        }

        document.querySelectorAll(".day-checkbox").forEach(cb => {
            cb.checked = daysList.includes(parseInt(cb.value, 10));
        });

        const inputStartTime = document.getElementById("inputStartTime");
        const inputEndTime = document.getElementById("inputEndTime");
        const inputStartDate = document.getElementById("inputStartDate");
        const inputEndDate = document.getElementById("inputEndDate");

        if (inputStartTime) inputStartTime.value = startTimeRaw;
        if (inputEndTime) inputEndTime.value = endTimeRaw;
        if (inputStartDate) inputStartDate.value = startDateRaw;
        if (inputEndDate) inputEndDate.value = endDateRaw;

        updateModalVisibility();
    }

    const btnOpenConditionModal = document.getElementById("btnOpenConditionModal");
    if (btnOpenConditionModal) {
        btnOpenConditionModal.addEventListener("click", function () {
            const firstProduct = document.querySelector("tr[data-product]")?.getAttribute("data-product") || "ADULT_ONEWAY";
            openModalForProduct(firstProduct);
        });
    }

    document.querySelectorAll(".btn-configure-rule").forEach(btn => {
        btn.addEventListener("click", function () {
            const productCode = this.getAttribute("data-product");
            openModalForProduct(productCode);
        });
    });

    const btnApplyCondition = document.getElementById("btnApplyCondition");
    if (btnApplyCondition) {
        btnApplyCondition.addEventListener("click", function () {
            const productCode = document.getElementById("modalProductCode").value;
            const conditionType = document.querySelector('input[name="conditionType"]:checked')?.value || "RECURRING_WEEKLY";

            const selectedDays = Array.from(document.querySelectorAll(".day-checkbox:checked"))
                .map(cb => parseInt(cb.value, 10));
            const startTime = document.getElementById("inputStartTime")?.value || "";
            const endTime = document.getElementById("inputEndTime")?.value || "";
            const startDate = document.getElementById("inputStartDate")?.value || "";
            const endDate = document.getElementById("inputEndDate")?.value || "";

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
            draft.startTime = startTime ? `${startTime}:00` : null;
            draft.endTime = endTime ? `${endTime}:00` : null;

            if (conditionType === "TEMPORARY_PROMO") {
                draft.daysOfWeek = [];
                draft.startDate = startDate || null;
                draft.endDate = endDate || null;
            } else {
                draft.daysOfWeek = selectedDays;
                draft.startDate = null;
                draft.endDate = null;
            }

            const row = document.querySelector(`tr[data-product="${productCode}"]`);
            if (row) {
                const badgeContainer = row.querySelector(".temporal-condition-badge-container");
                if (badgeContainer) {
                    const timeSuffix = (startTime && endTime) ? ` (${startTime}-${endTime})` : "";
                    const badgeText = conditionType === "RECURRING_WEEKLY"
                        ? `${selectedDays.length} Days${timeSuffix}`
                        : `Promo (${startDate || "Start"} to ${endDate || "End"})${timeSuffix}`;
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

    // --- 3. Helpers, discard & batch save to PostgreSQL drafts ---
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
        showBanner(
            'Modifications are held in client staging buffer. Click "Save Availability Drafts" to persist across pages.',
            "info"
        );
    }

    if (btnDiscard) {
        btnDiscard.addEventListener("click", function () {
            stagedAvailabilityDrafts.isDirty = false;
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
            .then(async response => {
                const body = await response.json().catch(() => ({}));
                if (!response.ok) {
                    throw new Error(body.message || `Server rejected changes (${response.status}).`);
                }
                return body;
            })
            .then(data => {
                stagedAvailabilityDrafts.isDirty = false;
                stagedAvailabilityDrafts.drafts = {};
                if (changesCounter) changesCounter.textContent = "0";
                if (btnDiscard) btnDiscard.setAttribute("disabled", "true");

                showBanner(data.message || "Availability rules saved to PostgreSQL drafts.", "success");

                document.querySelectorAll(".staged-badge").forEach(badge => {
                    if (badge.textContent.includes("STAGED")) {
                        badge.className = "badge bg-info-subtle text-info-emphasis border border-info-subtle staged-badge";
                        badge.innerHTML = `<i class="bi bi-database-check me-1"></i>Saved in DB`;
                    }
                });
            })
            .catch(error => {
                console.error("Staging error:", error);
                btnSave.classList.remove("disabled");
                btnSave.removeAttribute("disabled");
                showBanner(error.message || "Error saving availability changes.", "danger");
            });
        });
    }

    // --- 4. Unsaved-changes "Are you sure?" navigation & filter guard ---
    let pendingLeaveAction = null;
    const unsavedModalEl = document.getElementById("unsavedNavigationModal");
    const unsavedModal = unsavedModalEl ? new bootstrap.Modal(unsavedModalEl) : null;
    const btnConfirmLeave = document.getElementById("btnConfirmLeavePage");

    function guardLeave(actionFn) {
        if (!stagedAvailabilityDrafts.isDirty) {
            actionFn();
            return;
        }
        pendingLeaveAction = actionFn;
        if (unsavedModal) {
            unsavedModal.show();
        } else if (window.confirm("You have unsaved availability changes that will not persist. Are you sure you want to leave?")) {
            stagedAvailabilityDrafts.isDirty = false;
            actionFn();
        }
    }

    if (btnConfirmLeave) {
        btnConfirmLeave.addEventListener("click", function () {
            stagedAvailabilityDrafts.isDirty = false;
            if (unsavedModal) unsavedModal.hide();
            if (typeof pendingLeaveAction === "function") {
                const fn = pendingLeaveAction;
                pendingLeaveAction = null;
                fn();
            }
        });
    }

    // Guard sidebar & navbar links
    document.querySelectorAll("aside a.nav-link, aside a[href], header a[href]").forEach(link => {
        link.addEventListener("click", function (event) {
            const href = this.getAttribute("href");
            if (!href || href.startsWith("#")) return;
            if (stagedAvailabilityDrafts.isDirty) {
                event.preventDefault();
                guardLeave(() => {
                    window.location.href = href;
                });
            }
        });
    });

    // Guard origin, destination, and effective date filter inputs
    const filterForm = document.getElementById("availabilityFilterForm");
    const siteSelect = document.getElementById("siteSelect");
    const destSelect = document.getElementById("destSelect");
    const pricingDateInput = document.getElementById("pricingDateInput");

    [siteSelect, destSelect, pricingDateInput].forEach(control => {
        if (!control || !filterForm) return;
        let previousValue = control.value;
        control.addEventListener("focus", () => { previousValue = control.value; });
        control.addEventListener("change", function () {
            if (stagedAvailabilityDrafts.isDirty) {
                const nextValue = control.value;
                control.value = previousValue;
                guardLeave(() => {
                    control.value = nextValue;
                    filterForm.submit();
                });
            } else {
                filterForm.submit();
            }
        });
    });

    // Guard global tenant switcher dropdown in navbar
    const tenantSelect = document.getElementById("globalTenantSelect");
    if (tenantSelect && tenantSelect.form) {
        let prevTenant = tenantSelect.value;
        tenantSelect.addEventListener("focus", () => { prevTenant = tenantSelect.value; });
        tenantSelect.onchange = null;
        tenantSelect.addEventListener("change", function () {
            if (stagedAvailabilityDrafts.isDirty) {
                const nextTenant = tenantSelect.value;
                tenantSelect.value = prevTenant;
                guardLeave(() => {
                    tenantSelect.value = nextTenant;
                    tenantSelect.form.submit();
                });
            } else {
                tenantSelect.form.submit();
            }
        });
    }

    // Native browser tab close / hard refresh fallback
    window.addEventListener("beforeunload", function (event) {
        if (stagedAvailabilityDrafts.isDirty) {
            event.preventDefault();
            event.returnValue = "";
        }
    });
});