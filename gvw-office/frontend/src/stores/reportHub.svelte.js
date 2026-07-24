/**
 * Svelte store for feedback data
 */
export const feedbackStore = $state({
    data: [],
    loading: false
});

/**
 * Svelte store for bug report data
 */
export const bugReportStore = $state({
    data: [],
    loading: false
});