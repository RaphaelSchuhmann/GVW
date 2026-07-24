<script>
    import Chip from "../Chip.svelte";
    import { formatISODateString } from "../../services/dateTimeUtils.js";
    import { tick } from "svelte";
    import FileSelector from "../FileSelector.svelte";
    import Spinner from "../Spinner.svelte";
    import Button from "../Button.svelte";
    import { downloadAttachments } from "../../services/reportService.svelte.js";

    let {
        data = $bindable({
            id: null,
            rev: null
        }),
        categoryMap,
        updateDescription,
        enableAttachments = false,
        updateAttachments,
        ...restProps
    } = $props();

    let _isOpen = $state(false);
    let isEditingDescription = $state(false);
    let descriptionDraft = $state("");
    let descriptionTextAreaRef = null;

    let savedState = $state([]);
    let workingState = $state([]);
    let isSavingAttachments = $state(false);

    $effect(() => {
        const attachments = data?.attachments ? [...data.attachments] : [];
        savedState = attachments;
        workingState = [...attachments];
    });

    export const isOpen = {
        get value() {
            return _isOpen;
        }
    };

    export function openViewer() {
        _isOpen = true;
    }

    export function closeViewer() {
        _isOpen = false;
    }

    function toggleEditDescription() {
        if (isEditingDescription) {
            saveDescription();
            isEditingDescription = false;
            descriptionDraft = "";
        } else {
            descriptionDraft = data.description;
            isEditingDescription = true;

            tick().then(() => {
                descriptionTextAreaRef?.focus();
            });
        }
    }

    function cancelEditDescription() {
        descriptionDraft = "";
        isEditingDescription = false;
    }

    async function saveDescription() {
        let description = descriptionDraft;
        if (descriptionDraft.trim().length === 0) description = "Keine Beschreibung";

        // Note: Parameters pattern always: documentId, documentRev, description
        //       It should also always return if it was successful as well as
        //       the new rev so the data can be updated
        const resp = await updateDescription(data.id, data.rev, description);

        if (resp.success) {
            data.rev = resp.rev;
            data.description = description;
        }
    }

    async function saveAttachmentChanges() {
        if (!enableAttachments || !data.id || !data.rev) return;
        isSavingAttachments = true;

        try {
            // resp: { success: boolean, rev: string|null }
            const resp = await updateAttachments?.(data.id, data.rev, workingState);

            if (resp?.success && resp?.rev) {
                const updatedFiles = [...workingState];
                for (let i = 0; i < updatedFiles.length; i++) {
                    if (updatedFiles[i] instanceof File) {
                        updatedFiles[i] = updatedFiles[i].name;
                    }
                }

                savedState = [...updatedFiles];
                workingState = [...savedState];

                data.rev = resp.rev;
                data.attachments = savedState;
            } else {
                workingState = [...savedState];
            }
        } finally {
            isSavingAttachments = false;
        }
    }

    function handleDownloadAttachments(e) {
        const itemId = e.currentTarget.dataset.id;
        downloadAttachments(itemId);
    }
</script>

