<script>
    import { chipColorMap, fontSizeMap } from "../lib/dynamicStyles";

    let {
        text = "",
        fontSize = "7",
        colorType = "default",
        ...restProps
    } = $props();
</script>

<p class={`${fontSizeMap[fontSize]} font-medium ${chipColorMap[colorType]} p-1 pl-3 pr-3 rounded-full text-center w-min text-nowrap`}>{text}</p>