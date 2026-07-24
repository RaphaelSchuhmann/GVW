import { EventSourcePolyfill } from 'event-source-polyfill';
import { lastRefresh } from "../stores/sseStore.svelte.js";
import { auth } from "../stores/auth.svelte.js";
import { logout } from "./userService.svelte.js";
import { push } from "svelte-spa-router";

const apiUrl = __API_URL__;

let eventSource = null;

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

export function teardownEventSource() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }
}