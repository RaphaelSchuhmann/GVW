<script>
    import { user } from "../../stores/user.svelte";
    import { appSettings } from "../../stores/appSettings.svelte.js";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Card from "../../components/Card.svelte";
    import ComingEvent from "../../components/ComingEvent.svelte";
    import VoiceDistribution from "../../components/VoiceDistribution.svelte";
    import Modal from "../../components/Modal.svelte";
    import Input from "../../components/Input.svelte";
    import Button from "../../components/Button.svelte";
    import { tryUpdateMaxMembers } from "../../services/appSettingsService.svelte";
    import MobileSidebar from "../../components/MobileSidebar.svelte";
    import { dashboardStore } from "../../stores/dashboard.svelte.js";
    import { prepareEvents, getVoiceCounts } from "../../services/dashboardService.svelte.js";
    import Spinner from "../../components/Spinner.svelte";

    /** @type {import("../../components/Modal.svelte").default} */
    let voiceDistributionSettingsModal = null;

    let isSubmitting = $state(false);

    let maxMembers = $state("");

    let events = $derived(prepareEvents());
    let activeMembers = $derived.by(() => {
        let count = 0;
        for (const m of dashboardStore.members) {
            if (m.status === "active") count++;
        }
        return count;
    });
    let voiceCounts = $derived(getVoiceCounts())

    /**
     * Updates the maximum members per voice setting
     * Validates input and handles API response with appropriate toast messages
     */
    async function updateMaxMembersVoiceDistribution() {
        isSubmitting = true;
        let updatedMaxMembers = Number(maxMembers);

        if (isNaN(updatedMaxMembers) || maxMembers.length === 0 || updatedMaxMembers < 1) {
            updatedMaxMembers = appSettings.maxMembers;
        }

        try {
            await tryUpdateMaxMembers(updatedMaxMembers);
        } finally {
            isSubmitting = false;
        }

        voiceDistributionSettingsModal.hideModal();
    }

    let sidebarOpen = $state(false);

    function openSidebar() { sidebarOpen = true; }
</script>

