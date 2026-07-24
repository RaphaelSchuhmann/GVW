import { tick } from "svelte";
import { addBlock, handleAutoLink, isCaretAtBoundary } from "./textEditorService.svelte.js";
import { sanitize } from "./utils.js";

export function createEditorHandlers(contentStore) {
    /**
     * Handles keyboard interactions inside an editor block.
     *
     * Supports:
     * - Auto-link detection on space/enter
     * - Enter key block splitting
     * - Backspace rich-link deletion handling
     * - Arrow key navigation between blocks
     *
     * Updates the internal `content` array to stay in sync with DOM changes.
     *
     * @param {KeyboardEvent} e - The keyboard event
     * @returns {void}
     */
    function handleKeyDown(e) {
        const currentBlock = e.currentTarget;
        let content = contentStore.value;
        const index = content.findIndex(i => i.id === currentBlock.dataset.id);

        handleAutoLink(e, currentBlock, (updatedHtml) => {
            content[index].data = updatedHtml;
        });

        if (e.key === "Enter") { handleEnter(e, currentBlock, content); }

        if (e.key === "Backspace") { handleBackspace(e, currentBlock, content); }

        if (e.key === "ArrowUp") { handleArrowUp(e, currentBlock, content); }

        if (e.key === "ArrowDown") { handleArrowDown(e, currentBlock, content); }
    }

    return { handleKeyDown };
}

function handleEnter(e, currentBlock, content) {
    if (e.shiftKey) return;

    e.preventDefault();

    const selection = window.getSelection();
    const range = selection.getRangeAt(0);

    const splitRange = document.createRange();
    splitRange.setStart(range.startContainer, range.startOffset);
    splitRange.selectNodeContents(currentBlock);
    splitRange.setStart(range.startContainer, range.startOffset);

    const docFragment = splitRange.extractContents();

    const id = currentBlock.dataset.id;

    const tempDiv = document.createElement("div");
    tempDiv.appendChild(docFragment);
    const newHTML = tempDiv.innerHTML;

    // Update internal data
    const index = content.findIndex(i => i.id === id);
    content[index].data = sanitize(currentBlock.innerHTML);

    // Create new block
    const newBlockId = addBlock(content, index, newHTML, "text");
    if (newBlockId) {
        tick().then(() => {
            const newBlock = document.querySelector(`[data-id="${newBlockId}"]`);
            if (newBlock) newBlock.focus();
        });
    }
}

function getPreviousTargetNode(range) {
    const { startContainer, startOffset } = range;

    if (startContainer.nodeType === Node.TEXT_NODE && startOffset === 0) {
        return startContainer.previousSibling;
    }
    if (startContainer.nodeType === Node.ELEMENT_NODE) {
        return startContainer.childNodes[startOffset - 1];
    }
    return startContainer;
}

function handleBackspace(e, currentBlock, content) {
    const selection = window.getSelection();

    if (selection?.rangeCount > 0) {
        const range = selection.getRangeAt(0);
        const targetNode = getPreviousTargetNode(range);

        const structuralLink = targetNode?.closest?.("a") || targetNode;

        if (structuralLink?.dataset?.richLink === "true" || (structuralLink?.tagName === "A" && structuralLink.querySelector("img, span"))) {
            e.preventDefault();

            const childNode = structuralLink;
            if (childNode && typeof childNode.remove === "function") {
                childNode.remove();
            }

            const index = content.findIndex(i => i.id === currentBlock.dataset.id);
            if (index !== -1) {
                content[index].data = sanitize(currentBlock.innerHTML);
            }
            return;
        }
    }

    if (currentBlock.textContent.trim().length > 0 || currentBlock.querySelector("img")) {
        return;
    }

    if (content.length > 1) {
        e.preventDefault();

        const index = content.findIndex(i => i.id === currentBlock.dataset.id);
        const previousIndex = index > 0 ? index - 1 : 0;
        const previousId = content[previousIndex].id;

        content.splice(index, 1);

        tick().then(() => {
            const prevEl = document.querySelector(`[data-id="${previousId}"]`);
            if (!prevEl) return;

            prevEl.focus();

            const range = document.createRange();
            range.selectNodeContents(prevEl);
            range.collapse(false);

            const sel = window.getSelection();
            if (sel) {
                sel.removeAllRanges();
                sel.addRange(range);
            }
        });
    }
}

function handleArrowUp(e, currentBlock, content) {
    if (isCaretAtBoundary(currentBlock, "top")) {
        e.preventDefault();
        const index = content.findIndex(i => i.id === currentBlock.dataset.id);
        if (index > 0) {
            const block = document.querySelector(`[data-id="${content[index - 1].id}"]`);
            if (block) {
                block.focus();

                const selection = window.getSelection();
                const range = document.createRange();
                range.selectNodeContents(block);
                range.collapse(false); // Move to end

                selection.removeAllRanges();
                selection.addRange(range);
            }
        }
    }
}

function handleArrowDown(e, currentBlock, content) {
    if (isCaretAtBoundary(currentBlock, "bottom")) {
        e.preventDefault();
        const index = content.findIndex(i => i.id === currentBlock.dataset.id);
        if (index > -1) {
            tick().then(() => {
                const block = document.querySelector(`[data-id="${content[index + 1].id}"]`);
                if (block) {
                    block.focus();

                    const selection = window.getSelection();
                    const range = document.createRange();
                    range.selectNodeContents(block);
                    range.collapse(true); // Move to start

                    selection.removeAllRanges();
                    selection.addRange(range);
                }
            });
        }
    }
}