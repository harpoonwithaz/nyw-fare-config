document.addEventListener("DOMContentLoaded", () => {

    const publishBtn =
        document.getElementById("publishConfigurationBtn");

    const reviewSection =
        document.getElementById("review-content");

    if (!publishBtn || !reviewSection) return;

    const tenantCode = reviewSection.dataset.tenantCode;
    const canPublish = reviewSection.dataset.canPublish === "true";

    let publishing = false;
    let outcomeUncertain = false;

    function updateButton() {

        publishBtn.disabled =
            !canPublish ||
            publishing ||
            outcomeUncertain ||
            tenantCode !== "NYW_SANDBOX";

        if (!canPublish) {

            publishBtn.title =
                "Only Client Services users can publish configurations.";

        } else if (tenantCode !== "NYW_SANDBOX") {

            publishBtn.title =
                "Select NYW_SANDBOX before publishing.";

        } else if (outcomeUncertain) {

            publishBtn.title =
                "Check publication history before retrying.";

        } else {

            publishBtn.title =
                "Publish pending configuration changes.";
        }
    }

    publishBtn.addEventListener("click", async () => {

        if (!canPublish || publishing || publishBtn.disabled) return;

        const confirmed = window.confirm(
            "Publish pending configuration changes for NYW_SANDBOX?\n\n" +
            "This action will apply staged changes to Caracal " +
            "and publish the updated sandbox catalog."
        );

        if (!confirmed) return;

        publishing = true;
        updateButton();

        const originalText = publishBtn.innerHTML;

        publishBtn.textContent = "Publishing...";

        try {

            const csrfToken =
                document.querySelector('meta[name="_csrf"]')?.content;

            const csrfHeader =
                document.querySelector('meta[name="_csrf_header"]')?.content;

            if (!csrfToken || !csrfHeader) {

                window.alert("Missing CSRF token.");
                return;
            }

            const response = await fetch("/api/publishing/publish", {

                method: "POST",

                credentials: "same-origin",

                headers: {
                    [csrfHeader]: csrfToken
                }
            });

            const message = await response.text();

            if (!response.ok) {

                if (response.status >= 500) {
                    outcomeUncertain = true;
                }

                window.alert(
                    "Publication unsuccessful:\n" +
                    (message || `HTTP ${response.status}`) +
                    "\n\nCheck the current configuration before retrying."
                );

                return;
            }

            window.alert(
                message || "Configuration published successfully."
            );

            window.location.reload();

        } catch (error) {

            outcomeUncertain = true;

            window.alert(
                "Publication could not be confirmed. " +
                "Check publication history before retrying."
            );

            console.error("Publication request failed:", error);

        } finally {

            publishing = false;

            publishBtn.innerHTML = originalText;

            updateButton();
        }
    });

    updateButton();
});
