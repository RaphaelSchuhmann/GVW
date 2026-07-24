<script>
    import Modal from "./Modal.svelte";
    import { getCategoryCount, getLibraryCategories } from "../services/libraryService.svelte";
    import { addScoreCategory, deleteScoreCategory } from "../services/appSettingsService.svelte";

    let {
        isMobile = false,
        height = "auto",
        width = "1/3",
        ...restProps
    } = $props();

    /** @type {import("./Modal.svelte").default} */
    let modal = null;

    let categoryInput = $state("");
    let categories = $derived(getLibraryCategories(false));

    export function showModal() {
        modal.showModal();
    }
    
    async function submitCategory() {
        const successful = await addScoreCategory($state.snapshot(categoryInput));

        if (successful) {
            categoryInput = "";
        }
    }
</script>

<Modal title="Kategorien" subTitle="Verwaltung von Kategorien" bind:this={modal} isMobile={isMobile}>
    <div class="flex flex-col items-center border-2 border-gv-border rounded-2 mt-5">
        <div class="max-h-90 overflow-y-auto w-full">
            {#each categories as category, i (i)}
                <div class="border-b-2 border-gv-border p-2 w-full flex items-center justify-start gap-1">
                    <p class="text-dt-4 p-2">{category}</p>
                    <p class="text-dt-4 p-2">Lieder: {getCategoryCount(category)}</p>
                    <div class="flex h-full ml-auto items-center justify-end">
                        <button class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2 disabled:opacity-50 disabled:cursor-not-allowed"
                                disabled={getCategoryCount(category) > 0} onclick={async () => await deleteScoreCategory(category)}>
                            <span class="material-symbols-rounded text-icon-dt-4">delete</span>
                        </button>
                    </div>
                </div>
            {/each}
        </div>
        <div class="w-full p-2 flex items-center justify-start gap-1">
            <button class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2"
                    onclick={async () => {if (categoryInput.length > 0) await submitCategory();}}>
                <span class="material-symbols-rounded text-icon-dt-4">{categoryInput.length === 0 ? "add" : "check"}</span>
            </button>
            <input type="text" placeholder="Neue Kategorie" class="text-dt-4 w-full p-2" bind:value={categoryInput}>
        </div>
    </div>
</Modal>

