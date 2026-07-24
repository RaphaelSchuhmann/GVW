<script>
    import { slide } from 'svelte/transition';
    import { cubicOut } from 'svelte/easing';

    let {
        expanded = $bindable(false),
        title = "",
        children,
        ...restProps
    } = $props();

    function toggleExpanded() { expanded = !expanded; }
</script>

<div class="w-full flex flex-col gap-2 p-4 items-start justify-start rounded-2 shadow-md bg-white">
    <div class="flex w-full items-center">
        <button
            type="button"
            class="flex flex-1 items-center cursor-pointer text-left gap-2"
            onclick={toggleExpanded}
        >
            <span class="text-dt-5 text-gv-dark-text font-semibold text-nowrap truncate">{title}</span>
            <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text ml-auto">
                { expanded ? "arrow_drop_up" : "arrow_drop_down" }
            </span>
        </button>
    </div>
    
    {#if expanded}
        <div transition:slide={{ duration: 200, easing: cubicOut }} class="w-full max-h-[30vh] overflow-y-auto mt-2">
            {@render children?.()}
        </div>
    {/if}
</div>