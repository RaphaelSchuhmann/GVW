<script>
    import ToastStack from "../../components/ToastStack.svelte";
    import DesktopSidebar from "../../components/DesktopSidebar.svelte";
    import Button from "../../components/Button.svelte";
    import { push } from "svelte-spa-router";
    import Spinner from "../../components/Spinner.svelte";
    import TextEditor from "../../components/TextEditor/TextEditor.svelte";
    import InfoViewer from "../../components/TextEditor/InfoViewer.svelte";
    import {
        getReport,
        reportTypeMap,
        updateAttachments,
        updateDescription,
        updateReport
    } from "../../services/reportService.svelte.js";
    import { user } from "../../stores/user.svelte.js";
    import { pendingImages, previewUrls } from "../../services/textEditorService.svelte.js";

    let {
        reportData,
        ...restProps
    } = $props();

    /**
     * Reference to the text editor info viewer.
     * Controls visibility and lifecycle of the viewer dialog.
     * @type {import("../../components/TextEditor/InfoViewer.svelte").default}
     */
    let infoViewerRef = null;

    /**
     * Reference to the text editor.
     * @type {import("../../components/TextEditor/TextEditor.svelte").default}
     */
    let editorRef = null;

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
        draft = JSON.parse(JSON.stringify(reportData));
        isEditing = true;
        infoViewerRef?.closeViewer();
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

    /**
     * Saves the current report state to the backend and refreshes local data afterward.
     *
     * This function:
     * - Collects the latest editor state and draft content
     * - Sends an update request including optimistic revision control (`rev`)
     * - Updates the local revision number after a successful save
     * - Resets editing state and UI flags
     * - Reloads the full report from the backend to ensure consistency
     * - Clears temporary caches (pending images and preview URLs)
     *
     * Important behavior:
     * - Always reloads the report after saving to avoid stale state drift
     * - Resets editor state (`isEditing`, `draft`) regardless of success or failure
     * - Clears in-memory image caches after save
     *
     * @async
     * @function saveReport
     * @returns {Promise<void>}
     */
    async function saveReport() {
        editorRef.getContent();

        isSubmitting = true;

        const data = {
            id: reportData.id,
            rev: reportData.rev,
            title: draft.title,
            editor: user.name,
            content: draft.content,
        };

        try {
            reportData.rev = await updateReport(data);
        } finally {
            isSubmitting = false;
            isEditing = false;
            draft = null;

            reportData = await getReport(reportData.id);
            pendingImages.clear();
            previewUrls.clear();
        }
    }

    /**
     * Toggles the visibility state of the information viewer panel.
     *
     * If the viewer is currently open, it will be closed.
     * If it is currently closed, it will be opened.
     *
     * This function acts as a simple UI toggle wrapper around the
     * `infoViewerRef` controller.
     *
     * @function toggleInfoViewer
     * @returns {void}
     */
    function toggleInfoViewer() {
        if (infoViewerRef.isOpen.value) {
            infoViewerRef.closeViewer();
        } else {
            infoViewerRef.openViewer();
        }
    }
</script>

<ToastStack />

<main class="flex overflow-hidden">
    <DesktopSidebar currentPage="reports" />
    <div class="flex flex-col w-full h-dvh overflow-hidden p-8 min-h-0 gap-2">
        <div class="flex w-full items-center p-2">
            <div class="flex items-center justify-start gap-4 max-w-2/5">
                <p class="text-gv-dark-text text-dt-3 text-nowrap truncate">{reportData?.title}</p>
                <!--TODO: Implement in 1.1-->
                <!--<button-->
                <!--    class="flex items-center gap-2 p-1.5 pl-3 pr-3 border-2 border-gv-border rounded-2 cursor-pointer hover:bg-gv-input-bg">-->
                <!--    <span class="material-symbols-rounded text-gv-dark-text text-icon-dt-6">download</span>-->
                <!--    <span class="text-gv-dark-text text-dt-7 font-semibold">PDF</span>-->
                <!--</button>-->
            </div>
            <div class="flex items-center justify-start gap-2 ml-auto">
                {#if !isEditing}
                    <Button type="secondary" padding="3" onclick={async () => await push("/reports")}>
                        <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">arrow_back</span>
                        <p class="text-dt-6 text-gv-dark-text ml-2">Zurück</p>
                    </Button>
                    <Button type="primary" padding="3" onclick={startEditing}>
                        <span class="material-symbols-rounded text-icon-dt-6">edit</span>
                        <p class="text-dt-6 ml-2">Bearbeiten</p>
                    </Button>
                    <button
                        class="bg-gv-primary p-3.5 pl-4 pr-4 text-white cursor-pointer hover:bg-gv-primary-hover rounded-1 flex items-center justify-center"
                        onclick={toggleInfoViewer}>
                        <span class="material-symbols-rounded text-icon-dt-6">info</span>
                    </button>
                {:else}
                    <Button type="primary" onclick={saveReport}>
                        {#if isSubmitting}
                            <Spinner light={true} />
                            <p>Speichern...</p>
                        {:else}
                            Speichern
                        {/if}
                    </Button>
                    <Button type="secondary" onclick={cancelEditing}>
                        Abbrechen
                    </Button>
                {/if}
            </div>
        </div>
        <div class="flex items-start w-full h-full overflow-hidden gap-4 p-2">
            <TextEditor bind:this={editorRef} isEditing={isEditing} itemData={reportData} draft={draft} page="reportEditor" />
            <InfoViewer bind:this={infoViewerRef} bind:data={reportData} categoryMap={reportTypeMap}
                        updateDescription={updateDescription} updateAttachments={updateAttachments} enableAttachments={true} />
        </div>
    </div>
</main>