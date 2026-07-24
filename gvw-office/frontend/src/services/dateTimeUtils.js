export const currentYear = new Date().getFullYear();
export const currentMonth = new Date().getMonth();

/**
 * Returns the number of days in a given month and year
 * @param {number} year - The year
 * @param {number} month - The month (0-indexed)
 * @returns {number} Number of days in the month
 */
export function daysInMonth(year, month) {
    return new Date(year, month + 1, 0).getDate();
}

/**
 * Returns the first weekday of a month (Monday = 0, Sunday = 6)
 * @param {number} year - The year
 * @param {number} month - The month (0-indexed)
 * @returns {number} First weekday of the month (0-6, Monday first)
 */
export function firstWeekdayOfMonth(year, month) {
    let day = new Date(year, month, 1).getDay();
    return day === 0 ? 6 : day - 1;
}

/**
 * Checks if a given day/month/year is today
 * @param {number} day - Day of the month
 * @param {number} month - Month (0-indexed)
 * @param {number} year - Year
 * @returns {boolean} True if the date is today
 */
export function isToday(day, month, year) {
    let today = new Date();
    return (day === today.getDate() && month === today.getMonth() && year === today.getFullYear());
}

/**
 * Formats an ISO date string into a localized German date string.
 *
 * Responsibilities:
 * - Parses the provided date string into a JavaScript `Date` object
 * - Formats the date using the German locale (`de-DE`)
 * - Returns the formatted date in `dd.MM.yyyy` format
 *
 * Behavior:
 * - Returns a formatted date string if parsing is successful
 * - Returns `"Ungültiges Datum"` if the input cannot be parsed
 *
 * @function formatISODateString
 * @param {string} dateStr - ISO date string (e.g. "2026-04-10T11:55:00Z")
 * @returns {string} Formatted date string or fallback message
 */
export function formatISODateString(dateStr) {
    if (!dateStr) return "Ungültiges Datum";
    const date = new Date(dateStr);
    if (Number.isNaN(date.getTime())) return "Ungültiges Datum";

    return new Intl.DateTimeFormat('de-DE', {
        day: "2-digit",
        month: "2-digit",
        year: 'numeric',
        timeZone: "UTC",
    }).format(date);
}

/**
 * Converts a German-formatted date string (DD.MM.YYYY) into an ISO 8601 string.
 *
 * Responsibilities:
 * - Parses a date string in German format (day.month.year)
 * - Creates a UTC-based Date object to avoid timezone shifts
 * - Returns the date in ISO 8601 format
 *
 * Behavior:
 * - Returns null if input is falsy
 * - Assumes input is a valid date string in "DD.MM.YYYY" format
 * - Does not perform validation on malformed input
 *
 * @function germanDateToISO
 * @param {string} germanDate - Date string in format "DD.MM.YYYY"
 * @returns {string|null} ISO 8601 formatted date string (e.g. "2026-04-09T00:00:00.000Z") or null if input is invalid
 */
export function germanDateToISO(germanDate) {
    if (!germanDate) return null;

    const [day, month, year] = germanDate.split('.');
    if (!day || !month || !year) return null;

    const date = new Date(Date.UTC(year, month - 1, day));
    if (Number.isNaN(date.getTime())) return null;

    return date.toISOString();
}

/**
 * Gets the last day of the current month.
 * * Logic: The Date constructor treats day '0' as the last day of the previous month.
 * By targeting (Current Month + 1) and Day 0, JavaScript rolls back one day
 * from the start of next month to the exact end of the current month.
 * * @returns {Date} Date object representing the last day of current month
 */
export function getLastDayOfCurrentMonth() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();

    // Setting the day to 0 returns the last day of the month prior to the one provided
    return new Date(year, month + 1, 0);
}

/**
 * Checks whether a given string is a valid ISO 8601 date string.
 *
 * Responsibilities:
 * - Verifies the input is a string
 * - Attempts to parse the string into a Date object
 * - Ensures the parsed date matches the original ISO string exactly
 *
 * Behavior:
 * - Returns false if input is not a string
 * - Returns false if the date is invalid or cannot be parsed
 * - Returns true only if the string is a strictly valid ISO 8601 format
 *
 * @function isISOString
 * @param {string} str - The string to validate
 * @returns {boolean} True if the string is a valid ISO 8601 date string, otherwise false
 */
export function isISOString(str) {
    if (typeof str !== "string") return false;

    const isIsoFormat = str.includes('T') && str.endsWith('Z');
    if (!isIsoFormat) return false;

    const d = new Date(str);
    return !Number.isNaN(d.getTime());
}

/**
 * Converts a year value into an ISO 8601 date string representing January 1st of that year.
 *
 * Responsibilities:
 * - Creates a UTC-based Date object for January 1st of the given year
 * - Returns the date in ISO 8601 format
 *
 * Behavior:
 * - Returns an empty string if input is falsy
 * - Assumes the input is a valid year (number or numeric string)
 * - Does not perform validation on malformed input
 *
 * @function yearToISOString
 * @param {number|string} year - The year to convert (e.g. 2026)
 * @returns {string} ISO 8601 formatted date string (e.g. "2026-01-01T00:00:00.000Z") or empty string if input is invalid
 */
export function yearToISOString(year) {
    if (!year) return "";
    if (!/^\d{4}$/.test(String(year))) return "";

    const date = new Date(Date.UTC(Number(year), 0, 1));
    if (Number.isNaN(date.getTime())) return "";
    return date.toISOString();
}

/**
 * Extracts the year component from a valid ISO 8601 date string.
 *
 * Responsibilities:
 * - Validates the input as a proper ISO 8601 string
 * - Extracts and returns the year portion (YYYY)
 *
 * Behavior:
 * - Returns an empty string if input is falsy or not a valid ISO string
 * - Relies on isISOString() for validation
 *
 * @function getYearFromISOString
 * @param {string} dateStr - ISO 8601 date string (e.g. "2026-01-01T00:00:00.000Z")
 * @returns {string} The extracted year (e.g. "2026") or empty string if invalid
 */
export function getYearFromISOString(dateStr) {
    if (!dateStr || !isISOString(dateStr)) return "";

    return dateStr.split('-')[0];
}

/**
 * Removes the milliseconds from a given timestamp that should be a 
 * valid ISO 8601 date string.
 * 
 * @function removeMillisecondsFromTimeStamp
 * @param {string} ts - ISO 8601 date string (e.g. "2026-01-01T00:00:00.000Z")
 * @returns {string} The date string without the milliseconds or same string if millisenconds weren't found
 */
export function removeMillisecondsFromTimeStamp(ts) {
    if (typeof ts !== "string") return ts ?? "";
    if (!ts.includes(".")) return ts;
    return ts.slice(0, ts.indexOf("."));
}
