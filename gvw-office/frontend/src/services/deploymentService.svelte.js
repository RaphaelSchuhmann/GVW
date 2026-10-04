import {
    apiAddDeployment,
    apiCheckDeployment,
    apiDeleteDeployment,
    apiGetDeployment, apiUpdateDeploymentInformation
} from "../api/apiDeployments.svelte.js";
import { handleGenericErrors, handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";
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
    getDeployment: false,
    checkDeployment: false,
    addDeployment: false,
    addMigration: false,
    removeDeployment: false,
    removeMigration: false,
    updateDeployment: false,
    deploy: false,
    testMigrations: false
}

const pendingChecks = new Map();

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

export async function getFullDeployment(id) {
    if (isFetching.getDeployment) return null;

    isFetching.getDeployment = true;

    try {
        const { resp, body } = await apiGetDeployment(id);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return null;

        const requiredFields = [
            "id",
            "rev",
            "title",
            "date",
            "startTime",
            "endTime",
            "commit",
            "migrations",
            "version"
        ];

        if (!(body !== null && typeof body === "object" && requiredFields.every(field => field in body))) return null;

        return body;
    } finally {
        isFetching.getDeployment = false;
    }
}

export async function deploymentExists(id) {
    if (!id) return false;

    if (pendingChecks.has(id)) return await pendingChecks.get(id);

    isFetching.checkDeployment = true;

    const request = (async () => {
        try {
            const { resp } = await apiCheckDeployment(id);
            const normalized = normalizeResponse(resp);

            if (normalized.status === 404) return false;

            if (handleGenericErrors(normalized)) return true;

            return true;
        } catch (e) {
            return true;
        } finally {
            pendingChecks.delete(id);
            if (pendingChecks.size === 0) {
                isFetching.checkDeployment = false;
            }
        }
    })();

    pendingChecks.set(id, request);
    return await request;
}

export async function deleteDeployment(id) {
    if (isFetching.deleteDeployment) return;

    isFetching.deleteDeployment = true;

    try {
        const { resp } = await apiDeleteDeployment(id);
        const normalized = normalizeResponse(resp);

        if (handleGenericErrors(normalized)) return;

        addToast({
            title: "Deployment gelöscht",
            subTitle: viewport.isMobile ? "" : "Das Deployment wurde erfolgreich gelöscht.",
            type: "success"
        });
    } finally {
        isFetching.deleteDeployment = false;
    }
}

export async function updateDeployment(data) {
    if (isFetching.updateDeployment) return;

    isFetching.updateDeployment = true;

    try {
        const { resp } = await apiUpdateDeploymentInformation(data);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return;

        addToast({
            title: "Änderungen gespeichert",
            subTitle: viewport.isMobile ? "" : "Ihre Änderungen wurden erfolgreich gespeichert.",
            type: "success"
        });
    } finally {
        isFetching.updateDeployment = false;
    }
}