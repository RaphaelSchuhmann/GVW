import {apiGetAdminDashboardData, apiGetData} from "../api/apiDashboard.js";
import {normalizeResponse} from "../api/http.svelte.js";
import {handleGlobalApiError} from "../api/globalErrorHandler.svelte.js";
import {addToast} from "../stores/toasts.svelte.js";
import {viewport} from "../stores/viewport.svelte.js";
import {dashboardStore} from "../stores/dashboard.svelte.js";
import {formatISODateString} from "./dateTimeUtils.js";
import {getEventOccurrenceByEvent} from "./eventsService.svelte.js";
import {user} from "../stores/user.svelte.js";
import {adminDashboardStore} from "../stores/adminDashboard.svelte.js";

let isFetching = {
    userDashboard: false,
    adminDashboard: false,
};

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

    if (isFetching.userDashboard) return;
    isFetching.userDashboard = true;

    try {
        const {resp, body} = await apiGetData();
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
        isFetching.userDashboard = false;
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

export async function loadAdminDashboardData() {
    if (isFetching.adminDashboard || user.role !== "admin") return;
    isFetching.adminDashboard = true;

    try {
        const {resp, body} = await apiGetAdminDashboardData();
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return;

        if (
            !Number.isFinite(body?.feedbackCount) || body.feedbackCount < 0 ||
            !Number.isFinite(body?.bugReportCount) || body.bugReportCount < 0 ||
            !Number.isFinite(body?.averageSentiment) || body.averageSentiment < 0 ||
            !Number.isFinite(body?.userCount) || body.userCount < 0 ||
            !Number.isFinite(body?.orphanedUserCount) || body.orphanedUserCount < 0 ||
            typeof body?.mostUsedHash !== "string"
        ) {
            addToast({
                title: "Fehler beim laden",
                subTitle: viewport.isMobile ? "" : "Die Dashboard Daten sind unvollständig zurückgekommen.",
                type: "warning"
            });
            return;
        }

        adminDashboardStore.reportHub.feedbackCount = body.feedbackCount;
        adminDashboardStore.reportHub.bugReportCount = body.bugReportCount;
        adminDashboardStore.reportHub.averageSentiment = body.averageSentiment;
        adminDashboardStore.reportHub.mostUsedHash = body.mostUsedHash || "N/A";
        adminDashboardStore.userManagement.userCount = body.userCount;
        adminDashboardStore.userManagement.orphanedUserCount = body.orphanedUserCount;
    } finally {
        isFetching.adminDashboard = false;
    }
}