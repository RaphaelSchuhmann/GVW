import { httpDelete, httpGet, httpPost, parseBodySafe } from "./http.svelte";

const apiUrl = __API_URL__;

/**
 * Fetches all feedbacks
 *
 * Sends a GET request to `/support/feedback/all` endpoint with an Authorization header.
 *
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include complete feedback objects
 *
 * @example
 * const { resp, body } = await apiGetFeedbacks();
 *
 * if (resp?.ok) {
 *   console.log("Feedbacks: ", body);
 * }
 */
export async function apiGetFeedbacks() {
    const resp = await httpGet(`${apiUrl}/support/feedback/all`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Fetches details of a user feedback
 *
 * Sends a GET request to `/support/feedback/details/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - the id of the user feedback
 * 
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include a complete feedback object
 *
 * @example
 * const { resp, body } = await apiGetFeedbackDetails();
 *
 * if (resp?.ok) {
 *   console.log("Feedback: ", body);
 * }
 */
export async function apiGetFeedbackDetails(id) {
    const resp = await httpGet(`${apiUrl}/support/feedback/details/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes a user feedback
 *
 * Sends a DELETE request to `/support/feedback/delete/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The id of the user feedback to delete
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiDeleteFeedback("id-1");
 *
 * if (resp?.ok) {
 *   console.log("Feedback deleted");
 * }
 */
export async function apiDeleteFeedback(id) {
    const resp = await httpDelete(`${apiUrl}/support/feedback/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Adds a new user feedback
 *
 * Sends a POST request to `/support/feedback/add` endpoint with an Authorization header.
 *
 * @param {Object} feedback - The feedback to add
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiAddFeedback({
 *     title: "Feedback",
 *     category: "functionality",
 *     message: "some message",
 *     sentiment: 3,
 *     route: "Dashboard",
 *     appVersion: "v1.0",
 * });
 *
 * if (resp?.ok) {
 *   console.log("Feedback added");
 * }
 */

export async function apiAddFeedback(feedback) {
    const resp = await httpPost(`${apiUrl}/support/feedback/add`, {
        title: feedback.title,
        category: feedback.category,
        message: feedback.message,
        sentiment: feedback.sentiment,
        route: feedback.route,
        appVersion: feedback.appVersion,
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}