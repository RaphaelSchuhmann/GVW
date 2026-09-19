<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { ensureUserData } from "../../services/userService.svelte";
    import { auth } from "../../stores/auth.svelte";
    import { user } from "../../stores/user.svelte";
    import { route } from "../../services/utils.js";
    import { lastRefresh } from "../../stores/sseStore.svelte";
    import { untrack } from "svelte";
    import { getAllBugReports, getAllFeedbacks } from "../../services/reportHubService.svelte";

    import DashboardReportHubDesktop from "./AdminDashboardReportHubDesktop.svelte";
    import DashboardReportHubMobile from "./AdminDashboardReportHubMobile.svelte";
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
        })();
    });

    $effect(() => {
        const _triggerFeedback = lastRefresh.FEEDBACK;
        const _triggerBug = lastRefresh.BUG;

        if (!ready) return;

        untrack(() => {
            getAllFeedbacks();
            getAllBugReports();
        });
    });
</script>

<GlobalLoader loading={!ready}>
    {#if viewport.isMobile}
        <DashboardReportHubMobile />
    {:else}
        <DashboardReportHubDesktop />
    {/if}
</GlobalLoader>
