<script>
    import { paddingMap } from "../lib/dynamicStyles";
    import { widthMap, heightMap } from "../lib/dynamicStyles";

    let {
        height = "3/5",
        width = "1/3",
        padding = "5",
        children = undefined,
        ...restProps
    } = $props();
</script>
<div class={`${widthMap[width]} ${heightMap[height]} rounded-1 bg-white border-2 border-gv-border flex flex-col items-center ${paddingMap[padding]}`}>
    {@render children()}
</div>