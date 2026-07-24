import {
    apiAddEvent,
    apiCheckEvent,
    apiDeleteEvent,
    apiUpdateEvent,
    apiUpdateEventStatus
} from "../api/apiEvents.svelte";
import { handleGenericErrors, handleGlobalApiError } from "../api/globalErrorHandler.svelte";
import { normalizeResponse } from "../api/http.svelte";
import { addToast } from "../stores/toasts.svelte";
import { viewport } from "../stores/viewport.svelte";
import { eventsStore } from "../stores/events.svelte";
import { formatISODateString, getLastDayOfCurrentMonth, isISOString } from "./dateTimeUtils.js";

export const typeMap = {
    "all": "Alle Typen",
    "practice": "Proben",
    "meeting": "Meeting",
    "concert": "Konzerte",
    "other": "Sonstiges",
    // Reversed
    "Alle Typen": "all",
    "Proben": "practice",
    "Meeting": "meeting",
    "Konzerte": "concert",
    "Sonstiges": "other"
};

export const statusMap = {
    "upcoming": "Bevorstehend",
    "finished": "Abgeschlossen",
    "Bevorstehend": "upcoming",
    "Abgeschlossen": "finished"
};

export const ordinalMap = {
    "1": "ersten",
    "2": "zweiten",
    "3": "dritten",
    "4": "vierten",
    "5": "fünften"
};

export const weekDayMap = {
    "1": "Montag",
    "2": "Dienstag",
    "3": "Mittwoch",
    "4": "Donnerstag",
    "5": "Freitag",
    "6": "Samstag",
    "7": "Sonntag"
};

export const modeMap = {
    "Einmalig": "single",
    "Wöchentlich": "weekly",
    "Monatlich": "monthly",
    "single": "Einmalig",
    "weekly": "Wöchentlich",
    "monthly": "Monatlich"
};

const isFetching = {
    checkEvent: false,
    newEvent: false,
    updateEvent: false,
    updateStatus: false,
    deleteEvent: false
};

/**
 * Returns a human-readable description of how often an event occurs.
 *
 * Supports:
 * - weekly events → "Jede Woche am <Wochentag>"
 * - monthly events (by weekday or date)
 *
 * @param {string} eventId - ID of the event to resolve
 * @returns {string} Localized description of the occurrence or "Unbekannt" if event not found
 */
export function getEventOccurrenceById(eventId) {
    const event = eventsStore.raw.find(item => item.id === eventId);

    if (!event) return "Unbekannt";
    if (event.mode === "single") return formatISODateString(event.date);
    if (event.mode === "weekly") return getWeeklyOccurrence(event);
    if (event.mode === "monthly" && event.recurrence) return getMonthlyOccurrence(event);

    return "Unbekannt";
}

/**
 * Returns a human-readable description of how often an event occurs.
 *
 * Supports:
 * - weekly events → "Jede Woche am <Wochentag>"
 * - monthly events (by weekday or date)
 *
 * @param {Object} event - Event to resolve
 * @returns {string} Localized description of the occurrence or "Unbekannt" if event not found
 */
export function getEventOccurrenceByEvent(event) {
    if (!event) return "Unbekannt";
    if (event.mode === "single") return formatISODateString(event.date);
    if (event.mode === "weekly") return getWeeklyOccurrence(event);
    if (event.mode === "monthly") return getMonthlyOccurrence(event);

    return "Unbekannt";
}

/**
 * Generates a weekly recurrence string based on the event's base date.
 *
 * @param {Object} event
 * @param {string} event.date - Date string in DMY format (dd.mm.yyyy)
 * @returns {string} e.g. "Jede Woche am Montag"
 */
function getWeeklyOccurrence(event) {
    const date = new Date(event.date);
    if (Number.isNaN(date.getTime())) return "Unbekannt";
    const dayIndex = date.getUTCDay();
    const weekDayKey = dayIndex === 0 ? "7" : String(dayIndex);
    return `Jede Woche am ${weekDayMap[weekDayKey]}`;
}

