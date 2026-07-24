/**
 * Svelte store for toast notifications
 */
export const toasts = $state([]);

/**
 * Adds a new toast notification to the store
 * @param {Object} options - Toast configuration
 * @param {string} options.title - Toast title
 * @param {string} [options.subTitle] - Toast subtitle/message
 * @param {string} [options.type="info"] - Toast type (info, success, error, warning)
 * @param {number} [options.timeout=5000] - Auto-dismiss timeout in milliseconds
 * @param {string} [options.typeId] - An optional id that determines the toast type for generic errors
 */
export function addToast({ title, subTitle = "", type = "info", timeout = 5000, typeId = "" }) {
    const id = crypto.randomUUID();

    toasts.push({ id, title, subTitle, type, typeId });

    setTimeout(() => {
        removeToast(id);
    }, timeout);
}

/**
 * Removes a toast notification by ID
 * @param {string} id - Toast ID to remove
 */
export function removeToast(id) {
    const index = toasts.findIndex(t => t.id === id);
    if (index !== -1) {
        toasts.splice(index, 1);
    }
}