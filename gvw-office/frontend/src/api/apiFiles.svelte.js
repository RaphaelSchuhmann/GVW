import { httpGet, parseBodySafe } from "./http.svelte.js";

const apiUrl = import.meta.env.VITE_API_URL;

/**
 * Loads a single file associated with a specific entry and service.
 *
 * Sends a GET request to the backend endpoint responsible for providing
 * the file of a document (e.g., sheet music PDF). If the request succeeds,
 * the response body is returned as a {@link Blob} so it can be handled as
 * a downloadable file or processed further in the client.
 *
 * If the response indicates an error, the body is safely parsed using
 * {@link parseBodySafe} to extract any available error information.
 *
 * If the HTTP request itself fails (e.g., network error), the function
 * returns `{ resp: null, body: null }`.
 *
 * @param {string} service - The name of the service whose documents file should be loaded.
 * @param {string} documentId - The unique identifier of the document whose files should be loaded.
 * @param {string} fileId - The unique identifier of the file which should be loaded.
 * @returns {Promise<{resp: Response|null, body: Blob|any|null}>}
 * An object containing:
 * - `resp`: The original {@link Response} object or `null` if the request failed.
 * - `body`: A {@link Blob} containing the file data on success, or the parsed error body on failure.
 */
export async function apiLoadFile(service, documentId, fileId) {
    const resp = await httpGet(`${apiUrl}/file/${service}/${documentId}/${fileId}`);
    if (!resp) return { resp: null, body: null };

    let body;
    if (resp.ok) {
        body = await resp.blob();
    } else {
        body = await parseBodySafe(resp)
    }

    return { resp, body };
}

/**
 * Downloads the document files associated with a specific entry.
 *
 * Sends a GET request to the backend endpoint responsible for providing
 * the files of a document (e.g., sheet music PDFs). If the request succeeds,
 * the response body is returned as a {@link Blob} so it can be handled as
 * a downloadable file or processed further in the client.
 *
 * If the response indicates an error, the body is safely parsed using
 * {@link parseBodySafe} to extract any available error information.
 *
 * If the HTTP request itself fails (e.g., network error), the function
 * returns `{ resp: null, body: null }`.
 *
 * @param {string} service - The name of the service whose documents files should be downloaded.
 * @param {string} id - The unique identifier of the document whose files should be downloaded.
 * @returns {Promise<{resp: Response|null, body: Blob|any|null}>}
 * An object containing:
 * - `resp`: The original {@link Response} object or `null` if the request failed.
 * - `body`: A {@link Blob} containing the file data on success, or the parsed error body on failure.
 */
export async function apiStreamFilesAsZip(service, id) {
    const resp = await httpGet(`${apiUrl}/file/download/${service}/${id}/zip`);
    if (!resp) return { resp: null, body: null };

    let body;
    if (resp.ok) {
        body = await resp.blob();
    } else {
        body = await parseBodySafe(resp);
    }

    return { resp, body };
}