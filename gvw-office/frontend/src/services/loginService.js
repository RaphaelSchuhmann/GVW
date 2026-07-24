import { authenticate, login } from "../api/apiAuth.svelte";

/**
 * Attempts to authenticate a user with the given email and password.
 * 
 * @param {string} email - The user's email address used for login
 * @param {string} password - The user's password.
 * @returns {Promise<{ resp: Response, body: any }>} 
 *           An object containing the raw fetch Response (`resp`) and the parsed JSON body (`body`).
 *          `body` typically includes `authToken`, `email`, `changePassword`, and `firstLogin` flags.
 * 
 * @example
 * const { resp, body } = await loginUser("user@example.com", "password123");
 * if (resp.ok) {
 *   console.log(body.authToken)
 * }
 */
export async function loginUser(email, password) {
    const { resp, body } = await login(email, password);
    return { resp, body };
}

/**
 * Attempts to automatically a authenticate the user using a token.
 * 
 * Sends a GET request to the /auth/auto endpoint with an Authorization header.
 * 
 * @param {string} token - the from localstorage loaded auth token
 * @returns {Promise<{ resp: Response, body: any }>} 
 *           An object containing the raw fetch Response (`resp`) and the parsed JSON body (`body`).
 *          `body` typically includes `ok` flag which can be discarded.
 * 
 * @example
 * const { resp, body } = await authenticateUser("token123");
 * if (resp.ok) {
 *   console.log("Authenticated");
 * }
 */
export async function authenticateUser(token) {
    const { resp, body } = await authenticate(token);
    return { resp, body };
}