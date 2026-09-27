<script>
    import { marginMap } from "../lib/dynamicStyles";
    import { addToast } from "../stores/toasts.svelte";
    import { viewport } from "../stores/viewport.svelte";
    import { filePreviewable, supportedFileTypes } from "../services/fileService.svelte.js";
    import { triggerFileDownload } from "../services/utils.js";
    import FilePreviewModal from "./FilePreviewModal.svelte";

    let {
        title = "",
        marginTop = "",
        page = "library",
        documentId = "",
        validTypes = supportedFileTypes,
        files = $bindable([]),
        wrapContent = false,
        disabled = false,
        fileDownloadDisabled = false,
        allowEditing = false,
        onChange = (val) => {},
        ...restProps
    } = $props();

    const activePage = page || "library";
    const icon = activePage === "library" ? "audio_file" : "draft";
    const acceptString = $derived.by(() => {
        let result = "";
        for (let i = 0; i < validTypes.length; i++) {
            result += (i === 0 ? "." : ",.") + validTypes[i];
        }
        return result;
    });

    let previewModal = null;

    /**
     * Handles file selection via a dynamic input element
     */
    async function addFile() {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = acceptString;
        input.multiple = false;

        input.onchange = () => {
            const file = input.files?.[0];
            if (!file) return;

            const duplicate = files.some((entry) => {
                const existingName = typeof entry === "string" ? entry : entry.name;
                return existingName === file.name;
            });

            if (!duplicate) {
                files = [...files, file];
                onChange?.(files);
            } else {
                addToast({
                    title: "Datei wird schon verwendet",
                    subTitle: !viewport.isMobile
                        ? "Die von ihnen ausgewählte Datei ist bereits im Anhang."
                        : "",
                    type: "warning"
                });
            }
        };

        input.click();
    }

    async function handleFileClick(file) {
        if (allowEditing) {
            removeFile(file);
            return;
        }

        if (fileDownloadDisabled || !documentId || !file.id) return;

        const previewableFileObject = await filePreviewable(page, documentId, file.id);

        if (!previewableFileObject.isPreviewable) {
            triggerFileDownload(previewableFileObject.blob, previewableFileObject.filename);
            return;
        }

        const fileObject = {
            isPreviewable: previewableFileObject.isPreviewable,
            title: file.name,
            extension: previewableFileObject.extension,
            blob: previewableFileObject.blob,
        }

        previewModal.showModal();
        previewModal.handlePreview(fileObject);
    }

    /**
     * Removes a file from the list
     * @param {Object} file
     */
    function removeFile(file) {
        files = files.filter((f) => f !== file);
        onChange?.(files);
    }
</script>

<FilePreviewModal bind:this={previewModal} />

<div class={`flex flex-col items-start justify-start gap-1 w-full ${marginMap[marginTop]}`} {...restProps}>
    {#if title}
        <p class="text-dt-6 font-medium">{title}</p>
    {/if}
    <div class="flex-1 min-w-0 overflow-x-auto w-full">
        <div class={`flex items-center justify-start gap-2 ${wrapContent ? "flex-wrap" : "flex-nowrap"}`}>
            {#if !disabled && allowEditing}
                <button
                    type="button"
                    class="shrink-0 flex items-center justify-center rounded-2 border-2 border-gv-border p-2 cursor-pointer hover:bg-gv-input-bg duration-200"
                    onclick={addFile}
                    aria-label="Datei hinzufügen"
                >
                    <span class="material-symbols-rounded text-dt-6">
                        attach_file_add
                    </span>
                </button>
            {/if}

            {#if files.length > 0}
                {#each files as file (file.name || file)}
                    <button
                        type="button"
                        {disabled}
                        class={`group shrink-0 relative flex items-center justify-center rounded-2 border-2 border-gv-border p-2 cursor-pointer ${!disabled && allowEditing ? "hover:bg-gv-input-bg" : ""} duration-200`}
                        onclick={() => handleFileClick(file)}
                    >
                        <div
                            class={`flex items-center gap-2 whitespace-nowrap transition-opacity duration-200 ${!disabled && allowEditing ? "group-hover:opacity-0" : ""}`}>
                            <span class="material-symbols-rounded text-icon-dt-6">
                                {icon}
                            </span>
                            <p class="text-gv-dark-text text-dt-7">
                                {file.name ? file.name : file}
                            </p>
                        </div>

                        {#if !disabled && allowEditing}
                            <div
                                class="pointer-events-none absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                                <span class="material-symbols-rounded text-red-600 text-icon-dt-6">
                                    attach_file_off
                                </span>
                            </div>
                        {/if}
                    </button>
                {/each}
            {:else if disabled || !allowEditing}
                <div
                    class="group shrink-0 relative flex items-center justify-center rounded-2 border-2 border-gv-border p-2 whitespace-nowrap">
                    <p class="text-gv-dark-text text-dt-7">
                        Keine Dateien angehängt
                    </p>
                </div>
            {/if}
        </div>
    </div>
</div>
