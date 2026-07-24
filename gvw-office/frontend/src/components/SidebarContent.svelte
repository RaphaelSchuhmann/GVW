<script>
    import { push } from "svelte-spa-router";
    import { user } from "../stores/user.svelte";
    import SidebarButton from "./SidebarButton.svelte";
    import { logout } from "../services/userService.svelte";
    import FeedbackModal from "./FeedbackModal.svelte";
    import ChangelogsModal from "./ChangelogsModal.svelte";

    let {
        currentPage = "",
        minimized = false,
        isMobile = false,
        ...restProps
    } = $props();

    /** @type {import("../components/ChangelogsModal.svelte").default} */
    let changelogModal = null;

    /** @type {import("../components/FeedbackModal.svelte").default} */
    let feedbackModal = null;

    const mitgliederAccess = ["admin", "board_member"];
    const reportsAccess = ["admin", "board_member", "secretary"];

    let userOptionsVisible = $state(false);
    let userOptionsIcon = $state("expand_all");

    function toggleUserOptions() {
        userOptionsVisible = !userOptionsVisible;
        userOptionsIcon = userOptionsVisible ? "collapse_all" : "expand_all";
    }

    async function handleLogout() {
        logout();
        await push("/");
    }

    function openFeedback() {
        feedbackModal.showModal();
        toggleUserOptions();
    }
</script>

<ChangelogsModal bind:this={changelogModal} isMobile={isMobile} />
<FeedbackModal bind:this={feedbackModal} isMobile={isMobile} />

