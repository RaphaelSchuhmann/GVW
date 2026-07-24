<script>
    import ToastStack from "../../components/ToastStack.svelte";
    import DesktopSidebar from "../../components/HelpCenter/DesktopSidebar.svelte";
    import Header from "../../components/HelpCenter/Header.svelte";
    import LandingPageDesktop from "./LandingPage/LandingPageDesktop.svelte";
    import { appSettings } from "../../stores/appSettings.svelte.js";
    import AddCategoryModal from "../../components/HelpCenter/AddCategoryModal.svelte";
    import { helpCenterStore } from "../../stores/helpCenterStore.svelte.js";
    import CategoryPageDesktop from "./CategoryPage/CategoryPageDesktop.svelte";
    import ArticlePageDesktop from "./ArticlePage/ArticlePageDesktop.svelte";
    import SearchModal from "../../components/HelpCenter/SearchModal.svelte";
    import Spinner from "../../components/Spinner.svelte";

    /**
     * Reference to the add category modal.
     * Used to programmatically open the add category dialog.
     * @type {import("../../components/HelpCenter/AddCategoryModal.svelte").default}
     */
    let addCategoryModalRef = null;

    /**
     * Reference to the search modal.
     * Used to programmatically open the search dialog.
     * @type {import("../../components/HelpCenter/SearchModal.svelte").default}
     */
    let searchModalRef = null;
</script>

<ToastStack />

<AddCategoryModal bind:this={addCategoryModalRef} isMobile={false} />

<SearchModal bind:this={searchModalRef} isMobile={false} />

<main class="flex h-screen overflow-hidden">
    <div class="flex flex-col w-dvw h-dvh overflow-hidden">
        <Header onClickAddCategory={addCategoryModalRef?.showModal} onClickSearchBar={searchModalRef?.openSearch} />

        <div class="flex w-dvw h-full overflow-hidden">
            <DesktopSidebar buttons={appSettings.helpCenterCategories} />

            {#if helpCenterStore.activeCategory === "" && helpCenterStore.activeArticle === null}
                <LandingPageDesktop searchBarButton={searchModalRef?.openSearch} />
            {:else if helpCenterStore.activeArticle !== null}
                <ArticlePageDesktop />
            {:else}
                <CategoryPageDesktop />
            {/if}
        </div>
    </div>
</main>