<script>
    import { user } from "../../stores/user.svelte";
    import { eventsStore } from "../../stores/events.svelte";
    import { typeMap, updateStatus, getEventOccurrenceById } from "../../services/eventsService.svelte";
    import { viewport } from "../../stores/viewport.svelte";

    import ToastStack from "../../components/ToastStack.svelte";
    import DesktopSidebar from "../../components/DesktopSidebar.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Button from "../../components/Button.svelte";
    import Filter from "../../components/Filter.svelte";
    import FilterTabBar from "../../components/FilterTabBar.svelte";
    import Chip from "../../components/Chip.svelte";
    import ContextMenu from "../../components/ContextMenu.svelte";
    import Card from "../../components/Card.svelte";
    import ConfirmDeleteModal from "../../components/ConfirmDeleteModal.svelte";
    import { push } from "svelte-spa-router";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";
    import { createContextMenu } from "../../lib/contextMenu.svelte";
    import { addToast } from "../../stores/toasts.svelte";
    import AddEventModal from "../../components/AddEventModal.svelte";

    // ================
    // MODAL REFERENCES
    // ================

    /**
     * Reference to the "Add Event" modal.
     * Controls visibility and lifecycle of the event creation dialog.
     * @type {import("../../components/AddEventModal.svelte").default}
     */
    let addEventModal = null;

    /**
     * Reference to the delete confirmation modal.
     * Used to initiate and confirm event deletion flow.
     * @type {import("../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteEventModal = null;

    // ============
    // DELETE EVENT
    // ============
    let eventTitle = $state("");

    /**
     * Initializes the event deletion process.
     *
     * Workflow:
     * 1. Close context menu.
     * 2. Resolve selected event from store.
     * 3. If not found, show error toast.
     * 4. Otherwise, open confirmation modal.
     *
     * @async
     * @returns {Promise<void>}
     */
    async function startDeleteEvent() {
        menu.data.open = false;
        const event = eventsStore.raw.find(item => item.id === menu.data.activeId);

        if (!event) {
            addToast({
                title: "Veranstaltung nicht gefunden",
                subTitle: "Die ausgewählte Veranstaltung wurde nicht gefunden. Bitte versuchen Sie es erneut.",
                type: "error"
            });
            return;
        }

        eventTitle = event.title;
        if (eventTitle) confirmDeleteEventModal.startDelete();
    }

    // ==================
    // CONTEXT MENU STATE
    // ==================
    /**
     * Reactive context menu instance for event actions.
     * Stores open state, position, and currently active event ID.
     */
    let menu = createContextMenu();

    /**
     * Toggles the status of the currently selected event.
     *
     * If no active event is selected, the function exits early.
     * After updating, the event list is refreshed.
     *
     * @async
     * @returns {Promise<void>}
     */
    async function handleSwitchStatus() {
        if (!menu.data.activeId) return;

        menu.data.open = false;
        await updateStatus(menu.data.activeId);

        menu.data.activeId = null;
        await fetchAndSetRaw();
    }

    function handleMenuOpenFromBtn(e) {
        const memberId = e.currentTarget.dataset.id;
        menu.openFromButton(e, memberId);
    }

    function handleMenuOpenFromEvent(e) {
        const memberId = e.currentTarget.dataset.id;
        menu.openFromEvent(e, memberId);
    }

    function closeMenu() { menu.data.open = false; }

    function showAddEventModal() {
        if (addEventModal) {
            addEventModal.show();
        }
    }
</script>

<svelte:window oncontextmenu={closeMenu} />

<ToastStack/>

