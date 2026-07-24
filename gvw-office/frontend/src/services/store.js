/**
 * Stores a value in localStorage with error handling
 * @param {string} key - Storage key
 * @param {string} value - Value to store
 */
export function setValue(key, value) {
    localStorage.setItem(key, value);
}

/**
 * Retrieves a value from localStorage with error handling
 * @param {string} key - Storage key to retrieve
 * @returns {string} Stored value or empty string if not found/error
 */
export function getValue(key) {
    const value = localStorage.getItem(key);
    return value ?? "";
}

/**
 * Removes a value from localStorage with error handling
 * @param {string} key - Storage key to remove
 */
export function clearValue(key) {
    localStorage.removeItem(key);
}