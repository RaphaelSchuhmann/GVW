<script>
    import SidebarButton from "./SidebarButton.svelte";
    import { push } from "svelte-spa-router";
    import { viewport } from "../stores/viewport.svelte.js";

    let {
        currentPage = "",
        ...restProps
    } = $props();
</script>

<div class="w-full flex items-center justify-start bg-white rounded-1 p-2 drop-shadow-[0_0_5px_rgba(0,0,0,0.1)]">
    <div class="w-full flex items-center justify-start gap-2 overflow-x-auto overflow-y-hidden no-scrollbar">
        <SidebarButton fontSize={viewport.isMobile ? "5" : "3"} rounded2={true} hugWidth={true} selected={currentPage === "overview"} minimized={false} onclick={async () => await push("/admin/overview")}>
            <span class="material-symbols-rounded text-icon-dt-4">space_dashboard</span>
            <p class="ml-2 text-nowrap">Übersicht</p>
        </SidebarButton>
        <SidebarButton fontSize={viewport.isMobile ? "5" : "3"} rounded2={true} hugWidth={true} selected={currentPage === "reportHub"} minimized={false} onclick={async () => await push("/admin/reportHub")}>
            <span class="material-symbols-rounded text-icon-dt-4">inbox</span>
            <p class="ml-2 text-nowrap">Berichte Hub</p>
        </SidebarButton>
        <SidebarButton fontSize={viewport.isMobile ? "5" : "3"} rounded2={true} hugWidth={true} selected={currentPage === "userManagement"} minimized={false} onclick={async () => await push("/admin/userManagement")}>
            <span class="material-symbols-rounded text-icon-dt-4">patient_list</span>
            <p class="ml-2 text-nowrap">Nutzerverwaltung</p>
        </SidebarButton>
    </div>
</div>

<style>
    .no-scrollbar::-webkit-scrollbar {
        display: none;
    }

    .no-scrollbar {
        -ms-overflow-style: none;
        scrollbar-width: none;
    }
</style>