<script>
    import { changelogsStore } from "../../stores/changelogs.svelte.js";
    import { viewport } from "../../stores/viewport.svelte.js";

    import ToastStack from "../../components/ToastStack.svelte";
    import DesktopSidebar from "../../components/DesktopSidebar.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Card from "../../components/Card.svelte";
    import AccordionList from "../../components/AccordionList.svelte";
    import ChangelogListItem from "../../components/ChangelogListItem.svelte";
    import Modal from "../../components/Modal.svelte";
    import Input from "../../components/Input.svelte";
    import Textarea from "../../components/Textarea.svelte";
    import Button from "../../components/Button.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import { addChangelog } from "../../services/changelogService.svelte.js";
    import HorizontalNavBar from "../../components/AdminHorizontalNavBar.svelte";
    import {push} from "svelte-spa-router";
    import {adminDashboardStore} from "../../stores/adminDashboard.svelte.js";

    /** @type {import("../../components/Modal.svelte").default} */
    let addChangelogModal = null;

    let isSubmitting = $state(false);

    let addChangelogInputs = $state({
        title: "",
        version: "",
        content: ""
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

    async function routeToReportHub() { await push("/admin/reportHub"); }
    async function routeToUserManagement() { await push("/admin/userManagement"); }
</script>

<ToastStack />

<Modal bind:this={addChangelogModal} title="Neuen Changelog hinzufügen" subTitle="Erfassen Sie hier die Changelogdaten"
       extraFunction={resetChangelogInputs}>
    <div class="flex items-center w-full gap-4 mt-5">
        <Input bind:value={addChangelogInputs.title} title="Titel" placeholder="Changelog v1.0" />
        <Input bind:value={addChangelogInputs.version} title="Version" placeholder="v1.0" />
    </div>
    <Textarea bind:value={addChangelogInputs.content} title="Inhalt" placeholder="Informationen über Änderungen..."
              height="h-[20vh]" marginTop="5" />
    <div class="w-full flex items-center justify-end mt-5 gap-2">
        <Button type="secondary" onclick={addChangelogModal.hideModal}>Abbrechen</Button>
        <Button type="primary" disabled={addChangelogBtnDisabled} onclick={submitNewChangelog}>
            {#if isSubmitting}
                <Spinner light={true} />
                <p>Speichern...</p>
            {:else}
                Hinzufügen
            {/if}
        </Button>
    </div>
</Modal>

<main class="flex h-screen overflow-hidden">
    <DesktopSidebar currentPage="adminDashboard" />
    <div class="flex-1 min-h-0 overflow-y-auto">
        <div class="flex flex-col w-full h-full flex-1 overflow-hidden p-10 min-h-0">
            <HorizontalNavBar currentPage="overview" />
            <PageHeader title="Admin Dashboard" subTitle=""
                        showSlot={false} marginTop="5" hideSubTitle={true} />
            <div class="flex max-[1300px]:flex-col min-[1300px]:h-full w-full gap-4 mt-10 overflow-y-auto">
                <div class="w-full h-full flex flex-col items-center">
                    <Card fillHeight={viewport.width > 1300}>
                        <div class="w-full flex items-center justify-start p-2">
                            <p class="font-medium text-gv-dark-text text-dt-3">Changelogs</p>
                            <button
                                aria-label="Neuen Changelog hinzufügen"
                                title="Neuen Changelog hinzufügen"
                                class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2 ml-auto"
                                onclick={addChangelogModal.showModal}
                            >
                                <span class="material-symbols-rounded text-icon-dt-4">add</span>
                            </button>
                        </div>
                        <div class="w-full h-full flex flex-col items-center max-[1300px]:max-h-[50vh]">
                            <AccordionList itemComponent={ChangelogListItem} list={changelogsStore} />
                        </div>
                    </Card>
                </div>
                <div class="w-full {viewport.width > 1300 ? 'h-full' : ''} gap-4 flex flex-col items-center">
                    <Card fillHeight={viewport.width > 1300}>
                        <div class="w-full flex items-center justify-start p-2">
                            <p class="font-medium text-gv-dark-text text-dt-3">Berichte Hub</p>
                        </div>
                        <div class="w-full h-full flex flex-col items-center max-[1300px]:max-h-[50vh] p-2 gap-4">
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">chat_info</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Feedback</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.reportHub.feedbackCount}</p>
                            </div>
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">bug_report</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Bug Reports</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.reportHub.bugReportCount}</p>
                            </div>
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded-filled text-icon-dt-5 text-gv-sentiment-selected">star</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Bewertung</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.reportHub.averageSentiment}</p>
                            </div>
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">language</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Hash</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.reportHub.mostUsedHash}</p>
                            </div>
                            <Button type="primary" onclick={routeToReportHub}>
                                <span class="text-dt-5">Details</span>
                                <span class="material-symbols-rounded">chevron_right</span>
                            </Button>
                        </div>
                    </Card>
                    <Card fillHeight={viewport.width > 1300}>
                        <div class="w-full flex items-center justify-start p-2">
                            <p class="font-medium text-gv-dark-text text-dt-3">Nutzerverwaltung</p>
                        </div>
                        <div class="w-full h-full flex flex-col items-center max-[1300px]:max-h-[50vh] p-2 gap-4">
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">groups</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Benutzer</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{adminDashboardStore.userManagement.userCount}</p>
                            </div>
                            <div class="flex w-full items-center justify-start gap-2">
                                <span class="material-symbols-rounded text-icon-dt-5 text-gv-dark-text">verified_off</span>
                                <p class="font-medium text-gv-dark-text text-dt-4">Orphaned Benutzer</p>
                                <p class="font-medium text-gv-light-text text-dt-4">{`${adminDashboardStore.userManagement.orphanedUserCount} / ${adminDashboardStore.userManagement.userCount}`}</p>
                            </div>
                            <Button type="primary" onclick={routeToUserManagement}>
                                <span class="text-dt-5">Details</span>
                                <span class="material-symbols-rounded">chevron_right</span>
                            </Button>
                        </div>
                    </Card>
                </div>
            </div>
        </div>
    </div>
</main>