import { httpDelete, httpGet, httpPatch, httpPost, parseBodySafe } from "./http.svelte";

const apiUrl = __API_URL__;

/**
 * Fetches all reports
 *
 * Sends a GET request to `/reports/all` endpoint with an Authorization header.
 *
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include complete report objects
 *
 * @example
 * const { resp, body } = await getReports();
 *
 * if (resp?.ok) {
 *   console.log("Reports: ", body);
 * }
 */
export async function apiGetReports() {
    const resp = await httpGet(`${apiUrl}/report/all`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Sends a check request to see whether a report still exists or not
 *
 * Sends a GET request to `/report/check/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The ID of the report to check
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body is expected to be empty.
 *
 * @example
 * const { resp } = await apiCheckReport("report-id");
 *
 * if (resp?.ok) {
 *   console.log("report checked");
 * }
 */
export async function apiCheckReport(id) {
    const resp = await httpGet(`${apiUrl}/report/check/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Retrieves a report by its unique identifier.
 *
 * Sends a GET request to the report endpoint and returns both
 * the raw response object and the parsed response body.
 *
 * @param {string} id - Unique report identifier.
 *
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 * The raw response and parsed response body.
 */
export async function apiGetReport(id) {
    const resp = await httpGet(`${apiUrl}/report/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Performs a deep search across reports.
 *
 * The request can be cancelled using the provided AbortSignal,
 * making this function suitable for live-search and autocomplete
 * scenarios where previous requests should be aborted when a new
 * query is entered.
 *
 * @param {string} query - Search query.
 * @param {AbortSignal} [signal] - Optional abort signal used to cancel the request.
 *
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 * The raw response and parsed response body.
 */
export async function apiDeepSearchReport(query, signal) {
    const resp = await httpGet(`${apiUrl}/report/search?q=${query}`, "", true, signal);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Adds a repot
 *
 * Sends a POST request to `/report/add` endpoint with an Authorization header.
 *
 * @param {Object} report - The report to add
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiAddReport({
 *     title: "Report",
 *     author: "Max Mustermann",
 *     type: "ReportType",
 *     description: "Keine Beschreibung"
 * });
 *
 * if (resp?.ok) {
 *   console.log("Report added");
 * }
 */
export async function apiAddReport(report) {
    const resp = await httpPost(`${apiUrl}/report/add`, {
        title: report.title,
        author: report.author,
        type: report.type,
        description: report.description
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes a report
 *
 * Sends a DELETE request to `/report/delete/{id}` endpoint with an Authorization header.
 *
 * @param {string} reportId - The id of the report to delete
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiDeleteReport("report-id");
 *
 * if (resp?.ok) {
 *   console.log("Report deleted");
 * }
 */
export async function apiDeleteReport(reportId) {
    const resp = await httpDelete(`${apiUrl}/report/delete/${reportId}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Updates the description of an existing report.
 *
 * Sends a PATCH request containing the report identifier,
 * the current revision number and the new description.
 *
 * @param {string} reportId - Unique report identifier.
 * @param {number} rev - Current report revision used for optimistic locking.
 * @param {string} description - New report description.
 *
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 * The raw response and parsed response body.
 */
export async function apiUpdateReportDescription(reportId, rev, description) {
    const resp = await httpPatch(`${apiUrl}/report/update/description`, {
        id: reportId,
        rev: rev,
        description: description
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Uploads and updates the attachment set of an existing report.
 *
 * The provided FormData may contain:
 * - New files to upload
 * - References to existing attachments that should be retained
 * - Additional metadata required by the backend
 *
 * @param {FormData} formData - Multipart form data containing attachment information.
 * @param {string} reportId - Unique report identifier.
 *
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 * The raw response and parsed response body.
 */
export async function apiUploadReportAttachments(formData, reportId) {
    const resp = await httpPatch(`${apiUrl}/report/${reportId}/update/attachments`, formData, "", true, true);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Downloads the report attachments associated with a specific report.
 *
 * Sends a GET request to the backend endpoint responsible for providing
 * the attachments of a report. If the request succeeds,
 * the response body is returned as a {@link Blob} so it can be handled as
 * a downloadable file or processed further in the client.
 *
 * If the response indicates an error, the body is safely parsed using
 * {@link parseBodySafe} to extract any available error information.
 *
 * If the HTTP request itself fails (e.g., network error), the function
 * returns `{ resp: null, body: null }`.
 *
 * @param {string} id - The unique identifier of the report whose attachments should be downloaded.
 * @returns {Promise<{resp: Response|null, body: Blob|any|null}>}
 * An object containing:
 * - `resp`: The original {@link Response} object or `null` if the request failed.
 * - `body`: A {@link Blob} containing the file data on success, or the parsed error body on failure.
 */
export async function apiDownloadReportAttachments(id) {
    const resp = await httpGet(`${apiUrl}/report/${id}/attachments`);
    if (!resp) return { resp: null, body: null };

    let body;
    if (resp.ok) {
        body = await resp.blob();
    } else {
        body = await parseBodySafe(resp);
    }

    return { resp, body };
}

/**
 * Sends a multipart/form-data request to update a report.
 *
 * This API call is used for updates that include structured data and/or
 * file uploads, depending on the contents of the provided FormData.
 *
 * The request is sent as a PATCH operation and expects a structured response
 * body that is safely parsed using `parseBodySafe`.
 *
 * Behavior:
 * - Sends FormData directly without JSON serialization
 * - Uses HTTP PATCH against the report update endpoint
 * - Returns both raw response and parsed body
 * - Returns `{ resp: null, body: null }` if the request fails early
 *
 * @async
 * @param {FormData} formData - Payload containing report update data (and optionally files)
 * @returns {Promise<{resp: Response|null, body: any|null}>}
 */
export async function apiUpdateReport(formData) {
    const resp = await httpPatch(`${apiUrl}/report/update`, formData, "", true, true);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}