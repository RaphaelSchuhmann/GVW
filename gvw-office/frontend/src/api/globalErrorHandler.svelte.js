import { addToast, toasts } from "../stores/toasts.svelte";
import { viewport } from "../stores/viewport.svelte";
import { logout } from "../services/userService.svelte.js";
import { push } from "svelte-spa-router";

const DOMAINS = {
    "10": { default: "Authentifizierung" },
    "20": {
        default: "Systemeinstellungen",
        "001": "Hilfe-Center Kategorie",
        "003": "Genre"
    },
    "30": { default: "Benutzer" },
    "40": { default: "Dashboarddaten" },
    "50": { default: "Mitglied" },
    "60": { default: "Veranstaltung" },
    "70": { default: "Bericht" },
    "80": { default: "Noteneintrag", },
    "90": { default: "Feedback" },
    "11": { default: "Fehlerbericht" },
    "12": { default: "Changelog" },
    "13": { default: "Datei" },
    "14": {
        default: "Dokument",
        "004": "Dokumentinhalt"
    },
    "15": {
        default: "Artikel",
        "001": "Hilfe-Center Kategorie"
    }
};

const ACTIONS = {
    "01": "Laden",
    "02": "Abrufen",
    "03": "Hinzufügen",
    "04": "Speichern",
    "05": "Löschen",
    "06": "Synchronisierung",
    "07": "Login",
    "99": "Verarbeitung"
};

const STATUS_TEMPLATES = {
    "400": {
        title: "{action} fehlgeschlagen",
        subTitle: "{domain} - Fehlerhafte oder unvollständige Daten."
    },
    "404": {
        title: "{domain} nicht gefunden",
        subTitle: "Die gesuchte Ressource existiert nicht oder wurde gelöscht."
    },
    "409": {
        title: "Datenkonflikt",
        subTitle: "{domain} - Die Daten sind nicht aktuell oder bereits vergeben."
    }
};

const INFRA_ERRORS = {
    "NETWORK": {
        typeId: "3",
        title: "Keine Verbindung zum Server",
        subTitle: "Es konnte keine Verbindung zum Server aufgebaut werden. Bitte versuchen Sie es später erneut."
    },
    "SERVER": {
        typeId: "4",
        title: "Interner Serverfehler",
        subTitle: "Beim Verarbeiten Ihrer Anfrage ist ein Fehler aufgetreten."
    },
    "BAD_INPUT": {
        typeId: "5",
        title: "Eingabe ungültig",
        subTitle: "Die übermittelten Daten sind fehlerhaft oder unvollständig."
    }
};

/**
 * Handles all application-wide API errors.
 *
 * The function first checks for infrastructure and authentication related
 * errors via {@link handleGenericErrors}. If none match, it attempts to
 * generate a user-friendly error message from the backend error code.
 *
 * Error code format:
 * - DD AA SSS
 * - DD  = domain
 * - AA  = action
 * - SSS = status
 *
 * Returns `true` when a toast was shown or another global action was
 * performed and no further error handling should occur.
 *
 * Returns `false` when the response does not represent an error.
 *
 * @param {Object} result - Normalized API response.
 * @param {boolean} result.ok - Whether the request completed successfully.
 * @param {string} [result.errorType] - Normalized error category.
 * @param {string} [result.message] - Backend error code.
 *
 * @returns {boolean} Whether the error was handled globally.
 */
export function handleGlobalApiError(result) {
    if (result.ok) return false;

    const codeStr = result.message || "0000500";

    const domain = codeStr.substring(0, 2);
    const action = codeStr.substring(2, 4);
    const status = codeStr.substring(4, 7);
    const resource = codeStr.substring(7);

    // Handle generic errors like network or 500 or 401
    if (handleGenericErrors(result)) return true;

    const domainConfig = DOMAINS[domain];
    let domainWord = "System";
    if (domainConfig) {
        domainWord = domainConfig[resource] || domainConfig.default;
    }

    const actionWord = ACTIONS[action] || "Verarbeitung";
    const template = STATUS_TEMPLATES[status];

    let title = "Fehler aufgetreten";
    let subTitle = `Ein Fehler mit dem Code ${codeStr} ist aufgetreten`;

    if (template) {
        title = template.title.replace("{action}", actionWord).replace("{domain}", domainWord);
        subTitle = template.subTitle.replace("{domain}", domainWord);
    }

    addToast({
        title: title,
        subTitle: viewport.isMobile ? "" : subTitle,
        type: "error"
    });

    return true;
}

