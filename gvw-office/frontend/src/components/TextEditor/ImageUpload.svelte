<script>
    import { insertImageBlock } from "../../services/textEditorService.svelte";

    let {
        selected = $bindable(),
        disabled = false,
        activeBlock,
        items,
        ...restProps
    } = $props();

    let targetBlockId = null;

    let isOpen = $state(false);
    let menuRef = null;

    const validTypes = ["png", "jpg", "jpeg"];

    let acceptString = $derived.by(() => {
        let result = "";
        for (let i = 0; i < validTypes.length; i++) {
            result += (i === 0 ? "." : ",.") + validTypes[i];
        }
        return result;
    });

    function selectImage() {
        const input = document.createElement("input");
        input.type = "file";
        input.accept = acceptString;
        input.multiple = false;

        input.onchange = () => {
            const file = input.files?.[0];
            if (!file) return;

            selected = file;
            isOpen = false;

            const index = items.findIndex(i => i.id === targetBlockId);
            if (index === -1) return;

            insertImageBlock(selected, items, index);

            targetBlockId = null;
        };

        input.click();
    }

    function toggleOverlay() {
        isOpen = !isOpen;

        if (activeBlock) {
            targetBlockId = activeBlock;
        }
    }

    $effect(() => {
        const handleClickOutside = (event) => {
            if (menuRef && !menuRef.contains(event.target)) {
                isOpen = false;
            }
        };

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    });

    function handleSelectImage(e) {
        e.preventDefault();
        selectImage();
    }

    function handleToggleOverlay(e) {
        e.preventDefault();
        toggleOverlay();
    }
</script>

<div class={`flex flex-col items-start justify-start gap-2`} bind:this={menuRef}>
    <div class={`relative inline-block w-full`}>
        <button
            class={`p-2 flex items-center justify-center cursor-pointer rounded-2 hover:bg-gv-hover-effect text-gv-dark-text disabled:text-gv-dark-text/50 disabled:cursor-not-allowed`}
            onmousedown={handleToggleOverlay}
            disabled={disabled}
        >
            <span class="material-symbols-rounded text-icon-dt-4">image</span>
        </button>

        {#if isOpen}
            <div
                class={`rounded-2 mt-1 absolute w-min bg-white drop-shadow-md flex flex-col items-center z-999 p-2`}
            >
                <button
                    class="flex items-center justify-center gap-4 cursor-pointer hover:bg-gv-hover-effect text-gv-dark-text p-2 pl-4 pr-4 rounded-2"
                    onmousedown={handleSelectImage}
                >
                    <span class="material-symbols-rounded text-icon-dt-6">upload</span>
                    <span class="text-dt-6 text-nowrap">Bild Hochladen</span>
                </button>
            </div>
        {/if}
    </div>
</div>