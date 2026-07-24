import { httpGet, parseBodySafe } from "./http.svelte";

const apiUrl = __API_URL__;

/**
 * Fetches an image asset associated with a document.
 *
 * The request targets a feature-scoped editor asset endpoint and returns
 * the raw binary data as a Blob (e.g. images, attachments, etc.).
 *
 * @param {string} feature - Feature namespace for the asset (e.g. "editor").
 * @param {string} documentId - Unique identifier of the document.
 * @param {string} filename - Name of the requested file.
 *
 * @returns {Promise<{resp: Response|null, blob: Blob|null}>}
 * The raw response and the fetched binary blob.
 */
export async function apiGetDocumentImage(feature, documentId, filename){
    const resp = await httpGet(`${apiUrl}/editor/assets/${feature}/${documentId}/${filename}`);
    if (!resp) return { resp: null, blob: null };
    const blob = await resp.blob();
    return { resp, blob };
}

/**
 * Resolves a given URL through the backend resolver service.
 *
 * This endpoint is typically used to normalize or validate external URLs
 * before they are used within the application (e.g. preview generation,
 * metadata extraction, or safe linking).
 *
 * @param {string} url - The URL to resolve.
 *
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 * The raw response and parsed response body.
 */
export async function apiResolveURl(url) {
    const resp = await httpGet(`${apiUrl}/editor/resolve?url=${encodeURIComponent(url)}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}