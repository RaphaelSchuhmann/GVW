<script>
    import Card from "./Card.svelte";
    import { triggerFileDownload } from "../services/utils.js";

    let {
        filename = "",
        blob
    } = $props();

    const downloadable = $state(filename.includes("."));
    let url = $state(null);

    $effect(() => {
        if (!blob) return;
        url = URL.createObjectURL(blob);

        return () => {
            URL.revokeObjectURL(url);
        };
    });
</script>

<Card>
    <div class="flex flex-col w-full items-center justify-start gap-2 overflow-y-auto">
        <div class="flex items-center justify-start w-full">
            <p class="text-gv-dark-text text-dt-4 max-[1000px]:text-dt-5 font-medium w-full text-nowrap truncate">{filename}</p>
            {#if downloadable}
                <button
                    class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2"
                    onclick={() => triggerFileDownload(blob, filename)}
                    >
                    <span class="material-symbols-rounded text-icon-dt-5">download</span>
                </button>
            {/if}
        </div>
        <img src={url} alt={filename} class="rounded-2 w-full">
    </div>
</Card>