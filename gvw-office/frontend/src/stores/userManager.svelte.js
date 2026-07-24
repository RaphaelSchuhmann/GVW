/**
 * Svelte store for user manager data with filtering support
 */
export const userManagerStore = $state({
    raw: [],
    display: [],
    loading: false
});

/**
 * Svelte store for user manager filter state
 */
export const userManagerFilterState = $state({
    dropdown: "",
    tab: "",
    search: ""
});