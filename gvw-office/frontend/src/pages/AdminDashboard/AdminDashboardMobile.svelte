<script>
    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import MobileSidebar from "../../components/MobileSidebar.svelte";
    import ChangelogListItem from "../../components/ChangelogListItem.svelte";
    import { changelogsStore } from "../../stores/changelogs.svelte.js";
    import Card from "../../components/Card.svelte";
    import AccordionList from "../../components/AccordionList.svelte";
    import Modal from "../../components/Modal.svelte";
    import Textarea from "../../components/Textarea.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import Input from "../../components/Input.svelte";
    import Button from "../../components/Button.svelte";
    import { addChangelog } from "../../services/changelogService.svelte.js";
    import HorizontalNavBar from "../../components/AdminHorizontalNavBar.svelte";

    let sidebarOpen = $state(false);

    /** @type {import("../../components/Modal.svelte").default} */
    let addChangelogModal = null;

    // Add changelog

    let isSubmitting = $state(false);

    let addChangelogInputs = $state({
        title: "",
        version: "",
        content: "",
    });

    let addChangelogBtnDisabled = $derived(!(addChangelogInputs.title && addChangelogInputs.version && addChangelogInputs.content) || isSubmitting);

    async function submitNewChangelog() {
        isSubmitting = true;

        try {
            await addChangelog(addChangelogInputs);
        } finally {
            isSubmitting = false;
        }

        addChangelogModal.hideModal();
    }

    function resetChangelogInputs() {
        addChangelogInputs.title = "";
        addChangelogInputs.version = "";
        addChangelogInputs.content = "";
    }

    function openSidebar() { sidebarOpen = true; }
</script>

<ToastStack isMobile={true} />

<Modal isMobile={true} bind:this={addChangelogModal} title="Neuen Changelog hinzufügen" subTitle="Erfassen Sie hier die Changelogdaten"
       extraFunction={resetChangelogInputs}>
    <Input bind:value={addChangelogInputs.title} title="Titel" placeholder="Changelog v1.0" marginTop="5"/>
    <Input bind:value={addChangelogInputs.version} title="Version" placeholder="v1.0" marginTop="5"/>
    <Textarea bind:value={addChangelogInputs.content} title="Inhalt" placeholder="Informationen über Änderungen..." height="h-[20vh]" marginTop="5"/>
    <div class="w-full flex items-center justify-end mt-5 gap-2">
        <Button type="secondary" onclick={addChangelogModal.hideModal}>Abbrechen</Button>
        <Button type="primary" disabled={addChangelogBtnDisabled} onclick={submitNewChangelog} isSubmit={true}>
            {#if isSubmitting}
                <Spinner light={true} />
                <p>Speichern...</p>
            {:else}
                Hinzufügen
            {/if}
        </Button>
    </div>
</Modal>

<MobileSidebar currentPage="adminDashboard" bind:isOpen={sidebarOpen} />

<main class="flex overflow-hidden">
    <div class="flex-1 min-h-0 overflow-y-auto">
        <div class="flex flex-col w-full flex-1 overflow-hidden p-7 min-h-0">
            <div class="w-full flex items-center justify-start">
                <button class="flex items-center justify-center" onclick={openSidebar}>
                    <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">menu</span>
                </button>
            </div>
            <div class="mt-5">
                <HorizontalNavBar currentPage="overview"/>
            </div>
            <PageHeader title="Admin Dashboard" subTitle=""
                        showSlot={false} marginTop="5" hideSubTitle={true} />
            <div class="flex flex-col w-full gap-4 mt-10">
                <Card>
                    <div class="w-full flex items-center justify-start p-1">
                        <p class="text-gv-dark-text text-dt-4">Changelogs</p>
                        <button
                            aria-label="Neuen Changelog hinzufügen"
                            title="Neuen Changelog hinzufügen"
                            class="flex items-center justify-center p-1 cursor-pointer hover:bg-gv-hover-effect rounded-2 ml-auto"
                            onclick={addChangelogModal.showModal}
                        >
                            <span class="material-symbols-rounded text-icon-dt-5">add</span>
                        </button>
                    </div>
                    <div class="w-full flex flex-col items-center max-h-[45vh]">
                        <AccordionList itemComponent={ChangelogListItem} list={changelogsStore} />
                    </div>
                </Card>
                <Card>
                    <p>Card2</p>
                </Card>
                <Card>
                    <p>Card3</p>
                </Card>
            </div>
        </div>
    </div>
</main>