/**
 * Svelte store for events data with filtering support
 */
export const eventsStore = $state({
    raw: [],
    display: [],
    loading: false
});

/**
 * Svelte store for events filter state
 */
export const eventsFilterState = $state({
    dropdown: "",
    tab: "",
    search: ""
});