<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { membersStore } from "../../stores/members.svelte";
    import { push } from "svelte-spa-router";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";
    import { user } from "../../stores/user.svelte";

    import MemberDetailsDesktop from "./MemberDetailsDesktop.svelte";
    import MemberDetailsMobile from "./MemberDetailsMobile.svelte";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { memberExists } from "../../services/membersService.svelte.js";
    import { addToast } from "../../stores/toasts.svelte.js";
    import Spinner from "../../components/Spinner.svelte";

    let isGlobalLoading = $derived(user.name.length === 0);

    const hash = window.location.hash;
    const queryString = hash.split("?")[1];
    const params = new URLSearchParams(queryString);

    const memberId = params.get("id");

    let isEditing = $state(params.get("editing") === "true");

    const memberData = $derived.by(() => {
        if (!memberId) return null;
        return membersStore.raw.find(item => item.id === memberId) || null;
    });

    let ready = false;

    $effect(() => {
        if (!user.loaded) return;

        if (user.role !== "admin" && user.role !== "board_member") {
            push("/dashboard");
        }

        if (!memberId) {
            push("/members");
        } else if (membersStore.raw.length === 0) {
            init("members");
        } else if (!memberData) {
            push("/members");
        }

        ready = true;
    });

    let isDeleting = $state(false);

    $effect(() => {
        const _trigger = lastRefresh.MEMBERS;

        if (!ready || isDeleting) return;

        (async () => {
            const exists = await memberExists(memberId);
            if (!exists) {
                addToast({
                    title: "Mitglied nicht mehr verfügbar",
                    subTitle: viewport.isMobile ? "" : "Dieses Mitglied wurde gelöscht und ist nicht mehr verfügbar.",
                    type: "error"
                });

                await fetchAndSetRaw();
                await push("/members");
            }
        })();
    });

    function updateIsEditing(val) { isEditing = val; }

    function updateIsDeleting(val) { isDeleting = val; }
</script>

{#if memberData && !isGlobalLoading}
    {#key memberData.rev}
        {#if viewport.isMobile}
            <MemberDetailsMobile {memberData} bind:isEditing bind:isDeleting onChangeIsEditing={updateIsEditing}
                                 onChangeIsDeleting={updateIsDeleting} />
        {:else}
            <MemberDetailsDesktop {memberData} bind:isEditing bind:isDeleting onChangeIsEditing={updateIsEditing}
                                  onChangeIsDeleting={updateIsDeleting} />
        {/if}
    {/key}
{:else}
    <div class="w-full h-screen flex justify-center items-center">
        <Spinner title="GVW Office" subTitle="Daten werden geladen..." />
    </div>
{/if}