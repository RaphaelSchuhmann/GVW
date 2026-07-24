<script>
    import {
        addBlock,
        bulkInsertImageBlocks,
        getActiveStylesInRange,
        handleAutoLink,
        isCaretAtBoundary
    } from "../../services/textEditorService.svelte";
    import { tick } from "svelte";
    import { editorSelectionStore } from "../../stores/textEditorStore.svelte";
    import TextBlock from "./Blocks/TextBlock.svelte";
    import H1Block from "./Blocks/H1Block.svelte";
    import H2Block from "./Blocks/H2Block.svelte";
    import H3Block from "./Blocks/H3Block.svelte";
    import H4Block from "./Blocks/H4Block.svelte";
    import Image from "./Blocks/Image.svelte";
    import BlockQuote from "./Blocks/BlockQuote.svelte";
    import { sanitize } from "../../services/utils.js";
    import { textEditorConfigs } from "../../lib/textEditorConfig.svelte.js";
    import { createEditorHandlers } from "../../services/editorHandlers.js";

    let {
        reportId,
        title = $bindable(""),
        author = "",
        readingTime = "",
        content = $bindable([]),
        isEditing = false,
        activeBlock = $bindable(null),
        configKey,
        isMobile = false,
        ...restProps
    } = $props();

    const showAuthor = textEditorConfigs[configKey].showAuthor || false;
    let draggedIndex = $state(null);

    const contentStore = { get value() { return content; }, set value(v) { content = v; } };
    const { handleKeyDown } = createEditorHandlers(contentStore);

    /**
     * Sets the drag source index when starting a drag operation.
     *
     * Only works in editing mode. Stores the index of the dragged block
     * and configures the DataTransfer object for a move operation.
     *
     * @param {DragEvent} event - The drag start event
     */
    function handleDragStart(event) {
        if (!isEditing) return;
        draggedIndex = Number(event.currentTarget.dataset.index);
        event.dataTransfer.effectAllowed = "move";
    }

    /**
     * Sets the drag source index when starting a drag operation.
     *
     * Only works in editing mode. Stores the index of the dragged block
     * and configures the DataTransfer object for a move operation.
     *
     * @param {DragEvent} event - The drag start event
     */
    function handleDragEnter(event) {
        const index = Number(event.currentTarget.dataset.index);

        if (!isEditing || draggedIndex === null || draggedIndex === index) return;

        const updatedItems = [...content];
        const [draggedItem] = updatedItems.splice(draggedIndex, 1);
        updatedItems.splice(index, 0, draggedItem);

        draggedIndex = index;
        content = updatedItems;
    }

    /**
     * Resets drag state after a drag operation completes.
     *
     * Clears the internal dragged index reference.
     *
     * @returns {void}
     */
    function handleDragEnd() {
        draggedIndex = null;
    }

    /**
     * Initializes a contenteditable block with its HTML content.
     *
     * Sets the initial DOM content and optionally focuses the first block
     * in the editor for improved UX.
     *
     * @param {HTMLElement} node - The DOM node representing the block
     * @param {Object} item - The block data object
     * @param {string} item.id - Unique block identifier
     * @param {string} item.data - HTML content of the block
     *
     * @returns {{ update: Function, destroy: Function }}
     */
    function setup(node, item) {
        node.innerHTML = sanitize(item.data);
        const contentEntryIndex = content.findIndex(i => i.id === item.id);
        if (contentEntryIndex !== -1 && contentEntryIndex === 0) {
            node.focus();
        }

        return { update(newData) {}, destroy() {} };
    }

    /**
     * Handles image paste events inside the editor.
     *
     * Extracts image files from clipboard data and inserts them as
     * new image blocks at the current cursor position.
     *
     * Prevents default paste behavior when images are detected.
     *
     * @param {ClipboardEvent} e - The paste event
     * @returns {void}
     */
    function handlePaste(e) {
        const items = Array.from(e.clipboardData.items);
        const files = items.filter(item => item.type.startsWith("image/")).map(item => item.getAsFile());

        if (files.length === 0) return;

        e.preventDefault();

        const currentIndex = content.findIndex(item => item.id === activeBlock);
        if (currentIndex === -1) return;

        bulkInsertImageBlocks(files, content, currentIndex);
    }

    /**
     * Handles drag-and-drop image uploads into the editor.
     *
     * Extracts image files from the drop event and inserts them
     * into the document at the active block position.
     *
     * Prevents default browser drop behavior.
     *
     * @param {DragEvent} e - The drop event
     * @returns {void}
     */
    function handleDrop(e) {
        e.preventDefault();

        if (e.dataTransfer.items) {
            const currentIndex = content.findIndex(item => item.id === activeBlock);
            if (currentIndex === -1) return;

            const files = [];

            for (let i = 0; i < e.dataTransfer.items.length; i++) {
                if (e.dataTransfer.items[i].kind === "file") {
                    const file = e.dataTransfer.items[i].getAsFile();
                    if (file && file.type.startsWith("image/")) {
                        files.push(file);
                    }
                }
            }

            if (files.length > 0) {
                bulkInsertImageBlocks(files, content, currentIndex);
            }
        }
    }

    /**
     * Tracks the current text selection and updates the editor selection store.
     *
     * Runs on every `selectionchange` event and determines:
     * - Active block ID
     * - Current selection range
     * - Active formatting styles (bold, italic, underline)
     * - Block type context
     *
     * Keeps toolbar state in sync with the user's selection.
     */
    $effect(() => {
        const handleSelection = () => {
            const sel = window.getSelection();
            if (!sel || sel.rangeCount === 0) return;

            const range = sel.getRangeAt(0);

            const editable = range.commonAncestorContainer?.parentElement?.closest?.("[contenteditable=\"true\"]");
            // @ts-ignore
            const itemId = editable?.dataset?.id;

            const block = content.find(i => i.id === itemId);
            const { isBold, isItalic, isUnderline } = getActiveStylesInRange(range);

            editorSelectionStore.itemId = itemId;
            editorSelectionStore.root = sel.anchorNode?.parentElement ?? null;
            editorSelectionStore.range = range;

            editorSelectionStore.activeStyles = {
                isBold: isBold,
                isItalic: isItalic,
                isUnderline: isUnderline,
                blockType: block ? block.type : "text"
            };
        };

        document.addEventListener("selectionchange", handleSelection);
        return () => document.removeEventListener("selectionchange", handleSelection);
    });

    function handleDragOver(e) { e.preventDefault(); }


