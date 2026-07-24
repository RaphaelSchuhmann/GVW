<script>

    let {
        selected = false,
        children,
        ...restProps
    } = $props();
</script>

<button
    class={`p-2 w-full text-gv-dark-text cursor-pointer flex items-center justify-start duration-50
            ${selected
                ? "border-l-3 border-gv-primary text-gv-primary"
                : "hover:border-l-3 hover:border-gv-input-placeholder hover:text-gv-input-placeholder"}
          `}
    {...restProps}
>
    {#if children}
        {@render children()}
    {/if}
</button>