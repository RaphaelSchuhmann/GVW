/**
 * Svelte store for deployment data with filtering support
 */
export const deploymentStore = $state({
    raw: [],
    display: [],
    loading: false
});

/**
 * Svelte store for deployment filter state
 */
export const deploymentFilterState = $state({
    dropdown: "",
    tab: "",
    search: ""
});