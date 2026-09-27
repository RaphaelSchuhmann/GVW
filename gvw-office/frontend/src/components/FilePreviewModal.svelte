<script>
    import Modal from "./Modal.svelte";
    import { previewTypesMap, supportedPreviewTypes } from "../services/fileService.svelte.js";
    import { viewport } from "../stores/viewport.svelte.js";

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

        if (!fileObj || !fileObj.title || !fileObj.extension ||
            !fileObj.blob || !supportedPreviewTypes.has(fileObj.extension)) {
            modalRef.hideModal();
        }

        file = fileObj;
        type = previewTypesMap[file.extension];

        if (!type) {
            modalRef.hideModal();
        }

        console.log("here1");

        if (type === "PDF") {
            handlePDFPreview(file.blob, file.title);
            modalRef.hideModal();
        }
    }

    function handlePDFPreview(blob, name) {
        if (!blob) return;

        const pdfBlob = new Blob([blob], { type: 'application/pdf' });
        const url = URL.createObjectURL(pdfBlob);

        const encodedName = encodeURIComponent(name);
        const blobWithFilename = url + `#filename=${encodedName}`;

        window.open(`/pdfjs/web/viewer.html?file=${encodeURIComponent(blobWithFilename)}`, "_blank");

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
       title="Dateivorschau" width="auto" height="auto">
</Modal>