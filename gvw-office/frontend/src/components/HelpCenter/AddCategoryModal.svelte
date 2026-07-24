<script>
    import Modal from "../Modal.svelte";
    import Input from "../Input.svelte";
    import Textarea from "../Textarea.svelte";
    import Button from "../Button.svelte";
    import Spinner from "../Spinner.svelte";
    import { addHelpCenterCategory } from "../../services/helpCenterService.svelte.js";
    import { loadAppSettings } from "../../services/appSettingsSyncService.svelte.js";

    let {
        isMobile = false,
        ...restProps
    } = $props();

    /**
     * Reference to the modal.
     * Used to programmatically open the dialog.
     * @type {import("../../components/Modal.svelte").default}
     */
    let modalRef = null;

    let isSubmitting = $state(false);

    let inputs = $state({
        title: "",
        icon: "",
        description: ""
    });

    let addDisabled = $derived(!inputs.title || !inputs.icon);

    async function submit() {
        isSubmitting = true;

        try {
            if (!inputs.description) {
                inputs.description = "Keine Beschreibung";
            }

            await addHelpCenterCategory(inputs);
        } finally {
            isSubmitting = false;
            await loadAppSettings();
            hideModal();
        }
    }

    function cleanup() {
        inputs.title = "";
        inputs.icon = "";
        inputs.description = "";
    }

    export function showModal() {
        if (modalRef) modalRef.showModal();
    }

    export function hideModal() {
        if (modalRef) modalRef.hideModal();
    }
</script>

<Modal bind:this={modalRef} title="Kategorie hinzufügen" hideSubTitle={true} isMobile={isMobile}
       extraFunction={cleanup} width="1/2">
    <div class="flex flex-col w-full items-center justify-start gap-4">
        {#if isMobile}
            <Input bind:value={inputs.title} title="Titel" placeholder="Kategorie Titel" />
            <Input bind:value={inputs.icon} title="Icon (fonts.google.com/icons)" placeholder="groups" />
        {:else}
            <div class="flex w-full items-center gap-2">
                <Input bind:value={inputs.title} title="Titel" placeholder="Kategorie Titel" />
                <Input bind:value={inputs.icon} title="Icon (fonts.google.com/icons)" placeholder="groups" />
            </div>
        {/if}

        <Textarea bind:value={inputs.description} title="Beschreibung" placeholder="Kurze Beschreibung..." height="h-1/5" />

        <div class="w-full flex items-center justify-end gap-4">
            <Button type="secondary" onclick={hideModal}>Abbrechen</Button>
            <Button type="primary" disabled={addDisabled || isSubmitting} onclick={submit} isSubmit={true}>
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