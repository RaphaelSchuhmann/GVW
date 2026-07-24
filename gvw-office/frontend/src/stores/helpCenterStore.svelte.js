/**
 * Svelte store for help center data with filtering support
 */
export const helpCenterStore = $state({
    activeCategory: "",
    articles: [],
    activeArticle: null,
    isLoading: false
});