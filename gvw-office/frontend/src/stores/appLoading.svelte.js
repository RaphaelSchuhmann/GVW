import { user } from "./user.svelte.js";

/**
 * Tracks whether the required application fonts have finished loading.
 *
 * @type {boolean}
 */
let fontsLoaded = $state(false);

/**
 * Initializes the application font loader.
 *
 * Waits for the Material Symbols Rounded font to become available and
 * ensures that all document fonts have finished loading before marking
 * the font loading process as complete.
 *
 * This function does nothing when executed in a non-browser environment.
 *
 * @async
 * @returns {Promise<void>}
 */
export async function initFontLoader() {
    if (typeof document === "undefined") return;

    try {
        await document.fonts.load('1em "Material Symbols Rounded"');
        await document.fonts.ready;
    } finally {
        fontsLoaded = true;
    }
}

/**
 * Checks whether the application has completed its required initialization steps.
 *
 * The application is considered ready when:
 * - A user has been successfully loaded.
 * - The loaded user has a valid name.
 * - Required application fonts have finished loading.
 *
 * @returns {boolean} Whether the application is ready to be displayed.
 */
export function isAppReady() {
    const userReady = user.loaded && user.name.length > 0;
    return userReady && fontsLoaded;
}