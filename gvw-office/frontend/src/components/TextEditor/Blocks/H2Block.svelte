<script>
    import SanitizedHTML from "../../SanitizedHTML.svelte";

    let {
        item = $bindable(),
        isEditing,
        handleKeyDown,
        setup,
        activeBlock = $bindable(),
        handlePaste,
        handleDrop,
        ...restProps
    } = $props();

    function handleFocus() { activeBlock = item.id; }
    function handleBlur() { activeBlock = null; }
</script>

{#if isEditing}
    <div
        contenteditable="true"
        role="textbox"
        tabindex="0"
        aria-multiline="true"
        aria-label="Edit text content"
        class="w-full leading-none text-gv-dark-text outline-none whitespace-normal break-all overflow-wrap-anywhere text-dt-h2 font-bold"
        class:select-none={!isEditing}
        data-id={item.id}
        onkeydown={handleKeyDown}
        use:setup={item}
        onfocus={handleFocus}
        onblur={handleBlur}
        onpaste={handlePaste}
        ondrop={handleDrop}
    ></div>
{:else}
    <div class="w-full leading-none text-gv-dark-text outline-none whitespace-normal break-all overflow-wrap-anywhere text-dt-h2 font-bold">
        <SanitizedHTML htmlContent={item.data} />
    </div>
{/if}