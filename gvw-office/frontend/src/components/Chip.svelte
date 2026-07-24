<script>
    import { fontSizeMap } from "../lib/dynamicStyles";

    let {
        text = "",
        fontSize = "7",
        ...restProps
    } = $props();
</script>

<p class={`${fontSizeMap[fontSize]} text-gv-secondary-text bg-gv-secondary p-1 pl-3 pr-3 rounded-full text-center w-min text-nowrap`}>{text}</p>