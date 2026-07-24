import { httpGet, httpPatch, httpPost, parseBodySafe } from "./http.svelte";

// @ts-ignore
const apiUrl = __API_URL__;

/**
 * Attempts to load app settings
 * 
 * Sends a GET request to `/settings/get` endpoint without an Authorization header.
 * 
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 * 
 * The response body may include application-settings-related data such as
 * score categories and max members.
 * 
 * @example
 * const { resp, body } = await getSettings();
 * if (resp?.ok) {
 *   console.log(body?.maxMembers);
 * }
 */
export async function apiGetSettings() {
    const resp = await httpGet(`${apiUrl}/settings/get`, "", false);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Attempts to update the maximum members in the app settings
 * 
 * Sends a PATCH request to `/settings/update/max-members` endpoint with and Authorization header.
 * 
 * @param {number} maxMembers - The maximum amount of members per voice.
 * @param {string} rev - The revision of the max members state.
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 * 
 * The response body may include an ok flag which can be discarded.
 * 
 * @example
 * const { resp, body } = await updateMaxMembers(10, "1-rev");
 * if (resp?.ok) {
 *   console.log("Max members updated: ", body.rev);
 * }
 */
export async function apiUpdateMaxMembers(maxMembers, rev) {
    const resp = await httpPatch(`${apiUrl}/settings/update/max-members`, { maxMembers: maxMembers, rev: rev });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Attempts to add a new score category to the app settings
 * 
 * Sends a POST request to `/settings/update/categories/add` endpoint with an Authorization header
 * 
 * @param {string} type - the internal type of the category
 * @param {string} displayValue - the displayed value of the type
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 * 
 * The response body may include an ok flag which can be discarded.
 * 
 * @example
 * const { resp } = await addCategory("new_category", "New Category");
 * if (resp?.ok) {
 *   console.log("Added category");
 * }
 */
export async function apiAddCategory(type, displayValue) {
    const resp = await httpPost(`${apiUrl}/settings/add/category`, { type: type, displayName: displayValue });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Attempts to remove a score category from teh app settings
 * 
 * Sends a POST request to `/settings/update/categories/remove` endpoint with an Authorization header
 * 
 * @param {string} type - the internal type of the category which should be removed
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 * 
 * The response body may include an ok flag which can be discarded.
 * 
 * @example
 * const { resp } = await removeCategory("removed_category");
 * if (resp?.ok) {
 *   console.log("Removed category")
 * }
 */
export async function apiRemoveCategory(type) {
    const resp = await httpPost(`${apiUrl}/settings/remove/category`, { type: type });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}