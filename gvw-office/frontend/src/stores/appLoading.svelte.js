import { user } from "./user.svelte.js";

let fontsLoaded = $state(false);

export async function initFontLoader() {
    if (typeof document === "undefined") return;

    try {
        await document.fonts.load('1em "Material Symbols Rounded"');
        await document.fonts.ready;
    } catch (err) {
        console.warn("Font loading check timed out or failed: ", err);
    } finally {
        fontsLoaded = true;
    }
}

export function isAppReady() {
    const userReady = user.loaded && user.name.length > 0;
    return userReady && fontsLoaded;
}