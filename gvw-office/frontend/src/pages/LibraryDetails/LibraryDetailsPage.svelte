<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { push } from "svelte-spa-router";
    import { libraryStore } from "../../stores/library.svelte";

    import LibraryDetailsDesktop from "./LibraryDetailsDesktop.svelte";
    import LibraryDetailsMobile from "./LibraryDetailsMobile.svelte";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";
    import { user } from "../../stores/user.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import {getFullScore, scoreExists} from "../../services/libraryService.svelte.js";
    import { addToast } from "../../stores/toasts.svelte.js";
    import Spinner from "../../components/Spinner.svelte";
    import EventDetailsMobile from "../EventDetails/EventDetailsMobile.svelte";
    import GlobalLoader from "../../components/GlobalLoader.svelte";

    const hash = window.location.hash;
    const queryString = hash.split("?")[1];
    const params = new URLSearchParams(queryString);

    const scoreId = params.get("id");

    let isEditing = $state(params.get("editing") === "true");

    let scoreData = $state(null);
    let ready = $state(false);

    $effect(() => {
        if (!user.loaded) return;

        async function loadScore() {
            if (
                isEditing &&
                user.role !== "board_member" &&
                user.role !== "admin" &&
                user.role !== "librarian" &&
                user.role !== "conductor"
            ) {
                isEditing = false;
            }

            if (!scoreId) {
                await push("/library");
                return;
            }

            ready = false;

            const result = await getFullScore(scoreId);

            if (!result) {
                await push("/library");
                return;
            }

            scoreData = result;
            ready = true;
        }

        loadScore();
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

    let isLoading = $derived(!scoreData || !ready || !scoreId);
</script>

<GlobalLoader loading={isLoading}>
    {#if scoreData}
        {#if viewport.width < 800}
            <LibraryDetailsMobile
                    {scoreData}
                    bind:isEditing
                    bind:isDeleting
                    onChangeIsEditing={updateIsEditing}
                    onChangeIsDeleting={updateIsDeleting}
            />
        {:else}
            <LibraryDetailsDesktop
                    {scoreData}
                    bind:isEditing
                    bind:isDeleting
                    onChangeIsEditing={updateIsEditing}
                    onChangeIsDeleting={updateIsDeleting}
            />
        {/if}
    {/if}
</GlobalLoader>