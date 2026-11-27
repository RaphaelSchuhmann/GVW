<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { ensureUserData } from "../../services/userService.svelte";
    import { auth } from "../../stores/auth.svelte";
    import { user } from "../../stores/user.svelte";
    import { route } from "../../services/utils.js";
    import { lastRefresh } from "../../stores/sseStore.svelte";
    import { untrack } from "svelte";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";

    import DashboardDeploymentsDesktop from "./AdminDashboardDeploymentsDesktop.svelte";
    import DashboardDeploymentsMobile from "./AdminDashboardDeploymentsMobile.svelte";
    import GlobalLoader from "../../components/GlobalLoader.svelte";

    let ready = $state(false);

    $effect(() => {
        if (!auth.token) return;

        (async () => {
            await ensureUserData();
            if (!auth.token) return;

            if (user.role !== "admin") {
                await route("/dashboard");
                return;
            }

            ready = true;

            void init("deployments");
        })();
    });

    $effect(() => {
        const _trigger = lastRefresh.DEPLOYMENTS;

        if (!ready) return;

        untrack(() => {
            fetchAndSetRaw();
        });
    });
</script>

<GlobalLoader loading={!ready}>
    {#if viewport.isMobile}
        <DashboardDeploymentsMobile />
    {:else}
        <DashboardDeploymentsDesktop />
    {/if}
</GlobalLoader>