<script>
    import { flip } from "svelte/animate";
    import { viewport } from "../stores/viewport.svelte.js";

    let {
        itemComponent: ItemComponent,
        list,
        usePadding = true,
        ...restProps
    } = $props();

    let items = $derived(list.data);

    let expandedId = $state(null);

    function toggleItem(id) {
        if (expandedId === id) {
            expandedId = null;
        } else {
            expandedId = id;
        }
    }
</script>

<div
    class={`w-full h-full flex flex-col items-center overflow-y-auto gap-4 overflow-x-hidden ${viewport.isMobile && usePadding ? "p-1" : "p-2"}`}>
    {#if items.length !== 0}
        {#each items as item, i (i)}
            <div animate:flip={{ duration: 300 }} class="w-full">
                <ItemComponent data={item} expanded={expandedId === item.id} onToggle={toggleItem} />
            </div>
        {/each}
    {:else}
        <p class="text-dt-6 text-gv-dark-text text-left w-full">Keine Daten gefunden...</p>
    {/if}
</div>