<script>
    import { viewport } from "../stores/viewport.svelte.js";
    import { addToast } from "../stores/toasts.svelte.js";
    import { fetchAndSetRaw } from "../services/filterService.svelte.js";
    import { voiceMapD2I, statusMapD2I, newMember } from "../services/membersService.svelte.js";
    import { roleMapD2I } from "../services/userService.svelte.js";
    import Dropdown from "./Dropdown.svelte";
    import Spinner from "./Spinner.svelte";
    import DefaultDatepicker from "./DefaultDatepicker.svelte";
    import YearDatepicker from "./YearDatepicker.svelte";
    import Button from "./Button.svelte";
    import Input from "./Input.svelte";
    import Modal from "./Modal.svelte";

    let { isMobile = false } = $props();

    /**
     * Reference to the "Add Member" modal.
     * Controls visibility and lifecycle of the member creation dialog.
     * @type {import("../../components/Modal.svelte").default}
     */
    let addMemberModal = null;

    /**
     * Reactive state object representing the input fields
     * of the "Add Member" form.
     *
     * Dropdown values are stored as display labels
     * and mapped to backend enums on submission.
     */
    let memberInput = $state({
        name: "",
        surname: "",
        email: "",
        phone: "",
        address: "",
        voice: null,
        status: null,
        role: null,
        birthdate: "",
        joined: ""
    });

    let isSubmitting = $state(false);

    const REQUIRED_MEMBER_FIELDS = ["name", "surname", "email", "phone", "address", "birthdate", "joined"];
    const DROPDOWN_MEMBER_FIELDS = ["voice", "status", "role"];

    const addDisabled = $derived.by(() => {
        const hasEmptyFields = REQUIRED_MEMBER_FIELDS.some(key => {
            const val = memberInput[key];
            return !val || val.trim() === "";
        });

        const hasUnselectedDropdowns = DROPDOWN_MEMBER_FIELDS.some(key => {
            const val = memberInput[key];
            return !val || val.toLowerCase() === "wählen";
        });

        return hasEmptyFields || hasUnselectedDropdowns || isSubmitting;
    });

    /**
     * Resets all input fields of the "Add Member" form
     * to their initial default values.
     *
     * Called after successful submission or when closing the modal.
     */
    function resetAddInputs() {
        memberInput.name = "";
        memberInput.surname = "";
        memberInput.email = "";
        memberInput.phone = "";
        memberInput.address = "";
        memberInput.birthdate = "";
        memberInput.joined = "";
        memberInput.voice = null;
        memberInput.status = null;
        memberInput.role = null;
    }

    /**
     * Submits the new member to the backend.
     *
     * Workflow:
     * 1. Map dropdown display values to backend enum values.
     * 2. Send member payload to API.
     * 3. Close the modal.
     * 4. Refresh member list from backend.
     *
     * @async
     * @returns {Promise<void>}
     */
    async function submitMember() {
        isSubmitting = true;

        const payload = {
            ...$state.snapshot(memberInput),
            voice: voiceMapD2I[memberInput.voice],
            status: statusMapD2I[memberInput.status],
            role: roleMapD2I[memberInput.role]
        };

        if (!payload.voice || !payload.status || !payload.role) {
            addToast({
                title: "Ungültige Auswahl",
                subTitle: "Bitte prüfen Sie Stimmlage, Status und Rolle.",
                type: "error"
            });
            return;
        }

        try {
            await newMember(payload);
        } finally {
            isSubmitting = false;
        }

        addMemberModal.hideModal();
        await fetchAndSetRaw();

    }

    export function show() {
        addMemberModal.showModal();
    }

    export function hide() {
        addMemberModal.hideModal();
    }

    function updateVoice(value) { memberInput.voice = value; }

    function updateStatus(value) { memberInput.status = value; }

    function updateRole(value) { memberInput.role = value; }

    function updateBirthdate(value) { memberInput.birthdate = value; }

    function updateJoined(value) { memberInput.joined = value; }
</script>

<Modal
    bind:this={addMemberModal}
    extraFunction={resetAddInputs}
    title="Neues Mitglied hinzufügen"
    subTitle="Erfassen Sie hier die Mitgliedsdaten"
    width={viewport.width > 1300 ? "2/5" : "3/5"}
    isMobile={isMobile}
>
    <div class="w-full h-full flex flex-col items-center justify-start gap-4">
        <div class="flex flex-col w-full md:flex-row md:items-center gap-4">
            <Input bind:value={memberInput.name} title="Vorname" placeholder="Max" />
            <Input bind:value={memberInput.surname} title="Nachname" placeholder="Mustermann" mdMarginTop="0" />
        </div>

        <Input bind:value={memberInput.email} title="E-Mail" placeholder="max.mustermann@email.com" />

        <Input bind:value={memberInput.phone} title="Telefon" placeholder="01701234 5678" />

        <Input bind:value={memberInput.address} title="Adresse" placeholder="Hauptstraße 1..." />

        <div class="w-full flex flex-col md:flex-row md:items-center gap-4">
            <Dropdown onChange={updateVoice} title="Stimmlage"
                      options={["1. Tenor", "2. Tenor", "1. Bass", "2. Bass"]} mdMarginTop="0" showDropshadow={true} />

            <Dropdown onChange={updateStatus} title="Status" options={["Aktiv", "Passiv"]}
                      mdMarginTop="0" showDropshadow={true} />

            <Dropdown onChange={updateRole} title="Rolle"
                      options={["Mitglied", "Vorstand", "Schriftführer", "Chorleitung", "Notenwart"]} displayTop={true}
                      mdMarginTop="0" showDropshadow={true} />
        </div>

        <div class="w-full flex flex-col xl:flex-row xl:items-center gap-4">
            <div class="flex flex-col items-start w-full">
                <p class="text-dt-6 font-medium mb-1">Geburtsdatum</p>
                <DefaultDatepicker onChange={updateBirthdate} />
            </div>

            <div class="flex flex-col items-start w-full">
                <p class="text-dt-6 font-medium mb-1">Mitglied seit</p>
                <YearDatepicker onChange={updateJoined} />
            </div>
        </div>

        <!-- Action Buttons -->
        <div class="w-full flex items-center justify-end mt-5 gap-4">
            <Button type="secondary" onclick={hide}>Abbrechen</Button>
            <Button type="primary" disabled={addDisabled} onclick={submitMember} isSubmit={true}>
                {#if isSubmitting}
                    <Spinner light={true} />
                    <p>Speichern...</p>
                {:else}
                    Hinzufügen
                {/if}
            </Button>
        </div>
    </div>
</Modal>