/**
 * Generates a monthly recurrence description.
 *
 * Supports two types:
 * - "weekday": e.g. "Jeden Monat am 2. Dienstag"
 * - "date": calculates next valid occurrence date
 *
 * @param {Object} event
 * @param {Object} event.recurrence - Recurrence configuration
 * @returns {string}
 */
function getMonthlyOccurrence(event) {
    const { recurrence } = event;

    if (recurrence.monthlyKind === "weekday") {
        return `Jeden Monat am ${ordinalMap[recurrence.ordinal]} ${weekDayMap[recurrence.weekDay]}`;
    }

    if (recurrence.monthlyKind === "date") {
        return calculateMonthlyDateOccurrence(recurrence.dayOfMonth);
    }

    return "Unbekannt";
}

/**
 * Calculates the next valid occurrence date for a fixed day-of-month recurrence.
 *
 * Handles:
 * - Months with fewer days (e.g. Feb → fallback to last day)
 * - If the day already passed this month → shift to next month
 *
 * @param {number} dayOfMonth - Desired day of month (1–31)
 * @returns {string} Formatted date string in dd.mm.yyyy format
 */
function calculateMonthlyDateOccurrence(dayOfMonth) {
    const today = new Date();
    const targetDate = new Date(today.getFullYear(), today.getMonth(), 1);
    const currentMonthDay = Math.min(dayOfMonth, getLastDayOfCurrentMonth().getDate());

    if (currentMonthDay < today.getDate()) {
        targetDate.setMonth(targetDate.getMonth() + 1);
    }

    const lastDayOfTargetMonth = new Date(
        targetDate.getFullYear(),
        targetDate.getMonth() + 1,
        0
    ).getDate();
    targetDate.setDate(Math.min(dayOfMonth, lastDayOfTargetMonth));

    const dd = String(targetDate.getDate()).padStart(2, "0");
    const mm = String(targetDate.getMonth() + 1).padStart(2, "0");
    const yyyy = targetDate.getFullYear();
    return `${dd}.${mm}.${yyyy}`;
}

/**
 * Parses a supported date string into its numeric components (day, month, year).
 *
 * Responsibilities:
 * - Accepts date strings in either ISO 8601 or German format (DD.MM.YYYY)
 * - Converts ISO strings into German format via formatISODateString()
 * - Validates that the resulting date is a real calendar date
 *
 * Behavior:
 * - Returns null if input is not a string or is empty
 * - Returns null if the format does not match DD.MM.YYYY
 * - Returns null if the date is invalid (e.g. 31.02.2024)
 *
 * @function parseSupportedDateParts
 * @param {string} dateStr - Date string in ISO 8601 or "DD.MM.YYYY" format
 * @returns {{day: number, month: number, year: number} | null} Parsed date parts or null if invalid
 */
function parseSupportedDateParts(dateStr) {
    if (typeof dateStr !== "string" || !dateStr) return null;

    const formatted = isISOString(dateStr) ? formatISODateString(dateStr) : dateStr;
    const match = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(formatted);
    if (!match) return null;

    const [, dayStr, monthStr, yearStr] = match;
    const day = Number(dayStr);
    const month = Number(monthStr);
    const year = Number(yearStr);
    const date = new Date(year, month - 1, day);

    if (
        Number.isNaN(date.getTime()) ||
        date.getFullYear() !== year ||
        date.getMonth() !== month - 1 ||
        date.getDate() !== day
    ) {
        return null;
    }

    return { day, month, year };
}

/**
 * Gets weekday number from a date string (Monday = 1, Sunday = 7)
 * @param {string} dateStr - Date string in DD.MM.YYYY or ISO 8601 format
 * @returns {number} Weekday number (1-7, Monday first)
 */
