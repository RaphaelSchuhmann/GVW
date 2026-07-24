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

    function handleInput(e) { item.data = e.currentTarget.innerHTML; }
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
        class="w-full text-base text-gv-dark-text outline-none whitespace-normal break-all overflow-wrap-anywhere border-l-2 border-gv-border-bar pl-2"
        class:select-none={!isEditing}
        data-id={item.id}
        onkeydown={handleKeyDown}
        use:setup={item}
        onfocus={handleFocus}
        onblur={handleBlur}
        oninput={handleInput}
        onpaste={handlePaste}
        ondrop={handleDrop}
    ></div>
{:else}
    <div class="w-full text-base text-gv-dark-text outline-none whitespace-normal break-all overflow-wrap-anywhere border-l-2 border-gv-border-bar pl-2">
        <SanitizedHTML htmlContent={item.data} />
    </div>
{/if}