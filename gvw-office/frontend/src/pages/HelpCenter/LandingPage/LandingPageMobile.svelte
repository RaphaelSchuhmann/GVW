<script>
    import PageHeader from "../../../components/PageHeader.svelte";
    import { user } from "../../../stores/user.svelte.js";
    import Card from "../../../components/Card.svelte";
    import { appSettings } from "../../../stores/appSettings.svelte.js";
    import ManageFeaturedCategoriesModal from "../../../components/HelpCenter/ManageFeaturedCategoriesModal.svelte";
    import { helpCenterStore } from "../../../stores/helpCenterStore.svelte.js";

    /**
     * Reference to the manage category modal.
     * Used to programmatically open the manage category dialog.
     * @type {import("../../../components/HelpCenter/ManageFeaturedCategoriesModal.svelte").default}
     */
    let manageFeaturedCategoriesModalRef = null;

    function setActiveCategory(e) { helpCenterStore.activeCategory = e.currentTarget.dataset.id; }
</script>

<ManageFeaturedCategoriesModal bind:this={manageFeaturedCategoriesModalRef} isMobile={true} />

<div class="flex flex-col w-full h-full items-start justify-start gap-4 overflow-y-auto">
    <div class="flex flex-col items-start justify-start gap-2">
        <p class="font-bold text-dt-3 text-gv-primary tracking-[8%]">HILFE-CENTER</p>
        <PageHeader title="Wie können wir helfen?"
                    hideSubTitle={true}
                    showSlot={false} />
    </div>

    <div
        class="w-full h-full flex flex-col items-start justify-start gap-2">
        <div class="flex items-center justify-start w-full">
            <p class="text-dt-3 font-bold">Kategorien</p>
            {#if user.role === "admin"}
                <button
                    class="flex items-center justify-center ml-auto cursor-pointer hover:bg-gv-hover-effect rounded-2 p-2 duration-75"
                    onclick={manageFeaturedCategoriesModalRef?.showModal}>
                    <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">discover_tune</span>
                </button>
            {/if}
        </div>
        <div class="flex flex-col items-center justify-start w-full h-full gap-2">
            {#each appSettings.helpCenterCategories as category (category.id)}
                {#if category.isFeatured}
                    <button class="w-full cursor-pointer group" data-id={category.id} onclick={setActiveCategory}>
                        <Card>
                            <div class="flex flex-col items-start justify-start gap-4 w-full h-full p-2">
                                <span
                                    class="rounded-2 bg-gv-bg-bar p-4 text-icon-dt-2 text-gv-primary material-symbols-rounded">
                                    {category.icon}
                                </span>
                                <p class="text-dt-3 text-gv-dark-text">{category.title}</p>
                                <div class="flex items-center justify-start gap-2">
                                    <p class="text-gv-light-text text-dt-5">{category.articleCount} Artikel</p>
                                    <span
                                        class="material-symbols-rounded text-icon-dt-6 text-gv-light-text group-hover:ml-1 duration-200">chevron_right</span>
                                </div>
                            </div>
                        </Card>
                    </button>
                {/if}
            {/each}
        </div>
    </div>
</div>