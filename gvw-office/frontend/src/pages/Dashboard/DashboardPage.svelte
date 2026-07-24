<script>
    import { viewport } from "../../stores/viewport.svelte";
    import DashboardDesktop from "./DashboardDesktop.svelte";
    import DashboardMobile from "./DashboardMobile.svelte";
    import { ensureUserData } from "../../services/userService.svelte";
    import { auth } from "../../stores/auth.svelte";
    import { user } from "../../stores/user.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { untrack } from "svelte";
    import { loadDashboardData } from "../../services/dashboardService.svelte.js";

    let isGlobalLoading = $derived(user.name.length === 0);
    let ready = false;

    $effect(() => {
        if (!auth.token) return;

        (async () => {
            await ensureUserData();
            if (!auth.token) return;
            await loadDashboardData();
            ready = true;
        })();
    });

    $effect(() => {
        const _memberTrigger = lastRefresh.MEMBERS;
        const _eventTrigger = lastRefresh.EVENTS;
        const _scoreTrigger = lastRefresh.SCORES;
        const _settingsTrigger = lastRefresh.SETTINGS;

        if (!ready) return;

        untrack(() => {
            loadDashboardData();
        });
    });
</script>

{#if isGlobalLoading}
    <div class="w-full h-screen flex justify-center items-center">
        <Spinner title="GVW Office" subTitle="Daten werden geladen..." />
    </div>
{:else}
    {#if viewport.isMobile}
        <DashboardMobile />
    {:else}
        <DashboardDesktop />
    {/if}
{/if}