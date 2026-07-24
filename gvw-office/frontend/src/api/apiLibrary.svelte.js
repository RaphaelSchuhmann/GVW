import { httpDelete, httpGet, httpPatch, httpPost, parseBodySafe } from "./http.svelte";

const apiUrl = __API_URL__;

/**
 * Fetches all scores
 *
 * Sends a GET request to `/library/all` endpoint with an Authorization header.
 *
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include complete score objects
 *
 * @example
 * const { resp, body } = await getScores();
 *
 * if (resp?.ok) {
 *   console.log("Scores: ", body);
 * }
 */
export async function apiGetScores() {
    const resp = await httpGet(`${apiUrl}/library/all`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Sends a check request to see whether a score still exists or not
 *
 * Sends a GET request to `/library/check/{id}` endpoint with an Authorization header.
 * @param {string} id - The ID of the score to check
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body is expected to be empty.
 *
 * @example
 * const { resp } = await apiCheckScore("score-id");
 *
 * if (resp?.ok) {
 *   console.log("score checked");
 * }
 */
export async function apiCheckScore(id) {
    const resp = await httpGet(`${apiUrl}/library/check/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes a score with the given ID
 *
 * Sends a DELETE request to `/library/delete/{id}` endpoint with an Authorization header
 * and a JSON body containing the score ID.
 *
 * @param {string} id - The ID of the score to delete
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body will be empty on success.
 *
 * @example
 * const { resp, body } = await deleteScore("1234567890");
 *
 * if (resp?.ok) {
 *   console.log("Deleted score with ID 1234567890");
 * }
 */
export async function apiDeleteScore(id) {
    const resp = await httpDelete(`${apiUrl}/library/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Downloads the score files associated with a specific library entry.
 *
 * Sends a GET request to the backend endpoint responsible for providing
 * the files of a score (e.g., sheet music PDFs). If the request succeeds,
 * the response body is returned as a {@link Blob} so it can be handled as
 * a downloadable file or processed further in the client.
 *
 * If the response indicates an error, the body is safely parsed using
 * {@link parseBodySafe} to extract any available error information.
 *
 * If the HTTP request itself fails (e.g., network error), the function
 * returns `{ resp: null, body: null }`.
 *
 * @param {string} id - The unique identifier of the score whose files should be downloaded.
 * @returns {Promise<{resp: Response|null, body: Blob|any|null}>}
 * An object containing:
 * - `resp`: The original {@link Response} object or `null` if the request failed.
 * - `body`: A {@link Blob} containing the file data on success, or the parsed error body on failure.
 */
export async function apiDownloadScoreFiles(id) {
    const resp = await httpGet(`${apiUrl}/library/${id}/files`);
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
 * Adds a new score to the library.
 *
 * Sends a POST request to `/library/new` with the provided form data.
 *
 * @param {FormData} formData - The form data containing the score details and files.
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body will be empty on success.
 *
 * @example
 * const formData = new FormData();
 * formData.append("title", "My Score");
 * formData.append("file", fileInput.files[0]);
 * const { resp, body } = await addScore(formData);
 *
 * if (resp?.ok) {
 *   console.log("Added new score");
 * }
 */
export async function apiAddScore(formData) {
    const resp = await httpPost(`${apiUrl}/library/new`, formData, "", true, true);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Updates a score in the library.
 *
 * Sends a PATCH request to `/library/update` with the provided form data.
 *
 * @param {FormData} formData - The form data containing the score details and files.
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body will be empty on success.
 *
 * @example
 * const formData = new FormData();
 * formData.append("title", "My Score");
 * formData.append("file", fileInput.files[0]);
 * const { resp, body } = await updateScore(formData);
 *
 * if (resp?.ok) {
 *   console.log("Updated score: ", body.rev);
 * }
 */
export async function apiUpdateScore(formData) {
    const resp = await httpPatch(`${apiUrl}/library/update`, formData, "", true, true);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}