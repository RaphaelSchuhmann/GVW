<script>
    import Card from "./Card.svelte";
    import Chip from "./Chip.svelte";
    import { reportTypeMap, deleteReport, highlight } from "../services/reportService.svelte.js";
    import { push } from "svelte-spa-router";
    import SanitizedHTML from "./SanitizedHTML.svelte";

    let {
        id = "",
        title = "",
        date = "",
        author = "",
        type = "",
        additionalText = "",
        isMobile = false,
        deletable = true,
        isSearchResult = false,
        ...restProps
    } = $props();

    async function openReport() {
        if (isMobile) return;
        await push(`/reports/details?id=${id}`);
    }
</script>

<Card>
    <div class={`items-start w-full ${isMobile ? "flex-col" : "flex"}`}>
        <button class="flex flex-col items-start gap-4 cursor-pointer w-full" onclick={openReport}>
            <div class="flex items-center gap-2">
                <span class="material-symbols-rounded text-gv-primary text-icon-dt-4">docs</span>
                <span class="text-gv-dark-text text-dt-5 text-nowrap truncate text-left">{title}</span>
            </div>
            <div class="flex items-center gap-4">
                <div class="flex items-center gap-2">
                    <span class="material-symbols-rounded text-gv-light-text text-icon-dt-6">calendar_today</span>
                    <span class="text-gv-light-text text-dt-6 text-left">{date}</span>
                </div>
                <div class="flex items-center gap-2 w-full">
                    <span class="material-symbols-rounded text-gv-light-text text-icon-dt-6">person</span>
                    <span class="text-gv-light-text text-dt-6 text-left text-nowrap truncate w-full">{author}</span>
                </div>
            </div>
            {#if isSearchResult}
                <div class="flex w-full justify-start text-left">
                    <SanitizedHTML htmlContent={additionalText} />
                </div>
            {:else}
                <span class="text-gv-light-text mt-2 text-dt-6 line-clamp-2 truncate text-left">{additionalText}</span>
            {/if}
        </button>
        <div class={`flex items-center ${isMobile ? "justify-start mt-4" : "justify-end"} gap-4 ml-auto`}>
            <Chip text={reportTypeMap[type] || reportTypeMap["other"]} />

            <button
                class="flex items-center gap-2 p-1.5 pl-3 pr-3 border-2 border-gv-border rounded-2 cursor-pointer hover:bg-gv-input-bg">
                <span class="material-symbols-rounded text-gv-dark-text text-icon-dt-6">download</span>
                <span class="text-gv-dark-text text-dt-7 font-semibold">PDF</span>
            </button>

            {#if !isMobile}
                {#if deletable}
                    <button
                        class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2 hover:text-gv-delete-hover"
                        onclick={async () => await deleteReport(id)}>
                        <span class="material-symbols-rounded text-icon-dt-5">delete</span>
                    </button>
                {/if}
            {/if}
        </div>
    </div>
</Card>