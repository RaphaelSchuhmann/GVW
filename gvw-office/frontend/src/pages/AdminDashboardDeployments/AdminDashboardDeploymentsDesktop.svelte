<script>
    import { deploymentStore } from "../../stores/deployments.svelte.js";
    import { viewport } from "../../stores/viewport.svelte";
    import { formatISODateString, } from "../../services/dateTimeUtils.js";
    import { addDeployment } from "../../services/deploymentService.svelte.js";

    import ToastStack from "../../components/ToastStack.svelte";
    import DesktopSidebar from "../../components/DesktopSidebar.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import HorizontalNavBar from "../../components/AdminHorizontalNavBar.svelte";
    import Button from "../../components/Button.svelte";
    import SearchBar from "../../components/SearchBar.svelte";
    import Filter from "../../components/Filter.svelte";
    import Modal from "../../components/Modal.svelte";
    import Input from "../../components/Input.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import DefaultDatepicker from "../../components/DefaultDatepicker.svelte";
    import TimePicker from "../../components/TimePicker.svelte";
    import Table from "../../components/Table.svelte";
    import Chip from "../../components/Chip.svelte";
    import { push } from "svelte-spa-router";
    import { membersStore } from "../../stores/members.svelte.js";
    import { statusMapI2D } from "../../services/membersService.svelte.js";
    import Card from "../../components/Card.svelte";

    /**
     * Reference to the "Add Deployment" modal.
     * Controls visibility and lifecycle of the deployment creation dialog.
     * @type {import("../../components/Modal.svelte").default}
     */
    let addDeploymentModal = null;

    let isSubmitting = $state(false);

    let deploymentInput = $state({
        title: "",
        version: "",
        date: "",
        startTime: "",
        endTime: ""
    });

    const addDisabled = $derived(
        (!deploymentInput.title || deploymentInput.title.trim() === "") ||
        (!deploymentInput.version || deploymentInput.version.trim() === "") ||
        (!deploymentInput.date || deploymentInput.date.trim() === "") ||
        (!deploymentInput.startTime || deploymentInput.startTime.trim() === "") ||
        (!deploymentInput.endTime || deploymentInput.endTime.trim() === "") ||
        isSubmitting
    );

    /**
     * Resets all input fields of the "Add User" form
     * to their initial default values.
     *
     * Called after successful submission or when closing the modal.
     */
    function resetAddInputs() {
        deploymentInput.title = "";
        deploymentInput.version = "";
        deploymentInput.date = "";
        deploymentInput.startTime = "";
        deploymentInput.endTime = "";
    }

    async function submitDeployment() {
        isSubmitting = true;

        try {
            await addDeployment(deploymentInput);
        } finally {
            isSubmitting = false;
        }

        addDeploymentModal.hideModal();
    }

    function showAddDeploymentModal() {
        if (addDeploymentModal) {
            addDeploymentModal.showModal();
        }
    }

    function hideAddDeploymentModal() {
        if (addDeploymentModal) {
            addDeploymentModal.hideModal();
        }
    }

    function updateDate(val) { deploymentInput.date = val; }

    function updateStartTime(val) { deploymentInput.startTime = val; }

    function updateEndTime(val) { deploymentInput.endTime = val; }
</script>

<ToastStack />

<Modal bind:this={addDeploymentModal} extraFunction={resetAddInputs}
       title="Deployment hinzufügen" subTitle="Erfassen Sie hier die Deploymentdaten">
    <div class="flex flex-col w-full h-full gap-5 overflow-visible">
        <div class="flex items-center gap-4">
            <Input bind:value={deploymentInput.title} title="Titel" placeholder="Deployment #001" />
            <Input bind:value={deploymentInput.version} title="Version" placeholder="1.1.0" />
        </div>

        <div class="flex items-center gap-4">
            <TimePicker title="Start" selected={deploymentInput.startTime} onChange={updateStartTime} />
            <TimePicker title="Ende" selected={deploymentInput.endTime} onChange={updateEndTime} />
        </div>

        <DefaultDatepicker title="Datum" onChange={updateDate} />

        <div class="w-full flex items-center justify-end gap-4">
            <Button type="secondary" onclick={hideAddDeploymentModal}>Abbrechen</Button>
            <Button type="primary" disabled={addDisabled} onclick={submitDeployment} isSubmit={true}>
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

