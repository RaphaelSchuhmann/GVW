import {
    apiAddBugReport,
    apiDeleteBugReport,
    apiGetBugReportDetails,
    apiGetBugReports
} from "../api/apiBugReport.svelte";
import { apiAddFeedback, apiDeleteFeedback, apiGetFeedbackDetails, apiGetFeedbacks } from "../api/apiFeedback.svelte";
import { handleGlobalApiError } from "../api/globalErrorHandler.svelte";
import { normalizeResponse } from "../api/http.svelte";
import { appSettings } from "../stores/appSettings.svelte";
import { bugReportStore, feedbackStore } from "../stores/reportHub.svelte";
import { addToast } from "../stores/toasts.svelte";
import { viewport } from "../stores/viewport.svelte";

export const severityMap = {
    "_low": "Niedrig",
    "_medium": "Mittel",
    "_high": "Hoch",
    "_severe": "Schwer",
    "Niedrig": "_low",
    "Mittel": "_medium",
    "Hoch": "_high",
    "Schwer": "_severe"
};

let currentlyDeleting = {
    FEEDBACK: new Set(),
    BUG_REPORT: new Set()
}

let isFetching = {
    allFeedbacks: false,
    allBugReports: false,
    submitFeedback: false,
    submitBugReport: false,
}

/**
 * Extracts displayable dropdown items from a bidirectional mapping object.
 *
 * Responsibilities:
 * - Iterates through a map containing internal keys and display values
 * - Filters out internal keys (prefixed with "_")
 * - Ensures each value is only included once
 *
 * Behavior:
 * - Returns an empty array if map is falsy
 * - Ignores duplicate or already processed entries
 *
 * @function getDropdownItemsFromMap
 * @param {Object<string, string>} map - Mapping of internal keys to display values and vice versa
 * @returns {string[]} Array of display names for dropdown usage
 */
export function getDropdownItemsFromMap(map) {
    const categories = map || {};
    return Object.keys(categories).filter(key => !key.startsWith("_"));
}

/**
 * Fetches all feedback entries and updates the feedback store.
 *
 * Responsibilities:
 * - Prevents duplicate concurrent requests
 * - Calls the feedback API endpoint
 * - Handles global and local API errors
 * - Updates the feedback store with retrieved data
 *
 * Behavior:
 * - Returns early if a fetch is already in progress
 * - Displays a toast on failure
 *
 * @function getAllFeedbacks
 * @returns {Promise<void>}
 */
export async function getAllFeedbacks() {
    if (isFetching.allFeedbacks) return;
    isFetching.allFeedbacks = true;

    try {
        const { resp, body } = await apiGetFeedbacks();
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;
        
        Object.assign(feedbackStore, { data: body.feedbacks });
    } finally {
        isFetching.allFeedbacks = false;
    }
}

/**
 * Fetches all bug reports and updates the bug report store.
 *
 * Responsibilities:
 * - Prevents duplicate concurrent requests
 * - Calls the bug report API endpoint
 * - Handles global and local API errors
 * - Updates the bug report store with retrieved data
 *
 * Behavior:
 * - Returns early if a fetch is already in progress
 * - Displays a toast on failure
 *
 * @function getAllBugReports
 * @returns {Promise<void>}
 */
export async function getAllBugReports() {
    if (isFetching.allBugReports) return;
    isFetching.allBugReports = true;

    try {
        const { resp, body } = await apiGetBugReports();
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;
        
        Object.assign(bugReportStore, { data: body.reports });
    } finally {
        isFetching.allBugReports = false;
    }
}

/**
 * Submits a new feedback or bug report based on the provided type.
 *
 * Responsibilities:
 * - Maps form data to the correct payload structure
 * - Enriches data with metadata (route, app version, environment)
 * - Delegates submission to the appropriate handler
 *
 * Behavior:
 * - Does nothing if type is missing or unsupported
 *
 * @function submitNewItem
 * @param {Object} data - Form data containing user input
 * @param {string} type - Submission type ("Feedback" or "Fehler melden")
 * @returns {Promise<void>}
 */
