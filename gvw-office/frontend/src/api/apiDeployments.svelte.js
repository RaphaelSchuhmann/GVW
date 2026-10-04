import { httpDelete, httpGet, httpPatch, httpPost, parseBodySafe } from "./http.svelte";

const apiUrl = import.meta.env.VITE_API_URL;

export async function apiGetDeploymentsToday() {
    const resp = await httpGet(`${apiUrl}/deployment/today`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

export async function apiGetAllDeployments() {
    const resp = await httpGet(`${apiUrl}/deployment/all`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

export async function apiGetDeployment(id) {
    const resp = await httpGet(`${apiUrl}/deployment/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

export async function apiCheckDeployment(id) {
    const resp = await httpGet(`${apiUrl}/deployment/check/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

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

export async function apiAddMigration(migration) {
    const resp = await httpPost(`${apiUrl}/deployment/add/migration`, {
        deploymentId: migration.deploymentId,
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

export async function apiDeleteDeployment(id) {
    const resp = await httpDelete(`${apiUrl}/deployment/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

export async function apiDeleteMigration(deploymentId, migrationId) {
    const resp = await httpDelete(`${apiUrl}/deployment/delete/${deploymentId}/${migrationId}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

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