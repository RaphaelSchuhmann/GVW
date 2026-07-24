import { httpDelete, httpGet, httpPatch, httpPost, parseBodySafe } from "./http.svelte";

const apiUrl = __API_URL__;

/**
 * Fetches all members
 *
 * Sends a GET request to `/members/all` endpoint with an Authorization header.
 *
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body may include complete member objects
 *
 * @example
 * const { resp, body } = await apiGetMembers();
 *
 * if (resp?.ok) {
 *   console.log("Members: ", body);
 * }
 */
export async function apiGetMembers() {
    const resp = await httpGet(`${apiUrl}/members/all`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Sends a check request to see whether a member still exists or not
 *
 * Sends a GET request to `/members/check/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The ID of the member to check
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * The response body is expected to be empty.
 *
 * @example
 * const { resp } = await apiCheckMember("member-id");
 *
 * if (resp?.ok) {
 *   console.log("member checked");
 * }
 */
export async function apiCheckMember(id) {
    const resp = await httpGet(`${apiUrl}/members/check/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Adds a new member
 *
 * Sends a POST request to `/members/add` endpoint with an Authorization header.
 *
 * @param {Object} member - The member object to add
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiAddMember({
 *     name: "John",
 *     surname: "Doe",
 *     email: "john.doe@example.com",
 *     phone: "0123456",
 *     address: "Home 1",
 *     voice: "tenor1",
 *     status: "active",
 *     role: "member",
 *     birthday: "2000-01-01"
 *     joined: "2026",
 * });
 *
 * if (resp?.ok) {
 *   console.log("Member added");
 * }
 */
export async function apiAddMember(member) {
    const resp = await httpPost(`${apiUrl}/members/add`, member);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Deletes a member
 *
 * Sends a DELETE request to `/members/delete/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The id of the member to delete
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiDeleteMember(1);
 *
 * if (resp?.ok) {
 *   console.log("Member deleted");
 * }
 */
export async function apiDeleteMember(id) {
    const resp = await httpDelete(`${apiUrl}/members/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Switches a member's status
 *
 * Sends a PATCH request to `/members/update/status/{id}` endpoint with an Authorization header.
 *
 * @param {string} id - The id of the member to update
 * @param {string} rev - The revision of the member to update
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp, body } = await apiUpdateMemberStatus("member-id", "1-rev");
 *
 * if (resp?.ok) {
 *   console.log("Member status updated: ", body.rev);
 * }
 */
export async function apiUpdateMemberStatus(id, rev) {
    const resp = await httpPatch(`${apiUrl}/members/update/status/${id}`, {rev: rev});
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Resets a member's password
 *
 * Sends a POST request to `/user/reset/password/{id}` endpoint with an Authorization header.
 *
 * @param {number} id - The id of the member whose password to reset
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp } = await apiResetMembersPassword(1);
 *
 * if (resp?.ok) {
 *   console.log("Member password reset");
 * }
 */
export async function apiResetMembersPassword(id) {
    const resp = await httpPost(`${apiUrl}/user/reset/password/${id}`, {});
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Updates a member
 *
 * Sends a PATCH request to `/members/update` endpoint with an Authorization header.
 *
 * @param {Object} member - The member object to update
 * @returns {Promise<{ resp: Response | null, body: any | null }>}
 * An object containing the raw fetch Response (`resp`) and the parsed
 * JSON response body (`body`). Returns `{ resp: null, body: null }`
 * if the request fails before a response is received.
 *
 * @example
 * const { resp, body } = await apiUpdateMember({
 *     id: 1,
 *     rev: 1,
 *     name: "John",
 *     surname: "Doe",
 *     email: "john.doe@example.com",
 *     phone: "0123456",
 *     address: "Home 1",
 *     voice: "tenor1",
 *     status: "active",
 *     role: "member",
 *     birthday: "2000-01-01"
 *     joined: "2026",
 * });
 *
 * if (resp?.ok) {
 *   console.log("Member updated: " + body.rev_member + " " + body.rev_user);
 * }
 */
export async function apiUpdateMember(member) {
    const resp = await httpPatch(`${apiUrl}/members/update`, {
        id: member.id,
        name: member.name,
        surname: member.surname,
        email: member.email,
        phone: member.phone,
        address: member.address,
        voice: member.voice,
        status: member.status,
        role: member.role,
        birthdate: member.birthdate,
        joined: member.joined,
        rev: member.rev
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}