export function getWeekDayFromDateStringMondayFirst(dateStr) {
    const parts = parseSupportedDateParts(dateStr);
    if (!parts) return Number.NaN;
    const date = new Date(parts.year, parts.month - 1, parts.day);

    const jsDay = date.getDay(); // 0–6 (Sun–Sat)

    // Convert to 1–7 (Mon–Sun)
    return jsDay === 0 ? 7 : jsDay;
}

/**
 * Gets the ordinal week number within a month from a date string (1-5).
 *
 * Responsibilities:
 * - Parses the date string into components
 * - Calculates the ordinal week based on the day of the month
 *
 * Behavior:
 * - Returns NaN if the input is invalid
 *
 * @function getOrdinalFromDateString
 * @param {string} dateStr - Date string in DD.MM.YYYY or ISO 8601 format
 * @returns {number} Ordinal week number (1-5) or NaN if invalid
 */
export function getOrdinalFromDateString(dateStr) {
    const parts = parseSupportedDateParts(dateStr);
    return parts ? Math.ceil(parts.day / 7) : Number.NaN;
}

/**
 * Extracts the day of the month from a supported date string.
 *
 * Responsibilities:
 * - Parses the date string into components
 * - Returns the day value (1-31)
 *
 * Behavior:
 * - Returns NaN if the input is invalid
 *
 * @function getDayOfMonthFromDate
 * @param {string} dateStr - Date string in DD.MM.YYYY or ISO 8601 format
 * @returns {number} Day of the month (1-31) or NaN if invalid
 */
export function getDayOfMonthFromDate(dateStr) {
    const parts = parseSupportedDateParts(dateStr);
    return parts?.day ?? Number.NaN;
}

const pendingChecks = new Map();

/**
 * Checks whether an event with the given ID exists in the system.
 *
 * Responsibilities:
 * - Prevents duplicate API calls by reusing an in-flight request (`pendingChecks`)
 * - Validates input (returns false if no ID is provided)
 * - Performs API request to verify existence
 * - Delegates global API errors to the global handler
 *
 * Behavior:
 * - Returns `false` if:
 *   - No ID is provided
 *   - The API responds with HTTP 404 (event does not exist)
 * - Returns `true` if:
 *   - The event exists (any non-404 successful response)
 *   - A global API error occurs (e.g. UNAUTHORIZED, NETWORK)
 *   - An unexpected exception is thrown
 *
 * Notes:
 * - Concurrent calls share the same promise via `pendingChecks`
 *   to avoid redundant network requests.
 * - Errors default to `true` to avoid blocking dependent flows
 *   (e.g. route guards or navigation logic).
 *
 * @async
 * @function eventExists
 * @param {string} id - ID of the event to check
 * @returns {Promise<boolean>} Whether the event exists or should be treated as existing
 */
export async function eventExists(id) {
    if (!id) return false;

    if (pendingChecks.has(id)) return await pendingChecks.get(id);

    isFetching.checkEvent = true;

    const request = (async () => {
        try {
            const { resp } = await apiCheckEvent(id);
            const normalized = normalizeResponse(resp);

            if (normalized.status === 404) return false;

            if (handleGenericErrors(normalized)) return true;

            return true;
        } catch (e) {
            return true;
        } finally {
            pendingChecks.delete(id);
            if (pendingChecks.size === 0) {
                isFetching.checkEvent = false;
            }
        }
    })();

    pendingChecks.set(id, request);
    return await request;
}

/**
 * Adds a new event using the provided event data.
 *
 * Handles:
 * - Duplicate request prevention.
 * - API call to add the event.
 * - Global API error delegation.
 * - Error-specific and success toast feedback.
 *
 * Error cases handled explicitly:
 * - BADREQUEST
 * - CONFLICT
 * - Generic fallback error
 *
 * On success, a confirmation toast is displayed.
 *
 * @async
 * @function addEvent
 * @param {Object} event - The event object to be added.
 * @returns {Promise<void>}
 */
