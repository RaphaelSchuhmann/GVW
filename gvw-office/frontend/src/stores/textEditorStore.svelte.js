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

export const editorMetadataStore = $state({
    activeFeature: "",
})