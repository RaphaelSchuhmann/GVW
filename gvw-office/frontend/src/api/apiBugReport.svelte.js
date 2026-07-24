import { viewport } from "../stores/viewport.svelte";
import { httpDelete, httpGet, httpPost, parseBodySafe } from "./http.svelte";

const apiUrl = __API_URL__;

/**
 * Fetches all bug reports
 *
 * Sends a GET request to `/support/bugs/all` endpoint with an Authorization header.
 *
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include complete bug report objects
 *
 * @example
 * const { resp, body } = await apiGetBugReports();
 *
 * if (resp?.ok) {
 *   console.log("Bug Reports: ", body);
 * }
 */
export async function apiGetBugReports() {
    const resp = await httpGet(`${apiUrl}/support/bugs/all`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Fetches details of a bug report
 *
 * Sends a GET request to `/support/bugs/details/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - the id of the bug report
 * 
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include a complete bug report object
 *
 * @example
 * const { resp, body } = await apiGetBugReportDetails();
 *
 * if (resp?.ok) {
 *   console.log("Bug Report: ", body);
 * }
 */
export async function apiGetBugReportDetails(id) {
    const resp = await httpGet(`${apiUrl}/support/bugs/details/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes a bug report
 *
 * Sends a DELETE request to `/support/bugs/delete/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The id of the bug report to delete
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiDeleteBugReport("id-1");
 *
 * if (resp?.ok) {
 *   console.log("Bug Report deleted");
 * }
 */
export async function apiDeleteBugReport(id) {
    const resp = await httpDelete(`${apiUrl}/support/bugs/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Adds a bug report
 *
 * Sends a POST request to `/support/bugs/add` endpoint with an Authorization header.
 *
 * @param {Object} bugReport - The bug report to add
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiAddBugReport({
 *     title: "Bug",
 *     severity: "low",
 *     stepsToReproduce: "steps",
 *     route: "Dashboard",
 *     appVersion: "v1.0",
 *     os: "Windows 10",
 *     browser: "Google Chrome",
 *     viewport: "1920x1080"
 * });
 *
 * if (resp?.ok) {
 *   console.log("Bug Report added");
 * }
 */

export async function apiAddBugReport(bugReport) {
    const resp = await httpPost(`${apiUrl}/support/bugs/add`, {
        title: bugReport.title,
        severity: bugReport.severity,
        stepsToReproduce: bugReport.stepsToReproduce,
        route: bugReport.route,
        appVersion: bugReport.appVersion,
        os: bugReport.os,
        browser: bugReport.browser,
        viewport: bugReport.viewport,
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}