<main class="flex h-screen overflow-hidden">
    <DesktopSidebar currentPage="adminDashboard" />
    <div class="flex-1 min-h-0 overflow-y-auto">
        <div class="flex flex-col w-full h-full flex-1 overflow-hidden p-10 min-h-0">
            <HorizontalNavBar currentPage="deployments" />
            <PageHeader
                title="Deployments"
                subTitle=""
                hideSubTitle={true}
                marginTop="5"
            >
                {#if viewport.width > 1000}
                    <Button type="primary" onclick={showAddDeploymentModal}>
                        <span class="material-symbols-rounded text-icon-dt-4 mr-2">add</span>
                        <p class="text-dt-4 text-nowrap">Deployment hinzufügen</p>
                    </Button>
                {/if}
            </PageHeader>

            {#if viewport.width <= 1000}
                <Button type="primary" onclick={showAddDeploymentModal} marginTop="4">
                    <span class="material-symbols-rounded text-icon-dt-5">add</span>
                    <p class="text-dt-6 text-nowrap max-[430px]:ml-2">Deployment hinzufügen</p>
                </Button>
            {/if}

            <div class="flex min-[1300px]:items-center max-[1300px]:flex-col w-full gap-2 mt-5">
                <SearchBar page="deployments" />
                <div class="min-[1300px]:w-1/4 w-full">
                    <Filter page="deployments"
                            options={["Alle Typen", "Scheduled", "Running", "Successful", "Cancelled", "Failed"]}
                            textWrap={false} />
                </div>
            </div>

            <Table items={deploymentStore.display} marginTop="5" breakpoint={1300}
                   emptyText="Es wurden keine Deployments gefunden!">
                {#snippet header()}
                    <th scope="col" class="px-6 py-3 font-bold">Titel</th>
                    <th scope="col" class="px-6 py-3 font-bold">Status</th>
                    <th scope="col" class="px-6 py-3 font-bold">Datum</th>
                    <th scope="col" class="px-6 py-3 font-bold">Commit</th>
                    <th scope="col" class="px-6 py-3 font-bold">Hash</th>
                {/snippet}

                {#snippet row(deployment)}
                    <tr class="border-t-2 border-gv-border cursor-pointer hover:bg-gv-hover-effect"
                        onclick={async () => await push(`/admin/deployment/details?id=${deployment.id}&editing=false`)}>
                        <td class="px-6 py-4">
                            <p class="min-[1300px]:text-dt-5 text-dt-7 text-gv-dark-text text-nowrap truncate">{deployment.title}</p>
                        </td>
                        <td class="px-6 py-4">
                            <Chip text={deployment.type} colorType={deployment.type.toLowerCase()} />
                        </td>
                        <td class="px-6 py-4">
                            <div class="flex flex-col items-start  h-full overflow-hidden gap-2">
                                <div class="flex items-center justify-start gap-2">
                                    <span
                                        class="material-symbols-rounded min-[1300px]:text-icon-dt-6 text-icon-dt-7 text-gv-dark-text">
                                        calendar_month
                                    </span>
                                    <p class="min-[1300px]:text-dt-7 text-dt-8 text-gv-dark-text">{formatISODateString(deployment.date)}</p>
                                </div>
                                <div class="flex items-center justify-start gap-2">
                                    <span
                                        class="material-symbols-rounded min-[1300px]:text-icon-dt-6 text-icon-dt-7 text-gv-dark-text">
                                        schedule
                                    </span>
                                    <p class="min-[1300px]:text-dt-7 text-dt-8 text-gv-dark-text">{`${deployment.startTime} - ${deployment.endTime} Uhr`}</p>
                                </div>
                            </div>
                        </td>
                        <td class="px-6 py-4">
                            {#if deployment.commit}
                                <a class="flex items-center justify-center gap-2 cursor-pointer rounded-2 p-2 text-gv-dark-text border-2 border-gv-border hover:bg-gv-hover-effect no-underline">
                                    <span>{deployment.commit}</span>
                                    <span class="material-symbols-rounded text-dt-4">open_in_new</span>
                                </a>
                            {:else}
                                <p class="min-[1300px]:text-dt-5 text-dt-7 text-gv-dark-text text-nowrap truncate">N/A</p>
                            {/if}
                        </td>
                        <td class="px-6 py-4">
                            <p class="min-[1300px]:text-dt-6 text-dt-7 text-gv-dark-text text-nowrap truncate">{deployment.hash}</p>
                        </td>
                    </tr>
                {/snippet}

                {#snippet mobileItem(deployment, index)}
                    <div class="flex items-start justify-start p-5 gap-2 w-full {deploymentStore.display.length - 1 === index ? '' : 'border-b'} border-gv-border">
                        <p class="text-gv-dark-text text-dt-5">{deployment.title}</p>
                        <div class="ml-auto">
                            <Chip text={deployment.type} colorType={deployment.type.toLowerCase()} />
                        </div>
                    </div>
                {/snippet}
            </Table>
        </div>
    </div>
</main>
