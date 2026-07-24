/**
 * Svelte store for report data with filtering support
 */
export const reportsStore = $state({
    raw: [],
    display: [],
    loading: false
});

/**
 * Svelte store for report data from deep search
 */
export const reportDeepSearchStore = $state({
    data: [],
    query: "",
    loading: false
});

/**
 * Svelte store for reports filter state
 */
export const reportsFilterState = $state({
    dropdown: "",
    tab: "",
    search: ""
});