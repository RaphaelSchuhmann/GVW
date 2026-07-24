<script>
    import { viewport } from "../../stores/viewport.svelte";

    let {
        isHeader = false,
        ...restProps
    } = $props();

    const placeholderString = $derived(getPlaceholderString());

    function getPlaceholderString() {
        if (isHeader) {
            return "Artikel suchen...";
        } else {
            return viewport.width > 1000 ? "Nach Artikeln, Funktionen oder Stichwörter suchen..." : "Artikel suchen...";
        }
    }
</script>

<button
    class="flex items-center justify-start gap-2 bg-gv-input-bg border border-gv-separator rounded-1 cursor-text p-2 {isHeader ? 'w-82.5' : ''} pr-10 hover:opacity-75 duration-75"
    {...restProps}>
    <span class="material-symbols-rounded {isHeader && viewport.width > 1000 ? 'text-icon-dt-6' : 'text-icon-dt-7'} text-gv-input-placeholder">search</span>
    <span class="{isHeader && viewport.width > 1000 ? 'text-dt-5' : 'text-dt-6'} text-gv-input-placeholder">{placeholderString}</span>
</button>