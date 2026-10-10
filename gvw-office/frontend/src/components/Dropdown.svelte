<script>
    import { marginMap, paddingMap } from "../lib/dynamicStyles";
    import { capitalizeWords } from "../services/utils.js";

    let {
        selected = $bindable("wählen"),
        options = [],
        marginTop = "",
        padding = "2",
        title = "",
        bgWhite = false,
        disableMinWidth = false,
        onChange = undefined,
        textWrap = true,
        displayTop = false,
        doCapitalizeWords = true,
        showDropshadow = false,
        fillWidth = true,
        usePortal = true,
        ...restProps
    } = $props();

    let open = $state(false);
    let dropdownRef = $state(null);
    let buttonRef = $state(null); // Ref specifically for the button bounding rect
    let menuRef = $state(null);

    const minWidth = $derived.by(() => {
        if (disableMinWidth || options.length === 0) return 0;

        let longestLength = 0;

        for (const option of options) {
            if (option.length > longestLength) {
                longestLength = option.length;
            }
        }

        return Math.max(longestLength * 8 + 80, 120);
    });


    function calculatePortalPosition() {
        if (!buttonRef || !menuRef) return;

        const rect = buttonRef.getBoundingClientRect();
        const menuHeight = menuRef.getBoundingClientRect().height;

        Object.assign(menuRef.style, {
            position: "fixed",
            top: `${displayTop ? rect.top - menuHeight : rect.bottom}px`,
            left: `${rect.left}px`,
            width: `${Math.max(rect.width, minWidth)}px`,
            minWidth: "0",
            boxSizing: "border-box",
            margin: "0",
            zIndex: "99999"
        });
    }


    function selectOption(event) {
        const option = event.currentTarget.dataset.option;
        selected = option;
        open = false;

        onChange?.($state.snapshot(option));
    }

    $effect(() => {
        const handleClickOutside = (event) => {
            if (
                dropdownRef &&
                !dropdownRef.contains(event.target) &&
                !event.target.closest(".dropdown-portal-menu")
            ) {
                open = false;
            }
        };

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    });

    function toggleDropdown() {
        open = !open;
        if (!open || options.length === 0) return;

        requestAnimationFrame(() => {
            calculatePortalPosition();

            if (selected.toLowerCase() === "wählen") return;

            const index = options.indexOf(selected);
            if (index === -1) return;

            const selectedElement = menuRef?.children[index];
            selectedElement?.scrollIntoView({ block: "center", behavior: "smooth" });
        });
    }

    function portal(node) {
        document.body.appendChild(node);
        return {
            destroy() {
                if (node.parentNode) {
                    node.parentNode.removeChild(node);
                }
            }
        };
    }

    const dropShadow = $derived(displayTop ? "drop-shadow-[0_-15px_5px_rgba(0,0,0,0.2)]" : "drop-shadow-[0_15px_2px_rgba(0,0,0,0.2)]");
</script>

<div
    class={`flex flex-col items-start ${fillWidth ? 'w-full' : 'w-auto'} ${marginMap[marginTop]} gap-1`}
    bind:this={dropdownRef}
    {...restProps}
>
    {#if title}
        <p class="text-dt-6">{title}</p>
    {/if}

    <div class="relative inline-block w-full">
        <button
            bind:this={buttonRef}
            type="button"
            class={`flex items-center w-full ${bgWhite ? "bg-white" : "bg-gv-input-bg"} ${open && options.length > 0 ? !displayTop ? "rounded-t-1" : "rounded-b-1" : "rounded-1"} text-dt-6 ${paddingMap[padding]} pl-3 pr-3 cursor-pointer text-gv-dark-text hover:bg-gv-hover-effect`}
            {...minWidth > 0 ? { style: `min-width: ${minWidth}px` } : {}}
            onclick={toggleDropdown}
        >
            <div class="flex w-full">
                <p class={`${selected === "wählen" ? "text-gv-input-placeholder" : "text-gv-dark-text"} ${textWrap ? "text-wrap" : "text-nowrap"}`}>
                    {doCapitalizeWords ? capitalizeWords(selected) : selected}
                </p>
                <span class="material-symbols-rounded ml-auto">
                    {open ? "arrow_drop_up" : "arrow_drop_down"}
                </span>
            </div>
        </button>

        {#if open && options.length > 0}
            {#if usePortal}
                <div
                    use:portal
                    bind:this={menuRef}
                    class={`dropdown-portal-menu ${showDropshadow ? dropShadow : ""} ${bgWhite ? "bg-white" : "bg-gv-input-bg"} ${displayTop ? "rounded-t-1" : "rounded-b-1"} max-h-[20vh] flex flex-col items-center overflow-y-auto`}
                >
                    {@render dropdownItems()}
                </div>
            {:else}
                <div
                    bind:this={menuRef}
                    class={`absolute ${showDropshadow ? dropShadow : ""} w-full ${bgWhite ? "bg-white" : "bg-gv-input-bg"} ${displayTop ? "bottom-10.5 rounded-t-1" : "rounded-b-1"} max-h-[20vh] flex flex-col items-center z-999 overflow-y-auto`}
                    {...minWidth > 0 ? { style: `min-width: ${minWidth}px` } : {}}
                >
                    {@render dropdownItems()}
                </div>
            {/if}
        {/if}
    </div>
</div>

{#snippet dropdownItems()}
    {#each options as option, i (i)}
        <button
            type="button"
            class={`text-left p-2 pl-4 pr-4 cursor-pointer hover:bg-gv-hover-effect w-full self-stretch rounded-1 ${textWrap ? "text-wrap" : "text-nowrap"}`}
            data-option={option}
            onclick={selectOption}
        >
            {doCapitalizeWords ? capitalizeWords(option) : option}
        </button>
    {/each}
{/snippet}
