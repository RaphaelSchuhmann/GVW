import { apiGetSettings } from "../api/apiAppSettings.svelte";
import { handleGlobalApiError } from "../api/globalErrorHandler.svelte";
import { normalizeResponse } from "../api/http.svelte";
import { appSettings } from "../stores/appSettings.svelte.js";
import { lastRefresh } from "../stores/sseStore.svelte.js";
import { untrack } from "svelte";

let isFetching = false;
let isRunning = false;

/**
 * Initializes reactive synchronization of application settings.
 *
 * Responsibilities:
 * - Subscribes to `lastRefresh.SETTINGS` changes (e.g. via SSE updates)
 * - Triggers a settings reload whenever a refresh event occurs
 *
 * Notes:
 * - Uses Svelte reactivity (`$effect`) instead of polling
 * - Safe to call multiple times, but should typically be initialized once per view
 *
 * Intended to be called during a view lifecycle (e.g. onMount).
 *
 * @function initSettingsSync
 * @returns {void}
 */
export async function initSettingsSync() {
    if (isRunning) return;
    isRunning = true;

    $effect.root(() => {
        $effect(() => {
            const _trigger = lastRefresh.SETTINGS;

            untrack(() => {
                loadAppSettings();
            });
        });
    });
}

/**
 * Fetches the latest global application settings from the API
 * and updates the `appSettings` store.
 *
 * Prevents concurrent fetches using an internal `isFetching` flag,
 * ensuring that multiple calls (e.g., from an interval) do not cause
 * overlapping updates or race conditions.
 *
 * Handles global API errors and user-facing error feedback internally.
 * This function does not return the fetched data and is safe to call
 * repeatedly.
 *
 * @async
 * @returns {Promise<void>}
 */
export async function loadAppSettings() {
    if (isFetching) return;
    isFetching = true;

    try {
        const { resp, body } = await apiGetSettings();

        const normalizedResponse = normalizeResponse(resp);
        if (handleGlobalApiError(normalizedResponse)) return;

        Object.assign(appSettings, body);
    } finally {
        isFetching = false;
    }
}