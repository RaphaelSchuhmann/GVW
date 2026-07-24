<script>
    import { user } from "../../../stores/user.svelte.js";
    import { appSettings } from "../../../stores/appSettings.svelte.js";
    import { helpCenterStore } from "../../../stores/helpCenterStore.svelte.js";
    import Button from "../../../components/Button.svelte";
    import ConfirmDeleteModal from "../../../components/ConfirmDeleteModal.svelte";
    import { getArticle, getArticles, updateHelpCenterArticle } from "../../../services/helpCenterService.svelte.js";
    import { addToast } from "../../../stores/toasts.svelte.js";
    import TextEditor from "../../../components/TextEditor/TextEditor.svelte";
    import { pendingImages, previewUrls } from "../../../services/textEditorService.svelte.js";
    import Spinner from "../../../components/Spinner.svelte";
    import { viewport } from "../../../stores/viewport.svelte.js";

    /**
     * Reference to the confirm delete modal.
     * Used to programmatically open the confirm deletion dialog.
     * @type {import("../../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteArticleModal = null;

    /**
     * Reference to the text editor.
     * @type {import("../../../components/TextEditor/TextEditor.svelte").default}
     */
    let editorRef = null;

    const category = appSettings.helpCenterCategories.find(cat => cat.id === helpCenterStore.activeCategory);

    let articleData = $derived(helpCenterStore.activeArticle);

    let isEditing = $state(false);
    let isSubmitting = $state(false);

    let draft = $state(null);

    /**
     * Initializes edit mode for the current report.
     *
     * Creates a deep clone of `reportData` and assigns it to `draft`
     * to allow non-destructive editing. Enables the editing state.
     */
    function startEditing() {
        draft = JSON.parse(JSON.stringify(articleData));
        isEditing = true;
    }

    /**
     * Cancels the current editing session.
     *
     * Discards the draft object and exits edit mode
     * without persisting any changes.
     */
    function cancelEditing() {
        draft = null;
        isEditing = false;
    }

    async function saveChanges() {
        editorRef.getContent();

        isSubmitting = true;

        const data = {
            id: articleData.id,
            rev: articleData.rev,
            title: draft.title,
            content: draft.content
        };

        try {
            articleData.rev = await updateHelpCenterArticle(data);
        } finally {
            isSubmitting = false;
            isEditing = false;
            draft = null;

            await getArticle(articleData.id);
            pendingImages.clear();
            previewUrls.clear();
        }
    }

    function startDelete() {
        if (!confirmDeleteArticleModal) return;

        if (!articleData.id) {
            addToast({
                title: "Artikel nicht gefunden",
                subTitle: "Der ausgewählte Artikel wurde nicht gefunden. Bitte versuchen Sie es erneut.",
                type: "error"
            });
            return;
        }

        confirmDeleteArticleModal.startDelete();
    }

    function clearActiveArticle() {  helpCenterStore.activeArticle = null; }
</script>

<ConfirmDeleteModal action="deleteHelpArticle" title="Artikel löschen"
                    subTitle="Sind Sie sich sicher das sie diesen Artikel löschen möchten?" placeholder="Artikel1"
                    expectedInput={articleData?.title} id={articleData?.id}
                    onClose={async () => { clearActiveArticle(); await getArticles(); }}
                    bind:this={confirmDeleteArticleModal} />

<div class="flex flex-col w-full h-full items-start justify-start gap-4 p-10 overflow-y-auto">
    <div class="flex flex-col items-start justify-start gap-8 w-full h-full">
        <div class="w-full flex items-center justify-start">
            <button class="group cursor-pointer flex items-center justify-start gap-2 p-2"
                    onclick={clearActiveArticle}>
                <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">arrow_back</span>
                <span class="text-dt-5 text-gv-dark-text group-hover:underline">{category?.title || "Zurück"}</span>
            </button>
            {#if user.role === "admin" && !isEditing}
                <div class="flex items-center gap-4 ml-auto">
                    <Button type="delete" onclick={startDelete}>
                        <div class="flex items-center justify-center gap-2">
                            <span class="material-symbols-rounded text-icon-dt-6 max-[1200px]:text-icon-dt-7">delete</span>
                            <span class="text-nowrap min-[1200px]:text-dt-3 text-dt-5">Artikel löschen</span>
                        </div>
                    </Button>

                    {#if viewport.width > 1200}
                        <Button type="primary" onclick={startEditing}>
                            <div class="flex items-center justify-center gap-2">
                                <span class="material-symbols-rounded">edit</span>
                                <span class="text-nowrap">Bearbeiten</span>
                            </div>
                        </Button>
                    {/if}
                </div>
            {/if}

            {#if isEditing}
                <div class="flex items-center gap-4 ml-auto">
                    <Button type="secondary" onclick={cancelEditing}>
                        <span class="text-nowrap">Abbrechen</span>
                    </Button>

                    <Button type="primary" onclick={saveChanges}>
                        {#if isSubmitting}
                            <Spinner light={true} />
                            <p>Speichern...</p>
                        {:else}
                            Speichern
                        {/if}
                    </Button>
                </div>
            {/if}
        </div>
        <TextEditor bind:this={editorRef} isEditing={isEditing} itemData={articleData} draft={draft}
                    page="helpCenterEditor" />
    </div>
</div>
