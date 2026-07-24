<script>
    import PageHeader from "../../../components/PageHeader.svelte";
    import { user } from "../../../stores/user.svelte.js";
    import Card from "../../../components/Card.svelte";
    import SearchBarButton from "../../../components/HelpCenter/SearchBarButton.svelte";
    import { appSettings } from "../../../stores/appSettings.svelte.js";
    import ManageFeaturedCategoriesModal from "../../../components/HelpCenter/ManageFeaturedCategoriesModal.svelte";
    import { helpCenterStore } from "../../../stores/helpCenterStore.svelte.js";
    import { viewport } from "../../../stores/viewport.svelte.js";

    let {
        searchBarButton = () => {},
        ...restProps
    } = $props();

    /**
     * Reference to the manage category modal.
     * Used to programmatically open the manage category dialog.
     * @type {import("../../../components/HelpCenter/ManageFeaturedCategoriesModal.svelte").default}
     */
    let manageFeaturedCategoriesModalRef = null;

    function setActiveCategory(e) { helpCenterStore.activeCategory = e.currentTarget.dataset.id; }
</script>

<ManageFeaturedCategoriesModal bind:this={manageFeaturedCategoriesModalRef} isMobile={false} />

<div class="flex flex-col w-full h-full items-start justify-start gap-4 p-10 overflow-y-auto">
    <div class="flex flex-col items-start justify-start gap-2">
        <p class="font-bold text-dt-3 text-gv-primary tracking-[8%]">HILFE-CENTER</p>
        <PageHeader title="Wie können wir helfen?"
                    subTitle="Durchsuchen Sie unsere Wissensdatenbank oder wählen Sie eine Kategorie, um direkt zu den passenden Artikeln zu gelangen."
                    showSlot={false} />
    </div>

    <SearchBarButton onclick={searchBarButton} />

    <div
        class="w-full h-full flex flex-col items-start justify-start gap-2 p-5 bg-white rounded-1 drop-shadow-[0_0_5px_rgba(0,0,0,0.2)]">
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
        <div
            class="min-[1400px]:grid min-[1400px]:grid-cols-3 min-[1400px]:grid-rows-2 max-[1400px]:flex max-[1400px]:flex-col w-full h-full gap-4">
            {#each appSettings.helpCenterCategories as category (category.id)}
                {#if category.isFeatured}
                    <button class="h-full w-full cursor-pointer group" data-id={category.id}
                            onclick={setActiveCategory}>
                        <Card fillHeight={viewport.width > 1400}>
                            <div class="flex flex-col items-start justify-start gap-4 w-full h-full">
                                <span
                                    class="rounded-2 bg-gv-bg-bar p-4 text-icon-dt-2 text-gv-primary material-symbols-rounded">
                                    {category.icon}
                                </span>
                                <div class="flex flex-col items-start justify-start w-full">
                                    <p class="text-dt-3 text-gv-dark-text">{category.title}</p>
                                    <p class="text-dt-5 text-left text-gv-light-text text-wrap line-clamp-2 truncate">{category.description}</p>
                                </div>
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