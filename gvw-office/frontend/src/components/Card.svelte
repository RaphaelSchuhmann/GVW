<script>
    import { paddingMap, justifyMap, marginMap, roundedMap } from "../lib/dynamicStyles";

    let {
        padding = "3",
        justify = "justify-start",
        marginTop = "",
        rounded = "1",
        borderThickness = "2",
        fillHeight = false,
        children,
        ...restProps
    } = $props();
</script>

<!-- Note: only valid border thicknesses are 2 and 1 -->
<div class={`bg-white flex flex-col items-center min-h-0 ${justifyMap[justify]} ${roundedMap[rounded]} ${borderThickness === "2" ? "border-2" : "border"} border-gv-border w-full ${fillHeight ? "h-full" : ""} ${marginMap[marginTop]} ${paddingMap[padding]} overflow-hidden`}
     {...restProps}>
    {@render children()}
</div>
