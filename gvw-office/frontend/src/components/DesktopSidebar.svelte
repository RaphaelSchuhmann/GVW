<script>
    import SidebarContent from "./SidebarContent.svelte";
    import { uiStates } from "../stores/user.svelte.js";

    let {
        currentPage = "",
    } = $props();

    let minimized = $state(uiStates.sidebarMinimized || false);

    let toggleIcon = $derived(minimized ? "arrow_menu_open" : "arrow_menu_close");

    /**
     * Toggles the sidebar state
     */
    function toggleSidebarState() {
        minimized = !minimized;
        uiStates.sidebarMinimized = minimized;
    }
</script>

<div class="min-[1230px]:max-w-1/4 max-[1230px]:max-w-2/5 h-dvh flex flex-col items-center bg-gv-bg-bar border-r border-gv-border-bar">
    <div class="w-full flex items-center border-b border-gv-border-bar p-5">
        {#if !minimized}
            <div class="flex flex-col items-start justify-around h-full">
                <p class="text-dt-3 font-semibold text-gv-dark-turquoise">GVW Office</p>
                <p class="text-dt-8 text-gv-light-text">Weppersdorf</p>
            </div>
        {/if}

        <button
            type="button"
            class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center rounded-2 pt-2 pb-2"
            onclick={toggleSidebarState}
        >
            <span class="material-symbols-rounded text-icon-dt-1 text-gv-dark-text duration-150">
                {toggleIcon}
            </span>
        </button>
    </div>

    <SidebarContent currentPage={currentPage} minimized={minimized} />
</div>