export async function submitNewItem(data, type) {
    if (!type) return;

    if (type === "Feedback") {
        const feedback = {
            title: data.title,
            category: data.dropdown,
            message: data.textarea,
            sentiment: data.sentiment,
            route: "",
            appVersion: ""
        };

        const metaData = getMetadata();

        feedback.route = metaData.route;
        feedback.appVersion = metaData.appVersion;

        await addFeedback(feedback);
    } else if (type === "Fehler melden") {
        const bugReport = {
            title: data.title,
            severity: data.dropdown,
            stepsToReproduce: data.textarea,
            route: "",
            appVersion: "",
            os: "",
            browser: "",
            viewport: ""
        };

        const metaData = getMetadata();

        bugReport.route = metaData.route;
        bugReport.appVersion = metaData.appVersion;
        bugReport.os = metaData.os;
        bugReport.browser = metaData.browser;
        bugReport.viewport = metaData.viewport;

        await addBugReport(bugReport);
    }
}

/**
 * Sends a new feedback entry to the backend.
 *
 * Responsibilities:
 * - Prevents duplicate submissions
 * - Calls the feedback API endpoint
 * - Handles API errors and displays feedback to the user
 *
 * Behavior:
 * - Returns early if a submission is already in progress
 * - Displays success or error toast depending on outcome
 *
 * @function addFeedback
 * @param {Object} feedback - Feedback payload
 * @returns {Promise<void>}
 */
async function addFeedback(feedback) {
    if (isFetching.submitFeedback) return;
    isFetching.submitFeedback = true;

    try {
        const { resp } = await apiAddFeedback(feedback);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "Feedback abgeschickt",
            subTitle: viewport.isMobile ? "" : "Ihr Feedback wurde erfolgreich abgeschickt.",
            type: "success"
        });
    } finally {
        isFetching.submitFeedback = false;
    }
}

/**
 * Sends a new bug report to the backend.
 *
 * Responsibilities:
 * - Prevents duplicate submissions
 * - Calls the bug report API endpoint
 * - Handles API errors and displays feedback to the user
 *
 * Behavior:
 * - Returns early if a submission is already in progress
 * - Displays success or error toast depending on outcome
 *
 * @function addBugReport
 * @param {Object} bugReport - Bug report payload
 * @returns {Promise<void>}
 */
async function addBugReport(bugReport) {
    if (isFetching.submitBugReport) return;
    isFetching.submitBugReport = true;

    try {
        const { resp } = await apiAddBugReport(bugReport);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "Fehler gemeldet",
            subTitle: viewport.isMobile ? "" : "Der Fehler wurde erfolgreich gemeldet.",
            type: "success"
        });
    } finally {
        isFetching.submitBugReport = false;
    }
}

/**
 * Collects runtime metadata about the current client environment.
 *
 * Responsibilities:
 * - Extracts current route from the URL
 * - Retrieves application version from settings
 * - Detects browser and operating system
 * - Captures viewport dimensions
 *
 * Behavior:
 * - Falls back to "N/A" for unsupported browser APIs
 *
 * @function getMetadata
 * @returns {{
 *   route: string,
 *   appVersion: string,
 *   os: string,
 *   browser: string,
 *   viewport: string
 * }} Metadata object
 */
function getMetadata() {
    const metaData = {
        route: "",
        appVersion: "",
        os: "",
        browser: "",
        viewport: ""
    };

    metaData.route = globalThis.location.href.split("/").findLast(part => part !== "");

    metaData.appVersion = appSettings.appVersion;

    if (navigator.userAgentData) {
        const brand = navigator.userAgentData.brands.find(b =>
            b.brand !== 'Chromium' && b.brand !== 'Not(A:Brand)' && b.brand !== 'Not-A.Brand'
        );
        if (brand) {
            metaData.browser = `${brand.brand} ${brand.version}`;
        } else {
            metaData.browser = "N/A";
        }

        metaData.os = navigator.userAgentData.platform;
    } else {
        metaData.browser = "N/A";
        metaData.os = "N/A";
    }

    metaData.viewport = `${globalThis.innerWidth}x${globalThis.innerHeight}`;

    return metaData;
}

