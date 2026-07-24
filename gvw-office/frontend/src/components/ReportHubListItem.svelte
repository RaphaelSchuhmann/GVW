<script>
    import Chip from "./Chip.svelte";
    import Spinner from "./Spinner.svelte";

    let {
        id = "",
        title = "",
        chipText = "",
        isMobile = false,
        asyncDeleteFunction = () => {},
        onclick = () => {},
        ...restProps
    } = $props();

    let isDeleting = $state(false);

    async function deleteItem() {
        if (!id) return;
        isDeleting = true;

        try {
            await asyncDeleteFunction(id);
        } finally {
            isDeleting = false;
        }
    }

    function handleOnClick(event) {
        const id = event.currentTarget.dataset.id;
        onclick(id);
    }
</script>

<div class="w-full p-3 pl-4 flex items-center justify-start gap-2 border-b-2 border-gv-border">
    {#if isDeleting}
        <div class="flex justify-center items-center w-full">
            <Spinner/>
        </div>
    {:else}
        <button class="w-full flex items-center justify-between cursor-pointer" data-id={id} onclick={handleOnClick}>
            <span class="text-dt-6 text-gv-dark-text">{title}</span>
            <Chip text={chipText} />
        </button>
        <button
            aria-label="Eintrag entfernen"
            title="Eintrag entfernen"
            type="button"
            onclick={async (e) => { e.stopPropagation(); await deleteItem() }}
            class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2 hover:text-gv-delete-hover duration-100"
        >
            <span class="material-symbols-rounded text-icon-dt-5">delete</span>
        </button>
    {/if}
</div>