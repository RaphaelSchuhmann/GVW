<script>
    import { marginMap } from "../lib/dynamicStyles.js";

    let {
        title = "",
        subTitle = "",
        showSlot = true,
        slotCentered = false,
        marginTop = "",
        hideSubTitle = false,
        children = undefined,
        ...restProps
    } = $props();
</script>

<div class={`w-full flex items-center ${marginMap[marginTop]}`}>
    <div class={`flex flex-col items-start ${showSlot ? "max-w-1/2" : "w-full"}`}>
        <p class="min-[1000px]:text-dt-2 text-dt-3 text-gv-dark-text text-nowrap font-semibold">{title}</p>
        {#if !hideSubTitle}
            <p class="min-[1000px]:text-dt-5 text-dt-6 text-gv-light-text line-clamp-2">{subTitle}</p>
        {/if}
    </div>

    {#if showSlot && children}
        <div class={`flex ${slotCentered ? "items-center" : "items-start"} justify-start gap-2 h-full overflow-hidden ml-auto`}>
            {@render children()}
        </div>
    {/if}
</div>