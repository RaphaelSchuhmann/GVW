<script>
    import { appSettings } from "../../../stores/appSettings.svelte.js";
    import { helpCenterStore } from "../../../stores/helpCenterStore.svelte.js";
    import TextEditor from "../../../components/TextEditor/TextEditor.svelte";

    const category = appSettings.helpCenterCategories.find(cat => cat.id === helpCenterStore.activeCategory);
    const articleData = $derived(helpCenterStore.activeArticle);

    function clearActiveArticle() {  helpCenterStore.activeArticle = null; }
</script>

<div class="flex flex-col w-full h-full items-start justify-start gap-4 overflow-y-auto">
    <div class="flex flex-col items-start justify-start gap-8 w-full h-full">
        <div class="w-full flex items-center justify-start">
            <button class="group cursor-pointer flex items-center justify-start gap-2 p-2"
                    onclick={clearActiveArticle}>
                <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">arrow_back</span>
                <span class="text-dt-5 text-gv-dark-text group-hover:underline">{category?.title || "Zurück"}</span>
            </button>
        </div>
        <TextEditor isEditing={false} isMobile={true} itemData={articleData} draft={null} page="helpCenterEditor" />
    </div>
</div>
