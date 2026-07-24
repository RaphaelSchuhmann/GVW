<script>
    import { push } from "svelte-spa-router";
    import { membersStore } from "../../stores/members.svelte";
    import { statusMapI2D } from "../../services/membersService.svelte";
    import { viewport } from "../../stores/viewport.svelte";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Card from "../../components/Card.svelte";
    import Button from "../../components/Button.svelte";
    import SearchBar from "../../components/SearchBar.svelte";
    import Chip from "../../components/Chip.svelte";
    import MobileSidebar from "../../components/MobileSidebar.svelte";
    import AddMemberModal from "../../components/AddMemberModal.svelte";

    // ==================
    // MODAL REFERENCES
    // ==================
    /**
     * Reference to the "Add Member" modal.
     * Controls visibility and lifecycle of the member creation dialog.
     * @type {import("../../components/AddMemberModal.svelte").default}
     */
    let addMemberModal = null;

    let sidebarOpen = $state(false);

    function showAddMemberModal() {
        if (addMemberModal) {
            addMemberModal.show();
        }
    }

    function openSidebar() {
        sidebarOpen = true;
    }
</script>

<ToastStack isMobile={true} />

<AddMemberModal bind:this={addMemberModal} isMobile={true} />

<MobileSidebar currentPage="members" bind:isOpen={sidebarOpen} />

<main class="flex overflow-hidden">
    <div class="flex flex-col flex-1 min-h-0 w-full p-7 overflow-y-auto overflow-x-hidden">
        <div class="w-full flex items-center justify-start">
            <button class="flex items-center justify-center" onclick={openSidebar}>
                <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">menu</span>
            </button>
        </div>
        <PageHeader title="Mitglieder" subTitle="Verwaltung aller Vereinsmitglieder" showSlot={false} />

        <Button type="primary" onclick={showAddMemberModal} marginTop="4">
            <span class="material-symbols-rounded text-icon-dt-5">add</span>
            <p class="text-dt-6 text-nowrap max-[430px]:ml-2">Mitglied hinzufügen</p>
        </Button>

        <SearchBar placeholder="Mitglieder durchsuchen..." page="members" marginTop="5" />

        <Card padding="0" marginTop="5" borderThickness={viewport.width > 1300 ? "2" : "1"}>
            <div class="flex-1 min-h-0 overflow-y-auto w-full">
                {#if membersStore.display.length !== 0}
                    {#each membersStore.display as member (member.id)}
                        <button
                            class={`flex items-center w-full ${membersStore.display.indexOf(member) !== membersStore.display.length - 1 ? "border-b" : "border-none"} border-gv-border p-2`}
                            onclick={async () => await push(`/members/details?id=${member.id}&editing=false`)}>
                            <div class="flex flex-col items-start justify-between mr-auto max-w-3/4">
                                <p class="text-gv-dark-text text-dt-7">{`${member.name} ${member.surname}`}</p>
                                <div class="flex items-center justify-start gap-2">
                                    <span
                                        class="material-symbols-rounded text-icon-dt-7 text-gv-dark-turquoise">mail</span>
                                    <p class="text-dt-8 text-gv-dark-turquoise text-nowrap truncate">{member.email}</p>
                                </div>
                            </div>

                            <Chip text={statusMapI2D[member.status] || member.status} fontSize="7" />
                        </button>
                    {/each}
                {:else}
                    <p class="text-dt-3 text-gv-dark-text text-center w-full h-full p-10 font-semibold">Es wurden keine
                        Mitglieder gefunden!</p>
                {/if}
            </div>
        </Card>
    </div>
</main>