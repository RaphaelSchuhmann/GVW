import { auth } from "../stores/auth.svelte.js";

/**
 * Normalizes a fetch `Response` object into a consistent format.
 * 
 * Converts HTTP status codes and network errors into a standard oject
 * with `ok` and `errorType` fields, optionally including the original status code.
 * 
 * @param {Response | null} response 
 * @returns {{ ok: boolean, errorType?: string, status?: number }}
 * Returns `ok: true` for successfull responses, otherwise `ok: false` with a descriptive `errorType`.
 * 
 * @example
 * const normalized = normalizeResponse(resp);
 * if (!normalized.ok) console.error(normalized.errorType);
 */
export function normalizeResponse(response) {
    if (!response) return { ok: false, errorType: "NETWORK", message: "0000000" };
    if (response.ok) return { ok: true, status: response.status };

    let errorCode = response.headers.get("X-GVW-Error-Code");

    if (!errorCode) {
        errorCode = response.status >= 500 ? "0000500" : "0000400";
    }

    if (response.status === 400) return { ok: false, errorType: "BAD_REQUEST", status: 400, message: errorCode };
    if (response.status === 401) return { ok: false, errorType: "UNAUTHORIZED", status: 401, message: errorCode };
    if (response.status === 404) return { ok: false, errorType: "NOTFOUND", status: 404, message: errorCode };
    if (response.status === 409) return { ok: false, errorType: "CONFLICT", status: 409, message: errorCode };
    if (response.status === 429) return { ok: false, errorType: "REQUEST_TIMEOUT", status: 429, message: errorCode };
    if (response.status >= 500) return { ok: false, errorType: "SERVER", status: response.status, message: errorCode };

    return { ok: false, errorType: "HTTP_ERROR", status: response.status, message: errorCode };
}

/**
 * Performs an HTTP GET request with optional bearer token authorization.
 * 
 * @param {string} url - The endpoint URL to fetch.
 * @param {string} [customToken=""] - Optional token to override the authSvelte store token.
 * @param {boolean} [doAuth=true] - Whether to include the Authorization header.
 * @param {AbortSignal} [signal=null] - The abort signal from an AbortContorller.
 * @returns {Promise<Response | null>} The fetch response, or `null` if the request fails.
 * 
 * @example
 * const resp = await httpGet("/api/data");
 * if (resp?.ok) console.log(await resp.json());
 */
export async function httpGet(url, customToken = "", doAuth = true, signal = null) {
    const headers = new Headers();
    const token = customToken || auth.token;
    if (doAuth && token) headers.append("Authorization", `Bearer ${token}`);

    try {
        return await fetch(url, {
            method: "GET",
            headers: headers,
            signal: signal,
        });
    } catch {
        return null;
    }
}

/**
 * Performs an HTTP POST request with JSON body and optional bearer token authorization.
 *
 * @param {string} url - The endpoint URL to post to.
 * @param {any} body - The data to send as JSON in the request body.
 * @param {string} [customToken=""] - Optional token to override the authSvelte store token.
 * @param {boolean} [doAuth=true] - Whether to include the Authorization header.
 * @param {boolean} [usingFormData=false] - Set to true if sending FormData instead of JSON.
 * @returns {Promise<Response | null>} The fetch response, or `null` if the request fails.
 *
 * @example
 * const resp = await httpPost("/api/update", { name: "Raphael" });
 * if (resp?.ok) console.log("Update successful");
 */
export async function httpPost(url, body, customToken = "", doAuth = true, usingFormData = false) {
    const headers = new Headers();
    if (!usingFormData) headers.append("Content-Type", "application/json");

    const token = customToken || auth.token;
    if (doAuth && token) headers.append("Authorization", `Bearer ${token}`);

    try {
        return await fetch(url, {
            method: "POST",
            headers: headers,
            body: usingFormData ? body : JSON.stringify(body)
        });
    } catch {
        return null;
    }
}

/**
 * Performs an HTTP PATCH request with JSON body and optional bearer token authorization.
 *
 * @param {string} url - The endpoint URL to patch to.
 * @param {any} body - The data to send as JSON in the request body.
 * @param {string} [customToken=""] - Optional token to override the auth store token.
 * @param {boolean} [doAuth=true] - Whether to include the Authorization header.
 * @param {boolean} [usingFormData=false] - Set to true if sending FormData instead of JSON.
 * @returns {Promise<Response | null>} The fetch response, or `null` if the request fails.
 *
 * @example
 * const resp = await httpPatch("/api/update", { name: "Raphael" });
 * if (resp?.ok) console.log("Update successful");
 */
export async function httpPatch(url, body, customToken = "", doAuth = true, usingFormData = false) {
    const headers = new Headers();
    if (!usingFormData) headers.append("Content-Type", "application/json");

    const token = customToken || auth.token;
    if (doAuth && token) headers.append("Authorization", `Bearer ${token}`);

    try {
        return await fetch(url, {
            method: "PATCH",
            headers: headers,
            body: usingFormData ? body : JSON.stringify(body)
        });
    } catch {
        return null;
    }
}

/**
  * Performs an HTTP DELETE request with optional bearer token authorization.
 *
 * @param {string} url - The endpoint URL to delete to.
 * @param {string} [customToken=""] - Optional token to override the auth store token.
 * @param {boolean} [doAuth=true] - Whether to include the Authorization header.
 * @returns {Promise<Response | null>} The fetch response, or `null` if the request fails.
 *
 * @example
 * const resp = await httpDelete("/api/delete/{userId}");
 * if (resp?.ok) console.log("Delete successful");
 */
export async function httpDelete(url, customToken = "", doAuth = true) {
    const headers = new Headers();

    const token = customToken || auth.token;
    if (doAuth && token) headers.append("Authorization", `Bearer ${token}`);

    try {
        return await fetch(url, {
            method: "DELETE",
            headers: headers
        });
    } catch {
        return null;
    }
}

/**
 * Safely parses the body of a fetch `Response` as JSON.
 *
 * If the body is empty or not valid JSON, returns `null`.
 *
 * @param {Response} resp - The response to parse.
 * @returns {Promise<any|null>} The parsed JSON object, or `null` if parsing fails.
 *
 * @example
 * const resp = await httpGet("/api/data");
 * const data = await parseBodySafe(resp);
 * console.log(data);
 */
export async function parseBodySafe(resp) {
    const text = await resp.text();
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}