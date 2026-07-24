<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { ensureUserData } from "../../services/userService.svelte";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";

    import ReportsDesktop from "./ReportsDesktop.svelte";
    import ReportsMobile from "./ReportsMobile.svelte";
    import { auth } from "../../stores/auth.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { untrack } from "svelte";
    import Spinner from "../../components/Spinner.svelte";
    import { user } from "../../stores/user.svelte.js";
    import { push } from "svelte-spa-router";

    let isGlobalLoading = $derived(user.name.length === 0);

    let ready = false;

    $effect(() => {
        if (!auth.token) return;

        (async () => {
            if (user.role !== "admin" && user.role !== "board_member" && user.role !== "secretary") {
                await push("/dashboard");
                return;
            }

            await ensureUserData();
            await init("reports");
            ready = true;
        })();
    });

    $effect(() => {
        const _trigger = lastRefresh.REPORTS;

        if (!ready) return;

        untrack(() => {
            fetchAndSetRaw();
        });
    })
</script>

{#if isGlobalLoading}
    <div class="w-full h-screen flex justify-center items-center">
        <Spinner title="GVW Office" subTitle="Daten werden geladen..."/>
    </div>
{:else}
    {#if viewport.width < 800}
        <ReportsMobile />
    {:else}
        <ReportsDesktop />
    {/if}
{/if}