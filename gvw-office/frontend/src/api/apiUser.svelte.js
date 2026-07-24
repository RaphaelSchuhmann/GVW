import { httpDelete, httpGet, httpPatch, httpPost, parseBodySafe } from "./http.svelte";

// @ts-ignore
const apiUrl = __API_URL__;

/**
 * Fetches the authenticated user's data
 * 
 * Sends a GET request to `/user/data` endpoint with an Authorization header.
 * The server identifies the user from the auth token.
 * 
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 * 
 * The response body may include user-related data such as
 * email, role, name, address, and phone
 *  
 * @example
 * const { resp, body } = await getUserData();
 * 
 * if (resp?.ok) {
 *   console.log("User data: ", body);
 * }
 */
export async function apiGetUserData() {
    const resp = await httpGet(`${apiUrl}/user/data`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Sends a check request to see whether a user still exists or not
 *
 * Sends a GET request to `/user/admin/check/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The ID of the user to check
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body is expected to be empty.
 *
 * @example
 * const { resp } = await apiCheckUserAD("user-id");
 *
 * if (resp?.ok) {
 *   console.log("user checked");
 * }
 */
export async function apiCheckUserAD(id) {
    const resp = await httpGet(`${apiUrl}/user/admin/check/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Fetches all users for admin dashboard
 *
 * Sends a GET request to `/user/admin/users` endpoint with an Authorization header.
 *
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include complete admin dashboard user objects
 *
 * @example
 * const { resp, body } = await apiGetUsersAD();
 *
 * if (resp?.ok) {
 *   console.log("Users: ", body);
 * }
 */
export async function apiGetUsersAD() {
    const resp = await httpGet(`${apiUrl}/user/admin/users`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Adds a user
 *
 * Sends a POST request to `/user/admin/addUser` endpoint with an Authorization header.
 *
 * @param {Object} user - The user to add
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiAddUserAD({
 *     name: "Max Mustermann",
 *     email: "mail@test.com",
 *     phone: "01234",
 *     address: "Hauptstr. 1, 12345 Musterstadt",
 *     role: "member",
 * });
 *
 * if (resp?.ok) {
 *   console.log("User added");
 * }
 */
export async function apiAddUserAD(user) {
    const resp = await httpPost(`${apiUrl}/user/admin/addUser`, {
        name: user.name,
        email: user.email,
        phone: user.phone,
        address: user.address,
        role: user.role
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes a user
 *
 * Sends a DELETE request to `/user/admin/delete/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The id of the user to delete
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiDeleteUserAD("id-1");
 *
 * if (resp?.ok) {
 *   console.log("User deleted");
 * }
 */
export async function apiDeleteUserAD(id) {
    const resp = await httpDelete(`${apiUrl}/user/admin/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Updates a user
 *
 * Sends a PATCH request to `/user/admin/update` endpoint with an Authorization header.
 *
 * @param {Object} user - The user to update
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp, body } = await apiUpdateUserAD({
 *     id: "id-1",
 *     name: "Name",
 *     email: "email@mail.com",
 *     phone: "0123",
 *     address: "Address",
 *     type: "member",
 *     rev: "rev-1"
 * });
 *
 * if (resp?.ok) {
 *   console.log("User updated: ", body.rev);
 * }
 */
export async function apiUpdateUserAD(user) {
    const resp = await httpPatch(`${apiUrl}/user/admin/update`, {
        id: user.id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        address: user.address,
        role: user.type,
        rev: user.rev
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Resets a user's password
 *
 * Sends a POST request to `/user/admin/reset/password/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The id of the user whose password to reset
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiResetUserPasswordAD("id-1");
 *
 * if (resp?.ok) {
 *   console.log("User password reset");
 * }
 */
export async function apiResetUserPasswordAD(id) {
    const resp = await httpPost(`${apiUrl}/user/admin/reset/password/${id}`, {});
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}