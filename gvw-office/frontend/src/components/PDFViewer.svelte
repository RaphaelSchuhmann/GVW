<script>
    import * as pdfjs from "pdfjs-dist";
    import { tick } from 'svelte';
    import { triggerFileDownload } from "../services/utils.js";

    import pdfWorker from 'pdfjs-dist/build/pdf.worker.mjs?url';
    pdfjs.GlobalWorkerOptions.workerSrc = pdfWorker;

    let {
        filename,
        blob
    } = $props();

    const downloadable = $state(filename.includes("."));

    let container = $state(null);
    let canvasRefs = $state([]);
    let numPages = $state(0);
    let pdfDoc = $state(null);

    let resizeTimeout;

    $effect(() => {
        if (blob && container) {
            loadAndRenderPdf(blob);
        }
    });

    async function loadAndRenderPdf(pdf) {
        const arrayBuffer = await pdf.arrayBuffer();

        pdfDoc = await pdfjs.getDocument({ data: arrayBuffer }).promise;
        numPages = pdfDoc.numPages;

        canvasRefs = new Array(numPages).fill(null);

        await tick();

        await renderAllPages();
    }

    async function renderAllPages() {
        if (!pdfDoc || !container) return;

        for (let pageNum = 1; pageNum <= numPages; pageNum++) {
            await renderPage(pageNum);
        }
    }

    async function renderPage(pageNum) {
        const page = await pdfDoc.getPage(pageNum);
        const canvas = canvasRefs[pageNum - 1];
        if (!canvas) return;

        const ctx = canvas.getContext("2d");

        const unscaledViewport = page.getViewport({ scale: 1.0 });

        const containerWidth = container.clientWidth || unscaledViewport.width;

        const scale = containerWidth / unscaledViewport.width;
        const viewport = page.getViewport({ scale: scale });

        const outputScale = window.devicePixelRatio || 1;

        canvas.width = Math.floor(viewport.width * outputScale);
        canvas.height = Math.floor(viewport.height * outputScale);
        canvas.style.width = `${Math.floor(viewport.width)}px`;
        canvas.style.height = `${Math.floor(viewport.height)}px`;

        const transform = outputScale !== 1
            ? [outputScale, 0, 0, outputScale, 0, 0]
            : null;

        await page.render({ canvasContext: ctx, transform, viewport }).promise;
    }

    function handleResize() {
        clearTimeout(resizeTimeout);
        resizeTimeout = setTimeout(() => {
            if (pdfDoc) renderAllPages();
        }, 150);
    }
</script>

<svelte:window onresize={handleResize} />

<div class="flex flex-col items-center justify-start gap-2 w-full">
    <div class="flex items-center w-full justify-start">
        <p class="text-gv-dark-text text-dt-4 max-[1000px]:text-dt-5 font-medium w-full text-nowrap truncate">{filename}</p>
        {#if downloadable}
            <button
                class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2"
                onclick={() => triggerFileDownload(blob, filename)}
            >
                <span class="material-symbols-rounded text-icon-dt-5">download</span>
            </button>
        {/if}
    </div>
    <div class="flex flex-col items-center w-full justify-start" bind:this={container}>
        {#if pdfDoc}
            {#each Array(numPages) as _, i}
                <canvas bind:this={canvasRefs[i]}></canvas>
            {/each}
        {:else}
            PDF laden...
        {/if}
    </div>
</div>