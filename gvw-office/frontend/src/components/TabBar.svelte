<script>
    import { marginMap } from "../lib/dynamicStyles";
    import { crossfade } from 'svelte/transition';
    import { cubicOut } from 'svelte/easing';

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

    const [send, receive] = crossfade({
        duration: 300,
        easing: cubicOut
    });

    function handleSelect(e) { selected = e.currentTarget.dataset.title; }
</script>

<div class={`flex p-1 rounded-full bg-gv-input-bg gap-2 ${marginMap[marginTop]} overflow-x-auto w-full min-h-min`}>
    {#each contents as title, i (i)}
        <button
            type="button"
            disabled={disabled}
            class={`relative flex-1 p-2 rounded-full text-center z-10 text-dt-6 text-gv-dark transition-colors duration-150 disabled:cursor-not-allowed ${
                disabled ? 'cursor-default' : 'cursor-pointer hover:bg-gv-hover-effect/50'
            }`}
            data-title={title}
            onclick={handleSelect}
        >
            <span class="relative z-20">
                {title}
            </span>

            {#if selected === title}
                <div
                    class="absolute inset-0 bg-white rounded-full shadow-sm z-10"
                    in:receive={{ key: 'active-tab' }}
                    out:send={{ key: 'active-tab' }}
                ></div>
            {/if}
        </button>
    {/each}
</div>