import { httpDelete, httpGet, httpPost, parseBodySafe } from "./http.svelte.js";

const apiUrl = __API_URL__;

/**
 * Fetches all changelogs
 *
 * Sends a GET request to `/changelogs/all` endpoint with an Authorization header.
 *
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include complete changelog objects
 *
 * @example
 * const { resp, body } = await apiGetChangelogs();
 *
 * if (resp?.ok) {
 *   console.log("Changelogs: ", body);
 * }
 */
export async function apiGetChangelogs() {
    const resp = await httpGet(`${apiUrl}/changelogs/all`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Adds a changelog
 *
 * Sends a POST request to `/changelogs/add` endpoint with an Authorization header.
 *
 * @param {Object} changelog - The changelog to add
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiAddChangelog({
 *     version: "v1.0",
 *     title: "Changelog v1.0",
 *     content: "Changelog content",
 *     timestamp: new Date()
 * });
 *
 * if (resp?.ok) {
 *   console.log("Changelog added");
 * }
 */
export async function apiAddChangelog(changelog) {
    const resp = await httpPost(`${apiUrl}/changelogs/add`, {
        version: changelog.version,
        title: changelog.title,
        content: changelog.content,
        timestamp: changelog.timestamp
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes a changelog
 *
 * Sends a DELETE request to `/changelogs/delete/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The id of the changelog to delete
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiDeleteChangelog("1");
 *
 * if (resp?.ok) {
 *   console.log("Changelog deleted");
 * }
 */
export async function apiDeleteChangelog(id) {
    const resp = await httpDelete(`${apiUrl}/changelogs/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}