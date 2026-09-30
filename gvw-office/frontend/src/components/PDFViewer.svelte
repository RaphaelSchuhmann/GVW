<script>
    import { tick } from "svelte";
    import { triggerFileDownload } from "../services/utils.js";

    import * as pdfjs from "pdfjs-dist";
    import pdfWorker from "pdfjs-dist/build/pdf.worker.mjs?url";
    import Spinner from "./Spinner.svelte";

    pdfjs.GlobalWorkerOptions.workerSrc = pdfWorker;

    let {
        filename,
        blob
    } = $props();

    const downloadable = $state(filename.includes("."));

    let loading = $state(true);
    const renderedPages = new Set();

    let container = $state(null);
    let numPages = $state(0);
    let pdfDoc = $state(null);

    let resizeTimeout;
    let resizeObserver;

    $effect(() => {
        if (blob && container) {
            loadAndRenderPdf(blob);
        }
    });

    $effect(() => {
        if (!container || !pdfDoc) return;

        resizeObserver = new ResizeObserver(() => {
            clearTimeout(resizeTimeout);

            resizeTimeout = setTimeout(() => {
                if (pdfDoc && renderedPages > 0) rerenderVisiblePages();
            }, 150);
        });

        resizeObserver.observe(container);

        return () => {
            resizeObserver.disconnect();
            clearTimeout(resizeTimeout);
        };
    });

    async function loadAndRenderPdf(pdf) {
        loading = true;

        const arrayBuffer = await pdf.arrayBuffer();

        pdfDoc = await pdfjs.getDocument({ data: arrayBuffer }).promise;
        numPages = pdfDoc.numPages;

        loading = false;
        await tick();

        setupPageObserver();
    }

    async function rerenderVisiblePages() {
        for (const pageNum of renderedPages) {
            await renderPage(pageNum);
        }
    }

    async function renderPage(pageNum) {
        const page = await pdfDoc.getPage(pageNum);
        const canvas = container.querySelector(
            `canvas[data-page="${pageNum}"]`
        );
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

    function setupPageObserver() {
        const observer = new IntersectionObserver((entries) => {
            for (const entry of entries) {
                if (!entry.isIntersecting) continue;

                const pageNum = Number(entry.target.dataset.page);

                if (renderedPages.has(pageNum)) continue;

                renderedPages.add(pageNum);
                renderPage(pageNum);
            }
        }, {
            rootMargin: "1000px 0px"
        });

        const canvases = container.querySelectorAll("canvas[data-page]");

        for (const canvas of canvases) {
            observer.observe(canvas);
        }
    }
</script>

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
    <div class="flex flex-col items-center w-full justify-start gap-2" bind:this={container}>
        {#if loading || !pdfDoc}
            <div class="pt-10 pb-10 w-full flex items-center justify-center">
                <Spinner simple={true} width="1/6" />
            </div>
        {:else}
            {#each Array(numPages) as _, i}
                <canvas data-page={i + 1} class="border border-gv-border rounded-2"></canvas>
            {/each}
        {/if}
    </div>
</div>