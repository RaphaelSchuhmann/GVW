import {
    apiAddDeployment, apiAddMigration,
    apiCheckDeployment,
    apiDeleteDeployment, apiDeleteMigration,
    apiGetDeployment, apiGetDeploymentsToday, apiUpdateDeploymentInformation
} from "../api/apiDeployments.svelte.js";
import { handleGenericErrors, handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";
import { normalizeResponse } from "../api/http.svelte.js";
import { addToast } from "../stores/toasts.svelte.js";
import { viewport } from "../stores/viewport.svelte.js";

/**
 * Set of allowed migration action types.
 * @type {Set<string>}
 */
export const migrationActions = new Set(["ADD", "DELETE", "RENAME", "CONDITIONAL"]);

/**
* Mapping dictionary for translating deployment type filter labels between UI strings and API values.
* @type {Object<string, string>}
*/
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
    testMigrations: false,
    today: false
}

const pendingChecks = new Map();

/**
 * Adds a new deployment and triggers a success toast notification upon completion.
 * @param {Object} data - The deployment payload to add.
 * @returns {Promise<void>}
 */
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

/**
 * Fetches a full deployment object by its ID, validating that all required fields are present.
 * @param {string} id - The unique ID of the deployment.
 * @returns {Promise<Object|null>} The full deployment object, or null if validation or fetching fails.
 */
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

/**
 * Checks whether a specific deployment still exists in the database.
 * Deduplicates concurrent requests for the same ID via a pending checks map.
 * @param {string} id - The unique ID of the deployment to check.
 * @returns {Promise<boolean>} True if the deployment exists, false if it returns 404.
 */
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
        } catch {
            // If an exception is thrown assume that the deployment still exists
            // and wait for another sse event to try again as there is no 100% chance
            // that the deployment was deleted after the sse event was received
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

/**
 * Deletes a deployment by its ID and triggers a success toast notification.
 * @param {string} id - The unique ID of the deployment to delete.
 * @returns {Promise<void>}
 */
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

/**
 * Updates an existing deployment's information and triggers a success toast notification.
 * @param {Object} data - The updated deployment information.
 * @returns {Promise<void>}
 */
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

/**
 * Adds a migration to a deployment and triggers a success toast notification.
 * @param {Object} data - The migration payload.
 * @returns {Promise<void>}
 */
export async function addMigration(data) {
    if (isFetching.addMigration) return;

    isFetching.addMigration = true;

    try {
        const { resp } = await apiAddMigration(data);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return;

        addToast({
            title: "Migration hinzugefügt",
            subTitle: viewport.isMobile ? "" : "Die neue Migration wurde erfolgreich zum Deployment hinzugefügt.",
            type: "success"
        });
    } finally {
        isFetching.addMigration = false;
    }
}

/**
 * Deletes a specific migration from a deployment and triggers a success toast notification.
 * @param {string} deploymentId - The ID of the parent deployment.
 * @param {string} migrationId - The ID of the migration to delete.
 * @returns {Promise<void>}
 */
export async function deleteMigration(deploymentId, migrationId) {
    if (!deploymentId || !migrationId || isFetching.removeMigration) return;

    isFetching.removeMigration = true;

    try {
        const { resp } = await apiDeleteMigration(deploymentId, migrationId);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return;

        addToast({
            title: "Migration gelöscht",
            subTitle: viewport.isMobile ? "" : "Die Migration wurde erfolgreich aus dem Deployment entfernt.",
            type: "success"
        })
    } finally {
        isFetching.removeMigration = false;
    }
}

/**
 * Fetches the list of scheduled deployment times for today.
 * @returns {Promise<Array>} An array of today's deployment time strings, or an empty array if failed.
 */
export async function getTodayDeployments() {
    if (isFetching.today) return [];

    isFetching.today = true;

    try {
        const { resp, body } = await apiGetDeploymentsToday();
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return [];

        return body.times;
    } finally {
        isFetching.today = false;
    }
}
