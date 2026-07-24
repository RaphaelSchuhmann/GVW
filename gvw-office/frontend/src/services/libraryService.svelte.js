import { appSettings } from "../stores/appSettings.svelte";
import { libraryStore } from "../stores/library.svelte";
import {
    apiAddScore,
    apiCheckScore,
    apiDeleteScore,
    apiDownloadScoreFiles,
    apiUpdateScore
} from "../api/apiLibrary.svelte";
import { normalizeResponse } from "../api/http.svelte";
import { handleGenericErrors, handleGlobalApiError } from "../api/globalErrorHandler.svelte";
import { addToast } from "../stores/toasts.svelte";
import { viewport } from "../stores/viewport.svelte";
import { triggerFileDownload } from "./utils.js";

export const voiceMap = {
    "t": "Tenor",
    "t1": "1. Tenor",
    "t2": "2. Tenor",
    "b": "Bass",
    "b1": "1. Bass",
    "b2": "2. Bass",
    "s": "Sopran",
    "a": "Alt",
    "Tenor": "t",
    "1. Tenor": "t1",
    "2. Tenor": "t2",
    "Bass": "b",
    "1. Bass": "b1",
    "2. Bass": "b2",
    "Sopran": "s",
    "Alt": "a"
};

let isFetching = {
    checkScore: false,
    newScore: false,
    updateScore: false,
    deleteScore: false,
    downloadScore: false
};

/**
 * Returns a list of display names for all available score categories.
 *
 * The function reads the category mapping from `appSettings.scoreCategories`.
 * Categories are internally stored as bidirectional key-value pairs
 * (e.g., `type -> displayName` and `displayName -> type`). This function
 * detects those pairs and extracts a single user-facing display name.
 *
 * Selection logic:
 * - Entries with empty keys, `"all"`, or already processed pairs are ignored.
 * - If one of the pair contains underscores, the value without underscores
 *   is used as the display name.
 * - Otherwise, the entry containing at least one capital letter is used.
 *
 * Optionally prepends `"Alle Kategorien"` to the list.
 *
 * @param {boolean} includeAll - Whether the result should include the default
 * `"Alle Kategorien"` entry at the beginning of the list.
 * @returns {string[]} An array of category display names.
 */
export function getLibraryCategories(includeAll) {
    const categories = appSettings.scoreCategories || {};
    const displayNames = [];
    const processedKeys = new Set();

    Object.keys(categories).forEach((key) => {
        // Skip default entries and already processed keys
        if (key === "" || key === "all" || categories[key] === "all" || processedKeys.has(key)) return;

        const value = categories[key];
        // If this key maps to a value that maps back to this key, we have a bidirectional pair
        if (categories[value] === key) {
            let displayName;

            // Check if either key or value has underscores
            if (key.includes("_") || value.includes("_")) {
                // Take the one without underscores as the display name
                displayName = key.includes("_") ? value : key;
            } else {
                // Neither has underscores, take the one with at least one capital letter
                const keyHasCapital = /[A-Z]/.test(key);
                displayName = keyHasCapital ? key : value;
            }

            displayNames.push(displayName);
            processedKeys.add(key);
            processedKeys.add(value);
        }
    });

    return includeAll ? ["Alle Kategorien", ...displayNames] : displayNames;
}

/**
 * Counts how many library entries belong to a specific category.
 *
 * The function resolves the internal category type using
 * `appSettings.scoreCategories` and iterates over all entries
 * in `libraryStore.raw` to determine how many scores match it.
 *
 * @param {string} category - The display name of the category.
 * @returns {number} The number of scores assigned to the category.
 */
export function getCategoryCount(category) {
    const categoriesMap = appSettings.scoreCategories || {};
    const categoryType = categoriesMap[category];
    const items = libraryStore.raw;

    let count = 0;
    for (const item in items) {
        if (items[item].type === categoryType) count++;
    }

    return count;
}

const pendingChecks = new Map();

/**
 * Checks whether a score with the given ID exists in the system.
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
 *   - The API responds with HTTP 404 (score does not exist)
 * - Returns `true` if:
 *   - The score exists (any non-404 successful response)
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
 * @function scoreExists
 * @param {string} id - ID of the score to check
 * @returns {Promise<boolean>} Whether the score exists or should be treated as existing
 */
export async function scoreExists(id) {
    if (!id) return false;

    if (pendingChecks.has(id)) return await pendingChecks.get(id);

    isFetching.checkScore = true;

    const request = (async () => {
        try {
            const { resp } = await apiCheckScore(id);
            const normalized = normalizeResponse(resp);

            if (normalized.status === 404) return false;

            if (handleGenericErrors(normalized)) return true;

            return true;
        } catch (e) {
            return true;
        } finally {
            pendingChecks.delete(id);
            if (pendingChecks.size === 0) {
                isFetching.checkScore = false;
            }
        }
    })();

    pendingChecks.set(id, request);
    return await request;
}

/**
 * Deletes a score from the library.
 *
 * Sends a delete request to the backend and handles the response.
 * Depending on the returned error type, different user-facing
 * notifications are displayed.
 *
 * Supported error cases:
 * - `NOTFOUND` → The score no longer exists.
 * - `BADREQUEST` → Invalid input data was provided.
 * - Other errors → Generic deletion failure.
 *
 * A success toast is displayed when the score is deleted successfully.
 *
 * @param {string} id - The unique identifier of the score to delete.
 * @returns {Promise<void>}
 */
