<script>
    import Input from "../Input.svelte";
    import { getArticle, searchArticles } from "../../services/helpCenterService.svelte.js";
    import Chip from "../Chip.svelte";
    import Card from "../Card.svelte";
    import Spinner from "../Spinner.svelte";
    import { highlight } from "../../services/reportService.svelte.js";
    import { sanitize } from "../../services/utils.js";
    import SanitizedHTML from "../SanitizedHTML.svelte";

    let {
        isMobile = false,
        ...restProps
    } = $props();

    let debounce;
    $effect(() => {
        return () => clearTimeout(debounce);
    });

    let inputEl = $state(null);
    let closeBtn = null;

    let searchSequence = 0;
    let isVisible = $state(false);
    let isSearching = $state(false);

    let searchedTerm = $state("");
    let results = $state([]);

    /**
     * Handles keyboard events
     */
    function handleKeyDown(event) {
        if (event.key === "Escape") {
            closeBtn?.click();
        }
    }

    export function openSearch() {
        isVisible = true;
        search("");
    }

    export function closeSearch() {
        isVisible = false;
        results = [];
        inputEl.value = "";
    }

    /**
     * Handles search input with debouncing
     */
    function handleInput(event) {
        const value = event.target.value;
        clearTimeout(debounce);
        debounce = setTimeout(() => {
            search(value);
        }, 800);
    }

    async function search(value) {
        const sequence = ++searchSequence;
        isSearching = true;

        try {
            const response = await searchArticles(value);
            if (sequence === searchSequence && Array.isArray(response)) {
                results = response;
                searchedTerm = value;
            }
        } finally {
            if (sequence === searchSequence) {
                isSearching = false;
            }
        }
    }

    $effect(() => {
        window.addEventListener("keydown", handleKeyDown);
        return () => {
            window.removeEventListener("keydown", handleKeyDown);
        };
    });
</script>

{#if isVisible}
    <div
        class="z-999 top-0 left-0 w-dvw h-dvh flex bg-gv-overlay
               {isMobile ? 'fixed items-end' : 'fixed items-center justify-center'}"
    >
        <div
            class="bg-white flex flex-col p-2 overflow-hidden max-h-[90vh]
                   {isMobile ? 'w-full h-8/9 rounded-t-1' : `w-3/5 rounded-1`}"
        >
            <div class="w-full flex-1 min-h-0 flex flex-col overflow-y-scroll overflow-x-hidden gap-2">
                <div class="w-full flex items-center justify-start gap-2 pl-1">
                    <Input placeholder="Artikel suchen..." title="" showTitle={false} oninput={handleInput}
                           bind:this={inputEl} />
                    <button
                        type="button"
                        class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2"
                        bind:this={closeBtn}
                        onclick={closeSearch}
                    >
                        <span class="material-symbols-rounded text-icon-dt-2">close</span>
                    </button>
                </div>

                <div class="flex flex-col items-center w-full h-full overflow-x-hidden overflow-y-auto gap-2">
                    {#if results.length > 0}
                        {#each results as article (article.id)}
                            {@render articleItem(article)}
                        {/each}
                    {:else if isSearching}
                        <div
                            class="gap-5 bg-white flex flex-col items-center min-h-0 justify-center rounded-1 border-2 border-gv-border w-full h-full p-3 pt-10 pb-10 overflow-hidden border-dashed">
                            <Spinner width="2/5" />
                        </div>
                    {:else}
                        <div
                            class="gap-5 bg-white flex flex-col items-center min-h-0 justify-center rounded-1 border-2 border-gv-border w-full h-full p-3 overflow-hidden border-dashed">
                            <div class="flex items-center justify-center gap-4">
                                <span class="material-symbols-rounded text-icon-dt-4">search_off</span>
                                <p class="text-gv-dark-text text-dt-3 font-medium pt-10 pb-10">Keine Artikel
                                    gefunden...</p>
                            </div>
                        </div>
                    {/if}
                </div>
            </div>
        </div>
    </div>
{/if}

{#snippet articleItem(article)}
    <button class="w-full cursor-pointer group"
            onclick={async () => {await getArticle($state.snapshot(article.id)); closeSearch();}}>
        {#if isMobile}
            <Card>
                <div class="flex flex-col items-start justify-start w-full h-full gap-2">
                    <p class="text-gv-dark-text font-bold text-dt-3">{article.title}</p>
                    <div class="flex w-full justify-start">
                        <SanitizedHTML htmlContent={highlight(article.snippet, searchedTerm)} />
                    </div>
                    <div class="w-full flex items-center justify-start gap-2 flex-wrap">
                        {#each article.tags as tag, i (i)}
                            <Chip text={tag} />
                        {/each}
                    </div>
                </div>
            </Card>
        {:else}
            <Card>
                <div class="flex items-center justify-start gap-4 w-full h-full">
                    <div class="flex flex-col items-start justify-start gap-2 max-w-1/3">
                        <p class="text-gv-dark-text font-bold text-dt-3">{article.title}</p>
                        <div class="flex w-full justify-start">
                            <SanitizedHTML htmlContent={highlight(article.snippet, searchedTerm)} />
                        </div>
                        <div class="w-full flex items-center justify-start gap-2">
                            {#each article.tags as tag, i (i)}
                                <Chip text={tag} />
                            {/each}
                        </div>
                    </div>
                    <div class="ml-auto h-full flex flex-col items-center justify-center">
                                    <span
                                        class="material-symbols-rounded text-icon-dt-4 text-gv-light-text duration-200">chevron_right</span>
                    </div>
                </div>
            </Card>
        {/if}
    </button>
{/snippet}