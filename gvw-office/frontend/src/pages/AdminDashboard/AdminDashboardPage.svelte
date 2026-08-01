<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { ensureUserData } from "../../services/userService.svelte";
    import { auth } from "../../stores/auth.svelte";
    import { user } from "../../stores/user.svelte";
    import { push } from "svelte-spa-router";
    import { getChangelogs } from "../../services/changelogService.svelte.js";

    import DashboardDesktop from "./AdminDashboardDesktop.svelte";
    import DashboardMobile from "./AdminDashboardMobile.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { untrack } from "svelte";
    import GlobalLoader from "../../components/GlobalLoader.svelte";

    let ready = $state(false);

    $effect(() => {
        if (!auth.token) return;

        (async () => {
            if (user.role !== "admin") {
                await push("/dashboard");
                return;
            }

            await ensureUserData();
            if (!auth.token) return;
            await getChangelogs();
            ready = true;
        })();
    });

    $effect(() => {
        const _triggerChangelogs = lastRefresh.CHANGELOGS;

        if (!ready) return;

        untrack(() => {
            getChangelogs();
        });
    });
</script>

<GlobalLoader loading={!ready}>
    {#if viewport.isMobile}
        <DashboardMobile />
    {:else}
        <DashboardDesktop />
    {/if}
</GlobalLoader>
