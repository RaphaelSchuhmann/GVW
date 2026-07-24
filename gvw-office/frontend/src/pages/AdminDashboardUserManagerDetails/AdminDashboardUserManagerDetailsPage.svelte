<script>
    import { viewport } from "../../stores/viewport.svelte";
    import { membersStore } from "../../stores/members.svelte";
    import { push } from "svelte-spa-router";
    import { fetchAndSetRaw, init } from "../../services/filterService.svelte";
    import { user } from "../../stores/user.svelte";
    import { userManagerStore } from "../../stores/userManager.svelte.js";
    import { lastRefresh } from "../../stores/sseStore.svelte.js";
    import { addToast } from "../../stores/toasts.svelte.js";
    import { userExists } from "../../services/userService.svelte.js";

    import AdminDashboardUserManagerDetailsDesktop from "./AdminDashboardUserManagerDetailsDesktop.svelte";
    import AdminDashboardUserManagerDetailsMobile from "./AdminDashboardUserManagerDetailsMobile.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import LibraryDetailsMobile from "../LibraryDetails/LibraryDetailsMobile.svelte";

    let isGlobalLoading = $derived(user.name.length === 0);

    const hash = window.location.hash;
    const queryString = hash.split("?")[1];
    const params = new URLSearchParams(queryString);

    const userId = params.get("id");

    let isEditing = $state(params.get("editing") === "true");

    const userData = $derived.by(() => {
        if (!userId) return null;
        return userManagerStore.raw.find(item => item.id === userId) || null;
    });

    let ready = false;

    $effect(() => {
        if (!user.loaded) return;

        if (user.role !== "admin") {
            push("/dashboard");
        }

        if (!userId) {
            push("/admin/userManagement");
            return;
        } else if (membersStore.raw.length === 0) {
            init("userManager");
        } else if (!userData) {
            push("/admin/userManagement");
            return;
        }

        if (userData && !userData.isOrphan) {
            push("/admin/userManagement");
            return;
        }

        ready = true;
    });

    let isDeleting = $state(false);

    $effect(() => {
        const _trigger = lastRefresh.USER;

        if (!ready || isDeleting) return;

        (async () => {
            const exists = await userExists(userId);
            if (!exists) {
                addToast({
                    title: "Benutzer nicht mehr verfügbar",
                    subTitle: viewport.isMobile ? "" : "Dieser Benutzer wurde gelöscht und ist nicht mehr verfügbar.",
                    type: "error"
                });

                await fetchAndSetRaw();
                await push("/admin/userManagement");
            }
        })();
    });

    function updateIsEditing(val) { isEditing = val; }

    function updateIsDeleting(val) { isDeleting = val; }
</script>

{#if userData && !isGlobalLoading}
    {#key userData.rev}
        {#if viewport.isMobile}
            <AdminDashboardUserManagerDetailsMobile {userData} bind:isEditing bind:isDeleting
                                                    onChangeIsEditing={updateIsEditing}
                                                    onChangeIsDeleting={updateIsDeleting} />
        {:else}
            <AdminDashboardUserManagerDetailsDesktop {userData} bind:isEditing bind:isDeleting
                                                     onChangeIsEditing={updateIsEditing}
                                                     onChangeIsDeleting={updateIsDeleting} />
        {/if}
    {/key}
{:else}
    <div class="w-full h-screen flex justify-center items-center">
        <Spinner title="GVW Office" subTitle="Daten werden geladen..." />
    </div>
{/if}