import {
    apiAddReport,
    apiCheckReport,
    apiDeleteReport, apiDownloadReportAttachments,
    apiGetReport, apiUpdateReport,
    apiUpdateReportDescription, apiUploadReportAttachments
} from "../api/apiReports.svelte.js";
import { normalizeResponse } from "../api/http.svelte.js";
import { handleGenericErrors, handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";
import { addToast } from "../stores/toasts.svelte.js";
import { viewport } from "../stores/viewport.svelte.js";
import { sanitize, triggerFileDownload } from "./utils.js";
import { reportsStore } from "../stores/report.svelte.js";
import { pendingImages } from "./textEditorService.svelte.js";

export const reportTypeMap = {
    "Jahresbericht": "annualReport",
    "Protokoll": "protocol",
    "Versammlungsbericht": "gatheringReport",
    "Sonstigerbericht": "other",
    "annualReport": "Jahresbericht",
    "protocol": "Protokoll",
    "gatheringReport": "Versammlungsbericht",
    "other": "Sonstigerbericht"
};

export const reportTypeFilterMap = {
    "": "all",
    "Alle Typen": "all",
    "Jahresbericht": "annualReport",
    "Protokoll": "protocol",
    "Versammlungsbericht": "gatheringReport",
    "Sonstigerbericht": "other",
    "all": "Alle Typen",
    "annualReport": "Jahresbericht",
    "protocol": "Protokoll",
    "gatheringReport": "Versammlungsbericht",
    "other": "Sonstigerbericht"
};

let isFetching = {
    add: false,
    delete: false,
    check: false,
    getReport: false,
    updateDescription: false,
    uploadAttachment: false,
    downloadAttachments: false,
    updateReport: false,
};

const pendingChecks = new Map();

/**
 * Wraps matches in <mark> tags
 * @param {string} text - The full text to search in
 * @param {string} term - The word to highlight
 */
export function highlight(text, term) {
    let safeText = sanitize(text);

    if (!term || term.length < 2) return safeText;

    const safeTerm = sanitize(term.trim());

    const escapedTerm = safeTerm.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const regex = new RegExp(`(${escapedTerm})`, "gi");

    return safeText.replace(regex, "<mark class=\"highlight\">$1</mark>");
}

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
 * @function reportExists
 * @param {string} id - ID of the event to check
 * @returns {Promise<boolean>} Whether the event exists or should be treated as existing
 */
export async function reportExists(id) {
    if (!id) return false;

    if (pendingChecks.has(id)) return await pendingChecks.get(id);

    isFetching.check = true;

    const request = (async () => {
        try {
            const { resp } = await apiCheckReport(id);
            const normalized = normalizeResponse(resp);

            if (normalized.status === 404) return false;

            if (handleGenericErrors(normalized)) return true;

            return true;
        } catch (e) {
            return true;
        } finally {
            pendingChecks.delete(id);
            if (pendingChecks.size === 0) {
                isFetching.check = false;
            }
        }
    })();

    pendingChecks.set(id, request);
    return await request;
}

/**
 * Retrieves a report by its identifier.
 *
 * The request is executed through the API layer and all global
 * API errors are handled automatically.
 *
 * @param {string} id - Unique report identifier.
 *
 * @returns {Promise<Object|null>}
 * The report data or `null` if the request failed.
 */
export async function getReport(id) {
    if (!id) return null;
    isFetching.getReport = true;

    try {
        const { resp, body } = await apiGetReport(id);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return null;

        return body;
    } finally {
        isFetching.getReport = false;
    }
}

/**
 * Creates a new report.
 *
 * Concurrent create operations are prevented while a request
 * is already in progress. A success toast is displayed when
 * the report was created successfully.
 *
 * @param {Object} report - Report creation payload.
 *
 * @returns {Promise<void>}
 */
export async function addReport(report) {
    if (isFetching.add) return;
    isFetching.add = true;

    try {
        const { resp } = await apiAddReport(report);
        const normalizedResp = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResp)) return;

        addToast({
            title: "Bericht erstellt",
            subTitle: viewport.isMobile ? "" : "Der Bricht wurde erfolgreich erstellt.",
            type: "success"
        });
    } finally {
        isFetching.add = false;
    }
}

/**
 * Deletes an existing report.
 *
 * Concurrent delete operations are prevented while a request
 * is already in progress. A success toast is displayed when
 * the report was deleted successfully.
 *
 * @param {string} reportId - Unique report identifier.
 *
 * @returns {Promise<void>}
 */
export async function deleteReport(reportId) {
    if (isFetching.delete) return;
    isFetching.delete = true;

    try {
        const { resp } = await apiDeleteReport(reportId);
        const normalizedResp = normalizeResponse(resp);

        if (handleGlobalApiError(normalizedResp)) return;

        addToast({
            title: "Bericht gelöscht",
            subTitle: viewport.isMobile ? "" : "Der Bericht wurde erfolgreich gelöscht.",
            type: "success"
        });
    } finally {
        isFetching.delete = false;
    }
}

/**
 * Updates the description of an existing report.
 *
 * Uses optimistic locking via the report revision number.
 * Validation and API errors are reported to the user through
 * toast notifications.
 *
 * @param {string} reportId - Unique report identifier.
 * @param {number} rev - Current report revision.
 * @param {string} description - New report description.
 *
 * @returns {Promise<{success: boolean, rev: number|null}>}
 * Whether the update succeeded and the latest revision number.
 */
