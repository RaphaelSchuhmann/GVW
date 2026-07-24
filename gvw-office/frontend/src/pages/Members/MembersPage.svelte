<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { ensureUserData } from "../../services/userService.svelte";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";
    import { user } from "../../stores/user.svelte";
    import { push } from "svelte-spa-router";

    import MembersDesktop from "./MembersDesktop.svelte";
    import MembersMobile from "./MembersMobile.svelte";
    import { auth } from "../../stores/auth.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { untrack } from "svelte";
    import Spinner from "../../components/Spinner.svelte";

    let isGlobalLoading = $derived(user.name.length === 0);

    let ready = false;

    $effect(() => {
        if (!user.loaded) return;

        if (user.role !== "admin" && user.role !== "board_member") {
            push("/dashboard");
            return;
        }

        if (!auth.token) return;

        (async () => {
            await ensureUserData();
            await init("members");
            ready = true;
        })();
    });

    $effect(() => {
        const _trigger = lastRefresh.MEMBERS;

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
    {#if viewport.isMobile}
        <MembersMobile />
    {:else}
        <MembersDesktop />
    {/if}
{/if}
