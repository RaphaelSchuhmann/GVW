import { EventSourcePolyfill } from 'event-source-polyfill';
import { lastRefresh } from "../stores/sseStore.svelte.js";
import { auth } from "../stores/auth.svelte.js";
import { logout } from "./userService.svelte.js";
import { push } from "svelte-spa-router";

const apiUrl = __API_URL__;

/**
 * Active Server-Sent Events connection instance.
 *
 * @type {EventSourcePolyfill|null}
 */
let eventSource = null;

/**
 * Initializes the Server-Sent Events connection used for receiving
 * real-time application updates.
 *
 * Creates a new SSE connection if one does not already exist and
 * authenticates the request using the current user's access token.
 *
 * Incoming refresh events update the corresponding timestamp in the
 * refresh store. Authentication failures cause the user to be logged out
 * and redirected to the login page.
 *
 * @returns {void}
 */
export function initSSE() {
    if (eventSource) return;

    eventSource = new EventSourcePolyfill(`${apiUrl}/api/sync/stream`, {
        headers: {
            "Authorization": `Bearer ${auth.token}`
        }
    });

    eventSource.addEventListener('refresh', (e) => {
       const type = e.data;
       if (lastRefresh[type] !== undefined) {
           lastRefresh[type] = Date.now();
       }
    });

    eventSource.onerror = async (err) => {
        if (err.status === 401 || err.status === 403) {
            logout();
            await push("/?cpwErr=false");
        }
    }
}

/**
 * Closes and removes the active Server-Sent Events connection.
 *
 * This prevents further events from being received and allows a new
 * connection to be created by calling {@link initSSE}.
 *
 * @returns {void}
 */
export function teardownEventSource() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }
}