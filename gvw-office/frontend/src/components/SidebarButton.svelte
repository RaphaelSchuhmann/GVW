<script>
    import { marginMap, fontSizeMap } from "../lib/dynamicStyles";

    let {
        selected = false,
        marginTop = "",
        minimized = false,
        hugWidth = false,
        rounded2 = false,
        fontSize = "3",
        children,
        ...restProps
    } = $props();
</script>

<button
    class={`${hugWidth ? "w-auto p-2 pr-4" : "w-full p-2"} ${marginMap[marginTop]} ${rounded2 ? "rounded-2" : "rounded-1"}
           text-black ${fontSizeMap[fontSize]} cursor-pointer flex items-center
           ${minimized ? "justify-center aspect-square" : "justify-start gap-2"}
           ${selected ? "bg-gv-primary text-white" : "hover:bg-gv-secondary hover:text-gv-secondary-text"}`}
    {...restProps}
>
    {#if children}
        {@render children()}
    {/if}
</button>