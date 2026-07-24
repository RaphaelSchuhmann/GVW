<script>
    import { marginMap, fontColorMap, paddingMap } from "../lib/dynamicStyles";

    let {
        type = "primary",
        marginTop = "",
        width = "w-full",
        fontColor = "",
        padding = "2",
        disabled = false,
        isSubmit = false,
        isCancel = false,
        onclick,
        children,
        ...restProps
    } = $props();

    let buttonEl = null;

    // Validate type (using a derived-like approach or just local variable)
    const validTypes = ["primary", "secondary", "contextMenu", "delete"];
    let activeType = $derived(validTypes.includes(type) ? type : "primary");

    /**
     * Keyboard shortcut logic
     * @param {{ key: string; preventDefault: () => void; }} event
     */
    function handleKeyDown(event) {
        if (!isSubmit && !isCancel) return;
        if (activeType === "contextMenu" || activeType === "delete") return;

        if (isSubmit && event.key === "Enter") {
            event.preventDefault();
            buttonEl?.click();
        } else if (isCancel && event.key === "Escape") {
            event.preventDefault();
            buttonEl?.click();
        }
    }

    $effect(() => {
        if (isSubmit || isCancel) {
            window.addEventListener("keydown", handleKeyDown);
            // Cleanup function replaces onDestroy
            return () => window.removeEventListener("keydown", handleKeyDown);
        }
    });

    const baseClasses = "flex items-center justify-center rounded-1 min-[768px]:text-dt-4 min-[470px]:text-dt-5 text-dt-6 duration-200 disabled:opacity-50 disabled:cursor-not-allowed";

    const typeClasses = {
        primary: `bg-gv-primary pl-4 pr-4 text-white hover:bg-gv-primary-hover ${paddingMap[padding]} ${width} ${marginMap[marginTop]}`,
        contextMenu: `bg-transparent w-full pl-4 p-1 pr-4 hover:bg-gv-hover-effect ${fontColorMap[fontColor]}`,
        delete: `bg-gv-semi-transparent-red pl-4 pr-4 text-gv-delete hover:bg-gv-semi-transparent-red-hover disabled:hover:bg-gv-semi-transparent-red-hover ${paddingMap[padding]} ${width} ${marginMap[marginTop]}`,
        secondary: `bg-white border-2 border-gv-border pl-4 pr-4 hover:bg-gv-hover-effect ${paddingMap[padding]} ${width} ${marginMap[marginTop]} ${fontColorMap[fontColor]}`
    };
</script>

<button
    bind:this={buttonEl}
    disabled={disabled}
    class="{baseClasses} {typeClasses[activeType] || typeClasses.primary} cursor-pointer"
    {...restProps}
    {onclick}
>
    {#if children}
        {@render children()}
    {/if}
</button>