export async function updateDescription(reportId, rev, description) {
    if (isFetching.updateDescription || !reportId || !rev || !description) return { success: false, rev: null };
    isFetching.updateDescription = true;

    try {
        const { resp, body } = await apiUpdateReportDescription(reportId, rev, description);
        const normalizedResp = normalizeResponse(resp);

        if (!normalizedResp.ok) {
            // NOTE: SonarQube issues can be ignored as error handling will be
            //       refactored in the next task!
            if (normalizedResp.errorType === "BADREQUEST") {
                addToast({
                    title: "Ungültige Beschreibung",
                    subTitle: "Die angegebene Beschreibung ist ungültig. Bitte versuchen Sie es später erneut.",
                    type: "error"
                });
            } else if (normalizedResp.errorType === "NOTFOUND") {
                addToast({
                    title: "Bericht nicht gefunden",
                    subTitle: "Der angegebene Bericht konnte nicht gefunden werden. Bitte versuchen Sie es später erneut.",
                    type: "error"
                });
            } else {
                addToast({
                    title: "Fehler beim Speichern",
                    subTitle: "Beim Speichern des Berichts ist ein Fehler aufgetreten. Bitte versuchen Sie es später erneut.",
                    type: "error"
                });
            }
        }

        return { success: true, rev: body.rev };
    } finally {
        isFetching.updateDescription = false;
    }
}

/**
 * Updates the attachment set of an existing report.
 *
 * Existing attachment identifiers are preserved while newly
 * added File instances are uploaded as multipart data.
 * The operation uses optimistic locking via the report revision.
 *
 * @param {string} reportId - Unique report identifier.
 * @param {number} rev - Current report revision.
 * @param {(string|File)[]} attachments - Attachment identifiers and/or new files.
 *
 * @returns {Promise<{success: boolean, rev: number|null}>}
 * Whether the update succeeded and the latest revision number.
 */
export async function updateAttachments(reportId, rev, attachments) {
    if (!reportId || !rev || !attachments || isFetching.uploadAttachment) return { success: false, rev: null };

    isFetching.uploadAttachment = true;

    try {
        const formData = new FormData();

        const attachmentIds = [];

        for (const item of attachments) {
            if (item instanceof File) {
                formData.append("files", item, item.name);
            } else {
                attachmentIds.push(item);
            }
        }

        formData.append("metadata",
            new Blob([
                JSON.stringify({
                    rev: rev,
                    attachments: attachmentIds
                })
            ], {
                type: "application/json"
            })
        );

        const { resp, body } = await apiUploadReportAttachments(formData, reportId);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return { success: false, rev: null };

        if (!body?.rev) {
            handleGenericErrors({ ok: false, errorType: "SERVER", message: "0000500" });
            return { success: false, rev: null };
        }

        return { success: true, rev: body.rev };
    } catch (e) {
        return { success: false, rev: null };
    } finally {
        isFetching.uploadAttachment = false;
    }
}

/**
 * Downloads all attachments associated with a report.
 *
 * If the report has no attachments, an informational toast is
 * displayed. Successful downloads are automatically triggered
 * in the browser and acknowledged with a success toast.
 *
 * @param {string} reportId - Unique report identifier.
 *
 * @returns {Promise<void>}
 */
export async function downloadAttachments(reportId) {
    if (!reportId || isFetching.downloadAttachments) return;

    isFetching.downloadAttachments = true;

    try {
        const { resp, body } = await apiDownloadReportAttachments(reportId);
        const normalized = normalizeResponse(resp);

        if (resp.status === 204) {
            addToast({
                title: "Keine Anhänge",
                subTitle: viewport.isMobile ? "" : "Dieser Bericht besitzt keine Anhänge.",
                type: "info"
            });
            return;
        }

        if (handleGlobalApiError(normalized)) return;

        const reportName = reportsStore.raw.find(i => i.id === reportId).title || "Bericht";
        triggerFileDownload(body, `${reportName} - Anhänge`);

        addToast({
            title: "Download erfolgreich",
            subTitle: viewport.isMobile ? "" : "Die Anhänge wurden erfolgreich heruntergeladen.",
            type: "success"
        });
    } finally {
        isFetching.downloadAttachments = false;
    }
}

export async function updateReport(reportData) {
    if (isFetching.updateReport) return reportData.rev || "";

    isFetching.updateReport = true;

    try {
        const formData = new FormData();

        formData.append("reportData", new Blob([
            JSON.stringify({
                id: reportData.id,
                rev: reportData.rev,
                title: reportData.title,
                editor: reportData.editor,
                content: reportData.content,
            })
        ], {
            type: "application/json"
        }));

        const images = pendingImages.values();

        for (const item of images) {
            formData.append("files", item, item.name);
        }

        const { resp, body } = await apiUpdateReport(formData);
        const normalized = normalizeResponse(resp);

        if (handleGlobalApiError(normalized)) return reportData.rev;
        if (!body.rev || body.rev === "") return reportData.rev;

        addToast({
            title: "Bericht aktualisiert",
            subTitle: viewport.isMobile ? "" : "Der Bericht wurde erfolgreich aktualisiert.",
            type: "success"
        });

        return body.rev;
    } finally {
        isFetching.updateReport = false;
    }
}