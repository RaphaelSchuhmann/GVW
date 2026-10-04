<script>
    import { viewport } from "../stores/viewport.svelte.js";
    import Card from "./Card.svelte";

    let {
        items = [],
        breakpoint = 1300,
        emptyText = "Keine Einträge gefunden!",
        header,
        row,
        mobileItem,
        empty,
        marginTop
    } = $props();

    let isDesktop = $derived(viewport.width > breakpoint);
</script>

<Card padding="0" marginTop={marginTop} borderThickness={isDesktop ? "2" : "1"}>
    <div class="flex-1 min-h-0 overflow-y-auto w-full">
        {#if items.length > 0}
            {#if isDesktop}
                <table class="w-full text-left border-gv-border">
                    <thead class="sticky top-0 z-10 bg-white min-[1300px]:text-dt-4 text-dt-6 text-gv-dark-text">
                    <tr>
                        {#if header}
                            {@render header()}
                        {/if}
                    </tr>
                    </thead>
                    <tbody>
                    {#each items as item, index (item.id)}
                        {#if row}
                            {@render row(item, index)}
                        {/if}
                    {/each}
                    </tbody>
                </table>
            {:else}
                <div class="flex flex-col w-full">
                    {#each items as item, index (item.id)}
                        {#if mobileItem}
                            {@render mobileItem(item, index)}
                        {/if}
                    {/each}
                </div>
            {/if}
        {:else}
            {#if empty}
                {@render empty()}
            {:else}
                <p class="text-dt-3 text-gv-dark-text text-center w-full h-full p-10 font-semibold">
                    {emptyText}
                </p>
            {/if}
        {/if}
    </div>
</Card>