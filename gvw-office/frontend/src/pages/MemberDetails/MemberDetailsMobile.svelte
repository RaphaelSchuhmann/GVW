<script>
    import { push } from "svelte-spa-router";
    import { resetMemberPassword, updateMember, statusMapI2D, statusMapD2I, voiceMapI2D, voiceMapD2I } from "../../services/membersService.svelte";
    import { viewport } from "../../stores/viewport.svelte";
    import { roleMapI2D, roleMapD2I } from "../../services/userService.svelte";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Button from "../../components/Button.svelte";
    import Input from "../../components/Input.svelte";
    import ConfirmDeleteModal from "../../components/ConfirmDeleteModal.svelte";
    import Dropdown from "../../components/Dropdown.svelte";
    import YearDatepicker from "../../components/YearDatepicker.svelte";
    import DefaultDatepicker from "../../components/DefaultDatepicker.svelte";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import { formatISODateString, getYearFromISOString } from "../../services/dateTimeUtils.js";

    let {
        memberData,
        isEditing = $bindable(false),
        isDeleting = $bindable(false),
        onChangeIsEditing = () => {},
        onChangeIsDeleting = () => {},
        ...restProps
    } = $props();

    let draft = $state(null);
    let isSubmitting = $state(false);

    /**
     * Initializes edit mode for the current member.
     *
     * Creates a deep clone of `memberData` and assigns it to `draft`
     * to allow non-destructive editing. Enables the editing state.
     */
    function startEditing() {
        draft = JSON.parse(JSON.stringify(memberData));
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

    const REQUIRED_MEMBER_FIELDS = ["name", "surname", "email", "phone", "address", "voice", "status", "role", "birthdate", "joined"];

    const hasChanges = $derived.by(() => {
        if (!draft || !memberData) return false;
        const allFieldsFilled = REQUIRED_MEMBER_FIELDS.every(field => {
            const value = draft[field];
            return value !== null && value !== undefined && String(value).trim() !== "";
        });

        if (!allFieldsFilled) return false;
        return REQUIRED_MEMBER_FIELDS.some(field => draft[field] !== memberData[field]);
    });

    /**
     * Pre-effect that ensures a draft exists when entering edit mode.
     *
     * If editing is enabled but no draft is present,
     * a deep clone of the current member data is created.
     *
     * Acts as a safety mechanism against inconsistent state.
     */
    $effect.pre(() => {
        if (isEditing && !draft) {
            draft = JSON.parse(JSON.stringify(memberData));
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
    async function updateMemberData() {
        isSubmitting = true;
        try {
            await updateMember($state.snapshot(draft));
        } finally {
            isSubmitting = false;
            onChangeIsEditing(false);
            draft = null;
        }
    }

    /**
     * Navigates back to the members overview page.
     *
     * - Refreshes the raw member list
     * - Performs route navigation to `/members`
     *
     * Ensures the overview reflects the latest persisted state.
     */
    async function routeToMembers() {
        await fetchAndSetRaw();
        await push("/members");
    }

    // ==================
    // MODAL REFERENCES
    // ==================
    /**
     * Reference to the delete confirmation modal.
     * Used to initiate and confirm member deletion flow.
     * @type {import("../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteMemberModal = null;

    function startDelete() {
        if (confirmDeleteMemberModal) {
            onChangeIsDeleting(true);
            confirmDeleteMemberModal.startDelete();
        }
    }

    function updateVoice(value) { draft.voice = voiceMapD2I[value]; }

    function updateStatus(value) { draft.status = statusMapD2I[value]; }

    function updateRole(value) { draft.role = value; }

    function updateBirthdate(value) { draft.birthdate = value; }

    function updateJoined(value) { draft.joined = value; }

    function disableIsDeleting() { onChangeIsDeleting(false); }
</script>

<ToastStack isMobile={true}></ToastStack>

<ConfirmDeleteModal expectedInput={`${memberData.name} ${memberData.surname}`} id={memberData.id}
                    title="Mitglied löschen" subTitle="Sind Sie sich sicher das Sie dieses Mitglied löschen möchten?"
                    action="deleteMember"
                    onClose={async () => {await push("/members")}}
                    onCancel={disableIsDeleting}
                    bind:this={confirmDeleteMemberModal} isMobile={true}
/>

<main class="flex h-screen overflow-hidden">
    <div class="flex flex-col min-h-0 w-full p-7 overflow-hidden">
        <PageHeader title="Mitglied" subTitle={`Daten von "${memberData?.name ?? ""} ${memberData?.surname ?? ""}"`}>
            {#if viewport.width > 900}
                {#if !isEditing}
                    <Button type="secondary" onclick={routeToMembers()}>
                        <span class="material-symbols-rounded text-icon-dt-5">arrow_back</span>
                        <p class="ml-2 text-dt-3">Zurück</p>
                    </Button>
                {/if}
            {:else}
                <button
                    type="button"
                    class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2"
                    onclick={async () => await routeToMembers()}
                >
                    <span class="material-symbols-rounded text-icon-dt-2">close</span>
                </button>
            {/if}
        </PageHeader>

        <div class="flex-1 min-h-0 overflow-y-auto w-full">
            <div class="flex flex-col items-center gap-5 min-[1500px]:w-1/2 min-[1200px]:w-2/3 w-full mt-5">
                {#if viewport.width < 900 && !isEditing}
                    <div class="flex items-center gap-2 w-full">
                        <Button type="delete" onclick={startDelete}>
                            <span class="material-symbols-rounded mr-2">delete</span>
                            Löschen
                        </Button>
                        <Button type="primary" onclick={startEditing}>
                            <span class="material-symbols-rounded mr-2">person_edit</span>
                            Bearbeiten
                        </Button>
                    </div>
                    <div class="flex items-center w-full">
                        <Button type="secondary" onclick={async () => await resetMemberPassword(memberData.id)}>Passwort
                            Zurücksetzten
                        </Button>
                    </div>
                {/if}

                {#if !isEditing}
                    <div class="flex items-center gap-4 w-full max-[900px]:flex-col">
                        <Input
                            value={memberData.name}
                            title="Vorname"
                            readonly={!isEditing}
                        />
                        <Input
                            value={memberData.surname}
                            title="Nachname"
                            readonly={true}
                        />
                    </div>

                    <Input value={memberData.email} title="E-Mail" placeholder="max.mustermann@email.com"
                           readonly={true} />
                    <Input value={memberData.phone} title="Telefon" placeholder="01701234 5678"
                           readonly={true} />
                    <Input value={memberData.address} title="Adresse" placeholder="Hauptstraße 1..."
                           readonly={true} />

                    <div class="w-full flex items-center gap-4 max-[900px]:flex-col">
                        <Input value={voiceMapI2D[memberData.voice] || memberData.voice} title="Stimmlage" readonly={true} />
                        <Input value={statusMapI2D[memberData.status] || memberData.status} title="Status" readonly={true} />
                        <Input value={roleMapI2D[memberData.role] || memberData.role} title="Rolle" readonly={true} />
                    </div>

                    <div class="w-full flex items-center min-[900px]:gap-4 gap-5 max-[900px]:flex-col">
                        <Input value={formatISODateString(memberData.birthdate)} title="Geburtsdatum" readonly={true} />
                        <Input value={getYearFromISOString(memberData.joined)} title="Mitglied seit" readonly={true} />
                    </div>
                {:else}
                    <div class="flex items-center min-[900px]:gap-4 gap-5 w-full max-[900px]:flex-col">
                        <Input
                            bind:value={draft.name}
                            title="Vorname"
                        />
                        <Input
                            bind:value={draft.surname}
                            title="Nachname"
                        />
                    </div>

                    <Input bind:value={draft.email} title="E-Mail"
                           placeholder="max.mustermann@email.com" />
                    <Input bind:value={draft.phone} title="Telefon" placeholder="01701234 5678" />
                    <Input bind:value={draft.address} title="Adresse" placeholder="Hauptstraße 1..." />

                    <div class="w-full flex items-center min-[900px]:gap-4 gap-5 max-[900px]:flex-col">
                        <Dropdown onChange={updateVoice} selected={voiceMapI2D[draft.voice] || draft.voice}
                                  title="Stimmlage"
                                  options={["1. Tenor", "2. Tenor", "1. Bass", "2. Bass"]} showDropshadow={true} />

                        <Dropdown onChange={updateStatus}
                                  selected={statusMapI2D[draft.status] || draft.status} title="Status"
                                  options={["Aktiv", "Passiv"]} showDropshadow={true} />

                        <Dropdown onChange={updateRole} selected={roleMapI2D[draft.role] || draft.role}
                                  title="Rolle"
                                  options={["Mitglied", "Vorstand", "Schriftführer", "Chorleitung", "Notenwart"]} showDropshadow={true} />
                    </div>

                    <div class="w-full flex items-center min-[900px]:gap-4 gap-5 max-[900px]:flex-col">
                        <div class="flex flex-col items-start w-full">
                            <p class="text-dt-6 font-medium mb-1">Geburtsdatum</p>
                            <DefaultDatepicker onChange={updateBirthdate}
                                               selected={draft.birthdate} />
                        </div>

                        <div class="flex flex-col items-start w-full">
                            <p class="text-dt-6 font-medium mb-1">Mitglied seit</p>
                            <YearDatepicker onChange={updateJoined} selected={draft.joined} />
                        </div>
                    </div>
                {/if}

                {#if viewport.width > 900 && !isEditing}
                    <div class="flex items-center gap-4 w-full">
                        <Button type="delete" onclick={startDelete}>
                            <span class="material-symbols-rounded mr-2">delete</span>
                            Löschen
                        </Button>
                        <Button type="primary" onclick={startEditing}>
                            <span class="material-symbols-rounded mr-2">person_edit</span>
                            Bearbeiten
                        </Button>
                    </div>
                    <div class="flex items-center w-full">
                        <Button type="secondary" onclick={async () => await resetMemberPassword(memberData.id)}>Passwort
                            Zurücksetzten
                        </Button>
                    </div>
                {/if}

                {#if isEditing}
                    <div class="flex items-center w-full gap-2">
                        <Button type="secondary" onclick={cancelEditing} isCancel={true}>Abbrechen</Button>
                        <Button type="primary" disabled={!hasChanges || isSubmitting} onclick={async () => await updateMemberData()}>
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