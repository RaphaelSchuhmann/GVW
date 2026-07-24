/**
 * Svelte store for library data with filtering support
 */
export const libraryStore = $state({
    raw: [],
    display: [],
    loading: false
});

/**
 * Svelte store for library filter state
 */
export const libraryFilterState = $state({
    dropdown: "",
    tab: "",
    search: ""
});