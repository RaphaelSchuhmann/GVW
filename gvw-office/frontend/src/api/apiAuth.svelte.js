import { httpGet, httpPost, parseBodySafe } from "./http.svelte";

// @ts-ignore
const apiUrl = __API_URL__;

/**
 * Authenticates a user using email and password credentials.
 *
 * Sends a POST request to the `/auth/login` endpoint without an
 * Authorization header.
 *
 * @param {string} email - The user's email address.
 * @param {string} password - The user's password.
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include authentication-related data such as
 * an auth token and password-change flags.
 *
 * @example
 * const { resp, body } = await login("user@example.com", "password123");
 * if (resp?.ok) {
 *   console.log(body.authToken);
 * }
 */
export async function login(email, password) {
    const resp = await httpPost(`${apiUrl}/auth/login`, { email: email, password: password }, "", false);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Changes a user's password.
 *
 * Sends a POST request to the `/auth/changePw` endpoint without an
 * Authorization header.
 *
 * @param {Object} data - Password change payload.
 * @param {string} data.email - The user's email address.
 * @param {string} data.oldPassword - The user's current password.
 * @param {string} data.newPassword - The user's new password.
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await changePw({
 *   email: "user@example.com",
 *   oldPassword: "password123",
 *   newPassword: "NewPassword"
 * });
 *
 * if (resp?.ok) {
 *   console.log("Password changed");
 * }
 */
export async function changePw(data) {
    const resp = await httpPost(`${apiUrl}/auth/changePw`, data, "", false);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Attempts to authenticate a user using an existing authentication token.
 *
 * Sends a GET request to the `/auth/auto` endpoint with the token
 * provided as an Authorization header.
 *
 * @param {string} token - A previously issued authentication token.
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await authenticate(token);
 * if (resp?.ok) {
 *   console.log("Authenticated");
 * }
 */
export async function authenticate(token) {
    const resp = await httpGet(`${apiUrl}/auth/auto`, token);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}