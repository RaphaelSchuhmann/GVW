<script>
    import { marginMap } from "../lib/dynamicStyles";

    let {
        contents = [],
        selected = $bindable(""),
        marginTop = "",
        onChange = undefined,
        disabled = false,
        ...restProps
    } = $props();

    let lastEmittedSelection = $state("");

    $effect(() => {
        if (selected) {
            if (selected !== lastEmittedSelection) {
                lastEmittedSelection = selected;
                onChange?.(selected);
            }
        }
    });

    const selectedIndex = $derived(contents.findIndex(tab => tab === selected));

    function handleSelect(e) {
        if (disabled) return;
        selected = e.currentTarget.dataset.title;
    }
</script>

<div
        class={`relative grid w-full p-1 rounded-full bg-gv-input-bg ${marginMap[marginTop]} gap-2`}
        style={`grid-template-columns: repeat(${contents.length}, minmax(0, 1fr));`}
        {...restProps}
>
    {#if selectedIndex >= 0}
        <div
                class="absolute top-1 bottom-1 left-1 rounded-full bg-white shadow-sm transition-transform duration-300 ease-out z-0"
                style={`width: calc((100% - 0.5rem - (${contents.length - 1} * 0.5rem)) / ${contents.length}); transform: translateX(calc(${selectedIndex * 100}% + ${selectedIndex * 0.5}rem));`}
        ></div>
    {/if}

    {#each contents as title, i (i)}
        <button
                type="button"
                {disabled}
                class={`relative z-10 w-full p-1 rounded-full text-center text-dt-5 text-gv-dark transition-colors duration-150 ${
                disabled ? 'cursor-not-allowed opacity-60' : 'cursor-pointer hover:bg-gv-hover-effect/50'
            }`}
                data-title={title}
                onclick={handleSelect}
        >
            <span class="truncate block w-full">{title}</span>
        </button>
    {/each}
</div>