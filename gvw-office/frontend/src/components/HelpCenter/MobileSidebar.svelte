<script>
    import { fly, fade } from "svelte/transition";
    import SidebarContent from "./SidebarContent.svelte";

    let {
        currentCategory = $bindable(""),
        isOpen = $bindable(false),
        buttons,
        ...restProps
    } = $props();

    const closeSidebar = () => isOpen = false;

    function handleKey(e) {
        if (e.key === "Escape") {
            e.preventDefault();
            closeSidebar();
        }
    }
</script>

{#if isOpen}
    <button class="fixed inset-0 bg-black/50 z-40"
         role="button"
         tabindex="-1"
         aria-label="Close sidebar"
         onclick={closeSidebar}
         onkeydown={handleKey}
         transition:fade>
    </button>

    <div
        class="fixed inset-0 z-50 bg-gv-bg-bar flex flex-col"
        transition:fly={{ x: -300, duration: 200 }}
    >
        <div class="w-full flex items-center border-b border-gv-border-bar p-5">
            <div class="flex items-center justify-start h-full gap-2">
                <span class="material-symbols-rounded text-gv-dark-text text-dt-1">menu_book</span>
                <p class="text-dt-3 text-gv-dark-text font-bold">Hilfe-Center</p>
            </div>
            <button
                type="button"
                class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center rounded-2 pt-2 pb-2"
                onclick={closeSidebar}
            >
                <span class="material-symbols-rounded text-icon-dt-1 text-gv-dark-text duration-150">
                    arrow_menu_close
                </span>
            </button>
        </div>

        <SidebarContent currentCategory={currentCategory} isMobile={true} buttons={buttons} />
    </div>
{/if}