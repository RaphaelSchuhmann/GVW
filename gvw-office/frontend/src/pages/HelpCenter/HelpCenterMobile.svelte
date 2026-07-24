<script>
    import ToastStack from "../../components/ToastStack.svelte";
    import MobileSidebar from "../../components/HelpCenter/MobileSidebar.svelte";
    import { appSettings } from "../../stores/appSettings.svelte.js";
    import { helpCenterStore } from "../../stores/helpCenterStore.svelte.js";
    import LandingPageMobile from "./LandingPage/LandingPageMobile.svelte";
    import CategoryPageMobile from "./CategoryPage/CategoryPageMobile.svelte";
    import ArticlePageMobile from "./ArticlePage/ArticlePageMobile.svelte";
    import SearchModal from "../../components/HelpCenter/SearchModal.svelte";

    let sidebarOpen = $state(false);

    /**
     * Reference to the search modal.
     * Used to programmatically open the search dialog.
     * @type {import("../../components/HelpCenter/SearchModal.svelte").default}
     */
    let searchModalRef = null;

    function openSidebar() { sidebarOpen = true; }
</script>

<ToastStack isMobile={true}/>

<MobileSidebar buttons={appSettings.helpCenterCategories} isOpen={sidebarOpen} />

<SearchModal bind:this={searchModalRef} isMobile={true} />

<main class="flex overflow-hidden">
    <button class="rounded-full bg-gv-primary text-white p-3 flex items-center justify-center absolute top-6 right-6" onclick={searchModalRef?.openSearch}>
        <span class="material-symbols-rounded text-icon-dt-4">search</span>
    </button>
    <div class="flex-1 min-h-0 overflow-y-auto h-screen">
        <div class="flex flex-col w-full flex-1 overflow-hidden p-7 min-h-0 gap-4 h-full">
            <div class="w-full flex flex-col items-start justify-start h-full">
                <button class="flex items-center justify-center" onclick={openSidebar}>
                    <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">menu</span>
                </button>

                <div class="flex flex-col w-full h-full items-start gap-2">
                    {#if helpCenterStore.activeCategory === "" && helpCenterStore.activeArticle === null}
                        <LandingPageMobile />
                    {:else if helpCenterStore.activeArticle}
                        <ArticlePageMobile />
                    {:else}
                        <CategoryPageMobile />
                    {/if}
                </div>
            </div>
        </div>
    </div>
</main>