export async function addEvent(event) {
    if (isFetching.newEvent) return;
    isFetching.newEvent = true;

    try {
        const { resp } = await apiAddEvent(event);

        const normalizedResponse = normalizeResponse(resp);
        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "Veranstaltung erstellt",
            subTitle: viewport.isMobile ? "" : "Die Veranstaltung wurde erfolgreich erstellt.",
            type: "success"
        });
    } finally {
        isFetching.newEvent = false;
    }
}

/**
 * Deletes an event by its ID via API call and handles all related UI feedback.
 *
 * Features:
 * - Prevents duplicate requests using `isFetching.deleteEvent`
 * - Handles global API errors centrally
 * - Displays success or error toasts based on response
 *
 * @param {string} id - ID of the event to delete
 * @returns {Promise<void>}
 */
export async function deleteEvent(id) {
    if (isFetching.deleteEvent) return;
    isFetching.deleteEvent = true;

    try {
        const { resp } = await apiDeleteEvent(id);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "Veranstaltung gelöscht",
            subTitle: viewport.isMobile ? "" : "Die Veranstaltung wurde erfolgreich gelöscht.",
            type: "success"
        });
    } finally {
        isFetching.deleteEvent = false;
    }
}

/**
 * Updates the status of a given event ID.
 *
 * Handles:
 * - Duplicate request prevention.
 * - API call to update the event status.
 * - Global API error delegation.
 * - Error-specific and success toast feedback.
 *
 * Error cases handled explicitly:
 * - NOTFOUND
 * - BADREQUEST
 * - Generic fallback error
 *
 * On success, a confirmation toast is displayed.
 *
 * @async
 * @function updateStatus
 * @param {string} id - Event UUID to update.
 * @returns {Promise<void>}
 */
export async function updateStatus(id) {
    if (isFetching.updateStatus) return;
    isFetching.updateStatus = true;

    try {
        const currentEvent = eventsStore.raw.find(e => e.id === id);
        if (!currentEvent) {
            handleUpdateError("NOTFOUND", "STATUS");
            return;
        }

        const { resp, body } = await apiUpdateEventStatus(id, currentEvent.rev);

        const normalizedResponse = normalizeResponse(resp);
        if (handleGlobalApiError(normalizedResponse)) return;

        const index = eventsStore.raw.findIndex(e => e.id === id);
        if (index !== -1) {
            eventsStore.raw[index] = {
                ...eventsStore.raw[index],
                rev: body.rev
            };
        }

        addToast({
            title: "Veranstaltungsstatus aktualisiert",
            subTitle: viewport.isMobile ? "" : "Der Status der Veranstaltung wurde erfolgreich aktualisiert.",
            type: "success"
        });
    } finally {
        isFetching.updateStatus = false;
    }
}

/**
 * Updates an event with the provided data.
 *
 * Handles:
 * - Duplicate request prevention.
 * - API call to update the event.
 * - Global API error delegation.
 * - Error-specific and success toast feedback.
 *
 * Error cases handled explicitly:
 * - BADREQUEST
 * - CONFLICT
 * - NOTFOUND
 * - Generic fallback error
 *
 * On success, a confirmation toast is displayed.
 *
 * @async
 * @function updateEvent
 * @param {Object} event - The updated event data.
 * @returns {Promise<void>}
 */
export async function updateEvent(event) {
    if (isFetching.updateEvent) return;
    isFetching.updateEvent = true;

    try {
        const { resp, body } = await apiUpdateEvent(event);

        const normalizedResponse = normalizeResponse(resp);
        if (handleGlobalApiError(normalizedResponse)) return;

        const index = eventsStore.raw.findIndex(e => e.id === event.id);
        if (index !== -1) {
            eventsStore.raw[index] = {
                ...eventsStore.raw[index],
                ...event,
                rev: body.rev
            };
        }

        addToast({
            title: "Veranstaltung aktualisiert",
            subTitle: viewport.isMobile ? "" : "Die Veranstaltung wurde erfolgreich aktualisiert.",
            type: "success"
        });
    } finally {
        isFetching.updateEvent = false;
    }
}