<div class="flex flex-col items-center w-full flex-1 overflow-y-auto">
    <div class="flex flex-col items-center w-full h-full flex-1 p-5">
        {#if user.role === "admin"}
            <SidebarButton selected={currentPage === "adminDashboard"} minimized={minimized}
                           onclick={async () => await push("/admin/overview")}>
                <span class="material-symbols-rounded text-icon-dt-4">dashboard_2_gear</span>
                {#if !minimized}<p class="ml-2">Admin</p>{/if}
            </SidebarButton>
        {/if}

        <SidebarButton selected={currentPage === "dashboard"} minimized={minimized} marginTop="5"
                       onclick={async () => await push("/dashboard")}>
            <span class="material-symbols-rounded text-icon-dt-4">dashboard</span>
            {#if !minimized}<p class="ml-2">Dashboard</p>{/if}
        </SidebarButton>

        {#if mitgliederAccess.includes(user.role)}
            <SidebarButton selected={currentPage === "members"} minimized={minimized} marginTop="5"
                           onclick={async () => await push("/members")}>
                <span class="material-symbols-rounded text-icon-dt-4">group</span>
                {#if !minimized}<p class="ml-2">Mitglieder</p>{/if}
            </SidebarButton>
        {/if}

        <SidebarButton selected={currentPage === "events"} minimized={minimized} marginTop="5"
                       onclick={async () => await push("/events")}>
            <span class="material-symbols-rounded text-icon-dt-4">calendar_today</span>
            {#if !minimized}<p class="ml-2">Veranstaltungen</p>{/if}
        </SidebarButton>

        {#if reportsAccess.includes(user.role)}
            <SidebarButton selected={currentPage === "reports"} minimized={minimized} marginTop="5"
                           onclick={async () => await push("/reports")}>
                <span class="material-symbols-rounded text-icon-dt-4">docs</span>
                {#if !minimized}<p class="ml-2">Berichte</p>{/if}
            </SidebarButton>
        {/if}

        <SidebarButton selected={currentPage === "library"} minimized={minimized} marginTop="5"
                       onclick={async () => await push("/library")}>
            <span class="material-symbols-rounded text-icon-dt-4">music_note_2</span>
            {#if !minimized}<p class="ml-2">Notenbibliothek</p>{/if}
        </SidebarButton>
    </div>

    <div class="flex flex-col items-center w-full mt-auto p-5">
        {#if !minimized}
            <div class="relative inline-block text-left w-full">
                <button onclick={toggleUserOptions}
                        class="flex w-full items-center bg-white border border-gv-border rounded-1 p-3 cursor-pointer hover:bg-gv-secondary-btn-hover duration-200 overflow-hidden">
                    <div class="flex flex-col items-start justify-around w-full">
                        {#if user.loaded}
                            <p class="text-dt-5 text-gv-dark-text truncate">{user.name}</p>
                            <p class="text-dt-8 text-gv-light-text truncate">{user.email}</p>
                        {:else}
                            <div class="animate-pulse h-5 w-40 bg-gray-200 rounded"></div>
                            <div class="animate-pulse h-5 w-24 bg-gray-200 rounded mt-2"></div>
                        {/if}
                    </div>
                    <span class="material-symbols-rounded text-icon-dt-2 ml-3">{userOptionsIcon}</span>
                </button>

                {#if userOptionsVisible}
                    <div
                        class="absolute bottom-22 w-full bg-white border border-gv-border rounded-1 p-2 flex flex-col items-center">
                        <button onclick={async () => {await push("/help"); toggleUserOptions()}}
                                class="w-full flex items-center rounded-2 cursor-pointer hover:bg-gv-hover-effect p-2 pl-3 pr-3 duration-150 text-dt-6">
                            <span class="material-symbols-rounded text-icon-dt-5 mr-2">menu_book</span>
                            Hilfe-Center
                        </button>
                        <button onclick={openFeedback}
                                class="w-full flex items-center rounded-2 cursor-pointer hover:bg-gv-hover-effect p-2 pl-3 pr-3 duration-150 text-dt-6">
                            <span class="material-symbols-rounded text-icon-dt-5 mr-2">chat_bubble</span>
                            Feedback
                        </button>
                        <button onclick={async () => {await changelogModal.showModal(); toggleUserOptions()}}
                                class="w-full flex items-center rounded-2 cursor-pointer hover:bg-gv-hover-effect p-2 pl-3 pr-3 duration-150 text-dt-6">
                            <span class="material-symbols-rounded text-icon-dt-5 mr-2">campaign</span>
                            Changelogs
                        </button>
                        <button onclick={handleLogout}
                                class="w-full flex items-center rounded-2 cursor-pointer hover:bg-gv-hover-effect p-2 pl-3 pr-3 duration-150 text-dt-6">
                            <span class="material-symbols-rounded text-icon-dt-5 mr-2">logout</span>
                            Abmelden
                        </button>
                    </div>
                {/if}
            </div>
        {:else}
            <div class="flex flex-col items-center gap-2">
                <SidebarButton minimized={minimized} onclick={async () => {await push("/help")}}>
                    <span class="material-symbols-rounded text-icon-dt-3">menu_book</span>
                </SidebarButton>
                <SidebarButton minimized={minimized} onclick={openFeedback}>
                    <span class="material-symbols-rounded text-icon-dt-3">chat_bubble</span>
                </SidebarButton>
                <SidebarButton minimized={minimized} onclick={async () => {await changelogModal.showModal()}}>
                    <span class="material-symbols-rounded text-icon-dt-3">campaign</span>
                </SidebarButton>
                <SidebarButton minimized={minimized} onclick={handleLogout}>
                    <span class="material-symbols-rounded text-icon-dt-3">logout</span>
                </SidebarButton>
            </div>
        {/if}
    </div>

    {#if !minimized}
        <div class="flex items-center justify-center p-5 border-t border-gv-border-bar w-full">
            <div class="flex flex-col items-start w-full bg-gv-secondary rounded-1 p-5">
                <p class="text-dt-5 text-gv-dark-text text-base/6">Gesangverein<br>Weppersdorf</p>
                <p class="text-dt-8 text-gv-light-text mt-3">© 2026 GVW Office</p>
            </div>
        </div>
    {/if}
</div>
