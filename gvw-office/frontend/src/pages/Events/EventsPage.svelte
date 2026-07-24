<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { ensureUserData } from "../../services/userService.svelte";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";

    import EventsDesktop from "./EventsDesktop.svelte";
    import EventsMobile from "./EventsMobile.svelte";
    import { auth } from "../../stores/auth.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { untrack } from "svelte";
    import Spinner from "../../components/Spinner.svelte";
    import { user } from "../../stores/user.svelte.js";
    import GlobalLoader from "../../components/GlobalLoader.svelte";

    let isLoading = $derived(user.name.length === 0);

    let ready = false;

    $effect(() => {
        if (!auth.token) return;

        (async () => {
            await ensureUserData();
            await init("events");
            ready = true;
        })();
    });

    $effect(() => {
        const _trigger = lastRefresh.EVENTS;

        if (!ready) return;

        untrack(() => {
            fetchAndSetRaw();
        });
    });
</script>

<GlobalLoader loading={isLoading}>
    {#if viewport.width < 870}
        <EventsMobile />
    {:else}
        <EventsDesktop />
    {/if}
</GlobalLoader>
