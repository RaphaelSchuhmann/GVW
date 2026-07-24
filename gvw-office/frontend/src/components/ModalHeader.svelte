<script>
    let {
        title = "",
        subTitle = "",
        hideSubTitle = false,
        onclick,
    } = $props();

    let closeBtn = null;

    /**
     * Handles keyboard events
     */
    function handleKeyDown(event) {
        if (event.key === "Escape") {
            closeBtn?.click();
        }
    }
    
    $effect(() => {
        window.addEventListener("keydown", handleKeyDown);
        return () => {
            window.removeEventListener("keydown", handleKeyDown);
        };
    });
</script>

<div class={`w-full flex ${hideSubTitle ? "items-center" : "items-start"}`}>
    <div class="flex flex-col items-start justify-around">
        <p class="min-[1500px]:text-dt-3 text-dt-4 max-[470px]:text-dt-5 text-gv-dark-text">
            {title}
        </p>
        {#if !hideSubTitle}
            <p class="min-[1500px]:text-dt-6 text-dt-7 text-gv-light-text">
                {subTitle}
            </p>
        {/if}
    </div>

    <button
        type="button"
        class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2"
        bind:this={closeBtn}
        {onclick}
    >
        <span class="material-symbols-rounded text-icon-dt-2">close</span>
    </button>
</div>