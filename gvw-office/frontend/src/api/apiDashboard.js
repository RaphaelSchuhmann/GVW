import { httpGet, parseBodySafe } from "./http.svelte.js";

const apiUrl = __API_URL__;

/**
 * Fetches dashboard data
 *
 * Sends a GET request to `/dashboard/data` endpoint with an Authorization header.
 *
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include complete dashboard data
 *
 * @example
 * const { resp, body } = await apiGetData();
 *
 * if (resp?.ok) {
 *   console.log("Dashboard data: ", body);
 * }
 */
export async function apiGetData() {
    const resp = await httpGet(`${apiUrl}/dashboard/data`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}