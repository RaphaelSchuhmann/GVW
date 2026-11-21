import {
    apiAddDeployment, apiAddMigration,
    apiCheckDeployment,
    apiDeleteDeployment, apiDeleteMigration,
    apiGetDeployment, apiGetDeploymentsToday,
    apiGetDeploymentUpdates, apiTriggerDeployment, apiTriggerMigrationDryRun, apiUpdateDeploymentInformation
} from "../api/apiDeployments.svelte.js";
import { handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";
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

/**
 * Tracks in-flight request flags to prevent duplicate concurrent API calls.
 * @type {Object<string, boolean>}
 */
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
    today: false,
    migrationDryRun: false,
    deploymentUpdates: false
};

/**
 * Map of in-flight deployment existence checks, keyed by deployment ID,
 * used to deduplicate concurrent requests for the same deployment.
 * @type {Map<string, Promise<boolean>>}
 */
const pendingChecks = new Map();

/**
 * Map of in-flight deployment update refreshes, keyed by deployment ID,
 * used to deduplicate concurrent requests for the same deployment.
 * Each promise resolves to an object containing the deployment's updates
 * array and its current revision (`rev`).
 * @type {Map<string, Promise<{updates: Array<Object>, rev: string}>>}
 */
const pendingDeploymentUpdateQueries = new Map();

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
        });
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
            "version",
            "updates"
        ];

        if (!(body !== null && typeof body === "object" && requiredFields.every(field => field in body))) return null;

        return body;
    } finally {
        isFetching.getDeployment = false;
    }
}

/**
 * Fetches all updates for a deployment by its ID, validating that all required fields are present.
 * @param {string} id - The unique ID of the deployment.
 * @returns {Promise<{updates: Array<Object>, rev: string}>} An object containing the deployment's updates
 * array and its current revision, or an object with an empty updates array and empty rev if validation or fetching fails.
 */
export async function getDeploymentUpdates(id) {
    if (!id) return { updates: [], rev: "" };

    if (pendingDeploymentUpdateQueries.has(id)) return await pendingDeploymentUpdateQueries.get(id);

    isFetching.deploymentUpdates = true;

    const request = (async () => {
        try {
            const { resp, body } = await apiGetDeploymentUpdates(id);
            const normalized = normalizeResponse(resp);

            if (handleGlobalApiError(normalized)) return { updates: [], rev: "" };

            const requiredFields = ["updates", "rev"]

            if (!(body !== null && typeof body === "object" && requiredFields.every(field => field in body))) return { updates: [], rev: "" };

            return { updates: body.updates, rev: body.rev };
        } finally {
            pendingDeploymentUpdateQueries.delete(id);
            if (pendingDeploymentUpdateQueries.size === 0) {
                isFetching.deploymentUpdates = false;
            }
        }
    })();

    pendingDeploymentUpdateQueries.set(id, request);
    return await request;
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

            if (handleGlobalApiError(normalized)) return true;

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
    if (isFetching.removeDeployment) return;

    isFetching.removeDeployment = true;

    try {
        const { resp } = await apiDeleteDeployment(id);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return;

        addToast({
            title: "Deployment gelöscht",
            subTitle: viewport.isMobile ? "" : "Das Deployment wurde erfolgreich gelöscht.",
            type: "success"
        });
    } finally {
        isFetching.removeDeployment = false;
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
        });
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

        return Array.isArray(body?.times) ? body.times : [];
    } finally {
        isFetching.today = false;
    }
}

/**
 * Triggers a migration dry-run for a deployment.
 * @param {string} deploymentId - The unique ID of the deployment to test.
 * @returns {Promise<void>}
 */
export async function triggerMigrationDryRun(deploymentId) {
    if (isFetching.migrationDryRun || !deploymentId) return;

    isFetching.migrationDryRun = true;

    try {
        const { resp } = await apiTriggerMigrationDryRun(deploymentId);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return;

        addToast({
            title: "Migration Test gestartet",
            subTitle: "Migration Test wurde erfolgreich gestartet.",
            type: "success"
        });
    } finally {
        isFetching.migrationDryRun = false;
    }
}

/**
 * Triggers the deployment process for a deployment.
 * @param {string} deploymentId - The unique ID of the deployment to trigger.
 * @returns {Promise<boolean>} True if the deployment was triggered successfully, false otherwise.
 */
export async function triggerDeployment(deploymentId) {
    if (isFetching.deploy || !deploymentId) return false;

    isFetching.deploy = true;

    try {
        const { resp } = await apiTriggerDeployment(deploymentId);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return false;

        addToast({
            title: "Deployment gestartet",
            subTitle: "Deployment wurde erfolgreich gestartet.",
            type: "success"
        });

        return true;
    } finally {
        isFetching.deploy = false;
    }
}