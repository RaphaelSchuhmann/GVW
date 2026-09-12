<script>
    import { user } from "../../stores/user.svelte";
    import { appSettings } from "../../stores/appSettings.svelte.js";
    import { tryUpdateMaxMembers } from "../../services/appSettingsService.svelte";

    import ToastStack from "../../components/ToastStack.svelte";
    import DesktopSidebar from "../../components/DesktopSidebar.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Card from "../../components/Card.svelte";
    import ComingEvent from "../../components/ComingEvent.svelte";
    import VoiceDistribution from "../../components/VoiceDistribution.svelte";
    import Modal from "../../components/Modal.svelte";
    import Input from "../../components/Input.svelte";
    import Button from "../../components/Button.svelte";
    import { dashboardStore } from "../../stores/dashboard.svelte.js";
    import { prepareEvents, getVoiceCounts } from "../../services/dashboardService.svelte.js";
    import Spinner from "../../components/Spinner.svelte";
    import Chip from "../../components/Chip.svelte";
    import { viewport } from "../../stores/viewport.svelte.js";

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
    let voiceCounts = $derived(getVoiceCounts());

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
</script>

<Modal bind:this={voiceDistributionSettingsModal} title="Stimmenverteilung Einstellungen"
       subTitle="Einstellungen für die Stimmverteilung"
       extraFunctionOnClose={false} extraFunction={() => maxMembers = String(appSettings.maxMembers)}>
    <Input title="Maximale Anzahl an Mitgliedern pro Stimme" type="number" marginTop="5" bind:value={maxMembers} />
    <div class="w-full flex items-center justify-end mt-5 gap-2">
        <Button type="secondary" onclick={voiceDistributionSettingsModal.hideModal}>Abbrechen</Button>
        <Button type="primary" disabled={isSubmitting} onclick={updateMaxMembersVoiceDistribution} isSubmit={true}>
            {#if isSubmitting}
                <Spinner light={true} />
                <p>Speichern...</p>
            {:else}
                Speichern
            {/if}
        </Button>
    </div>
</Modal>

<ToastStack />

<main class="flex h-screen overflow-hidden">
    <DesktopSidebar currentPage="dashboard" />
    <div class="flex-1 min-h-0 overflow-y-auto">
        <div class="flex flex-col w-full flex-1 overflow-hidden p-10 min-h-0">
            <PageHeader title="Dashboard" subTitle="Willkommen in GVW Office - Übersicht Gesangverein Weppersdorf"
                        showSlot={false} />

            <!-- Small cards -->
            <div class="flex max-[1300px]:flex-col items-center overflow-hidden mt-5 gap-5 min-[1300px]:max-h-1/7">
                <Card padding="5" justify="justify-around">
                    <div class="flex w-full items-center">
                        <p class="text-dt-5 text-gv-dark-text">Aktive Mitglieder</p>
                        <span
                            class="material-symbols-rounded text-icon-dt-4 text-gv-secondary-text ml-auto">group</span>
                    </div>
                    <p class="text-dt-3 text-black w-full text-start">
                        <span>{activeMembers}</span>
                        <span class="text-dt-4 text-gv-light-text"> / {dashboardStore.members.length}</span>
                    </p>
                </Card>
                <Card padding="5" justify="justify-around">
                    <div class="flex w-full items-center">
                        <p class="text-dt-5 text-gv-dark-text">Bevorstehende Events</p>
                        <span
                            class="material-symbols-rounded text-icon-dt-4 text-gv-secondary-text ml-auto">calendar_today</span>
                    </div>
                    <p class="text-dt-3 text-black w-full text-start">
                        <span>{dashboardStore.upcomingEvents.length}</span>
                        <span class="text-dt-4 text-gv-light-text"> / {dashboardStore.totalEvents}</span>
                    </p>
                </Card>
                <Card padding="5" justify="justify-around">
                    <div class="flex w-full items-center">
                        <p class="text-dt-5 text-gv-dark-text">Notenmaterial</p>
                        <span
                            class="material-symbols-rounded text-icon-dt-4 text-gv-secondary-text ml-auto">music_note</span>
                    </div>
                    <p class="text-dt-3 text-black w-full text-start">{dashboardStore.totalScores}</p>
                </Card>
            </div>

            <!-- Big cards -->
            <div
                class="w-full flex-1 mt-5 gap-5 flex max-[1300px]:flex-col max-[1300px]:h-[calc(100vh-100px)] h-full min-h-0 max-[1300px]:overflow-y-auto">
                <!-- Upcoming Events & Birthdays -->
                <div class="flex flex-col w-full gap-5">
                    <Card padding="5" fillHeight={viewport.width >= 1300}>
                        <p class="min-[1200px]:text-dt-3 text-dt-5 text-gv-dark-text w-full text-start flex flex-col mb-3">
                            <span>Kommende Veranstaltungen</span>
                        </p>
                        <div
                            class="w-full flex-1 min-h-0 overflow-x-hidden overflow-y-auto max-h-[45dvh] flex flex-col items-center pr-2 gap-5">
                            {#each events as event (event.id)}
                                <ComingEvent title={event.title} time={event.time} location={event.location}
                                             type={event.type} />
                            {/each}
                        </div>
                    </Card>

                    <Card padding="5" fillHeight={viewport.width >= 1300}>
                        <p class="min-[1200px]:text-dt-3 text-dt-5 text-gv-dark-text w-full text-start flex flex-col mb-3">
                            <span>Kommende Geburtstage</span>
                        </p>
                        <div
                            class="w-full flex-1 min-h-0 overflow-x-hidden overflow-y-auto max-h-[45dvh] flex flex-col items-center pr-2 gap-5">
                            {#each dashboardStore.upcomingBirthdays as birthday (birthday.id)}
                                <div class="flex items-center w-full">
                                    <p class="text-gv-dark-text text-dt-5">{birthday.name}</p>
                                    <div class="ml-auto">
                                        <Chip text={birthday.date} />
                                    </div>
                                </div>
                            {/each}
                        </div>
                    </Card>
                </div>

                <!-- Voice distribution -->
                <Card padding="5">
                    <div class="w-full h-full flex flex-col items-start gap-5">
                        <p class="text-dt-3 text-gv-dark-text w-full text-start flex flex-col">
                            Stimmenverteilung
                        </p>
                        <div
                            class="w-full min-h-0 overflow-x-hidden overflow-y-auto flex flex-col items-center pr-2 gap-10">
                            <VoiceDistribution voice="1. Tenor" voiceMembers={voiceCounts.tenor1}
                                               totalMembers={appSettings.maxMembers} />
                            <VoiceDistribution voice="2. Tenor" voiceMembers={voiceCounts.tenor2}
                                               totalMembers={appSettings.maxMembers} />
                            <VoiceDistribution voice="1. Bass" voiceMembers={voiceCounts.bass1}
                                               totalMembers={appSettings.maxMembers} />
                            <VoiceDistribution voice="2. Bass" voiceMembers={voiceCounts.bass2}
                                               totalMembers={appSettings.maxMembers} />
                        </div>
                        {#if user.role === "board_member" || user.role === "admin"}
                            <div class="w-full flex items-center justify-end pr-2 mt-5">
                                <button
                                    class="cursor-pointer flex items-center justify-center rounded-2 p-2 hover:bg-gv-hover-effect"
                                    onclick={voiceDistributionSettingsModal.showModal}>
                                    <span
                                        class="material-symbols-rounded text-icon-dt-2 text-gv-dark-text">settings</span>
                                </button>
                            </div>
                        {/if}
                    </div>
                </Card>
            </div>
        </div>
    </div>
</main>