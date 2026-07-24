<script>
    import { marginMap } from "../lib/dynamicStyles";
    import { addToast } from "../stores/toasts.svelte";
    import { viewport } from "../stores/viewport.svelte";

    let {
        title = "",
        marginTop = "",
        page = "library",
        validTypes = [],
        files = $bindable([]),
        disabled = false,
        wrapContent = false,
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
            })

            if (!duplicate) {
                files = [...files, file];
                onChange?.(files);
            } else {
                addToast({
                    title: "Datei wird schon verwendet",
                    subTitle: !viewport.isMobile
                        ? "Die von ihnen ausgewählte Datei ist bereits im Anhang."
                        : "",
                    type: "warning",
                });
            }
        };

        input.click();
    }

    /**
     * Removes a file from the list
     * @param {Event} event
     */
    function removeFile(event) {
        const file = event.currentTarget.dataset.file;
        files = files.filter((f) => f !== file);
        onChange?.(files);
    }
</script>

<div class={`flex flex-col items-start justify-start gap-1 w-full ${marginMap[marginTop]}`} {...restProps}>
    {#if title}
        <p class="text-dt-6 font-medium">{title}</p>
    {/if}
    <div class="flex-1 min-w-0 overflow-x-auto w-full">
        <div class={`flex items-center justify-start gap-2 ${wrapContent ? "flex-wrap" : "flex-nowrap"}`}>
            {#if !disabled}
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
                {#each files as file (file.name)}
                    <button
                        type="button"
                        {disabled}
                        class={`group shrink-0 relative flex items-center justify-center rounded-2 border-2 border-gv-border p-2 ${!disabled ? "hover:bg-gv-input-bg cursor-pointer" : ""} duration-200`}
                        data-file={file}
                        onclick={removeFile}
                    >
                        <div class={`flex items-center gap-2 whitespace-nowrap transition-opacity duration-200 ${!disabled ? "group-hover:opacity-0" : ""}`}>
                            <span class="material-symbols-rounded text-icon-dt-6">
                                {icon}
                            </span>
                            <p class="text-gv-dark-text text-dt-7">
                                {file.name ? file.name : file}
                            </p>
                        </div>

                        {#if !disabled}
                            <div class="pointer-events-none absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                                <span class="material-symbols-rounded text-red-600 text-icon-dt-6">
                                    attach_file_off
                                </span>
                            </div>
                        {/if}
                    </button>
                {/each}
            {:else if disabled}
                <div class={`group shrink-0 relative flex items-center justify-center rounded-2 border-2 border-gv-border p-2`}>
                    <div class={`flex items-center gap-2 whitespace-nowrap`}>
                        <p class="text-gv-dark-text text-dt-7">
                            Keine Dateien angehängt
                        </p>
                    </div>
                </div>
            {/if}
        </div>
    </div>
</div>
