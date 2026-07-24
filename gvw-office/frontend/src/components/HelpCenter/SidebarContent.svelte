<script>
    import { push } from "svelte-spa-router";
    import { user } from "../../stores/user.svelte";
    import { logout } from "../../services/userService.svelte";

    import SidebarButton from "./SidebarButton.svelte";
    import ChangelogsModal from "../ChangelogsModal.svelte";
    import FeedbackModal from "../FeedbackModal.svelte";
    import { helpCenterStore } from "../../stores/helpCenterStore.svelte";

    let {
        currentCategory = $bindable(""),
        isMobile = false,
        buttons,
        ...restProps
    } = $props();

    /** @type {import("../../components/ChangelogsModal.svelte").default} */
    let changelogModal = null;

    /** @type {import("../../components/FeedbackModal.svelte").default} */
    let feedbackModal = null;

    $effect(() => {
        if (helpCenterStore.activeCategory === "") {
            currentCategory = "";
        } else {
            currentCategory = buttons.find(cat => cat.id === helpCenterStore.activeCategory)?.title;
        }
    });

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

    function openFeedbackModal() {
        feedbackModal.showModal();
        toggleUserOptions();
    }

    function selectCategory(event) {
        const button = event.currentTarget.dataset.button;
        currentCategory = button.title;
        helpCenterStore.activeCategory = button.id;
    }
</script>

<ChangelogsModal bind:this={changelogModal} isMobile={isMobile} />
<FeedbackModal bind:this={feedbackModal} isMobile={isMobile} />

<div class="flex flex-col items-center w-full flex-1 overflow-hidden">
    <div class="flex flex-col items-center w-full h-full flex-1 p-5 gap-4 overflow-y-auto">
        <button class="group cursor-pointer w-full flex items-center justify-start gap-2 p-2"
                onclick={async () => await push("/dashboard")}>
            <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">arrow_back</span>
            <span class="text-dt-3 text-gv-dark-text group-hover:underline">Dashboard</span>
        </button>
        {#each buttons as button (button.title)}
            <SidebarButton onclick={selectCategory} data-button={button}
                           selected={currentCategory === button.title}>
                <div class="flex items-center justify-start gap-2">
                    <span class="material-symbols-rounded text-icon-dt-4">{button.icon}</span>
                    <p class="text-dt-3">{button.title}</p>
                </div>
                <p class="text-dt-3 ml-auto">{button.articleCount}</p>
            </SidebarButton>
        {/each}
    </div>

    <div class="flex flex-col items-center w-full mt-auto p-5">
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
                    <button onclick={openFeedbackModal}
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
    </div>

    <div class="flex items-center justify-center p-5 border-t border-gv-border-bar w-full">
        <div class="flex flex-col items-start w-full bg-gv-secondary rounded-1 p-5">
            <p class="text-dt-5 text-gv-dark-text text-base/6">Gesangverein<br>Weppersdorf</p>
            <p class="text-dt-8 text-gv-light-text mt-3">© 2026 GVW Office</p>
        </div>
    </div>
</div>
