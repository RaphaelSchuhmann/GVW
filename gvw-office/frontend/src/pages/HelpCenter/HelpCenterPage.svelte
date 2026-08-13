<script>
    import { viewport } from "../../stores/viewport.svelte.js";
    import HelpCenterDesktop from "./HelpCenterDesktop.svelte";
    import HelpCenterMobile from "./HelpCenterMobile.svelte";
    import { ensureUserData } from "../../services/userService.svelte.js";
    import { auth } from "../../stores/auth.svelte.js";
    import Spinner from "../../components/Spinner.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { getArticles, categoryExists, articleExists } from "../../services/helpCenterService.svelte.js";
    import { helpCenterStore } from "../../stores/helpCenterStore.svelte.js";
    import { editorMetadataStore } from "../../stores/textEditorStore.svelte.js";
    import { addToast } from "../../stores/toasts.svelte.js";
    import GlobalLoader from "../../components/GlobalLoader.svelte";

    let ready = $state(false);

    $effect(() => {
        if (!auth.token) return;

        (async () => {
            await ensureUserData();
            if (!auth.token) return;
            editorMetadataStore.activeFeature = "help";
            ready = true;
        })();
    });

    $effect(() => {
        const _trigger = lastRefresh.HELP_CENTER;
        const _triggerStore = helpCenterStore.activeCategory;

        if (!ready) return;

        (async () => {
            await getArticles();

            if (helpCenterStore.activeCategory) {
                const activeCategoryExists = await categoryExists(helpCenterStore.activeCategory);
                if (!activeCategoryExists) {
                    addToast({
                        title: "Kategorie nicht mehr verfügbar",
                        subTitle: viewport.isMobile ? "" : "Diese Kategorie wurde gelöscht und ist nicht mehr verfügbar.",
                        type: "error"
                    });

                    helpCenterStore.activeCategory = "";
                    return;
                }
            }

            if (helpCenterStore.activeArticle?.id) {
                const activeArticleExists = await articleExists(helpCenterStore.activeArticle.id);
                if (!activeArticleExists) {
                    addToast({
                        title: "Artikel nicht mehr verfügbar",
                        subTitle: viewport.isMobile ? "" : "Dieser Artikel wurde gelöscht und ist nicht mehr verfügbar.",
                        type: "error"
                    });

                    helpCenterStore.activeArticle = null;
                    await getArticles();
                }
            }
        })();
    });
</script>

<GlobalLoader loading={!ready || helpCenterStore.isLoading}>
    {#if viewport.width <= 800}
        <HelpCenterMobile />
    {:else}
        <HelpCenterDesktop />
    {/if}
</GlobalLoader>