/**
 * Handles infrastructure-level and authentication-related API errors.
 *
 * This includes:
 * - Authentication failures
 * - Network connectivity issues
 * - Internal server errors
 * - Generic bad input errors
 *
 * If a matching error is found, an appropriate toast is displayed and
 * the function returns `true`.
 *
 * @param {Object} result - Normalized API response.
 * @param {boolean} result.ok - Whether the request completed successfully.
 * @param {string} [result.errorType] - Normalized error category.
 * @param {string} [result.message] - Backend error code.
 *
 * @returns {boolean} Whether the error was handled.
 */
export function handleGenericErrors(result) {
    if (result.ok) return false;

    const codeStr = result.message || "0000500";
    const domain = codeStr.substring(0, 2);
    const action = codeStr.substring(2, 4);

    if (handleAuthFailures(result, domain, action)) return true;

    if (codeStr === "0000000" || result.errorType === "NETWORK") {
        return dispatchInfraToast(INFRA_ERRORS.NETWORK);
    }

    if (codeStr === "0000500" || result.errorType === "SERVER") {
        return dispatchInfraToast(INFRA_ERRORS.SERVER);
    }

    if (codeStr === "0000400") {
        return dispatchInfraToast(INFRA_ERRORS.BAD_INPUT);
    }

    return false;
}

/**
 * Checks whether a toast with the given type identifier is already active.
 *
 * Used to prevent duplicate infrastructure and authentication messages
 * from being displayed simultaneously.
 *
 * @param {string} typeId - Unique toast type identifier.
 *
 * @private
 * @returns {boolean} `true` if a matching toast already exists.
 */
function toastAlreadyExists(typeId) {
    const toastsWithTypeId = toasts.filter(t => t.typeId === typeId);
    return toastsWithTypeId.length > 0;
}

/**
 * Displays an infrastructure-related error toast if one is not already shown.
 *
 * The provided configuration object is typically sourced from
 * {@link INFRA_ERRORS}.
 *
 * Duplicate toasts are suppressed based on the configuration's `typeId`.
 *
 * @param {Object} config - Infrastructure error configuration.
 * @param {string} config.typeId - Unique toast identifier.
 * @param {string} config.title - Toast title.
 * @param {string} config.subTitle - Toast subtitle.
 *
 * @private
 * @returns {boolean} Always returns `true`.
 */
function dispatchInfraToast(config) {
    if (toastAlreadyExists(config.typeId)) return true;

    addToast({
        title: config.title,
        subTitle: viewport.isMobile ? "" : config.subTitle,
        type: "error",
        typeId: config.typeId
    });
    return true;
}

/**
 * Handles authentication and authorization failures.
 *
 * Special cases:
 * - Login failures (invalid email/password)
 * - Expired or invalid authentication tokens
 *
 * Session expiration handling automatically:
 * - Displays a toast notification
 * - Logs the user out
 * - Redirects to the login page
 *
 * @param {Object} result - Normalized API response.
 * @param {string} result.errorType - Error category.
 * @param {string} domain - Parsed domain code from the backend error code.
 * @param {string} action - Parsed action code from the backend error code.
 *
 * @private
 * @returns {boolean} Whether an authentication-related error was handled.
 */
function handleAuthFailures(result, domain, action) {
    if (result.errorType !== "UNAUTHORIZED") return false;

    // Explicit Login Failure
    if (domain === "10" && action === "07") {
        addToast({
            title: "E-Mail oder Passwort falsch",
            subTitle: viewport.isMobile ? "" : "Die E-Mail-Adresse oder das Passwort ist ungültig.",
            type: "error",
            typeId: "1"
        });
        return true;
    }

    // General Session Expiration
    if (toastAlreadyExists("2")) return true;

    addToast({
        title: "Ungültiger Token",
        subTitle: viewport.isMobile ? "" : "Ihr Authentifizierungstoken ist abgelaufen. Bitte melden Sie sich erneut an.",
        type: "error",
        typeId: "2"
    });
    logout();
    push("/");
    return true;
}