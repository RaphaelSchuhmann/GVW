<script>
    import { route } from "../../services/utils.js";
    import { viewport } from "../../stores/viewport.svelte";
    import {
        getFullDeployment,
        updateDeployment,
        migrationActions,
        addMigration,
        deleteMigration,
    } from "../../services/deploymentService.svelte.js";
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
    import Modal from "../../components/Modal.svelte";
    import Dropdown from "../../components/Dropdown.svelte";
    import Checkbox from "../../components/Checkbox.svelte";
    import Migration from "../../components/Migration.svelte";

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

    let addMigrationInputs = $state({
        database: "",
        field: "",
        action: "",
        conditional: {
            condition: { field: "", value: "" },
            truePath: { field: "", value: "" },
            falsePath: { field: "", value: "" },
            negated: false
        },
        value: ""
    });

    let isSubmittingInformationChanges = $state(false);
    let isRunningMigrationTests = $state(false);
    let isSubmittingMigration = $state(false);

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

            const [startHours, startMins] = startTime.split(":").map(Number);
            const [endHours, endMins] = endTime.split(":").map(Number);

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

    const isNonEmpty = (str) => typeof str === "string" && str.trim().length > 0;

    const isAddMigrationDisabled = $derived.by(() => {
        const { database, action, field, value, conditional } = addMigrationInputs;

        if (!isNonEmpty(database)) return true;
        if (!isNonEmpty(action) || action.toLowerCase() === "wählen") return true;

        const actionUpper = action.toUpperCase();

        switch (actionUpper) {
            case "ADD":
            case "DELETE":
                return !isNonEmpty(field);

            case "RENAME":
                return !isNonEmpty(field) || !isNonEmpty(value);

            case "CONDITIONAL":
                return (
                    !isNonEmpty(conditional.condition.field) ||
                    !isNonEmpty(conditional.truePath.field) ||
                    !isNonEmpty(conditional.falsePath.field)
                );

            default:
                return true;
        }
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

    function startDeletingDeployment() {
        onChangeIsDeleting(true);
        confirmDeleteUserModal.startDelete();
    }

    async function submitMigration() {
        isSubmittingMigration = true;

        const data = {
            deploymentId: deploymentData.id,
            rev: deploymentData.rev,
            ...addMigrationInputs
        };

        if (data.action !== "CONDITIONAL") {
            data.conditional = null;
        }

        if (data.action !== "RENAME") {
            data.value = null;
        }

        if (data.action === "CONDITIONAL") {
            data.field = null;
        }

        try {
            await addMigration(data);
            deploymentData = await getFullDeployment(deploymentData.id);
        } finally {
            isSubmittingMigration = false;
            addMigrationModal.hideModal();
        }
    }

    function resetAddMigrationInputs() {
        addMigrationInputs = {
            database: "",
            field: "",
            action: "",
            conditional: {
                condition: { field: "", value: "" },
                truePath: { field: "", value: "" },
                falsePath: { field: "", value: "" },
                negated: false
            },
            value: ""
        };
    }

    async function deleteMigrationAndRefresh(migrationId) {
        await deleteMigration(deploymentData.id, migrationId);
        deploymentData = await getFullDeployment(deploymentData.id);
    }

    // Update deployment info
    function updateDate(val) { informationDraft.date = val; }

    function updateStartTime(val) { informationDraft.startTime = val; }

    function updateEndTime(val) { informationDraft.endTime = val; }

    // Update add migration
    function updateMigrationAction(val) {
        // Note no map required as the internal value is also the display value
        addMigrationInputs.action = val;

        if (val === "CONDITIONAL") {
            addMigrationInputs.field = "";
        }

        if (val !== "RENAME") {
            addMigrationInputs.value = "";
        }
    }
    function updateMigrationNegated(val) { addMigrationInputs.conditional.negated = val; }

    // ==================
    // MODAL REFERENCES
    // ==================
    /**
     * Reference to the delete confirmation modal.
     * Used to initiate and confirm deployment deletion flow.
     * @type {import("../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteUserModal = null;

    /**
     * Reference to the add migration modal.
     * Used to execute the add migration flow.
     * @type {import("../../components/Modal.svelte").default}
     */
    let addMigrationModal = $state(null);

    function disableIsDeleting() { onChangeIsDeleting(false); }
</script>

<ToastStack />

<ConfirmDeleteModal expectedInput={deploymentData.title} id={deploymentData.id}
                    title="Deployment löschen"
                    subTitle="Sind Sie sich sicher das Sie dieses Deployment löschen möchten?"
                    action="deleteDeployment"
                    onClose={routeToDeployments}
                    onCancel={disableIsDeleting}
                    bind:this={confirmDeleteUserModal}
/>

<Modal bind:this={addMigrationModal} extraFunction={resetAddMigrationInputs}
       title="Migration hinzufügen" subTitle="Erfassen Sie hier die Migrationdaten">
    <div class="flex flex-col items-center w-full gap-4">
        <div class="flex items-center gap-4 w-full">
            <Input title="Database" bind:value={addMigrationInputs.database} placeholder="members" />
            {#if migrationActions.has(addMigrationInputs.action) && addMigrationInputs.action !== "CONDITIONAL"}
                <Input title="Feld" bind:value={addMigrationInputs.field} placeholder="status" />
            {/if}
        </div>
        <Dropdown title="Aktion" options={Array.from(migrationActions.values())} onChange={updateMigrationAction} />
        {#if migrationActions.has(addMigrationInputs.action) && addMigrationInputs.action === "RENAME"}
            <Input title="Neuer Feld Name" bind:value={addMigrationInputs.value} placeholder="isActive" />
        {/if}

        {#if migrationActions.has(addMigrationInputs.action) && addMigrationInputs.action === "CONDITIONAL"}
            <div class="w-full flex flex-col items-start justify-start gap-2">
                <p class="text-dt-6 font-medium">Bedingung</p>
                <div class="flex items-center gap-4 w-full">
                    <Input title="Feld" bind:value={addMigrationInputs.conditional.condition.field} placeholder="status" />
                    <Input title="Erwarteter Wert" bind:value={addMigrationInputs.conditional.condition.value} placeholder="active" />
                </div>
                <Checkbox title="Negiert" onChange={updateMigrationNegated} />
            </div>

            <div class="w-full flex flex-col items-start justify-start gap-2">
                <p class="text-dt-6 font-medium">True Branch</p>
                <div class="flex items-center gap-4 w-full">
                    <Input title="Feld" bind:value={addMigrationInputs.conditional.truePath.field} placeholder="isActive" />
                    <Input title="Wert" bind:value={addMigrationInputs.conditional.truePath.value} placeholder="true" />
                </div>
            </div>

            <div class="w-full flex flex-col items-start justify-start gap-2">
                <p class="text-dt-6 font-medium">False Branch</p>
                <div class="flex items-center gap-4 w-full">
                    <Input title="Feld" bind:value={addMigrationInputs.conditional.falsePath.field} placeholder="isActive" />
                    <Input title="Wert" bind:value={addMigrationInputs.conditional.falsePath.value} placeholder="false" />
                </div>
            </div>
        {/if}
        <div class="w-full flex items-center justify-end mt-5 gap-4">
            <Button type="secondary" onclick={addMigrationModal.hideModal}>Abbrechen</Button>
            <Button type="primary" disabled={isAddMigrationDisabled} onclick={submitMigration}>
                {#if isSubmittingMigration}
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

                    <Input value={formatISODateString(deploymentData.date)} title="Datum" readonly={true} />

                    <div class="flex items-center gap-4 w-full max-[900px]:flex-col">
                        <Input value={deploymentData.startTime} title="Start" readonly={true} />
                        <Input value={deploymentData.endTime} title="Ende" readonly={true} />
                    </div>

                    <Input value={deploymentData.commit} title="Commit" placeholder="Commit hash" readonly={true} />
                {:else}
                    <div class="flex items-center gap-4 w-full max-[900px]:flex-col">
                        <Input bind:value={informationDraft.title} title="Titel" readonly={true} />
                        <Input bind:value={informationDraft.version} title="Version" readonly={true} />
                    </div>

                    <DefaultDatepicker title="Datum" position="bottom" onChange={updateDate}
                                       selected={formatISODateString(informationDraft.date)} />

                    <div class="flex items-center gap-4 w-full max-[900px]:flex-col">
                        <TimePicker title="Start" onChange={updateStartTime} selected={informationDraft.startTime} />
                        <TimePicker title="End" onChange={updateEndTime} selected={informationDraft.endTime} />
                    </div>

                    <Input bind:value={informationDraft.commit} title="Commit" placeholder="Commit hash" />
                {/if}

                {#if !isEditing}
                    <div class="flex items-center gap-4 w-full">
                        <Button type="delete" onclick={startDeletingDeployment} disabled={isRunningMigrationTests}>
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
                    <div class="flex w-full flex-col items-start justify-start gap-4 overflow-y-auto">
                        {#each deploymentData.migrations as migration, index (migration.id)}
                            <Migration migrationData={migration} deleteMigration={deleteMigrationAndRefresh} />
                            <div class="w-full h-0.5 bg-gv-border"></div>
                        {/each}
                    </div>

                    <div class="flex items-center gap-4 w-full">
                        <Button type="primary" onclick={addMigrationModal?.showModal}
                                disabled={isRunningMigrationTests}>
                            <span class="material-symbols-rounded mr-2">add</span>
                            Migration erstellen
                        </Button>
                        <Button type="primary" onclick={() => {}}
                                disabled={!deploymentData.migrations || deploymentData.migrations.length === 0 || isRunningMigrationTests}>
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