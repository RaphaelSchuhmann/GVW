<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { ensureUserData } from "../../services/userService.svelte";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";

    import LibraryDesktop from "./LibraryDesktop.svelte";
    import LibraryMobile from "./LibraryMobile.svelte";
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
            await init("library");
            ready = true;
        })();
    });

    $effect(() => {
        const _trigger = lastRefresh.SCORES;

        if (!ready) return;

        untrack(() => {
            fetchAndSetRaw();
        });
    })
</script>

<GlobalLoader loading={isLoading}>
    {#if viewport.width < 800}
        <LibraryMobile />
    {:else}
        <LibraryDesktop />
    {/if}
</GlobalLoader>