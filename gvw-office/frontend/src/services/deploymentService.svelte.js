import { apiAddDeployment } from "../api/apiDeployments.svelte.js";
import { handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";
import { normalizeResponse } from "../api/http.svelte.js";
import { addToast } from "../stores/toasts.svelte.js";
import { viewport } from "../stores/viewport.svelte.js";

export const deploymentTypeFilterMap = {
    "": "all",
    "Alle Typen": "all",
    "Scheduled": "SCHEDULED",
    "Running": "RUNNING",
    "Successful": "SUCCESSFUL",
    "Cancelled": "CANCELLED",
    "Failed": "FAILED",
    "all": "Alle Typen",
    "SCHEDULED": "Scheduled",
    "RUNNING": "Running",
    "SUCCESSFUL": "Successful",
    "CANCELLED": "Cancelled",
    "FAILED": "Failed"
};

const isFetching = {
    addDeployment: false,
    addMigration: false,
    removeDeployment: false,
    removeMigration: false,
    updateDeployment: false,
    deploy: false,
    testMigrations: false
}

export async function addDeployment(data) {
    if (isFetching.addDeployment) return;

    isFetching.addDeployment = true;

    try {
        const { resp } = await apiAddDeployment(data);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return;

        addToast({
            title: "Deployment hinzugefügt",
            subTitle: viewport.isMobile ? "" : "Das neue Deployment wurde erfolgreich hinzugefügt.",
            type: "success"
        })
    } finally {
        isFetching.addDeployment = false;
    }
}