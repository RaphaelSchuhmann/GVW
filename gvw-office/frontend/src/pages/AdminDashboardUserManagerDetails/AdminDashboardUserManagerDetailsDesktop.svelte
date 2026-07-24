<script>
    import { push } from "svelte-spa-router";
    import { viewport } from "../../stores/viewport.svelte";
    import { resetPassword, roleMapD2I, roleMapI2D, updateUser } from "../../services/userService.svelte";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import DesktopSidebar from "../../components/DesktopSidebar.svelte";
    import Button from "../../components/Button.svelte";
    import Input from "../../components/Input.svelte";
    import ConfirmDeleteModal from "../../components/ConfirmDeleteModal.svelte";
    import Dropdown from "../../components/Dropdown.svelte";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";
    import Spinner from "../../components/Spinner.svelte";

    let {
        userData,
        isEditing = $bindable(false),
        isDeleting = $bindable(false),
        onChangeIsEditing = () => {},
        onChangeIsDeleting = () => {},
        ...restProps
    } = $props();

    let draft = $state({
        name: "",
        email: "",
        phone: "",
        address: "",
        type: ""
    });
    let isSubmitting = $state(false);

    /**
     * Initializes edit mode for the current user.
     *
     * Creates a deep clone of `userData` and assigns it to `draft`
     * to allow non-destructive editing. Enables the editing state.
     */
    function startEditing() {
        const clone = JSON.parse(JSON.stringify(userData));

        const fields = ["name", "email", "phone", "address", "type"];
        fields.forEach(field => {
            if (clone[field] == null) clone[field] = "";
        });

        draft = clone;
        onChangeIsEditing(true);
    }

    /**
     * Cancels the current editing session.
     *
     * Discards the draft object and exits edit mode
     * without persisting any changes.
     */
    function cancelEditing() {
        draft = null;
        onChangeIsEditing(false);
    }

    const REQUIRED_USER_FIELDS = ["name", "email", "phone", "address", "type"];

    const hasChanges = $derived.by(() => {
        if (!draft || !userData) return false;

        const allFieldsFilled = REQUIRED_USER_FIELDS.every(field => {
            const value = draft[field];
            return value !== null && value !== undefined && String(value).trim() !== "";
        });

        if (!allFieldsFilled) return false;

        let isDifferent = false;
        for (const field of REQUIRED_USER_FIELDS) {
            const draftVal = draft[field] ?? "";
            const originalVal = userData[field] ?? "";

            if (draftVal !== originalVal) {
                isDifferent = true;
                break;
            }
        }

        return isDifferent;
    });

    /**
     * Pre-effect that ensures a draft exists when entering edit mode.
     *
     * If editing is enabled but no draft is present,
     * a deep clone of the current user data is created.
     *
     * Acts as a safety mechanism against inconsistent state.
     */
    $effect.pre(() => {
        if (isEditing && !draft) {
            draft = JSON.parse(JSON.stringify(userData));
        }
    });

    /**
     * Persists the current draft to the backend.
     *
     * - Sends a snapshot of the draft to the update API
     * - Exits edit mode and clears the draft
     *
     * Assumes validation has already been handled externally.
     */
    async function updateUserData() {
        isSubmitting = true;
        try {
            await updateUser($state.snapshot(draft));
        } finally {
            isSubmitting = false;
            onChangeIsEditing(false);
            draft = null;
        }
    }

    /**
     * Navigates back to the user overview page.
     *
     * - Refreshes the raw user list
     * - Performs route navigation to `/admin/userManagement`
     *
     * Ensures the overview reflects the latest persisted state.
     */
    async function routeToUserManager() {
        await fetchAndSetRaw();
        await push("/admin/userManagement");
    }

    function startDeleting() {
        onChangeIsDeleting(true);
        confirmDeleteUserModal.startDelete();
    }

    // ==================
    // MODAL REFERENCES
    // ==================
    /**
     * Reference to the delete confirmation modal.
     * Used to initiate and confirm user deletion flow.
     * @type {import("../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteUserModal = null;

    function resetUserPassword(e) { resetPassword(e.currentTarget.dataset.id); }

    function updateRole(value) { draft.type = roleMapD2I[value] || value; }

    function disableIsDeleting() { onChangeIsDeleting(false); }
</script>

<ToastStack />

<ConfirmDeleteModal expectedInput={userData.name} id={userData.id}
                    title="Benutzer löschen" subTitle="Sind Sie sich sicher das Sie diesen Benutzer löschen möchten?"
                    action="deleteUser"
                    onClose={async () => {await push("/admin/userManagement"); await fetchAndSetRaw();}}
                    onCancel={disableIsDeleting}
                    bind:this={confirmDeleteUserModal}
/>

<main class="flex h-screen overflow-hidden">
    <DesktopSidebar currentPage="adminDashboard" />
    <div class="flex flex-col min-h-0 w-full p-10 overflow-hidden">
        <PageHeader title="Benutzer" subTitle={`Daten von "${userData?.name ?? ""}"`}>
            {#if viewport.width > 900}
                {#if !isEditing}
                    <Button type="secondary" onclick={async () => await routeToUserManager()}>
                        <span class="material-symbols-rounded text-icon-dt-5">arrow_back</span>
                        <p class="ml-2 text-dt-3">Zurück</p>
                    </Button>
                {/if}
            {:else}
                <button
                    type="button"
                    class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2"
                    onclick={async () => await routeToUserManager()}
                >
                    <span class="material-symbols-rounded text-icon-dt-2">close</span>
                </button>
            {/if}
        </PageHeader>

        <div class="flex-1 min-h-0 overflow-y-auto w-full">
            <div class="flex flex-col items-center gap-5 min-[1500px]:w-1/2 min-[1200px]:w-2/3 w-full mt-5 p-0.5">
                {#if viewport.width < 900 && !isEditing}
                    <div class="flex items-center gap-2 w-full">
                        <Button type="delete" onclick={startDeleting}>
                            <span class="material-symbols-rounded mr-2">delete</span>
                            Löschen
                        </Button>
                        <Button type="primary" onclick={startEditing}>
                            <span class="material-symbols-rounded mr-2">person_edit</span>
                            Bearbeiten
                        </Button>
                    </div>
                    <div class="flex items-center w-full">
                        <Button type="secondary" data-id={userData.id} onclick={resetUserPassword}>
                            Passwort Zurücksetzten
                        </Button>
                    </div>
                {/if}

                {#if !isEditing}
                    <div class="flex items-center gap-4 w-full max-[900px]:flex-col">
                        <Input
                            value={userData.name}
                            title="Name"
                            readonly={!isEditing}
                        />
                        <Input
                            value={userData.email}
                            title="E-Mail"
                            readonly={true}
                        />
                    </div>

                    <Input value={userData.phone || "Unbekannt"} title="Telefon" placeholder="01701234 5678"
                           readonly={true} />

                    <Input value={userData.address || "Unbekannt"} title="Adresse" placeholder="Hauptstraße 1..."
                           readonly={true} />

                    <Input value={roleMapI2D[userData.type] || userData.type} title="Rolle" readonly={true} />
                {:else}
                    <div class="flex items-center min-[900px]:gap-4 gap-5 w-full max-[900px]:flex-col">
                        <Input
                            bind:value={draft.name}
                            title="Name"
                        />
                        <Input
                            bind:value={draft.email}
                            title="E-Mail"
                            placeholder="max.mustermann@email.com"
                        />
                    </div>

                    <Input bind:value={draft.phone} title="Telefon" placeholder="01701234 5678" />

                    <Input bind:value={draft.address} title="Adresse" placeholder="Hauptstraße 1..." />

                    <Dropdown onChange={updateRole}
                              title="Rolle"
                              options={["Admin", "Mitglied", "Vorstand", "Schriftführer", "Chorleitung", "Notenwart"]} />
                {/if}

                {#if viewport.width > 900 && !isEditing}
                    <div class="flex items-center gap-4 w-full">
                        <Button type="delete" onclick={startDeleting}>
                            <span class="material-symbols-rounded mr-2">delete</span>
                            Löschen
                        </Button>
                        <Button type="primary" onclick={startEditing}>
                            <span class="material-symbols-rounded mr-2">person_edit</span>
                            Bearbeiten
                        </Button>
                    </div>
                    <div class="flex items-center w-full">
                        <Button type="secondary" data-id={userData.id} onclick={resetUserPassword}>
                            Passwort Zurücksetzten
                        </Button>
                    </div>
                {/if}

                {#if viewport.width > 900 && isEditing}
                    <div class="flex items-center w-full gap-2">
                        <Button type="secondary" onclick={cancelEditing} isCancel={true}>Abbrechen</Button>
                        <Button type="primary" disabled={!hasChanges || isSubmitting}
                                onclick={async () => await updateUserData()}>
                            {#if isSubmitting}
                                <Spinner light={true} />
                                <p>Speichern...</p>
                            {:else}
                                Speichern
                            {/if}
                        </Button>
                    </div>
                {/if}
            </div>
        </div>
    </div>
</main>