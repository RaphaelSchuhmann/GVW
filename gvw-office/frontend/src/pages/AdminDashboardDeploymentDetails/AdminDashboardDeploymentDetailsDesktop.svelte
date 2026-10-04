<script>
    import { route } from "../../services/utils.js";
    import { viewport } from "../../stores/viewport.svelte";
    import { getFullDeployment, updateDeployment } from "../../services/deploymentService.svelte.js";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import DesktopSidebar from "../../components/DesktopSidebar.svelte";
    import Button from "../../components/Button.svelte";
    import ConfirmDeleteModal from "../../components/ConfirmDeleteModal.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import Input from "../../components/Input.svelte";
    import DefaultDatepicker from "../../components/DefaultDatepicker.svelte";
    import { formatISODateString } from "../../services/dateTimeUtils.js";
    import TimePicker from "../../components/TimePicker.svelte";

    let {
        deploymentData,
        isEditing = $bindable(false),
        isDeleting = $bindable(false),
        onChangeIsEditing = () => {},
        onChangeIsDeleting = () => {},
        ...restProps
    } = $props();

    let informationDraft = $state({
        title: "",
        version: "",
        date: "",
        startTime: "",
        endTime: "",
        commit: ""
    });

    let isSubmittingInformationChanges = $state(false);
    let isRunningMigrationTests = $state(false);

    /**
     * Initializes edit mode for the current user.
     *
     * Creates a deep clone of `deploymentData` and assigns it to `informationDraft`
     * to allow non-destructive editing. Enables the editing state.
     */
    function startEditing() {
        const clone = JSON.parse(JSON.stringify(deploymentData));

        const fields = ["title", "version", "date", "startTime", "endTime", "commit"];
        fields.forEach(field => {
            if (clone[field] == null) clone[field] = "";
        });

        informationDraft = clone;
        onChangeIsEditing(true);
    }

    /**
     * Cancels the current editing session.
     *
     * Discards the informationDraft object and exits edit mode
     * without persisting any changes.
     */
    function cancelEditing() {
        informationDraft = null;
        onChangeIsEditing(false);
    }

    const REQUIRED_DEPLOYMENT_FIELDS = ["title", "version", "date", "startTime", "endTime"];
    const ALL_DEPLOYMENT_FIELDS = [...REQUIRED_DEPLOYMENT_FIELDS, "commit"];

    const isDeployable = $derived.by(() => {
        if (isEditing) return true;

        const allFieldsFilled = ALL_DEPLOYMENT_FIELDS.every(field => {
            const value = deploymentData[field];
            return value !== null && value !== undefined && String(value).trim() !== "";
        });

        const deploymentIsNotRunning = deploymentData.type?.toUpperCase() !== "RUNNING";

        const isToday = (() => {
            if (!deploymentData.date) return false;
            const targetDate = new Date(deploymentData.date);
            const today = new Date();

            return (
                targetDate.getFullYear() === today.getFullYear() &&
                targetDate.getMonth() === today.getMonth() &&
                targetDate.getDate() === today.getDate()
            );
        })();

        const isWithinTimeWindow = (() => {
            const { startTime, endTime } = deploymentData;
            if (!startTime || !endTime) return false;

            const now = new Date();
            const currentMinutes = now.getHours() * 60 + now.getMinutes();

            const [startHours, startMins] = startTime.split(':').map(Number);
            const [endHours, endMins] = endTime.split(':').map(Number);

            const startMinutes = startHours * 60 + startMins;
            const endMinutes = endHours * 60 + endMins;

            if (startMinutes <= endMinutes) {
                return currentMinutes >= startMinutes && currentMinutes <= endMinutes;
            } else {
                return currentMinutes >= startMinutes || currentMinutes <= endMinutes;
            }
        })();

        return allFieldsFilled && deploymentIsNotRunning && isToday && isWithinTimeWindow && !isRunningMigrationTests;
    });

    const hasChanges = $derived.by(() => {
        if (!informationDraft || !deploymentData) return false;

        const allFieldsFilled = REQUIRED_DEPLOYMENT_FIELDS.every(field => {
            const value = informationDraft[field];
            return value !== null && value !== undefined && String(value).trim() !== "";
        });

        if (!allFieldsFilled) return false;

        let isDifferent = false;
        for (const field of ALL_DEPLOYMENT_FIELDS) {
            const informationDraftVal = informationDraft[field] ?? "";
            const originalVal = deploymentData[field] ?? "";

            if (informationDraftVal !== originalVal) {
                isDifferent = true;
                break;
            }
        }

        return isDifferent;
    });

    /**
     * Pre-effect that ensures a informationDraft exists when entering edit mode.
     *
     * If editing is enabled but no informationDraft is present,
     * a deep clone of the current user data is created.
     *
     * Acts as a safety mechanism against inconsistent state.
     */
    $effect.pre(() => {
        if (isEditing && !informationDraft) {
            informationDraft = JSON.parse(JSON.stringify(deploymentData));
        }
    });

    /**
     * Persists the current informationDraft to the backend.
     *
     * - Sends a snapshot of the informationDraft to the update API
     * - Exits edit mode and clears the informationDraft
     *
     * Assumes validation has already been handled externally.
     */
    async function updateDeploymentInformation() {
        isSubmittingInformationChanges = true;
        try {
            await updateDeployment($state.snapshot(informationDraft));
            deploymentData = await getFullDeployment(deploymentData.id);
        } finally {
            isSubmittingInformationChanges = false;
            onChangeIsEditing(false);
            informationDraft = null;
        }
    }

    async function deploy() {
        // Deployment logic here
        console.log("deploying deployment with id: ", deploymentData.id);

        await routeToDeployments();
    }

    /**
     * Navigates back to the user overview page.
     *
     * - Refreshes the raw user list
     * - Performs route navigation to `/admin/deployments`
     *
     * Ensures the overview reflects the latest persisted state.
     */
    async function routeToDeployments() {
        await fetchAndSetRaw();
        await route("/admin/deployments");
    }

    function startDeleting() {
        onChangeIsDeleting(true);
        confirmDeleteUserModal.startDelete();
    }

    function updateDate(val) { informationDraft.date = val; }
    function updateStartTime(val) { informationDraft.startTime = val; }
    function updateEndTime(val) { informationDraft.endTime = val; }

    // ==================
    // MODAL REFERENCES
    // ==================
    /**
     * Reference to the delete confirmation modal.
     * Used to initiate and confirm deployment deletion flow.
     * @type {import("../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteUserModal = null;

    function disableIsDeleting() { onChangeIsDeleting(false); }
</script>

<ToastStack />

<ConfirmDeleteModal expectedInput={deploymentData.title} id={deploymentData.id}
                    title="Deployment löschen" subTitle="Sind Sie sich sicher das Sie dieses Deployment löschen möchten?"
                    action="deleteDeployment"
                    onClose={routeToDeployments}
                    onCancel={disableIsDeleting}
                    bind:this={confirmDeleteUserModal}
/>

<main class="flex h-screen overflow-hidden">
    <DesktopSidebar currentPage="adminDashboard" />
    <div class="flex flex-col min-h-0 w-full p-10 overflow-hidden">
        <PageHeader title="Deployment" subTitle={`Daten von "${deploymentData?.title ?? ""}"`}>
            {#if viewport.width > 900}
                {#if !isEditing}
                    <Button type="secondary" onclick={routeToDeployments}>
                        <span class="material-symbols-rounded text-icon-dt-5">arrow_back</span>
                        <p class="ml-2 text-dt-3">Zurück</p>
                    </Button>
                    <Button type="primary" onclick={deploy} disabled={!isDeployable}>
                        <span class="material-symbols-rounded text-icon-dt-5">motion_play</span>
                        <p class="ml-2 text-dt-3">Deploy</p>
                    </Button>
                {/if}
            {:else}
                <button
                    type="button"
                    class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2"
                    onclick={async () => await routeToDeployments()}
                >
                    <span class="material-symbols-rounded text-icon-dt-2">close</span>
                </button>
            {/if}
        </PageHeader>

        <div class="flex-1 min-h-0 overflow-y-auto w-full">
            <!-- Deployment Information -->
            <div class="flex flex-col items-center gap-5 min-[1500px]:w-1/2 min-[1200px]:w-2/3 w-full mt-5 p-0.5">
                <div class="w-full flex items-center justify-start">
                    <p class="text-dt-3 text-gv-dark-text font-medium">Deployment Infromationen</p>
                </div>
                {#if !isEditing}
                    <div class="flex items-center gap-4 w-full max-[900px]:flex-col">
                        <Input value={deploymentData.title} title="Titel" />
                        <Input value={deploymentData.version} title="Version" />
                    </div>

                    <Input value={formatISODateString(deploymentData.date)} title="Datum" readonly={true}/>

                    <div class="flex items-center gap-4 w-full max-[900px]:flex-col">
                        <Input value={deploymentData.startTime} title="Start" readonly={true}/>
                        <Input value={deploymentData.endTime} title="Ende" readonly={true}/>
                    </div>

                    <Input value={deploymentData.commit} title="Commit" placeholder="Commit hash" readonly={true}/>
                {:else}
                    <div class="flex items-center gap-4 w-full max-[900px]:flex-col">
                        <Input bind:value={informationDraft.title} title="Titel" readonly={true}/>
                        <Input bind:value={informationDraft.version} title="Version" readonly={true}/>
                    </div>

                    <DefaultDatepicker title="Datum" position="bottom" onChange={updateDate} selected={formatISODateString(informationDraft.date)}/>

                    <div class="flex items-center gap-4 w-full max-[900px]:flex-col">
                        <TimePicker title="Start" onChange={updateStartTime} selected={informationDraft.startTime}/>
                        <TimePicker title="End" onChange={updateEndTime} selected={informationDraft.endTime}/>
                    </div>

                    <Input bind:value={informationDraft.commit} title="Commit" placeholder="Commit hash" />
                {/if}

                {#if !isEditing}
                    <div class="flex items-center gap-4 w-full">
                        <Button type="delete" onclick={startDeleting} disabled={isRunningMigrationTests}>
                            <span class="material-symbols-rounded mr-2">delete</span>
                            Löschen
                        </Button>
                        <Button type="primary" onclick={startEditing} disabled={isRunningMigrationTests}>
                            <span class="material-symbols-rounded mr-2">person_edit</span>
                            Bearbeiten
                        </Button>
                    </div>
                {/if}

                {#if viewport.width > 900 && isEditing}
                    <div class="flex items-center w-full gap-2">
                        <Button type="secondary" onclick={cancelEditing} isCancel={true}>Abbrechen</Button>
                        <Button type="primary" disabled={!hasChanges || isSubmittingInformationChanges}
                                onclick={async () => await updateDeploymentInformation()}>
                            {#if isSubmittingInformationChanges}
                                <Spinner light={true} />
                                <p>Speichern...</p>
                            {:else}
                                Speichern
                            {/if}
                        </Button>
                    </div>
                {/if}
            </div>

            <!-- Migrations -->
            <div class="flex flex-col items-center gap-5 min-[1500px]:w-1/2 min-[1200px]:w-2/3 w-full mt-5 p-0.5">
                <div class="w-full flex items-center justify-start">
                    <p class="text-dt-3 text-gv-dark-text font-medium">Migrations</p>
                </div>

                {#if !isEditing}
                    <div class="flex items-center gap-4 w-full">
                        <Button type="primary" onclick={startDeleting} disabled={isRunningMigrationTests}>
                            <span class="material-symbols-rounded mr-2">add</span>
                            Migration erstellen
                        </Button>
                        <Button type="primary" onclick={() => {}} disabled={!deploymentData.migrations || deploymentData.migrations.length === 0 || isRunningMigrationTests}>
                            {#if isRunningMigrationTests}
                                <Spinner light={true} />
                                <p>Testen...</p>
                            {:else}
                                <span class="material-symbols-rounded mr-2">experiment</span>
                                Test Migrations
                            {/if}
                        </Button>
                    </div>
                {/if}
            </div>
        </div>
    </div>
</main>