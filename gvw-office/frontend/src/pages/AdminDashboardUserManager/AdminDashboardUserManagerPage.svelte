<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { ensureUserData } from "../../services/userService.svelte";
    import { auth } from "../../stores/auth.svelte";
    import { user } from "../../stores/user.svelte";
    import { push } from "svelte-spa-router";
    import { lastRefresh } from "../../stores/sseStore.svelte";
    import { untrack } from "svelte";

    import DashboardUserManagerDesktop from "./AdminDashboardUserManagerDesktop.svelte";
    import DashboardUserManagerMobile from "./AdminDashboardUserManagerMobile.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";

    let isGlobalLoading = $derived(user.name.length === 0);

    let ready = false;

    $effect(() => {
        if (!auth.token) return;

        (async () => {
            await ensureUserData();
            if (!auth.token) return;

            if (user.role !== "admin") {
                await push("/dashboard");
                return;
            }

            ready = true;

            init("userManager");
        })();
    });

    $effect(() => {
        const _trigger = lastRefresh.USER;

        if (!ready) return;

        untrack(() => {
            fetchAndSetRaw();
        });
    });
</script>

{#if isGlobalLoading}
    <div class="w-full h-screen flex justify-center items-center">
        <Spinner title="GVW Office" subTitle="Daten werden geladen..." />
    </div>
{:else}
    {#if viewport.isMobile}
        <DashboardUserManagerMobile />
    {:else}
        <DashboardUserManagerDesktop />
    {/if}
{/if}