<Modal bind:this={voiceDistributionSettingsModal} title="Stimmenverteilung Einstellungen" subTitle="Einstellungen für die Stimmverteilung"
       extraFunctionOnClose={false} extraFunction={() => maxMembers = String(appSettings.maxMembers)} isMobile={true}>
    <Input title="Maximale anzahl an Mitgliedern pro Stimme" type="number" marginTop="5" bind:value={maxMembers}/>
    <div class="w-full flex items-center justify-end mt-5 gap-2">
        <Button type="secondary" onclick={voiceDistributionSettingsModal.hideModal}>Abbrechen</Button>
        <Button type="primary" disabled={isSubmitting} onclick={updateMaxMembersVoiceDistribution} isSubmit={true}>
            {#if isSubmitting}
                <Spinner light={true} />
                <p>Speichern...</p>
            {:else}
                Hinzufügen
            {/if}
        </Button>
    </div>
</Modal>

<ToastStack isMobile={true}/>

<MobileSidebar currentPage="dashboard" bind:isOpen={sidebarOpen} />

<main class="flex overflow-hidden">
    <div class="flex-1 min-h-0 overflow-y-auto">
        <div class="flex flex-col w-full flex-1 overflow-hidden p-7 min-h-0">
            <div class="w-full flex items-center justify-start">
                <button class="flex items-center justify-center" onclick={openSidebar}>
                    <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">menu</span>
                </button>
            </div>
            <PageHeader title="Dashboard" subTitle="Willkommen in GVW Office - Übersicht Gesangverein Weppersdorf" showSlot={false}/>

            <!-- Small cards -->
            <div class="flex flex-col items-center overflow-hidden mt-8 gap-4">
                <Card padding="5" justify="justify-around">
                    <div class="flex w-full items-center">
                        <p class="text-dt-6 text-gv-dark-text">Aktive Mitglieder</p>
                        <span class="material-symbols-rounded text-icon-dt-5 text-gv-secondary-text ml-auto">group</span>
                    </div>
                    <p class="text-dt-5 text-black w-full text-start">
                        <span>{activeMembers}</span>
                        <span class="text-dt-6 text-gv-light-text"> / {dashboardStore.members.length}</span>
                    </p>
                </Card>
                <Card padding="5" justify="justify-around">
                    <div class="flex w-full items-center">
                        <p class="text-dt-6 text-gv-dark-text">Bevorstehende Events</p>
                        <span
                            class="material-symbols-rounded text-icon-dt-5 text-gv-secondary-text ml-auto">calendar_today</span>
                    </div>
                    <p class="text-dt-5 text-black w-full text-start">
                        <span>{dashboardStore.upcomingEvents.length}</span>
                        <span class="text-dt-6 text-gv-light-text"> / {dashboardStore.totalEvents}</span>
                    </p>
                </Card>
                <Card padding="5" justify="justify-around">
                    <div class="flex w-full items-center">
                        <p class="text-dt-6 text-gv-dark-text">Notenmaterial</p>
                        <span
                            class="material-symbols-rounded text-icon-dt-5 text-gv-secondary-text ml-auto">music_note</span>
                    </div>
                    <p class="text-dt-5 text-black w-full text-start">{dashboardStore.totalScores}</p>
                </Card>
            </div>

            <!-- Big cards -->
            <div class="w-full flex-1 mt-8 gap-4 flex flex-col min-h-0 h-[calc(100vh-200px)] overflow-y-auto">
                <!-- Upcoming Events -->
                <Card padding="5">
                    <p class="text-dt-5 text-gv-dark-text w-full text-start flex flex-col mb-3">
                        <span>Kommende Veranstaltungen</span>
                        <span class="text-gv-light-text text-dt-6">Nächste Veranstaltungen und Proben</span>
                    </p>
                    <div class="w-full flex-1 min-h-0 overflow-x-hidden overflow-y-auto max-h-[47dvh] flex flex-col items-center pr-2">
                        {#each events as event (event.title)}
                            <ComingEvent margin="5" title={event.title} time={event.time} location={event.location}
                                         type={event.type} isMobile={true} />
                        {/each}
                    </div>
                </Card>

                <!-- Voice distribution -->
                <Card padding="5">
                    <div class="flex items-center w-full">
                        <p class="text-dt-5 text-gv-dark-text w-full text-start flex flex-col">
                            <span>Stimmenverteilung</span>
                            <span class="text-gv-light-text text-dt-6">Übersicht der Mitglieder nach Stimmlage</span>
                        </p>
                    </div>
                    <div class="w-full min-h-0 overflow-x-hidden overflow-y-auto flex flex-col items-center pr-2">
                        <VoiceDistribution isMobile={true} voice="1. Tenor" voiceMembers={voiceCounts.tenor1} totalMembers={appSettings.maxMembers} />
                        <VoiceDistribution isMobile={true} voice="2. Tenor" voiceMembers={voiceCounts.tenor2} totalMembers={appSettings.maxMembers} />
                        <VoiceDistribution isMobile={true} voice="1. Bass" voiceMembers={voiceCounts.bass1} totalMembers={appSettings.maxMembers} />
                        <VoiceDistribution isMobile={true} voice="2. Bass" voiceMembers={voiceCounts.bass2} totalMembers={appSettings.maxMembers} />
                    </div>
                    {#if user.role === "board_member" || user.role === "admin"}
                        <div class="w-full flex items-center justify-end pr-2 mt-5">
                            <button
                                class="cursor-pointer flex items-center justify-center rounded-2 p-2 hover:bg-gv-hover-effect"
                                onclick={voiceDistributionSettingsModal.showModal}>
                                <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">settings</span>
                            </button>
                        </div>
                    {/if}
                </Card>
            </div>
        </div>
    </div>
</main>