/**
 * Deletes a feedback entry by ID.
 *
 * Responsibilities:
 * - Prevents duplicate delete operations for the same ID
 * - Calls the delete feedback API endpoint
 * - Handles API errors and displays feedback to the user
 *
 * Behavior:
 * - Returns early if ID is invalid or already being deleted
 * - Displays success or error toast depending on outcome
 *
 * @function deleteFeedback
 * @param {string|number} id - ID of the feedback to delete
 * @returns {Promise<void>}
 */
export async function deleteFeedback(id) {
    if (!id || currentlyDeleting.FEEDBACK.has(id)) return;
    currentlyDeleting.FEEDBACK.add(id);

    try {
        const { resp } = await apiDeleteFeedback(id);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "User Feedback entfernt",
            subTitle: viewport.isMobile ? "" : "Das User Feedback wurde erfolgreich aus dem System entfernt.",
            type: "success"
        });
    } finally {
        currentlyDeleting.FEEDBACK.delete(id);
    }
}

/**
 * Deletes a bug report by ID.
 *
 * Responsibilities:
 * - Prevents duplicate delete operations for the same ID
 * - Calls the delete bug report API endpoint
 * - Handles API errors and displays feedback to the user
 *
 * Behavior:
 * - Returns early if ID is invalid or already being deleted
 * - Displays success or error toast depending on outcome
 *
 * @function deleteBugReport
 * @param {string|number} id - ID of the bug report to delete
 * @returns {Promise<void>}
 */
export async function deleteBugReport(id) {
    if (!id || currentlyDeleting.BUG_REPORT.has(id)) return;
    currentlyDeleting.BUG_REPORT.add(id);

    try {
        const { resp } = await apiDeleteBugReport(id);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "User Feedback entfernt",
            subTitle: viewport.isMobile ? "" : "Das User Feedback wurde erfolgreich aus dem System entfernt.",
            type: "success"
        });
    } finally {
        currentlyDeleting.BUG_REPORT.delete(id);
    }
}

/**
 * Retrieves detailed information for a feedback or bug report item.
 *
 * Responsibilities:
 * - Routes the request to the correct detail fetch function
 * - Supports multiple item types
 *
 * Behavior:
 * - Returns null if type is unsupported or input is invalid
 *
 * @function getItemDetails
 * @param {string|number} id - ID of the item
 * @param {string} type - Item type ("feedback" or "bug_reports")
 * @returns {Promise<Object|null>} Item details or null
 */
export async function getItemDetails(id, type) {
    if (!id || !type) return;

    switch (type) {
        case "feedback":
            return getFeedbackDetails(id);
    
        case "bug_reports":
            return getBugReportDetails(id);

        default:
            return null;
    }
}

/**
 * Fetches detailed information for a specific feedback entry.
 *
 * Responsibilities:
 * - Calls the feedback details API endpoint
 * - Handles API errors
 *
 * Behavior:
 * - Returns undefined on error
 *
 * @function getFeedbackDetails
 * @param {string|number} id - Feedback ID
 * @returns {Promise<Object|undefined>} Feedback details
 */
async function getFeedbackDetails(id) {
    const { resp, body } = await apiGetFeedbackDetails(id);
    const normalizedResponse = normalizeResponse(resp);

    if (handleGlobalApiError(normalizedResponse)) return;

    return body;
}

/**
 * Fetches detailed information for a specific bug report.
 *
 * Responsibilities:
 * - Calls the bug report details API endpoint
 * - Handles API errors
 *
 * Behavior:
 * - Returns undefined on error
 *
 * @function getBugReportDetails
 * @param {string|number} id - Bug report ID
 * @returns {Promise<Object|undefined>} Bug report details
 */
async function getBugReportDetails(id) {
    const { resp, body } = await apiGetBugReportDetails(id);
    const normalizedResponse = normalizeResponse(resp);

    if (handleGlobalApiError(normalizedResponse)) return;

    return body;
}