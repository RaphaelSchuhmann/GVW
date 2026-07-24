import { apiGetData } from "../api/apiDashboard.js";
import { normalizeResponse } from "../api/http.svelte.js";
import { handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";
import { addToast } from "../stores/toasts.svelte.js";
import { viewport } from "../stores/viewport.svelte.js";
import { dashboardStore } from "../stores/dashboard.svelte.js";
import { formatISODateString } from "./dateTimeUtils.js";
import { getEventOccurrenceByEvent } from "./eventsService.svelte.js";

let isFetching = false;

/**
 * Loads and populates dashboard data from the API.
 *
 * Responsibilities:
 * - Prevents duplicate requests using `isFetching`
 * - Fetches aggregated dashboard data (members, events, scores
 * - Delegates global API errors to global handler
 * - Displays a warning toast on non-OK responses
 * - Updates the dashboard store with the received data
 *
 * Behavior:
 * - Early exits if a request is already in progress
 * - On success, updates all relevant dashboard metrics
 * - On failure, shows a warning toast but does not throw
 *
 * @async
 * @function loadDashboardData
 * @returns {Promise<void>}
 */
export async function loadDashboardData() {

    if (isFetching) return;
    isFetching = true;

    try {
        const { resp, body } = await apiGetData();
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        if (
            !Array.isArray(body?.members) ||
            !Number.isFinite(body?.totalEvents) ||
            !Array.isArray(body?.upcomingEvents) ||
            !Number.isFinite(body?.totalScores)
        ) {
            addToast({
                title: "Fehler beim laden",
                subTitle: viewport.isMobile ? "" : "Die Dashboard Daten sind unvollständig zurückgekommen.",
                type: "warning"
            });
            return;
        }

        dashboardStore.members = body.members;
        dashboardStore.totalEvents = body.totalEvents;
        dashboardStore.upcomingEvents = body.upcomingEvents;
        dashboardStore.totalScores = body.totalScores;
    } finally {
        isFetching = false;
    }
}

/**
 * Transforms upcoming events into a simplified structure
 * suitable for UI display.
 *
 * Responsibilities:
 * - Maps raw `dashboardStore.upcomingEvents` into a UI-friendly format
 * - Combines date and time into a single `time` string
 *
 * @function prepareEvents
 * @returns {Array<Object>} List of formatted event objects
 * @returns {string} returns[].title - Event title
 * @returns {string} returns[].time - Combined date and time string
 * @returns {string} returns[].location - Event location
 * @returns {string} returns[].type - Event type
 */
export function prepareEvents() {
    return dashboardStore.upcomingEvents.map(event => {
        const date = event?.date ? formatISODateString(event?.date) : "Unbekannt";
        const time = event?.time || "";

        return {
            title: event?.title || "Unbekannt",
            time: time ? `${getEventOccurrenceByEvent(event)} - ${time} Uhr` : date,
            location: event?.location || "Unbekannt",
            type: event?.type || "other"
        };
    });
}

/**
 *
 * @returns {Object} Voice distribution counts
 * @returns {number} returns.tenor1 - Number of tenor1 members
 * @returns {number} returns.tenor2 - Number of tenor2 members
 * @returns {number} returns.bass1 - Number of bass1 members
 * @returns {number} returns.bass2 - Number of bass2 members
 */
export function getVoiceCounts() {
    return {
        tenor1: dashboardStore.members.filter(m => m.voice === "tenor1").length,
        tenor2: dashboardStore.members.filter(m => m.voice === "tenor2").length,
        bass1: dashboardStore.members.filter(m => m.voice === "bass1").length,
        bass2: dashboardStore.members.filter(m => m.voice === "bass2").length,
    };
}