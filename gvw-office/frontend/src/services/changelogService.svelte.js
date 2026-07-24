import { apiAddChangelog, apiDeleteChangelog, apiGetChangelogs } from "../api/apiChangelogs.svelte.js";
import { normalizeResponse } from "../api/http.svelte.js";
import { handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";
import { addToast } from "../stores/toasts.svelte.js";
import { viewport } from "../stores/viewport.svelte.js";
import { changelogsStore } from "../stores/changelogs.svelte.js";

let isFetching = {
    get: false,
    add: false,
    delete: false
};

/**
 * Fetches all changelogs from the backend and stores them in `changelogsStore`.
 *
 * Responsibilities:
 * - Prevents concurrent fetch requests
 * - Calls the changelogs API endpoint
 * - Normalizes and validates the response
 * - Handles global API errors
 * - Displays user feedback via toast notifications
 * - Updates the changelog store on success
 *
 * Behavior:
 * - Returns early if a fetch is already in progress
 * - Shows an error toast if the request fails
 * - Shows a warning toast if the response structure is invalid
 * - Updates `changelogsStore.data` when successful
 *
 * @async
 * @function getChangelogs
 * @returns {Promise<void>}
 */
export async function getChangelogs() {
    if (isFetching.get) return;
    isFetching.get = true;

    try {
        const { resp, body } = await apiGetChangelogs();
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        if (!Array.isArray(body?.changelogs)) {
            addToast({
                title: "Fehler beim laden der Changelogs",
                subTitle: viewport.isMobile ? "" : "Der Changelog Daten sind unvollständig zurückgekommen.",
                type: "warning"
            });
            return;
        }

        changelogsStore.data = body.changelogs;
    } finally {
        isFetching.get = false;
    }
}

/**
 * Sends a new changelog entry to the backend.
 *
 * Responsibilities:
 * - Prevents concurrent add requests
 * - Adds a timestamp to the changelog before sending
 * - Calls the API to persist the changelog
 * - Handles global and local API errors
 * - Displays success or error feedback via toast notifications
 *
 * Behavior:
 * - Returns early if another add request is in progress
 * - Adds `timestamp` as ISO string before sending
 * - Shows validation error toast for BADREQUEST responses
 * - Shows generic error toast for other failures
 * - Shows success toast when the changelog is added successfully
 *
 * @async
 * @function addChangelog
 * @param {Object} changelog - Changelog data to be created
 * @returns {Promise<void>}
 */
export async function addChangelog(changelog) {
    if (isFetching.add) return;
    isFetching.add = true;

    try {
        changelog.timestamp = new Date().toISOString();

        const { resp } = await apiAddChangelog(changelog);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "Changelog hinzugefügt",
            subTitle: viewport.isMobile ? "" : "Der neue Changelog wurde erfolgreich im System hinzugefügt.",
            type: "success"
        });
    } finally {
        isFetching.add = false;
    }
}

/**
 * Deletes a changelog entry by its ID.
 *
 * Responsibilities:
 * - Prevents concurrent delete requests
 * - Calls the API to delete the changelog
 * - Handles global API errors
 * - Delegates error-specific handling
 * - Displays success feedback via toast notifications
 *
 * Behavior:
 * - Returns early if a delete request is already in progress
 * - Calls `handleDeleteError` for non-OK responses
 * - Shows success toast when deletion succeeds
 *
 * @async
 * @function deleteChangelog
 * @param {string} changelogId - ID of the changelog to delete
 * @returns {Promise<void>}
 */
export async function deleteChangelog(changelogId) {
    if (!changelogId || isFetching.delete) return;
    isFetching.delete = true;

    try {
        const { resp } = await apiDeleteChangelog(changelogId);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "Changelog gelöscht",
            subTitle: viewport.isMobile ? "" : "Der Changelog wurde erfolgreich gelöscht.",
            type: "success"
        });
    } finally {
        isFetching.delete = false;
    }
}

/**
 * Formats raw changelog text into HTML.
 *
 * Responsibilities:
 * - Splits text into lines
 * - Converts lines starting with "-" into list items
 * - Converts lines ending with ":" into section headers
 *
 * Behavior:
 * - Returns an empty string if input is falsy
 * - Ignores lines that do not match any formatting rule
 *
 * @function formatChangelog
 * @param {string} rawText - Raw changelog text (newline-separated)
 * @returns {string} HTML-formatted changelog string
 */
export function formatChangelog(rawText) {
    if (!rawText) return "";

    const html = rawText
        .split("\n")
        .map(line => {
            const trimmed = line.trim();

            if (trimmed.startsWith("-")) {
                return `<li class="ml-4 list-disc">${trimmed.substring(1).trim()}</li>`;
            }

            if (trimmed.endsWith(":")) {
                return `<p class="font-bold mt-2">${line}</p>`;
            }

            return line;
        })
        .join("");

    return html;
}