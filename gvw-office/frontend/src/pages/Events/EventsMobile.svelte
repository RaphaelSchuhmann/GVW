<script>
    import { user } from "../../stores/user.svelte";
    import { eventsStore } from "../../stores/events.svelte";
    import { typeMap, getEventOccurrenceById, } from "../../services/eventsService.svelte";

    import PageHeader from "../../components/PageHeader.svelte";
    import Button from "../../components/Button.svelte";
    import Filter from "../../components/Filter.svelte";
    import FilterTabBar from "../../components/FilterTabBar.svelte";
    import Chip from "../../components/Chip.svelte";
    import Card from "../../components/Card.svelte";
    import { push } from "svelte-spa-router";
    import MobileSidebar from "../../components/MobileSidebar.svelte";
    import AddEventModal from "../../components/AddEventModal.svelte";

    // ================
    // MODAL REFERENCES
    // ================
    /**
     * Reference to the "Add Event" modal.
     * Controls visibility and lifecycle of the member creation dialog.
     * @type {import("../../components/AddEventModal.svelte").default}
     */
    let addEventModal = null;

    let sidebarOpen = $state(false);

    function openSidebar() { sidebarOpen = true; }

    function showAddEventModal() {
        if (addEventModal) {
            addEventModal.show();
        }
    }
</script>

<MobileSidebar currentPage="events" bind:isOpen={sidebarOpen} />

<AddEventModal bind:this={addEventModal} isMobile={true} />

<main class="flex h-screen overflow-hidden">
    <div class="flex flex-col w-full overflow-hidden p-7 min-h-0">
        <div class="w-full flex items-center justify-start">
            <button class="flex items-center justify-center" onclick={openSidebar}>
                <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">menu</span>
            </button>
        </div>
        <PageHeader title="Veranstaltungen" subTitle="Verwaltung von Events, Proben und Konzerten"
                    showSlot={false} />

        <Button type="primary" disabled={(user.role !== "admin" && user.role !== "board_member")} marginTop="5"
                onclick={showAddEventModal}>
            <span class="material-symbols-rounded min-[1900px]:text-icon-dt-4 text-icon-dt-5 mr-2">add</span>
            <p class="min-[1900px]:text-dt-4 text-dt-5">Veranstaltung hinzufügen</p>
        </Button>

        <div class="flex items-center mt-5 w-full">
            <Filter options={["Alle Typen", "Proben", "Meeting", "Konzerte", "Sonstiges"]} page="events" />
        </div>

        <FilterTabBar contents={["Bevorstehend", "Abgeschlossen"]} selected="Bevorstehend" marginTop="5"
                      page="events" />

        <div class="flex-1 min-h-0 overflow-y-auto mt-5">
            <div
                class="flex flex-col gap-4 overflow-y-auto overflow-x-hidden">
                {#each eventsStore.display as event (event.id)}
                    <button onclick={async () => { await push(`/events/details?id=${event.id}&editing=false`) }}>
                        <Card>
                            <div class="flex items-center w-full">
                                <p class="text-gv-dark-text text-dt-5 max-w-3/4 text-nowrap truncate">{event.title}</p>
                                <div class="ml-auto">
                                    <Chip text={typeMap[event.type]} />
                                </div>
                            </div>
                            <div class="flex items-start w-full mt-2 gap-10">
                                <div class="flex items-stretch gap-2">
                                    <span
                                        class="material-symbols-rounded text-icon-dt-8 text-gv-light-text">calendar_today</span>
                                    <p class="text-dt-6 text-gv-light-text text-left">{getEventOccurrenceById(event.id)}</p>
                                </div>
                                <div class="flex items-stretch gap-2">
                                    <span
                                        class="material-symbols-rounded text-icon-dt-8 text-gv-light-text">schedule</span>
                                    <p class="text-dt-6 text-gv-light-text">{event.time}</p>
                                </div>
                            </div>
                            <div class="flex items-center w-full mt-2 gap-2">
                                <span
                                    class="material-symbols-rounded text-icon-dt-6 text-gv-light-text">location_on</span>
                                <p class="text-dt-6 text-gv-dark-text text-nowrap truncate">{event.location}</p>
                            </div>
                        </Card>
                    </button>
                {/each}
            </div>
        </div>
    </div>
</main>