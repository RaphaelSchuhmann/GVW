/**
 * Svelte store for members data
 */
export const membersStore = $state({
    raw: [],
    display: [],
    loading: false
});

/**
 * Svelte store for members filter state
 */
export const membersFilterState = $state({
    dropdown: "",
    tab: "",
    search: ""
});