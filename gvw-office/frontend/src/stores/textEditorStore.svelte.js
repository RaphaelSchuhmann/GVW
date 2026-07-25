/**
 * Svelte store for editor selection data
 */
export const editorSelectionStore = $state({
    itemId: null,
    root: null,
    range: null,
    activeStyles: {
        isBold: false,
        isItalic: false,
        isUnderline: false,
        blockType: "text",
    }
});

/**
 * Svelte store for editor metadata
 */
export const editorMetadataStore = $state({
    activeFeature: "",
})