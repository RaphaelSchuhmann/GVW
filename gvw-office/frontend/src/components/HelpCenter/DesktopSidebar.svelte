<script>
    import SidebarContent from "./SidebarContent.svelte";

    let {
        currentCategory = $bindable(""),
        buttons,
    } = $props();
</script>

<div class="min-[1230px]:max-w-1/4 max-[1230px]:max-w-2/5 h-full flex flex-col items-center bg-gv-bg-bar border-r border-gv-border-bar">
    <SidebarContent currentCategory={currentCategory} buttons={buttons} />
</div>