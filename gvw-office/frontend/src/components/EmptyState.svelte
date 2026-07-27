<script>
    let {
        message = "Keine Ergebnisse",
        ...restProps
    } = $props();
</script>

<div
    class="gap-5 bg-white flex flex-col items-center min-h-0 justify-center rounded-1 border-2 border-gv-border w-full h-full p-3 overflow-hidden border-dashed">
    <div class="flex items-center justify-center gap-4">
        <span class="material-symbols-rounded text-icon-dt-4">search_off</span>
        <p class="text-gv-dark-text text-dt-3 font-medium pt-10 pb-10">{message}</p>
    </div>
</div>