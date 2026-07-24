<script>
    import Card from "../../../components/Card.svelte";
    import { appSettings } from "../../../stores/appSettings.svelte.js";
    import { helpCenterStore } from "../../../stores/helpCenterStore.svelte.js";
    import AddArticleModal from "../../../components/HelpCenter/AddArticleModal.svelte";
    import Chip from "../../../components/Chip.svelte";
    import { getArticle } from "../../../services/helpCenterService.svelte.js";

    /**
     * Reference to the add article modal.
     * Used to programmatically open the add article dialog.
     * @type {import("../../../components/HelpCenter/AddArticleModal.svelte").default}
     */
    let addArticleModalRef = null;

    const category = appSettings.helpCenterCategories.find(cat => cat.id === helpCenterStore.activeCategory);

    function clearActiveCategory() { helpCenterStore.activeCategory = ""; }
</script>

<AddArticleModal bind:this={addArticleModalRef} isMobile={true} />

<div class="flex flex-col w-full h-full items-start justify-start gap-4 overflow-y-auto">
    <div class="flex flex-col items-start justify-start gap-8 w-full h-full">
        <div class="flex flex-col items-start justify-start w-full gap-4 mt-4">
            <div class="w-full flex items-center justify-start">
                <button class="group cursor-pointer flex items-center justify-start gap-2"
                        onclick={clearActiveCategory}>
                    <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">arrow_back</span>
                    <span class="text-dt-5 text-gv-dark-text group-hover:underline">Alle Kategorien</span>
                </button>
            </div>
            <div class="w-full flex items-start justify-start gap-4">
                <span class="rounded-2 bg-gv-bg-bar p-4 text-icon-dt-5 text-gv-primary material-symbols-rounded">
                    {category?.icon}
                </span>
                <div class="flex flex-col items-start justify-start">
                    <p class="text-gv-dark-text text-dt-3">{category?.title}</p>
                    <p class="text-gv-light-text text-dt-5 text-wrap line-clamp-2 truncate">{category?.description}</p>
                </div>
            </div>
        </div>
        <div
            class="w-full h-full flex flex-col items-start justify-start gap-2 overflow-y-auto">
            {#if helpCenterStore.articles.length > 0}
                {#each helpCenterStore.articles as article (article.id)}
                    <button class="w-full cursor-pointer group"
                            onclick={async () => {await getArticle(article.id)}}>
                        <Card>
                            <div class="flex flex-col items-start justify-start w-full h-full gap-2">
                                <p class="text-gv-dark-text font-bold text-dt-3">{article.title}</p>
                                <p class="text-gv-light-text text-dt-5 line-clamp-2 truncate">{article.description}</p>
                                <div class="w-full flex items-center justify-start gap-2 flex-wrap">
                                    {#each article.tags as tag, i (i)}
                                        <Chip text={tag} />
                                    {/each}
                                </div>
                            </div>
                        </Card>
                    </button>
                {/each}
            {:else}
                <div
                    class="gap-5 bg-white flex flex-col items-center min-h-0 justify-start pt-20 rounded-1 border-2 border-gv-border w-full h-full p-3 overflow-hidden border-dashed">
                    <span class="rounded-2 bg-gv-bg-bar p-4 text-icon-dt-1 text-gv-primary material-symbols-rounded">
                        {category?.icon}
                    </span>
                    <div class="gap-2 flex flex-col items-center justify-center">
                        <p class="text-gv-dark-text font-medium text-dt-3 text-center">Noch keine Artikel vorhanden</p>
                    </div>
                </div>
            {/if}
        </div>
    </div>


</div>