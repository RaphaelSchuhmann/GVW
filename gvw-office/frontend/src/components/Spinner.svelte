<script>
    import Spinner_SVG from "../assets/Spinner_SVG.svg";
    import Spinner_SVG_Light from "../assets/Spinner_SVG_Light.svg";
    import { widthMap, heightMap } from "../lib/dynamicStyles.js";

    let {
        title = "",
        subTitle = "",
        light = false,
        width = "3/5",
        height = "",
        simple = false,
        ...restProps
    } = $props();
</script>

{#if simple}
    <img src={light ? Spinner_SVG_Light : Spinner_SVG} alt="spinner_svg" class={`animate-spin ${widthMap[width]} ${heightMap[height]}`} />
{:else}
    <div class="flex flex-col items-center gap-5 w-1/5">
        <img src={light ? Spinner_SVG_Light : Spinner_SVG} alt="spinner_svg" class={`animate-spin ${widthMap[width]} ${heightMap[height]}`} />
        {#if title}
            <div class="flex flex-col items-center">
                <p class="text-dt-1 text-gv-dark-turquoise text-center font-semibold">{title}</p>

                {#if subTitle}
                    <p class="text-dt-3 text-gv-light-text text-center">{subTitle}</p>
                {/if}
            </div>
        {/if}
    </div>
{/if}
