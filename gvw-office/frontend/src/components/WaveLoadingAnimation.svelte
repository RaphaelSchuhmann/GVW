<script>
    import { widthMap, heightMap } from "../lib/dynamicStyles.js";

    let {
        title = "",
        subTitle = "",
        width = "1/2",
        height = "",
        ...restProps
    } = $props();
</script>

<div class="flex flex-col items-center justify-center gap-10 {widthMap[width]} {heightMap[height]}">
    <div class="bars-loader">
        <div class="bar"></div>
        <div class="bar"></div>
        <div class="bar"></div>
        <div class="bar"></div>
        <div class="bar"></div>
    </div>
    {#if title}
        <div class="flex flex-col items-center">
            <p class="text-dt-1 text-gv-dark-turquoise text-center font-semibold">{title}</p>

            {#if subTitle}
                <p class="text-dt-3 text-gv-light-text text-center">{subTitle}</p>
            {/if}
        </div>
    {/if}
</div>
