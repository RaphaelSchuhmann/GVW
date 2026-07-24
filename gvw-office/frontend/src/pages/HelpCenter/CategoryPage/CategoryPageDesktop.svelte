<script>
    import { user } from "../../../stores/user.svelte.js";
    import Card from "../../../components/Card.svelte";
    import { appSettings } from "../../../stores/appSettings.svelte.js";
    import { helpCenterStore } from "../../../stores/helpCenterStore.svelte.js";
    import Button from "../../../components/Button.svelte";
    import AddArticleModal from "../../../components/HelpCenter/AddArticleModal.svelte";
    import Chip from "../../../components/Chip.svelte";
    import ConfirmDeleteModal from "../../../components/ConfirmDeleteModal.svelte";
    import { getArticle, getArticles } from "../../../services/helpCenterService.svelte.js";
    import { addToast } from "../../../stores/toasts.svelte.js";
    import { justifyMap, marginMap, paddingMap, roundedMap } from "../../../lib/dynamicStyles.js";
    import { viewport } from "../../../stores/viewport.svelte.js";

    /**
     * Reference to the add article modal.
     * Used to programmatically open the add article dialog.
     * @type {import("../../../components/HelpCenter/AddArticleModal.svelte").default}
     */
    let addArticleModalRef = null;

    /**
     * Reference to the confirm delete modal.
     * Used to programmatically open the confirm deletion dialog.
     * @type {import("../../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteCategoryModal = null;

    const category = appSettings.helpCenterCategories.find(cat => cat.id === helpCenterStore.activeCategory);

    function startDelete() {
        if (!confirmDeleteCategoryModal) return;

        if (!helpCenterStore.activeCategory || !category) {
            addToast({
                title: "Kategorie nicht gefunden",
                subTitle: "Die ausgewählte Kategorie wurden nicht gefunden. Bitte versuchen Sie es erneut.",
                type: "error"
            });
            return;
        }

        if (category.id === helpCenterStore.activeCategory) confirmDeleteCategoryModal.startDelete();
    }

    function clearActiveCategory() { helpCenterStore.activeCategory = ""; }
</script>

<AddArticleModal bind:this={addArticleModalRef} isMobile={false} />
<ConfirmDeleteModal action="deleteHelpCategory" title="Kategorie löschen"
                    subTitle="Sind Sie sich sicher das sie diese Kategorie löschen möchten?" placeholder="Kategorie1"
                    expectedInput={category?.title} id={helpCenterStore.activeCategory}
                    onClose={async () => { await getArticles(); }} bind:this={confirmDeleteCategoryModal} />

<div class="flex flex-col w-full h-full items-start justify-start gap-4 p-10 overflow-y-auto">
    <div class="flex flex-col items-start justify-start gap-8 w-full h-full">
        <div class="flex flex-col items-start justify-start w-full gap-2 ">
            <div class="w-full flex items-center justify-start">
                <button class="group cursor-pointer flex items-center justify-start gap-2 p-2"
                        onclick={clearActiveCategory}>
                    <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">arrow_back</span>
                    <span class="text-dt-5 text-gv-dark-text group-hover:underline text-nowrap">Alle Kategorien</span>
                </button>
                {#if user.role === "admin" && viewport.width > 1050}
                    <div class="flex items-center gap-4 ml-auto">
                        <Button type="delete" disabled={helpCenterStore.articles.length > 0} onclick={startDelete}>
                            <div class="flex items-center justify-center gap-2">
                                <span class="material-symbols-rounded min-[1200px]:text-icon-dt-6 max-[1200px]:text-icon-dt-7">delete</span>
                                <span class="text-nowrap min-[1200px]:text-dt-4 max-[1200px]:text-dt-6">Kategorie löschen</span>
                            </div>
                        </Button>

                        <Button type="primary" onclick={addArticleModalRef?.showModal}>
                            <div class="flex items-center justify-center gap-2">
                                <span class="material-symbols-rounded min-[1200px]:text-icon-dt-6 max-[1200px]:text-icon-dt-7">post_add</span>
                                <span class="text-nowrap min-[1200px]:text-dt-4 max-[1200px]:text-dt-6">Artikel hinzufügen</span>
                            </div>
                        </Button>
                    </div>
                {/if}
            </div>
            <div class="w-full flex items-start justify-start gap-4">
                <span class="rounded-2 bg-gv-bg-bar p-4 min-[1000px]:text-icon-dt-2 text-icon-dt-4 text-gv-primary material-symbols-rounded">
                    {category?.icon}
                </span>
                <div class="flex flex-col items-start justify-start">
                    <p class="text-gv-dark-text min-[1000px]:text-dt-3 text-dt-4">{category?.title}</p>
                    <p class="text-gv-light-text min-[1000px]:text-dt-5 text-dt-5">{category?.description}</p>
                </div>
            </div>
        </div>
        <div
            class="w-full h-full flex flex-col items-start justify-start gap-2 p-5 bg-white rounded-1 overflow-y-auto drop-shadow-[0_0_5px_rgba(0,0,0,0.2)]">
            {#if helpCenterStore.articles.length > 0}
                {#each helpCenterStore.articles as article (article.id)}
                    <button class="w-full cursor-pointer group"
                            onclick={async () => {await getArticle(article.id)}}>
                        <Card>
                            <div class="flex items-center justify-start gap-4 w-full h-full">
                                <div class="flex flex-col items-start justify-start gap-2 max-w-1/3">
                                    <p class="text-gv-dark-text font-bold text-dt-3">{article.title}</p>
                                    <p class="text-gv-light-text text-dt-5 line-clamp-2 truncate">{article.description}</p>
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
                    </button>
                {/each}
            {:else}
                <div class="gap-5 bg-white flex flex-col items-center min-h-0 justify-center rounded-1 border-2 border-gv-border w-full h-full p-3 overflow-hidden border-dashed">
                    <span class="rounded-2 bg-gv-bg-bar p-4 text-icon-dt-1 text-gv-primary material-symbols-rounded">
                        {category?.icon}
                    </span>
                    <div class="gap-2 flex flex-col items-center justify-center">
                        <p class="text-gv-dark-text font-medium text-dt-3 text-center">Noch keine Artikel vorhanden</p>
                        <p class="text-gv-light-text text-dt-5 text-center">Für diese Kategorie wurden noch keine Hilfeartikel veröffentlicht.</p>
                    </div>
                </div>
            {/if}
        </div>
    </div>
</div>