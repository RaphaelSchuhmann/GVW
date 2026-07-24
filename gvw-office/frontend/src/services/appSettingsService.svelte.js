import { appSettings } from "../stores/appSettings.svelte.js";
import { apiAddCategory, apiRemoveCategory, apiUpdateMaxMembers } from "../api/apiAppSettings.svelte";
import { normalizeResponse } from "../api/http.svelte";
import { handleGlobalApiError } from "../api/globalErrorHandler.svelte";
import { addToast } from "../stores/toasts.svelte";
import { viewport } from "../stores/viewport.svelte";
import { loadAppSettings } from "./appSettingsSyncService.svelte";
import { getCategoryCount } from "./libraryService.svelte";

/**
 * Updates the maximum members app settings on the server and
 * synchronizes the local `appSettings` store on success.
 *
 * This function handles all API error cases internally via the
 * global error handler and user-facing toasts. It does not return
 * the updated value and is intended to be called directly from
 * UI or service layers.
 *
 * @param {number} value - The new maximum number of members per voice.
 * @returns {Promise<void>}
 */
export async function tryUpdateMaxMembers(value) {
    const { resp, body } = await apiUpdateMaxMembers(value, appSettings.rev);

    const normalizedResponse = normalizeResponse(resp);
    if (handleGlobalApiError(normalizedResponse)) return;

    Object.assign(appSettings, { maxMembers: value, rev: body.rev });

    addToast({
        title: "Erfolgreich gespeichert",
        subTitle: viewport.isMobile
            ? ""
            : "Die maximale Anzahl an Mitgliedern pro Stimme wurde erfolgreich aktualisiert und gespeichert.",
        type: "success"
    });
}

/**
 * Creates a new score category and persists it via the backend API.
 *
 * The provided category name is transformed into two representations:
 * - `displayValue` → The user-facing label shown in the UI.
 * - `type` → A normalized identifier used internally and by the backend
 *   (lowercase with spaces replaced by underscores).
 *
 * Validation rules:
 * - The display name must contain at least one uppercase letter or space.
 * - Underscores are not allowed in the display name.
 *
 * On success:
 * - A success toast is shown.
 * - Application settings are reloaded to refresh the category list.
 *
 * On failure:
 * - Appropriate error or warning toasts are displayed depending on
 *   validation results or backend response.
 *
 * @param {string} category - The user-provided name of the category.
 * @returns {Promise<boolean>} Resolves to `true` if the category was created successfully, otherwise `false`.
 */
export async function addScoreCategory(category) {
    const displayValue = category;
    const type = category.toLowerCase().replaceAll(" ", "_");

    if (type === displayValue || displayValue.includes("_")) {
        addToast({
            title: "Ungültige Eingabe",
            subTitle: viewport.isMobile ? "" : "Der Name muss mindestens einen Großbuchstaben oder ein Leerzeichen enthalten und darf keine Unterstriche verwenden.",
            type: "warning"
        });
        return false;
    }

    const { resp } = await apiAddCategory(type, displayValue);

    const normalizedResponse = normalizeResponse(resp);
    if (handleGlobalApiError(normalizedResponse)) return false;

    addToast({
        title: "Kategorie hinzugefügt",
        subTitle: viewport.isMobile ? "" : `Die Kategorie "${displayValue}" wurde erfolgreich zur Liste hinzugefügt.`,
        type: "success"
    });

    await loadAppSettings();

    return true;
}

/**
 * Deletes an existing score category if it is not currently in use.
 *
 * The function checks whether any songs are still assigned to the
 * given category before attempting deletion. If the category contains
 * songs, deletion is blocked and the user is notified via a toast.
 *
 * When deletion proceeds:
 * - The internal category `type` is resolved from application settings.
 * - A backend request is made to remove the category.
 * - A success toast is displayed upon completion.
 * - Application settings are reloaded to refresh the category list.
 *
 * If the API request fails, an error toast is displayed.
 *
 * @param {string} category - The display name of the category to remove.
 * @returns {Promise<void>}
 */
export async function deleteScoreCategory(category) {
    const categoryMap = appSettings.scoreCategories || {};
    const type = categoryMap[category];
    const count = getCategoryCount(category);

    if (count > 0) {
        addToast({
            title: "Kategorie kann nicht gelöscht werden",
            subTitle: viewport.isMobile ? "" : "Diese Kategorie enthält noch mindestens ein Lied. Bitte entfernen oder verschieben Sie die Lieder zuerst.",
            type: "error"
        });
        return;
    }

    if (type) {
        const { resp } = await apiRemoveCategory(type);

        const normalizedResponse = normalizeResponse(resp);
        if (handleGlobalApiError(normalizedResponse)) return;

        addToast({
            title: "Kategorie gelöscht",
            subTitle: viewport.isMobile ? "" : `Die Kategorie "${category}" wurde erfolgreich aus der Liste entfernt.`,
            type: "success"
        });

        await loadAppSettings();
    }
}