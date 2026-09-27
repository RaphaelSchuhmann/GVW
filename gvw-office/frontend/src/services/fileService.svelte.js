import { normalizeResponse } from "../api/http.svelte.js";
import { apiLoadFile } from "../api/apiFiles.svelte.js";
import { handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";
import { addToast } from "../stores/toasts.svelte.js";

export const supportedFileTypes = new Set(["pdf", "png", "jpg", "jpeg", "gif", "mp3", "wav", "midi", "mid", "xml", "musicxml", "mxl", "mscz", "mscx", "sib", "musx", "cap", "capx", "gp", "gp5", "gp3", "gp4", "gpx"]);

export const supportedPreviewTypes = new Set(["pdf", "png", "jpg", "jpeg", "gif", "mp3", "wav", "gp", "gp5", "gp3", "gp4", "gpx"]);

export const previewTypesMap = {
    "pdf": "PDF",
    "png": "img",
    "jpg": "img",
    "jpeg": "img",
    "gif": "img",
    "mp3": "audio",
    "wav": "audio",
    "gp": "at", // at -> alphaTab
    "gp5": "at",
    "gp3": "at",
    "gp4": "at",
    "gpx": "at"
};

export async function filePreviewable(service, documentId, fileId) {
    const { resp, body } = await apiLoadFile(service, documentId, fileId);
    const normalized = normalizeResponse(resp);

    if (handleGlobalApiError(normalized)) return;

    const blob = new Blob([body]);

    if (blob.size <= 0) {
        addToast({
            title: "Unerwarteter Fehler",
            subTitle: !isMobile ? "Die empfangenen Daten sind unvollständig. Bitte versuchen Sie es erneut." : "",
            type: "error"
        });
        return;
    }

    const contentDisposition = resp.headers.get("Content-Disposition");

    const filename = extractFileNameFromContentDisposition(contentDisposition);
    const dotIndex = filename.lastIndexOf(".");
    const extension = dotIndex === -1 ? "" : filename.substring(dotIndex + 1);

    return {
        isPreviewable: Boolean(extension) && supportedPreviewTypes.has(extension),
        extension: extension,
        filename: filename,
        blob: blob
    };
}

function extractFileNameFromContentDisposition(contentDisposition) {
    if (!contentDisposition) return "";

    const match = contentDisposition.match(/filename="(.*?)"/);
    return match && match.length > 0 ? match[1] : "";
}