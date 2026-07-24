<script>
    import Modal from "../Modal.svelte";
    import Input from "../Input.svelte";
    import Textarea from "../Textarea.svelte";
    import Button from "../Button.svelte";
    import Spinner from "../Spinner.svelte";
    import { addHelpCenterArticle, getArticles } from "../../services/helpCenterService.svelte.js";
    import { loadAppSettings } from "../../services/appSettingsSyncService.svelte.js";

    let {
        isMobile = false,
        ...restProps
    } = $props();

    let modalRef = null;

    let inputs = $state({
        title: "",
        description: "",
        tags: ""
    });

    let addDisabled = $derived(!inputs.title || !inputs.tags);
    let isSubmitting = $state(false);

    async function submit() {
        isSubmitting = true;

        try {
            const data = { title: inputs.title, description: inputs.description || "Keine Beschreibung", tags: inputs.tags.split(",").map(tag => tag.trim()) }
            await addHelpCenterArticle(data);
        } finally {
            isSubmitting = false;
            await loadAppSettings();
            hideModal();
        }
    }

    function cleanUp() {
        inputs.title = "";
        inputs.description = "";
        inputs.tags = "";
    }

    export function showModal() {
        if (modalRef) modalRef.showModal();
    }

    export function hideModal() {
        if (modalRef) modalRef.hideModal();
    }
</script>

<Modal bind:this={modalRef} title="Artikel Hinzufügen" hideSubTitle={true} extraFunction={cleanUp} isMobile={isMobile}>
    <div class="flex flex-col items-center justify-start w-full gap-4">
        <Input title="Titel" placeholder="Artikel Titel" bind:value={inputs.title} />
        <Textarea title="Beschreibung" placeholder="Kurze Beschreibung..." bind:value={inputs.description} />
        <Input title="Tags (Comma Separated)" placeholder="Tag #1, Tag #2" bind:value={inputs.tags} />
        <div class="w-full flex items-center justify-end gap-4">
            <Button type="secondary" onclick={hideModal}>Abbrechen</Button>
            <Button type="primary" disabled={addDisabled} onclick={submit} isSubmit={true}>
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