export async function deleteScore(id) {
    if (isFetching.deleteScore) return;
    isFetching.deleteScore = true;

    try {
        const { resp } = await apiDeleteScore(id);

        const normalizedResponse = normalizeResponse(resp);
        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "Eintrag gelöscht",
            subTitle: viewport.isMobile ? "" : "Der Eintrag wurde erfolgreich gelöscht.",
            type: "success"
        });
    } finally {
        isFetching.deleteScore = false;
    }
}

/**
 * Downloads all score files for a given library entry as a ZIP archive.
 *
 * Features:
 * - Prevents duplicate requests using `isFetching.downloadScore`
 * - Handles global API errors centrally
 * - Handles domain-specific download errors
 * - Triggers a browser download on success
 * - Displays success/error toasts
 *
 * @param {string} id - ID of the score/library entry to download
 * @returns {Promise<void>}
 */
export async function downloadScoreFiles(id) {
    if (isFetching.downloadScore) return;
    isFetching.downloadScore = true;

    try {
        const { resp, body } = await apiDownloadScoreFiles(id);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        const scoreName = libraryStore.raw.find(s => s.id === id)?.title ?? "Noten";
        triggerFileDownload(body, scoreName);

        addToast({
            title: "Download erfolgreich",
            subTitle: viewport.isMobile ? "" : "Die Noten wurden erfolgreich heruntergeladen.",
            type: "success"
        });
    } finally {
        isFetching.downloadScore = false;
    }
}

/**
 * Creates a new score entry in the library, including file uploads.
 *
 * Features:
 * - Prevents duplicate requests using `isFetching.newScore`
 * - Prepares multipart FormData (JSON + files)
 * - Handles global API errors
 * - Handles domain-specific validation errors
 * - Displays success/error toasts
 *
 * @param {Object} score - Score data including metadata and files
 * @param {File[]} score.files - Files to upload
 * @returns {Promise<void>}
 */
export async function addScore(score) {
    if (isFetching.newScore) return;
    isFetching.newScore = true;

    try {
        const formData = prepareScoreFormData(score);
        score.files.forEach(file => formData.append("files", file, file.name));

        const { resp } = await apiAddScore(formData);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "Eintrag hinzugefügt",
            subTitle: viewport.isMobile ? "" : "Der neue Eintrag wurde erfolgreich zur Notenbibliothek hinzugefügt.",
            type: "success"
        });
    } finally {
        isFetching.newScore = false;
    }
}

/**
 * Updates an existing score entry, including file changes.
 *
 * Handles:
 * - New file uploads
 * - Retaining existing files
 * - Detecting and sending removed files
 *
 * Features:
 * - Prevents duplicate requests using `isFetching.updateScore`
 * - Prepares multipart FormData (JSON + file diffs)
 * - Handles global API errors
 * - Handles domain-specific validation errors
 * - Displays success/error toasts
 *
 * @param {Object} score - Score data including metadata and file state
 * @param {(File|string)[]} score.files - Mixed array of new files (File) and existing file names (string)
 * @param {string[]} score.originalFiles - Original file names before modification
 *
 * @returns {Promise<void>}
 */
export async function updateScore(score) {
    if (isFetching.updateScore) return;
    isFetching.updateScore = true;

    try {
        const formData = prepareScoreFormData(score);

        const newFiles = score.files.filter(f => f instanceof File);
        const existingNames = score.files.filter(f => typeof f === "string");
        const removedFiles = score.originalFiles.filter(f => !existingNames.includes(f));

        formData.append("removedFiles", removedFiles);
        newFiles.forEach(f => formData.append("files", f, f.name));

        const { resp, body } = await apiUpdateScore(formData);
        const normalizedResponse = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResponse)) return;

        const index = libraryStore.raw.findIndex(s => s.id === score.id);
        if (index !== -1) {
            libraryStore.raw[index] = {
                ...libraryStore.raw[index],
                ...score,
                rev: body.rev
            };
        }

        addToast({
            title: "Änderungen gespeichert",
            subTitle: "Ihre Änderungen wurden erfolgreich gespeichert.",
            type: "success"
        });
    } finally {
        isFetching.updateScore = false;
    }
}

/**
 * Prepares a FormData object containing the core score metadata as JSON.
 *
 * The metadata is serialized into a Blob and appended under "scoreData",
 * allowing it to be sent alongside file uploads in a multipart request.
 *
 * Note:
 * - `id` may be undefined for new scores (valid for create operations)
 *
 * @param {Object} score - Score metadata
 * @returns {FormData} FormData object containing serialized score data
 */
function prepareScoreFormData(score) {
    const formData = new FormData();
    let rev = null;

    if (score.id) rev = libraryStore.raw.find(s => s.id === score.id).rev;

    const scoreData = {
        id: score.id, // Will be undefined for new scores, which is fine
        scoreId: score.scoreId,
        title: score.title,
        artist: score.artist,
        type: score.type,
        voices: score.voices,
        voiceCount: score.voiceCount,
        rev: rev
    };

    formData.append("scoreData", new Blob([JSON.stringify(scoreData)], {
        type: "application/json"
    }));

    return formData;
}