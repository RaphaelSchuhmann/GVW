<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { push } from "svelte-spa-router";
    import { libraryStore } from "../../stores/library.svelte";

    import LibraryDetailsDesktop from "./LibraryDetailsDesktop.svelte";
    import LibraryDetailsMobile from "./LibraryDetailsMobile.svelte";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";
    import { user } from "../../stores/user.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { scoreExists } from "../../services/libraryService.svelte.js";
    import { addToast } from "../../stores/toasts.svelte.js";
    import Spinner from "../../components/Spinner.svelte";
    import EventDetailsMobile from "../EventDetails/EventDetailsMobile.svelte";

    let isGlobalLoading = $derived(user.name.length === 0);

    const hash = window.location.hash;
    const queryString = hash.split("?")[1];
    const params = new URLSearchParams(queryString);

    const scoreId = params.get("id");

    let isEditing = $state(params.get("editing") === "true");

    const scoreData = $derived.by(() => {
        if (!scoreId) return null;
        return libraryStore.raw.find(item => item.id === scoreId) || null;
    });

    let ready = false;

    $effect(() => {
        if (!user.loaded) return;

        if (isEditing && (user.role !== "board_member" && user.role !== "admin" && user.role !== "librarian" && user.role !== "conductor")) isEditing = false;

        if (!scoreId) {
            push("/library");
        } else if (libraryStore.raw.length === 0) {
            init("library");
        } else if (!scoreData) {
            push("/library");
        }

        ready = true;
    });

    let isDeleting = $state(false);

    $effect(() => {
        const _trigger = lastRefresh.SCORES;

        if (!ready || isDeleting) return;

        (async () => {
            const exists = await scoreExists(scoreId);
            if (!exists) {
                addToast({
                    title: "Noteneintrag nicht mehr verfügbar",
                    subTitle: viewport.isMobile ? "" : "Dieser Noteneintrag wurde gelöscht und ist nicht mehr verfügbar.",
                    type: "error"
                });

                await fetchAndSetRaw();
                await push("/library");
            }
        })();
    });

    function updateIsEditing(val) { isEditing = val; }

    function updateIsDeleting(val) { isDeleting = val; }
</script>

{#if scoreData}
    {#key scoreData.rev}
        {#if viewport.width < 800}
            <LibraryDetailsMobile {scoreData} bind:isEditing bind:isDeleting onChangeIsEditing={updateIsEditing}
                                  onChangeIsDeleting={updateIsDeleting} />
        {:else}
            <LibraryDetailsDesktop {scoreData} bind:isEditing bind:isDeleting onChangeIsEditing={updateIsEditing}
                                   onChangeIsDeleting={updateIsDeleting} />
        {/if}
    {/key}
{:else}
    <div class="w-full h-screen flex justify-center items-center">
        <Spinner title="GVW Office" subTitle="Daten werden geladen..."/>
    </div>
{/if}