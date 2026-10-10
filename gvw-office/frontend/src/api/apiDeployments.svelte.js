import { httpDelete, httpGet, httpPatch, httpPost, parseBodySafe } from "./http.svelte";

const apiUrl = import.meta.env.VITE_API_URL;

/**
 * Fetches all deployments scheduled for today.
 * @returns {Promise<{resp: Response|null, body: any}>} The HTTP response and parsed response body.
 */
export async function apiGetDeploymentsToday() {
    const resp = await httpGet(`${apiUrl}/deployment/today`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Fetches all deployments from the backend.
 * @returns {Promise<{resp: Response|null, body: any}>} The HTTP response and parsed response body.
 */
export async function apiGetAllDeployments() {
    const resp = await httpGet(`${apiUrl}/deployment/all`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Fetches a single deployment by its ID.
 * @param {string} id - The unique ID of the deployment.
 * @returns {Promise<{resp: Response|null, body: any}>} The HTTP response and parsed response body.
 */
export async function apiGetDeployment(id) {
    const resp = await httpGet(`${apiUrl}/deployment/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Checks if a deployment exists by its ID.
 * @param {string} id - The unique ID of the deployment to check.
 * @returns {Promise<{resp: Response|null, body: any}>} The HTTP response and parsed response body.
 */
export async function apiCheckDeployment(id) {
    const resp = await httpGet(`${apiUrl}/deployment/check/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Creates a new deployment entry.
 * @param {Object} deployment - The deployment object.
 * @param {string} deployment.title - Title of the deployment.
 * @param {string} deployment.version - App version associated with the deployment.
 * @param {string} deployment.date - Scheduled date.
 * @param {string} deployment.startTime - Start time of the deployment.
 * @param {string} deployment.endTime - End time of the deployment.
 * @returns {Promise<{resp: Response|null, body: any}>} The HTTP response and parsed response body.
 */
export async function apiAddDeployment(deployment) {
    const resp = await httpPost(`${apiUrl}/deployment/add/deployment`, {
        title: deployment.title,
        version: deployment.version,
        date: deployment.date,
        startTime: deployment.startTime,
        endTime: deployment.endTime
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Adds a migration to an existing deployment.
 * @param {Object} migration - The migration object.
 * @param {string} migration.deploymentId - The ID of the parent deployment.
 * @param {string} migration.rev - Revision identifier.
 * @param {string} migration.database - Database name.
 * @param {string} migration.field - Field to alter.
 * @param {string} migration.action - Action to perform.
 * @param {string} migration.conditional - Conditional check string.
 * @param {string} migration.value - Value applied by the migration.
 * @returns {Promise<{resp: Response|null, body: any}>} The HTTP response and parsed response body.
 */
export async function apiAddMigration(migration) {
    const resp = await httpPost(`${apiUrl}/deployment/add/migration`, {
        deploymentId: migration.deploymentId,
        rev: migration.rev,
        database: migration.database,
        field: migration.field,
        action: migration.action,
        conditional: migration.conditional,
        value: migration.value,
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes a deployment by its ID.
 * @param {string} id - The unique ID of the deployment to delete.
 * @returns {Promise<{resp: Response|null, body: any}>} The HTTP response and parsed response body.
 */
export async function apiDeleteDeployment(id) {
    const resp = await httpDelete(`${apiUrl}/deployment/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes a specific migration from a deployment.
 * @param {string} deploymentId - The ID of the parent deployment.
 * @param {string} migrationId - The ID of the migration to delete.
 * @returns {Promise<{resp: Response|null, body: any}>} The HTTP response and parsed response body.
 */
export async function apiDeleteMigration(deploymentId, migrationId) {
    const resp = await httpDelete(`${apiUrl}/deployment/delete/${deploymentId}/${migrationId}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Updates existing deployment metadata.
 * @param {Object} deploymentInformation - The updated deployment information.
 * @param {string} deploymentInformation.id - The unique ID of the deployment.
 * @param {string} deploymentInformation.rev - Current revision identifier.
 * @param {string} [deploymentInformation.title] - Updated title.
 * @param {string} [deploymentInformation.version] - Updated app version.
 * @param {string} [deploymentInformation.date] - Updated date.
 * @param {string} [deploymentInformation.startTime] - Updated start time.
 * @param {string} [deploymentInformation.endTime] - Updated end time.
 * @param {string} [deploymentInformation.commit] - Updated commit hash.
 * @returns {Promise<{resp: Response|null, body: any}>} The HTTP response and parsed response body.
 */
export async function apiUpdateDeploymentInformation(deploymentInformation) {
    const resp = await httpPatch(`${apiUrl}/deployment/update`, {
        id: deploymentInformation.id,
        rev: deploymentInformation.rev,
        title: deploymentInformation.title,
        version: deploymentInformation.version,
        date: deploymentInformation.date,
        startTime: deploymentInformation.startTime,
        endTime: deploymentInformation.endTime,
        commit: deploymentInformation.commit,
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}
