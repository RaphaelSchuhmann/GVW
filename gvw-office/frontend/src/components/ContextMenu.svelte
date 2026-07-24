<script>
    let {
        open = $bindable(false),
        x = 0,
        y = 0,
        children
    } = $props();

    const close = () => (open = false);

    const preventClose = (e) => e.stopPropagation();
</script>

{#if open}
    <button
        type="button"
        class="fixed inset-0 z-40 w-screen h-screen border-none bg-transparent p-0 cursor-default"
        onclick={close}
        aria-label="Close context menu"
    ></button>
    
    <div
        class="fixed z-50 bg-white rounded-1 shadow-md min-w-45"
        {...{ style: `top:${y}px; left:${x}px` }}
        role="menu"
        tabindex="-1"
    >
        {#if children}
            {@render children()}
        {/if}
    </div>
{/if}