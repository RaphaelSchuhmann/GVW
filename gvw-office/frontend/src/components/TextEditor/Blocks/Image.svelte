<script>
    import { deleteBlock, getDocumentImage, previewUrls } from "../../../services/textEditorService.svelte.js";
    import Spinner from "../../Spinner.svelte";

    let {
        imageId,
        reportId,
        blockId,
        content,
        alt = "Bericht bild",
        isEditing = false,
        isMobile = false,
        ...restProps
    } = $props();

    let srcUrl = $state(null);
    let isLoading = $state(true);

    $effect(() => {
        isLoading = true;

        async function loadImage() {
            if (!imageId) {
                isLoading = false;
                return;
            }

            if (imageId.startsWith("temp_")) {
                const url = previewUrls.get(imageId);

                srcUrl = url ?? null;
                isLoading = false;
                return;
            }

            if (!reportId) return;

            const url = await getDocumentImage(reportId, imageId);
            if (!url) return;

            srcUrl = url ?? null;
            isLoading = false;
        }

        loadImage();
    });

    function handleDeleteBlock() { deleteBlock(content, blockId, true); }
</script>

{#if isLoading}
    <div class="flex items-center justify-center rounded-2 w-full border-2 border-gv-border p-10">
        <Spinner />
    </div>
{:else}
    <div class="relative group rounded-2 border-2 border-gv-border hover:border-gv-border-bar">
        {#if isEditing}
            <button
                class="absolute top-2 right-2 hidden group-hover:flex items-center justify-center cursor-pointer bg-gv-primary/20 rounded-2 border border-gv-primary p-2 text-gv-dark-text hover:text-gv-delete-hover"
                onclick={handleDeleteBlock}
            >
                <span class="material-symbols-rounded">delete</span>
            </button>
        {/if}
        {#if isMobile}
            <a href={srcUrl} target="_blank" rel="noopener noreferrer">
                <img src={srcUrl} alt={alt} class="rounded-2" />
            </a>
        {:else}
            <img src={srcUrl} alt={alt} class="rounded-2" />
        {/if}
    </div>
{/if}