/**
 * Capitalizes the first letter of each word in a string
 * @param {string} str - String to capitalize
 * @returns {string} String with each word capitalized
 */
export function capitalizeWords(str) {
    return str
        .toLowerCase()
        .replaceAll(/(^|[\s-_])\w/g, match => match.toUpperCase());
}

/**
 * Determines if a string contains a number
 * @param {string} str - String to check
 * @returns {boolean} True if string contains a number
 */
export function determineChoirType(str) {
    return /\d/.test(str);
}

/**
 * Triggers a browser download for the provided Blob.
 *
 * A temporary object URL is created and assigned to a dynamically
 * generated anchor element to initiate the download. The resulting
 * file is saved as a ZIP archive using the supplied name.
 *
 * The created object URL is automatically revoked after the download
 * has been triggered to prevent memory leaks.
 *
 * @param {Blob} blob - ZIP archive data to download.
 * @param {string} zipName - Base filename used for the downloaded ZIP file.
 *
 * @returns {void}
 */
export function triggerFileDownload(blob, zipName) {
    const url = URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = `${zipName}.zip`;
    a.click();

    // Cleanup URL reference in the next event loop tick
    setTimeout(() => URL.revokeObjectURL(url), 0);
}

export function renameFile(originalFile, newName) {
    const fileOptions = { type: originalFile.type };
    return new File([originalFile], newName, fileOptions);
}

const ALLOWED_TAGS = new Set([
    "b", "i", "u", "strong", "em", "a", "span", "img",
    "br", "li", "ul", "ol", "dl", "dt", "dd"
]);

const ALLOWED_ATTR = new Set([
    "href", "class", "target", "rel", "src", "alt", "data-rich-link", "data-id", "contenteditable"
]);

/**
 * Sanitizes HTML input by normalizing whitespace, removing disallowed tags,
 * and stripping out all non-allowlisted attributes.
 *
 * @param {string} html - Raw HTML string (typically from `innerHTML`)
 * @returns {string} Cleaned and normalized HTML string
 */
export function sanitize(html) {
    if (!html) return "";

    html = html
        .replaceAll("&nbsp;", " ")
        .replaceAll("\u00A0", " ")
        .replaceAll(/[ \t]+(\r?\n)/g, "$1")
        .replaceAll(/\n\s+/g, "\n");

    const parser = new DOMParser();
    const doc = parser.parseFromString(html, "text/html");

    function clean(node) {
        [...node.childNodes].forEach(child => {
            if (child.nodeType === Node.ELEMENT_NODE) {
                const tag = child.tagName.toLowerCase();

                if (ALLOWED_TAGS.has(tag)) {
                    // --- Attribute Filtering Added Here ---
                    // Convert NamedNodeMap to an array to safely delete attributes while iterating
                    [...child.attributes].forEach(attr => {
                        const attrName = attr.name.toLowerCase();
                        if (!ALLOWED_ATTR.has(attrName)) {
                            child.removeAttribute(attrName);
                        }
                    });

                    // Continue recursively cleaning children
                    clean(child);
                } else {
                    // Replace disallowed tag with its text/children content
                    child.replaceWith(...child.childNodes);
                }
            }
        });
    }

    clean(doc.body);

    return doc.body.innerHTML.trim();
}