{#if _isOpen}
    <div
        class="max-w-1/3 h-full flex-1 flex flex-col items-start justify-start rounded-1 bg-white drop-shadow-md min-h-0 gap-4 overflow-y-auto overflow-x-hidden">
        <div class="flex items-center w-full p-2 pl-4">
            <div class="flex items-center justify-start gap-4">
                <span class="material-symbols-rounded text-icon-dt-3 text-gv-dark-text">info</span>
                <p class="text-gv-dark-text text-dt-3">Details</p>
            </div>
            <button
                type="button"
                class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2"
                onclick={closeViewer}
            >
                <span class="material-symbols-rounded text-icon-dt-3">close</span>
            </button>
        </div>
        <div class="flex items-center justify-start w-full pl-4">
            <Chip text={categoryMap[data.type] || "Unbekannt"} fontSize="6" />
        </div>
        <div class="flex flex-col items-start justify-start w-full pl-4 pr-4 p-2 gap-4">
            <div class="flex items-center justify-start w-full">
                <div class="flex items-center justify-start gap-2">
                    <span class="material-symbols-rounded text-gv-dark-text text-icon-dt-5">person</span>
                    <p class="text-gv-dark-text text-dt-5">Author</p>
                </div>
                <p class="text-gv-dark-text text-dt-5 text-nowrap truncate ml-auto">{data.author}</p>
            </div>
            <div class="flex items-center justify-start w-full">
                <div class="flex items-center justify-start gap-2">
                    <span class="material-symbols-rounded text-gv-dark-text text-icon-dt-5">edit_square</span>
                    <p class="text-gv-dark-text text-dt-5">Zuletzt bearbeitet von</p>
                </div>
                <p class="text-gv-dark-text text-dt-5 text-nowrap truncate ml-auto">{data.lastEditedBy}</p>
            </div>
        </div>
        <div class="flex flex-col items-start justify-start w-full pl-4 pr-4 p-2 gap-4">
            <p class="text-gv-dark-text text-dt-4 font-bold">Info</p>
            <div class="flex items-center justify-start w-full">
                <div class="flex items-center justify-start gap-2">
                    <span class="material-symbols-rounded text-gv-dark-text text-icon-dt-5">list</span>
                    <p class="text-gv-dark-text text-dt-5">Länge</p>
                </div>
                <p class="text-gv-dark-text text-dt-5 text-nowrap truncate ml-auto">
                    {`${data.wordCount} ${data.wordCount > 1 ? "Wörter" : "Wort"} - ${data.readingTimeInMinutes} ${data.readingTimeInMinutes === 1 ? "Minute" : "Minuten"} lesen`}
                </p>
            </div>
            <div class="flex items-center justify-start w-full">
                <div class="flex items-center justify-start gap-2">
                    <span class="material-symbols-rounded text-gv-dark-text text-icon-dt-5">schedule</span>
                    <p class="text-gv-dark-text text-dt-5">Erstellt</p>
                </div>
                <p class="text-gv-dark-text text-dt-5 text-nowrap truncate ml-auto">{formatISODateString(data.createdAt)}</p>
            </div>
        </div>
        <div class="flex flex-col items-start justify-start w-full pl-4 pr-4 p-2 gap-4">
            <p class="text-gv-dark-text text-dt-4 font-bold">Beschreibung</p>
            <div class="flex items-start justify-start w-full">
                {#if isEditingDescription}
                    <textarea bind:this={descriptionTextAreaRef} bind:value={descriptionDraft}
                              class="resize-y bg-transparent border-none w-full text-gv-dark-text text-dt-5"></textarea>
                {:else}
                    <p class="text-gv-dark-text text-dt-5">{data.description}</p>
                {/if}
                <button
                    class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-1 rounded-2 duration-200"
                    onclick={toggleEditDescription}>
                    <span
                        class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">{isEditingDescription ? "check" : "edit"}</span>
                </button>
                {#if isEditingDescription}
                    <button
                        class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-1 rounded-2 duration-200"
                        onclick={cancelEditDescription}>
                    <span
                        class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">close</span>
                    </button>
                {/if}
            </div>
        </div>
        {#if enableAttachments}
            <div class="flex flex-col items-start justify-start w-full pl-4 pr-4 p-2 gap-4">
                <div class="flex items-center justify-start w-full gap-2">
                    <p class="text-gv-dark-text text-dt-4 font-bold">Anhänge</p>
                    <button
                        class="flex items-center justify-center border-2 border-gv-border rounded-2 cursor-pointer bg-white hover:bg-gv-hover-effect p-1.5 duration-200"
                        data-id={data.id}
                        onclick={handleDownloadAttachments}
                    >
                        <span class="material-symbols-rounded text-gv-dark-text text-icon-dt-6">download</span>
                    </button>
                </div>
                <div class="relative flex items-center justify-start w-full rounded-2">
                    {#if isSavingAttachments}
                        <div
                            class="absolute w-full h-full flex items-center justify-center bg-gv-overlay-blocker rounded-2">
                            <Spinner width="2/5" />
                        </div>
                    {/if}
                    <FileSelector
                        validTypes={["pdf", "png", "jpg", "jpeg", "gif", "mp3", "wav", "midi", "mid", "xml", "musicxml", "mxl", "mscz", "mscx", "sib", "musx", "cap", "capx", "gp", "gp5", "gp3", "gp4", "gpx"]}
                        bind:files={workingState} disabled={isSavingAttachments} wrapContent={true}
                        onChange={saveAttachmentChanges}
                    />
                </div>
            </div>
        {/if}
    </div>
{/if}