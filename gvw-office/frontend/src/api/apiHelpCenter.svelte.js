import { httpPost, httpDelete, httpPatch, parseBodySafe, httpGet } from "./http.svelte.js";

const apiUrl = __API_URL__;

/**
 * Sends a request to create a new help center category.
 *
 * @param {Object} inputs - Category data.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiAddCategory(inputs) {
    const resp = await httpPost(`${apiUrl}/help/category/add`, {
        title: inputs.title,
        icon: inputs.icon,
        description: inputs.description
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Sends a request to delete a help center category.
 *
 * @param {string} id - Category ID.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiDeleteCategory(id) {
    const resp = await httpDelete(`${apiUrl}/help/category/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Updates the featured help center categories.
 *
 * @param {Map<string, boolean>} featured - Map of featured category IDs.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiUpdateFeaturedCategories(featured) {
    const resp = await httpPatch(`${apiUrl}/help/category/update/featured`, { featured: Object.fromEntries(featured) });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Sends a request to create a new help center article.
 *
 * @param {Object} data - Article data.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiAddArticle(data) {
    const resp = await httpPost(`${apiUrl}/help/article/add`, {
        title: data.title,
        description: data.description,
        tags: data.tags,
        category: data.category
    });
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Sends a request to delete a help center article.
 *
 * @param {string} id - Article ID.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiDeleteArticle(id) {
    const resp = await httpDelete(`${apiUrl}/help/article/delete/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Retrieves all articles belonging to a category.
 *
 * @param {string} category - Category ID.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiGetArticles(category) {
    const resp = await httpGet(`${apiUrl}/help/article/get?category=${category}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Retrieves a help center article.
 *
 * @param {string} id - Article ID.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiGetArticle(id) {
    const resp = await httpGet(`${apiUrl}/help/article/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Updates an existing help center article.
 *
 * @param {FormData} formData - Multipart form containing the article data and uploaded images.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiUpdateArticle(formData) {
    const resp = await httpPatch(`${apiUrl}/help/article/update`, formData, "", true, true);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Checks whether a help center category exists.
 *
 * @param {string} id - Category ID.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiCheckCategory(id) {
    const resp = await httpGet(`${apiUrl}/help/category/check/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Checks whether a help center article exists.
 *
 * @param {string} id - Article ID.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiCheckArticle(id) {
    const resp = await httpGet(`${apiUrl}/help/article/check/${id}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}

/**
 * Searches help center articles.
 *
 * @param {string} term - Search term.
 * @returns {Promise<{resp: Response|null, body: Object|null}>}
 */
export async function apiSearchArticles(term) {
    const params = new URLSearchParams({ term });
    const resp = await httpGet(`${apiUrl}/help/article/search?${params}`);
    if (!resp) return { resp: null, body: null };
    const body = await parseBodySafe(resp);
    return { resp, body };
}