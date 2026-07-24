<script>
    import { filterRegistry } from "../lib/filterRegistry.svelte";
    import { marginMap } from "../lib/dynamicStyles";
    import { normalizeResponse } from "../api/http.svelte.js";
    import { handleGlobalApiError } from "../api/globalErrorHandler.svelte.js";

    let {
        marginTop = "",
        placeholder = "Suchen...",
        disabled = false,
        width = "w-full",
        isMobile = false,
        page = "",
        onblur = undefined,
        ...restProps
    } = $props();

    let searchEl = null;
    let debounce;
    let deepSearchDebounce;
    $effect(() => {
        () => clearTimeout(debounce);
        () => clearTimeout(deepSearchDebounce);
    });

    // Get filter logic from registry based on the "page" key
    const entry = filterRegistry[page];

    const filterState = entry?.filterState;
    const config = entry?.config ?? {};

    const doDeepSearch = config?.search.deepSearch;
    const deepSearchStore = config?.search.deepSearchStore || null;
    let abortController;

    let usedPlaceholder = config?.search?.placeholder ?? placeholder;

    /**
     * Handles search input with debouncing
     */
    function handleInput(event) {
        const value = event.target.value;
        clearTimeout(debounce);
        clearTimeout(deepSearchDebounce);
        debounce = setTimeout(() => {
            Object.assign(filterState, { search: value });
            if (doDeepSearch) {
                Object.assign(deepSearchStore, { query: "", data: [] });
            }
        }, 250);

        deepSearchDebounce = setTimeout(async () => {
            await performDeepSearch(value);
        }, 800);
    }

    /**
     * Performs a deepSearch if enabled in the filterRegistry.
     *
     * @param {string} query - The input from the user
     */
    async function performDeepSearch(query) {
        if (!doDeepSearch || !query || query.length < 3) return;

        if (abortController) abortController.abort();
        abortController = new AbortController();

        try {
            const { resp, body } = await config.search.deepSearchFetch(query, abortController.signal);
            const normalized = normalizeResponse(resp);

            if (handleGlobalApiError(normalized)) return;

            Object.assign(deepSearchStore, { data: body.data, query: query });
        } catch (err) {
            if (err.name === "AbortError") return; // Expected
        }
    }

    /**
     * Focuses the search input element (Exposed to parent via bind:this)
     */
    export function focus() {
        searchEl?.focus();
    }
</script>

<div class="flex items-stretch {width} {marginMap[marginTop]}">
    <div class="flex items-center justify-center px-3 mt-1 rounded-tl-1 rounded-bl-1 bg-gv-input-bg">
        <span class="material-symbols-rounded {!isMobile ? 'text-icon-dt-4' : 'text-icon-dt-6'} text-gv-input-placeholder">
            search
        </span>
    </div>

    <input
        bind:this={searchEl}
        {onblur}
        {disabled}
        placeholder={usedPlaceholder}
        oninput={handleInput}
        class="rounded-tr-1 rounded-br-1 w-full p-2 pr-3 bg-gv-input-bg text-black outline-gv-primary mt-1 {!isMobile ? 'text-dt-6' : 'text-dt-7'}"
        {...restProps}
    />
</div>