<ContextMenu bind:open={menu.data.open} x={menu.data.x} y={menu.data.y}>
    <Button type="contextMenu" onclick={async () =>  await push(`/events/details?id=${menu.data.activeId}&editing=false`)}>
        Details
    </Button>
    {#if user.role === "board_member" || user.role === "admin"}
        <Button type="contextMenu" onclick={async () =>  await push(`/events/details?id=${menu.data.activeId}&editing=true`)}>
            Bearbeiten
        </Button>
        <Button type="contextMenu" onclick={handleSwitchStatus}>Status ändern</Button>
        <Button type="contextMenu" fontColor="text-gv-delete" onclick={startDeleteEvent}>Löschen</Button>
    {/if}
</ContextMenu>

<ConfirmDeleteModal expectedInput={eventTitle} id={menu.data.activeId}
                    title="Veranstaltung löschen"
                    subTitle="Sind Sie sich sicher das Sie diese Veranstaltung löschen möchten?"
                    action="deleteEvent"
                    onClose={async () => {menu.data.open = false; menu.data.activeId = null; await fetchAndSetRaw();}}
                    bind:this={confirmDeleteEventModal}
/>

<AddEventModal bind:this={addEventModal} />

<!--Switches to mobile page if width is less than 870px!-->
<main class="flex h-screen overflow-hidden">
    <DesktopSidebar currentPage="events"/>

    <div class="flex flex-col w-full overflow-hidden p-10 min-h-0">
        <PageHeader title="Veranstaltungen" subTitle="Verwaltung von Events, Proben und Konzerten"
                    showSlot={viewport.width > 1200}>
            {#if viewport.width > 1200}
                <Button type="primary" disabled={(user.role !== "admin" && user.role !== "board_member")}
                        onclick={showAddEventModal}>
                    <span class="material-symbols-rounded min-[1900px]:text-icon-dt-4 text-icon-dt-5 mr-2">add</span>
                    <p class="min-[1900px]:text-dt-4 text-dt-5">Veranstaltung hinzufügen</p>
                </Button>
            {/if}
        </PageHeader>

        {#if viewport.width < 1200}
            <Button type="primary" disabled={(user.role !== "admin" && user.role !== "board_member")} marginTop="5"
                    onclick={showAddEventModal}>
                <span class="material-symbols-rounded min-[1900px]:text-icon-dt-4 text-icon-dt-5 mr-2">add</span>
                <p class="min-[1900px]:text-dt-4 text-dt-5">Veranstaltung hinzufügen</p>
            </Button>
        {/if}

        <div class="flex items-center mt-10 max-w-1/5">
            <Filter options={["Alle Typen", "Proben", "Meeting", "Konzerte", "Sonstiges"]} page="events" />
        </div>

        <FilterTabBar contents={["Bevorstehend", "Abgeschlossen"]} selected="Bevorstehend" marginTop="5"
                      page="events" />

        <div class="flex-1 min-h-0 overflow-y-auto mt-5">
            <div
                class="min-[1470px]:grid min-[1470px]:grid-cols-2 flex flex-col gap-4 overflow-y-auto overflow-x-hidden">
                {#each eventsStore.display as event (event.id)}
                    <Card data-id={event.id} oncontextmenu={handleMenuOpenFromEvent}>
                        <div class="flex items-center w-full">
                            <p class="text-gv-dark-text text-dt-3 max-w-3/4 text-nowrap truncate">{event.title}</p>
                            <div class="ml-auto">
                                <Chip text={typeMap[event.type]} />
                            </div>
                        </div>
                        <div class="flex items-center w-full mt-2 gap-10">
                            <div class="flex items-stretch gap-2">
                                <span
                                    class="material-symbols-rounded text-icon-dt-6 text-gv-light-text">calendar_today</span>
                                <p class="text-dt-6 text-gv-light-text">{getEventOccurrenceById(event.id)}</p>
                            </div>
                            <div class="flex items-stretch gap-2">
                                <span class="material-symbols-rounded text-icon-dt-6 text-gv-light-text">schedule</span>
                                <p class="text-dt-6 text-gv-light-text">{event.time}</p>
                            </div>
                            <button
                                class="flex items-center justify-center p-2 cursor-pointer hover:bg-gv-hover-effect rounded-2 ml-auto"
                                data-id={event.id}
                                onclick={handleMenuOpenFromBtn}>
                                <span class="material-symbols-rounded">more_horiz</span>
                            </button>
                        </div>
                        <div class="flex items-center w-full mt-2 gap-2">
                            <span class="material-symbols-rounded text-icon-dt-5 text-gv-light-text">location_on</span>
                            <p class="text-dt-5 text-gv-dark-text text-nowrap truncate">{event.location}</p>
                        </div>
                        <div class="flex items-center w-full mt-2">
                            <p class="text-dt-5 text-gv-light-text text-start text-nowrap truncate">{event.description}</p>
                        </div>
                    </Card>
                {/each}
            </div>
        </div>
    </div>
</main>