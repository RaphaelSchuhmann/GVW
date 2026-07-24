<script>
    import { addToast } from "../../stores/toasts.svelte.js";
    import { viewport } from "../../stores/viewport.svelte.js";
    import SanitizedHTML from "../SanitizedHTML.svelte";

    let {
        options = [],
        optionMap = {},
        selected = $bindable(""),
        onChange = (val) => {
        },
        width = "1/5",
        fillHeight = false,
        disabled = false,
        ...respProps
    } = $props();

    let dropdownRef = null;
    let open = $state(false);

    const HTML_TAG_REGEX = /<[^>]*>/g;

    function getVisualLength(html) {
        const textOnly = html.replace(HTML_TAG_REGEX, "");
        const hasIcon = html.includes("material-symbols-rounded");
        return textOnly.length + (hasIcon ? 4 : 0);
    }

    const minWidth = $derived.by(() => {
        if (options.length === 0) return 0;

        let maxVisualLength = 0;

        for (const html of options) {
            const length = getVisualLength(html);
            if (length > maxVisualLength) {
                maxVisualLength = length;
            }
        }

        return Math.max(maxVisualLength * 8, 30);
    });


    $effect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef && !dropdownRef.contains(event.target)) {
                open = false;
            }
        };

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    });

    function selectOption(option) {
        if (!optionMap || !optionMap[option]) {
            addToast({
                title: "Ein unerwarteter Fehler ist aufgetreten",
                subTitle: viewport.isMobile ? "" : "Beim verarbeiten ihrer Auswahl ist ein Fehler aufgetreten",
                type: "error"
            });
            return;
        }

        selected = option;
        open = false;

        onChange?.($state.snapshot(optionMap[option]));
    }

    function handleSelectOption(e) {
        e.preventDefault();
        const option = e.currentTarget.dataset.option;
        selectOption(option);
    }

    function toggleOpen(e) {
        e.preventDefault();
        open = !open;
    }
</script>

<div class={`flex flex-col items-start justify-start gap-2 ${fillHeight ? "h-full" : ""}`}
     {...minWidth > 0 ? { style: `min-width: ${minWidth}px` } : {}} bind:this={dropdownRef}>
    <div class={`relative inline-block w-full ${fillHeight ? "h-full" : ""}`}>
        <button
            class={`flex items-center justify-start cursor-pointer bg-gv-input-bg rounded-2 p-2 pl-2 pr-4 hover:opacity-85 transition-50 h-full ${fillHeight ? "h-full" : ""} disabled:cursor-not-allowed disabled:opacity-50`}
            {...minWidth > 0 ? { style: `min-width: ${minWidth}px` } : {}}
            disabled={disabled}
            onmousedown={toggleOpen}
        >
            <SanitizedHTML htmlContent={selected} />
        </button>

        {#if open && options.length > 0}
            <div
                class="rounded-2 mt-1 absolute w-full bg-gv-input-bg max-h-[20vh] flex flex-col items-center z-999 overflow-y-auto"
                {...minWidth > 0 ? { style: `min-width: ${minWidth}px` } : {}}
            >
                {#each options as option, i (i)}
                    <button
                        type="button"
                        class={`flex items-center text-left p-2 pl-4 pr-4 cursor-pointer hover:bg-gv-hover-effect w-full rounded-1 text-nowrap`}
                        data-option={option}
                        onmousedown={handleSelectOption}
                    >
                        <SanitizedHTML htmlContent={option} />
                    </button>
                {/each}
            </div>
        {/if}
    </div>
</div>
