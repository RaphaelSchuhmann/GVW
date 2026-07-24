import { httpDelete, httpGet, httpPatch, httpPost, parseBodySafe } from "./http.svelte";

const apiUrl = __API_URL__;

/**
 * Fetches all events
 *
 * Sends a GET request to `/events/all` endpoint with an Authorization header.
 *
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include complete event objects
 *
 * @example
 * const { resp, body } = await apiGetEvents();
 *
 * if (resp?.ok) {
 *   console.log("Events: ", body);
 * }
 */
export async function apiGetEvents() {
    const resp = await httpGet(`${apiUrl}/events/all`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Sends a check request to see whether an event still exists or not
 *
 * Sends a GET request to `/events/check/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The ID of the event to check
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body is expected to be empty.
 *
 * @example
 * const { resp } = await apiCheckEvent("event-id");
 *
 * if (resp?.ok) {
 *   console.log("event checked");
 * }
 */
export async function apiCheckEvent(id) {
    const resp = await httpGet(`${apiUrl}/events/check/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Adds an event
 *
 * Sends a POST request to `/events/add` endpoint with an Authorization header.
 *
 * @param {Object} event - The event to add
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiAddEvent({
 *     title: "Event",
 *     type: "type",
 *     date: "01.01.2000",
 *     time: "15:30",
 *     location: "Location XY",
 *     description: "Description",
 *     status: "status",
 *     mode: "single",
 *     recurrence: {
 *         monthlyKind: null,
 *         dayOfMonth: null,
 *         weekDay: null,
 *         ordinal: null
 *     }
 * });
 *
 * if (resp?.ok) {
 *   console.log("Event added");
 * }
 */
export async function apiAddEvent(event) {
    const resp = await httpPost(`${apiUrl}/events/add`, {
        title: event.title,
        type: event.type,
        date: event.date,
        time: event.time,
        location: event.location,
        description: event.description,
        status: event.status,
        mode: event.mode,
        recurrence: event.recurrence
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes an event
 *
 * Sends a DELETE request to `/events/delete/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The id of the event to delete
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiDeleteEvent(1);
 *
 * if (resp?.ok) {
 *   console.log("Event deleted");
 * }
 */
export async function apiDeleteEvent(id) {
    const resp = await httpDelete(`${apiUrl}/events/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Switches an event's status
 *
 * Sends a PATCH request to `/events/update/status/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The id of the event to update
 * @param {string} rev - The revision of the event to update
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp, body } = await apiUpdateEventStatus(1, 1);
 *
 * if (resp?.ok) {
 *   console.log("Event status updated: ", body.rev);
 * }
 */
export async function apiUpdateEventStatus(id, rev) {
    const resp = await httpPatch(`${apiUrl}/events/update/status/${id}`, {rev: rev});
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Updates an event
 *
 * Sends a PATCH request to `/events/update` endpoint with an Authorization header.
 *
 * @param {Object} event - The event to update
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp, body } = await apiUpdateEvent({
 *     id: 1,
 *     title: "Event",
 *     type: "type",
 *     date: "01.01.2000",
 *     time: "15:30",
 *     location: "Location XY",
 *     description: "Description",
 *     status: "status",
 *     mode: "single",
 *     recurrence: {
 *         monthlyKind: null,
 *         dayOfMonth: null,
 *         weekDay: null,
 *         ordinal: null
 *     },
 *     rev: 1
 * });
 *
 * if (resp?.ok) {
 *   console.log("Event updated: ", body.rev);
 * }
 */
export async function apiUpdateEvent(event) {
    const resp = await httpPatch(`${apiUrl}/events/update`, {
        id: event.id,
        title: event.title,
        type: event.type,
        date: event.date,
        time: event.time,
        location: event.location,
        description: event.description,
        status: event.status,
        mode: event.mode,
        recurrence: event.recurrence,
        rev: event.rev
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}