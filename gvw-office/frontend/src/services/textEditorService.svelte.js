import { normalizeResponse } from "../api/http.svelte.js";
import { apiGetDocumentImage, apiResolveURl } from "../api/apiTextEditor.svelte.js";
import { editorMetadataStore } from "../stores/textEditorStore.svelte.js";
import { handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";
import { renameFile, sanitize } from "./utils.js";

/**
 * Set of valid block types supported by the editor.
 *
 * Used to validate block creation and type switching operations.
 *
 * @type {Set<string>}
 */
const blockTypes = new Set(["text", "image", "file", "blockquote", "h1", "h2", "h3", "h4"]);

/**
 * Temporary in-memory registry for image files that are not yet uploaded.
 *
 * Key: temporary image ID (temp_* UUID filename)
 * Value: File object
 *
 * Used during editing before server persistence.
 *
 * @type {Map<string, File>}
 */
export const pendingImages = new Map();

/**
 * In-memory cache of preview URLs for locally created or uploaded images.
 *
 * Key: image ID (temp or persisted)
 * Value: object URL created via URL.createObjectURL
 *
 * Used to render images before or without server availability.
 *
 * @type {Map<string, string>}
 */
export const previewUrls = new Map();

/**
 * Inserts a new block into the document structure.
 *
 * Creates a block with a generated UUID and inserts it either:
 * - at the end of the list
 * - or relative to a given index
 *
 * Validates block type against `blockTypes`.
 *
 * @param {Array<Object>} items - Current block array
 * @param {number} index - Reference index for insertion
 * @param {string} content - Initial block content (HTML or data payload)
 * @param {string} type - Block type (must exist in blockTypes)
 * @param {boolean} [insertAtIndex=false] - Whether to insert at exact index
 * @returns {string|undefined} Generated block ID if successful
 */
export function addBlock(items, index, content, type, insertAtIndex = false) {
    if (index < 0 && items.length > 0 || index === -1) return;

    if (!blockTypes.has(type)) return;

    const id = crypto.randomUUID();

    const block = {
        id: id,
        type: type,
        data: content,
    };

    if (items.length === 0) {
        items.push(block);
        return id;
    }

    insertAtIndex ? items.splice(index, 0, block) : items.splice(index + 1, 0, block);

    return id;
}

/**
 * Updates the type of an existing block.
 *
 * Supports toggling behavior for blockquotes:
 * - If block is already "blockquote", it is reverted to "text"
 *
 * @param {string} blockId - Target block ID
 * @param {string} type - New block type
 * @param {Array<Object>} items - Block array
 * @returns {string|undefined} Updated block ID if successful
 */
export function updateBlockType(blockId, type, items) {
    if (!type || !blockTypes.has(type) || !blockId || items.length === 0) return;

    const index = items.findIndex(i => i.id === blockId);
    if (index === -1) return;

    // If the type is "blockquote" and the current type of the block
    // already is block quote it should just toggle back to "text"
    // as some users might expect that to happen.
    if (type === "blockquote" && items[index].type === type) {
        items[index].type = "text";
        return blockId;
    }

    items[index].type = type;
    return blockId;
}

/**
 * Deletes a block from the editor state.
 *
 * If the block represents an image, associated preview and pending
 * image references are also removed from in-memory caches.
 *
 * @param {Array<Object>} items - Block array
 * @param {string} blockId - ID of block to delete
 * @param {boolean} [isImage=false] - Whether block is an image block
 * @returns {void}
 */
export function deleteBlock(items, blockId, isImage = false) {
    if (!blockId) return;

    const index = items.findIndex(i => i.id === blockId);
    const block = items.find(i => i.id === blockId);
    if (!block) return;

    if (isImage) {
        const blockData = block.data;

        if (previewUrls.has(blockData)) {
            previewUrls.delete(blockId);
        }

        if (pendingImages.has(blockData)) {
            pendingImages.delete(blockId);
        }
    }

    items.splice(index, 1);
}

/**
 * Inserts a single image block into the editor.
 *
 * Creates a temporary image ID and stores:
 * - File in pendingImages
 * - Object URL in previewUrls
 *
 * The block references the temp ID until server upload replaces it.
 *
 * @param {File} file - Image file to insert
 * @param {Array<Object>} items - Block array
 * @param {number} insertAfterIndex - Index after which to insert block
 */
export function insertImageBlock(file, items, insertAfterIndex) {
    const tempId = `temp_${crypto.randomUUID()}.${file.name.split('.').pop()}`;

    const renamedFile = renameFile(file, tempId);
    pendingImages.set(tempId, renamedFile);
    previewUrls.set(tempId, URL.createObjectURL(renamedFile));

    const block = {
        id: crypto.randomUUID(),
        type: "image",
        data: tempId
    };

    if (items.length === 0) {
        items.push(block);
        return;
    }

    items.splice(insertAfterIndex + 1, 0, block);
}

/**
 * Inserts multiple image blocks in bulk.
 *
 * Each image is assigned a temporary ID and stored in:
 * - pendingImages (file reference)
 * - previewUrls (object URL for rendering)
 *
 * Blocks are inserted sequentially after the given index.
 *
 * @param {File[]} images - Array of image files
 * @param {Array<Object>} items - Block array
 * @param {number} insertAfterIndex - Insertion index
 */
export function bulkInsertImageBlocks(images, items, insertAfterIndex) {
    const newBlocks = [];

    for (const file of images) {
        const tempId = `temp_${crypto.randomUUID()}.${file.name.split('.').pop()}`;

        pendingImages.set(tempId, file);
        previewUrls.set(tempId, URL.createObjectURL(file));

        const block = {
            id: crypto.randomUUID(),
            type: "image",
            data: tempId
        };

        newBlocks.push(block);
    }
    items.splice(insertAfterIndex + 1, 0, ...newBlocks);
}

const pendingDocumentImages = new Set();

/**
 * Fetches a persisted image from the backend and returns a blob URL.
 *
 * Prevents duplicate requests using an in-memory request tracker.
 *
 * Steps:
 * - Prevents duplicate fetch per imageId
 * - Requests image blob from backend
 * - Validates response
 * - Returns object URL for rendering
 *
 * @async
 * @param {string} documentId - Parent document ID
 * @param {string} imageId - Image identifier
 * @returns {Promise<string|null>} Object URL or null if failed
 */
export async function getDocumentImage(documentId, imageId) {
    if (!documentId || !imageId) return null;

    if (pendingDocumentImages.has(imageId)) {
        return null;
    }

    pendingDocumentImages.add(imageId);

    try {
        const { resp, blob } = await apiGetDocumentImage(
            editorMetadataStore.activeFeature,
            documentId,
            imageId
        );

        const normalizedResp = normalizeResponse(resp);

        if (!normalizedResp.ok) {
            return null;
        }

        return URL.createObjectURL(blob);
    } finally {
        pendingDocumentImages.delete(imageId);
    }
}

/**
 * Applies inline text styling using browser execCommand API.
 *
 * Supports:
 * - bold (strong / b)
 * - italic (em / i)
 * - underline (u)
 *
 * @param {"strong"|"b"|"em"|"i"|"u"} action - Formatting action
 */
export function applyStyleInDOM(action) {
    if (!action) return;

    const sel = globalThis.getSelection();
    if (!sel || sel.rangeCount === 0) return;

    if (action === "strong" || action === "b") {
        document.execCommand("bold", false, null);
    } else if (action === "em" || action === "i") {
        document.execCommand("italic", false, null);
    } else if (action === "u") {
        document.execCommand("underline", false, null);
    }
}

/**
 * Detects URLs during typing and converts them into rich link elements.
 *
 * Triggered on space or enter key events.
 *
 * Behavior:
 * - Detects last word in current text node
 * - Validates URL format
 * - Resolves metadata (title + favicon)
 * - Replaces plain URL with rich link HTML element
 * - Syncs sanitized block content back to state
 *
 * @async
 * @param {KeyboardEvent} e - Keyboard event
 * @param {HTMLElement} currentBlock - Active editor block
 * @param {(html: string) => void} onDataSync - Callback for syncing content
 */
export async function handleAutoLink(e, currentBlock, onDataSync) {
    if (e.key !== " " && e.key !== "Enter") return;

    const selection = globalThis.getSelection();
    if (!selection.rangeCount) return;

    const range = selection.getRangeAt(0);
    const textNode = range.startContainer;

    if (textNode.nodeType !== Node.TEXT_NODE) return;

    const text = textNode.nodeValue;
    const cursorOffset = range.startOffset;

    const words = text.slice(0, cursorOffset).split(/\s+/);
    const lastWord = words.at(-1);

    const urlPattern = /^(https?:\/\/[^\s]+)$/i;

    if (urlPattern.test(lastWord)) {
        e.preventDefault();
        const startOffset = cursorOffset - lastWord.length;
        const replaceRange = document.createRange();
        replaceRange.setStart(textNode, startOffset);
        replaceRange.setEnd(textNode, cursorOffset);

        selection.removeAllRanges();
        selection.addRange(replaceRange);

        const completeUrl = lastWord.trim();

        const urlData = await resolveURL(completeUrl);

        const richLinkHtml = `
            <a href="${completeUrl}" target="_blank" contenteditable="false" class="inline-flex items-center gap-2 bg-gv-light-bg border border-gv-border px-3 py-1 rounded-md text-gv-toast-info select-all mx-1 my-0.5 pointer-events-auto hover:underline" data-rich-link="true">
                ${urlData.favicon === "icon" ? `<span class="material-symbols-rounded text-sm">link</span>` : `<img src="${urlData.favicon}" class="w-5 h-5 object-contain" alt=""/>`} 
                <span class="text-dt-7 font-medium">${urlData.title}</span>
            </a>`;

        document.execCommand("insertHTML", false, richLinkHtml);

        const sel = globalThis.getSelection();
        if (sel?.rangeCount) {
            const range = sel.getRangeAt(0);
            range.collapse(false);
        }

        onDataSync(sanitize(currentBlock.innerHTML));
    }
}

/**
 * Resolves metadata for a URL (title and favicon).
 *
 * Calls backend endpoint to extract metadata from the target page.
 *
 * Falls back to raw URL if resolution fails or backend errors occur.
 *
 * @async
 * @param {string} url - URL to resolve
 * @returns {Promise<{title: string, favicon: string}>}
 */
async function resolveURL(url) {
    if (!url) return { title: url, favicon: "icon" };

    const { resp, body } = await apiResolveURl(url);
    const normalized = normalizeResponse(resp);

    if (handleGlobalApiError(normalized)) return { title: url, favicon: "icon" };

    return { title: body.title || url, favicon: body.favicon || "icon" };
}

/**
 * Determines active text formatting styles within a selection range.
 *
 * Combines caret-based and range-based detection to infer whether
 * bold, italic, or underline formatting is active.
 *
 * @param {Range} range - The current selection range
 * @returns {{ isBold: boolean, isItalic: boolean, isUnderline: boolean }}
 */
export function getActiveStylesInRange(range) {
    if (range.collapsed) {
        return getStylesAtCaret(range);
    }

    const start = getStylesAtNode(range.startContainer);
    const end = getStylesAtNode(range.endContainer);

    let isBold = start.isBold || end.isBold;
    let isItalic = start.isItalic || end.isItalic;
    let isUnderline = start.isUnderline || end.isUnderline;

    let ancestor = range.commonAncestorContainer;
    let checkEl = ancestor.nodeType === Node.TEXT_NODE ? ancestor.parentElement : ancestor;

    while (checkEl) {
        if (checkEl.matches?.("strong, b")) isBold = true;
        if (checkEl.matches?.("em, i")) isItalic = true;
        if (checkEl.matches?.("u")) isUnderline = true;
        if (checkEl.getAttribute?.("contenteditable") === "true") break;
        checkEl = checkEl.parentElement;
    }

    if (ancestor instanceof HTMLElement || ancestor.nodeType === Node.TEXT_NODE) {
        const parentNode = ancestor.nodeType === Node.TEXT_NODE ? ancestor.parentElement : ancestor;

        // Grab only formatting elements inside the parent container
        const subNodes = parentNode.querySelectorAll("strong, b, em, i, u");
        subNodes.forEach(node => {
            // Check if this specific formatting node intersects the user's selection range
            if (range.intersectsNode(node)) {
                if (node.matches("strong, b")) isBold = true;
                if (node.matches("em, i")) isItalic = true;
                if (node.matches("u")) isUnderline = true;
            }
        });
    }

    return { isBold, isItalic, isUnderline };
}

/**
 * Retrieves formatting styles at the caret position.
 *
 * @param {Range} range - Selection range collapsed at caret
 * @returns {{ isBold: boolean, isItalic: boolean, isUnderline: boolean }}
 */
function getStylesAtCaret(range) {
    return getStylesAtNode(range.startContainer);
}

/**
 * Traverses DOM ancestors to detect active text formatting styles.
 *
 * Checks for <b>, <strong>, <i>, <em>, and <u> tags up the DOM tree
 * until reaching a contenteditable boundary.
 *
 * @param {Node} node - DOM node inside selection
 * @returns {{ isBold: boolean, isItalic: boolean, isUnderline: boolean }}
 */
function getStylesAtNode(node) {
    let el = node.nodeType === Node.TEXT_NODE ? node.parentElement : node;

    let isBold = false;
    let isItalic = false;
    let isUnderline = false;

    while (el) {
        if (el.matches?.("strong, b")) isBold = true;
        if (el.matches?.("em, i")) isItalic = true;
        if (el.matches?.("u")) isUnderline = true;

        if (el.getAttribute?.("contenteditable") === "true") break;

        el = el.parentElement;
    }

    return { isBold, isItalic, isUnderline };
}

/**
 * Determines whether the caret is near the top or bottom boundary of a block.
 *
 * Used for handling arrow key navigation between blocks.
 *
 * @param {HTMLElement} el - The block element
 * @param {"top"|"bottom"} side - Boundary to check
 * @returns {boolean}
 */
export function isCaretAtBoundary(el, side) {
    const selection = window.getSelection();
    if (selection.rangeCount === 0) return false;

    const range = selection.getRangeAt(0);
    const cursorRect = range.getClientRects()[0];
    const blockRect = el.getBoundingClientRect();

    if (!cursorRect) return true;

    if (side === "top") {
        return cursorRect.top - blockRect.top < 10;
    } else {
        return blockRect.bottom - cursorRect.bottom < 10;
    }
}