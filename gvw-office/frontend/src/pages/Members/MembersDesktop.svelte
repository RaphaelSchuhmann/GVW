<script>
    import { push } from "svelte-spa-router";
    import { membersStore } from "../../stores/members.svelte";
    import { voiceMapI2D, statusMapI2D, switchMemberStatus } from "../../services/membersService.svelte";
    import { addToast } from "../../stores/toasts.svelte";
    import { viewport } from "../../stores/viewport.svelte";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";
    import { createContextMenu } from "../../lib/contextMenu.svelte.js";
    import { getYearFromISOString } from "../../services/dateTimeUtils.js";

    import ToastStack from "../../components/ToastStack.svelte";
    import DesktopSidebar from "../../components/DesktopSidebar.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Card from "../../components/Card.svelte";
    import Button from "../../components/Button.svelte";
    import SearchBar from "../../components/SearchBar.svelte";
    import Chip from "../../components/Chip.svelte";
    import ContextMenu from "../../components/ContextMenu.svelte";
    import ConfirmDeleteModal from "../../components/ConfirmDeleteModal.svelte";
    import AddMemberModal from "../../components/AddMemberModal.svelte";
    import Table from "../../components/Table.svelte";

    // ==================
    // MODAL REFERENCES
    // ==================
    /**
     * Reference to the "Add Member" modal.
     * Controls visibility and lifecycle of the member creation dialog.
     * @type {import("../../components/AddMemberModal.svelte").default}
     */
    let addMemberModal = null;

    /**
     * Reference to the delete confirmation modal.
     * Used to initiate and confirm member deletion flow.
     * @type {import("../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteMemberModal = null;

    // -------------------
    // DELETE MEMBER STATE
    // -------------------
    /**
     * Stores the full name of the member currently selected
     * for deletion. Displayed inside the confirmation modal.
     */
    let memberName = $state("");

    /**
     * Initializes the member deletion process.
     *
     * Workflow:
     * 1. Close context menu.
     * 2. Resolve selected member from store.
     * 3. If not found, show error toast.
     * 4. Otherwise, open confirmation modal.
     *
     * @async
     * @returns {Promise<void>}
     */
    async function startDeleteMember() {
        menu.data.open = false;
        const member = membersStore.raw.find(item => item.id === menu.data.activeId);

        if (!member) {
            addToast({
                title: "Mitglied nicht gefunden",
                subTitle: "Das ausgewählte Mitglied wurde nicht gefunden. Bitte versuchen Sie es erneut.",
                type: "error"
            });
            return;
        }

        memberName = `${member?.name} ${member?.surname}`;
        if (memberName) confirmDeleteMemberModal.startDelete();
    }

    // ------------------
    // CONTEXT MENU STATE
    // ------------------
    /**
     * Reactive context menu instance for member actions.
     * Stores open state, position, and currently active member ID.
     */
    let menu = createContextMenu();

    /**
     * Toggles the status of the currently selected member.
     *
     * If no active member is selected, the function exits early.
     * After updating, the member list is refreshed.
     *
     * @async
     * @returns {Promise<void>}
     */
    async function handleSwitchStatus() {
        if (!menu.data.activeId) return;

        menu.data.open = false;
        await switchMemberStatus(menu.data.activeId);

        menu.data.activeId = null;
        await fetchAndSetRaw();
    }

    function showAddMemberModal() {
        if (addMemberModal) {
            addMemberModal.show();
        }
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
</script>

<svelte:window oncontextmenu={closeMenu} />

<ToastStack />

<AddMemberModal bind:this={addMemberModal} />

<ConfirmDeleteModal expectedInput={memberName} id={menu.data.activeId}
                    title="Mitglied löschen" subTitle="Sind Sie sich sicher das Sie dieses Mitglied löschen möchten?"
                    action="deleteMember"
                    onClose={async () => {menu.data.open = false; menu.data.activeId = null; await fetchAndSetRaw();}}
                    bind:this={confirmDeleteMemberModal}
/>

<ContextMenu bind:open={menu.data.open} x={menu.data.x} y={menu.data.y}>
    <Button onclick={async () => await push(`/members/details?id=${menu.data.activeId}&editing=false`)}
            type="contextMenu">
        Details
    </Button>
    <Button onclick={async () => await push(`/members/details?id=${menu.data.activeId}&editing=true`)}
            type="contextMenu">
        Bearbeiten
    </Button>
    <Button onclick={handleSwitchStatus} type="contextMenu">Status ändern</Button>
    <Button onclick={startDeleteMember} type="contextMenu" fontColor="text-gv-delete">Löschen</Button>
</ContextMenu>

<main class="flex overflow-hidden">
    <DesktopSidebar currentPage="members" />
    <div class="flex flex-col w-full h-dvh overflow-hidden p-10 min-h-0">
        <PageHeader title="Mitglieder" subTitle="Verwaltung aller Vereinsmitglieder" showSlot={viewport.width > 1000}>
            {#if viewport.width > 1000}
                <Button type="primary" onclick={showAddMemberModal}>
                    <span class="material-symbols-rounded text-icon-dt-4 mr-2">add</span>
                    <p class="text-dt-4 text-nowrap">Mitglied hinzufügen</p>
                </Button>
            {/if}
        </PageHeader>

        {#if viewport.width <= 1000}
            <Button type="primary" onclick={showAddMemberModal} marginTop="4">
                <span class="material-symbols-rounded text-icon-dt-5">add</span>
                <p class="text-dt-6 text-nowrap max-[430px]:ml-2">Mitglied hinzufügen</p>
            </Button>
        {/if}

        <SearchBar placeholder="Mitglieder durchsuchen..." page="members" marginTop="5" />

        <Table items={membersStore.display} marginTop="5" breakpoint={1300} emptyText="Es wurden keine Mitglieder gefunden!">
            {#snippet header()}
                <th scope="col" class="px-6 py-3 font-bold">Name</th>
                <th scope="col" class="px-6 py-3 font-bold">Stimmlage</th>
                <th scope="col" class="px-6 py-3 font-bold">Kontakt</th>
                <th scope="col" class="px-6 py-3 font-bold text-nowrap">Mitglied seit</th>
                <th scope="col" class="px-6 py-3 font-bold">Status</th>
                <th scope="col" class="px-6 py-3">
                    <button
                        class="flex items-center justify-center p-2 rounded-2 cursor-pointer hover:bg-gv-hover-effect"
                        onclick={fetchAndSetRaw}
                    >
                        <span
                            class="material-symbols-rounded min-[1300px]:text-icon-dt-5 text-icon-dt-6 font-bold text-gv-dark-text">
                            refresh
                        </span>
                    </button>
                </th>
            {/snippet}
            {#snippet row(member)}
                <tr class="border-t-2 border-gv-border"
                    data-id={member.id}
                    oncontextmenu={handleMenuOpenFromEvent}>
                    <td class="px-6 py-4">
                        <div class="flex flex-col items-start h-full overflow-hidden gap-1">
                            <p class="min-[1300px]:text-dt-6 text-dt-7 text-gv-dark-text text-nowrap truncate">{`${member.name} ${member.surname}`}</p>
                            <p class="min-[1300px]:text-dt-8 text-dt-8 text-gv-light-text text-nowrap truncate">{member.address}</p>
                        </div>
                    </td>
                    <td class="px-6 py-4">
                        <p class="min-[1300px]:text-dt-4 text-dt-7 text-gv-dark-text text-nowrap truncate">{voiceMapI2D[member.voice] || member.voice}</p>
                    </td>
                    <td class="px-6 py-4">
                        <div class="flex flex-col items-start  h-full overflow-hidden gap-2">
                            <div class="flex items-center justify-start gap-2">
                                <span
                                    class="material-symbols-rounded min-[1300px]:text-icon-dt-6 text-icon-dt-7 text-gv-dark-turquoise">
                                    mail
                                </span>
                                <p class="min-[1300px]:text-dt-7 text-dt-8 text-gv-dark-turquoise">{member.email}</p>
                            </div>
                            <div class="flex items-center justify-start gap-2">
                                <span
                                    class="material-symbols-rounded min-[1300px]:text-icon-dt-6 text-icon-dt-7 text-gv-light-text">
                                    phone
                                </span>
                                <p class="min-[1300px]:text-dt-7 text-dt-8 text-gv-light-text">{member.phone}</p>
                            </div>
                        </div>
                    </td>
                    <td class="px-6 py-4">
                        <p class="min-[1300px]:text-dt-4 text-dt-7 text-gv-dark-text text-nowrap truncate">{getYearFromISOString(member.joined)}</p>
                    </td>
                    <td class="px-6 py-4">
                        <Chip text={statusMapI2D[member.status] || member.status} />
                    </td>
                    <td class="px-6 py-4">
                        <button
                            class="flex items-center justify-center p-2 rounded-2 cursor-pointer hover:bg-gv-hover-effect"
                            data-id={member.id}
                            onclick={handleMenuOpenFromBtn}>
                            <span class="material-symbols-rounded text-icon-dt-6 text-gv-dark-text">
                                more_horiz
                            </span>
                        </button>
                    </td>
                </tr>
            {/snippet}

            {#snippet mobileItem(member, index)}
                <button
                    class={`flex items-center w-full ${index !== membersStore.display.length - 1 ? "border-b" : "border-none"} border-gv-border p-2`}
                    onclick={async () =>  await push(`/members/details?id=${member.id}&editing=false`)}>
                    <div class="flex flex-col items-start justify-between mr-auto max-w-3/4">
                        <p class="text-gv-dark-text text-dt-7">{`${member.name} ${member.surname}`}</p>
                        <div class="flex items-center justify-start gap-2">
                            <span class="material-symbols-rounded text-icon-dt-7 text-gv-dark-turquoise">mail</span>
                            <p class="text-dt-8 text-gv-dark-turquoise text-nowrap truncate">{member.email}</p>
                        </div>
                    </div>

                    <Chip text={statusMapI2D[member.status] || member.status} fontSize="7" />
                </button>
            {/snippet}
        </Table>
    </div>
</main>