</script>

<div class="h-full flex flex-col items-start justify-start gap-1 w-full overflow-y-auto overflow-x-hidden">
    {#if isEditing}
        <input type="text" class="text-dt-3 text-gv-dark-text ml-9.5" bind:value={title} placeholder="Berichttitel" />
    {:else}
        <p class={`text-dt-3 text-gv-dark-text`}>{title}</p>
    {/if}

    {#if !isEditing}
        <div class="flex w-full items-center justify-start gap-4">
            {#if showAuthor}
                <div class="flex items-center justify-start gap-2">
                    <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">person</span>
                    <p class="text-dt-6 text-gv-dark-text">{author}</p>
                </div>
            {/if}
            <div class="flex items-center justify-start gap-2">
                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">overview</span>
                <p class="text-dt-6 text-gv-dark-text">{readingTime} Minuten</p>
            </div>
        </div>
    {/if}

    {#each content as item, index (item.id)}
        <div
            class="group flex items-start justify-start gap-2 p-2 rounded transition-all duration-75 w-full"
            class:opacity-40={draggedIndex === index}
            data-index={index}
            ondragenter={handleDragEnter}
            ondragover={handleDragOver}
            aria-label="Draggable item"
            role="group"
        >
            {#if isEditing}
                <span
                    draggable={isEditing}
                    class="material-symbols-rounded text-gv-light-text cursor-grab active:cursor-grabbing select-none group-hover:opacity-100 opacity-0"
                    data-index={index}
                    ondragstart={handleDragStart}
                    ondragend={handleDragEnd}
                    aria-label="Drag to reorder"
                    role="presentation"
                >
                    drag_indicator
                </span>
            {/if}

            {#if item.type === "text"}
                <TextBlock bind:item={content[index]} isEditing={isEditing} handleKeyDown={handleKeyDown} setup={setup}
                           bind:activeBlock={activeBlock} handlePaste={handlePaste} handleDrop={handleDrop} />
            {:else if item.type === "h1"}
                <H1Block bind:item={content[index]} isEditing={isEditing} handleKeyDown={handleKeyDown} setup={setup}
                         bind:activeBlock={activeBlock} handlePaste={handlePaste} handleDrop={handleDrop} />
            {:else if item.type === "h2"}
                <H2Block bind:item={content[index]} isEditing={isEditing} handleKeyDown={handleKeyDown} setup={setup}
                         bind:activeBlock={activeBlock} handlePaste={handlePaste} handleDrop={handleDrop} />
            {:else if item.type === "h3"}
                <H3Block bind:item={content[index]} isEditing={isEditing} handleKeyDown={handleKeyDown} setup={setup}
                         bind:activeBlock={activeBlock} handlePaste={handlePaste} handleDrop={handleDrop} />
            {:else if item.type === "h4"}
                <H4Block bind:item={content[index]} isEditing={isEditing} handleKeyDown={handleKeyDown} setup={setup}
                         bind:activeBlock={activeBlock} handlePaste={handlePaste} handleDrop={handleDrop} />
            {:else if item.type === "blockquote"}
                <BlockQuote bind:item={content[index]} isEditing={isEditing} handleKeyDown={handleKeyDown} setup={setup}
                            bind:activeBlock={activeBlock} handlePaste={handlePaste} handleDrop={handleDrop} />
            {:else if item.type === "image"}
                <!-- Pointer events none prevents the raw image from stealing the drag target focus -->
                <div class="select-none flex justify-start w-full">
                    <Image reportId={reportId} imageId={item.data} content={content} blockId={item.id}
                           isEditing={isEditing} isMobile={isMobile} />
                </div>
            {/if}
        </div>
    {/each}
</div>