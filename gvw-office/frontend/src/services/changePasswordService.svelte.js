import { changePw } from "../api/apiAuth.svelte";

/**
 * Attempts to change the user's password with the given data.
 * 
 * Sends a POST request to the /auth/changePw endpoint without an Authorization header.
 * 
 * @param {string} email - the user's email.
 * @param {string} oldPw - the user's old/current password.
 * @param {string} newPw - the user's new password.
 * @returns {Promise<{ resp: Response, body: any }>} 
 *           An object containing the raw fetch Response (`resp`) and the parsed JSON body (`body`).
 *          `body` typically includes `ok` flag which can be discarded.
 * 
 * @example
 * const { resp, body } = await changePassword({email: "user@example.com", oldPw: "password123" newPw: "NewPassword"});
 * if (resp.ok) {
 *   console.log("Password changed")
 * }
 */
export async function changePassword(email, oldPw, newPw) {
    const { resp, body } = await changePw({ email: email, oldPassword: oldPw, newPassword: newPw });
    return { resp, body };
}