<script>
    import Modal from "./Modal.svelte";
    import { previewTypesMap, supportedPreviewTypes } from "../services/fileService.svelte.js";
    import { viewport } from "../stores/viewport.svelte.js";
    import ImagePreview from "./ImagePreview.svelte";
    import AudioPlayer from "./AudioPlayer.svelte";

    let {
        isMobile = viewport.isMobile
    } = $props();

    const urlObjects = [];

    let modalRef = null;
    let type = "";
    let file = null;

    window.addEventListener("beforeunload", () => {
        for (const url of urlObjects) {
            URL.revokeObjectURL(url);
        }
    });

    export function handlePreview(fileObj) {
        if (!modalRef) return;

        if (!fileObj || !fileObj.isPreviewable || !fileObj.title || !fileObj.extension ||
            !fileObj.blob || !supportedPreviewTypes.has(fileObj.extension)) {
            modalRef.hideModal();
        }

        file = fileObj;
        type = previewTypesMap[file.extension];

        if (!type) {
            modalRef.hideModal();
        }

        if (type === "PDF") {
            handlePDFPreview(file.blob, file.title);
            modalRef.hideModal();
        }
    }

    function handlePDFPreview(blob, name) {
        if (!blob) return;

        const pdfBlob = new Blob([blob], { type: 'application/pdf' });
        const fileName = name.endsWith(".pdf") ? name : name + ".pdf";

        const url = URL.createObjectURL(pdfBlob) + "#" + encodeURIComponent(fileName);

        window.open(`/pdfjs/web/viewer.html?file=${encodeURIComponent(url)}`, "_blank");

        urlObjects.push(url);
    }

    export function showModal() {
        modalRef.showModal();
    }

    export function hideModal() {
        modalRef.hideModal();
    }
</script>

<Modal bind:this={modalRef} {isMobile} hideSubTitle={true}
       title="Dateivorschau" width="1/2" height="auto">
    {#if type === "img"}
        <ImagePreview filename={file.title} blob={file.blob} />
    {:else if type === "audio"}
        <AudioPlayer filename={file.title} blob={file.blob} />
    {/if}
</Modal>