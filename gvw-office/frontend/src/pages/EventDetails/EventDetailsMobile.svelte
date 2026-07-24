<script>
    import { push } from "svelte-spa-router";
    import { viewport } from "../../stores/viewport.svelte";
    import {
        modeMap,
        ordinalMap,
        statusMap,
        typeMap,
        updateEvent,
        weekDayMap,
        getOrdinalFromDateString,
        getWeekDayFromDateStringMondayFirst, getDayOfMonthFromDate
    } from "../../services/eventsService.svelte";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";
    import { user } from "../../stores/user.svelte";
    import { formatISODateString } from "../../services/dateTimeUtils.js";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Button from "../../components/Button.svelte";
    import Input from "../../components/Input.svelte";
    import ConfirmDeleteModal from "../../components/ConfirmDeleteModal.svelte";
    import Dropdown from "../../components/Dropdown.svelte";
    import DefaultDatepicker from "../../components/DefaultDatepicker.svelte";
    import TabBar from "../../components/TabBar.svelte";
    import Checkbox from "../../components/Checkbox.svelte";
    import Textarea from "../../components/Textarea.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import TimePicker from "../../components/TimePicker.svelte";

    let {
        eventData,
        isEditing = $bindable(false),
        isDeleting = $bindable(false),
        onChangeIsEditing = () => {},
        onChangeIsDeleting = () => {},
        ...restProps
    } = $props();

    let draft = $state(null);
    let isSubmitting = $state(false);

    const currentDate = isEditing ? draft?.date : eventData?.date;
    const ordinal = $derived(getOrdinalFromDateString(currentDate));
    const weekDay = $derived(getWeekDayFromDateStringMondayFirst(currentDate));

    /**
     * Initializes edit mode for the current event.
     *
     * Creates a deep clone of `eventData` and assigns it to `draft`
     * to allow non-destructive editing. Enables the editing state.
     */
    function startEditing() {
        draft = JSON.parse(JSON.stringify(eventData));
        onChangeIsEditing(true);
    }

    /**
     * Cancels the current editing session.
     *
     * Discards the draft object and exits edit mode
     * without persisting any changes.
     */
    function cancelEditing() {
        draft = null;
        onChangeIsEditing(false);
    }

    const REQUIRED_EVENT_FIELDS = ["title", "type", "status", "location", "date", "time", "mode"];

    const hasChanges = $derived.by(() => {
        if (!draft || !eventData) return false;

        const isFilled = REQUIRED_EVENT_FIELDS.every(field => draft[field]?.toString().trim());
        if (!isFilled) return false;

        const baseChanged =
            draft.title !== eventData.title ||
            draft.type !== eventData.type ||
            draft.status !== eventData.status ||
            draft.location !== eventData.location ||
            draft.date !== eventData.date ||
            draft.time !== eventData.time ||
            draft.mode !== eventData.mode;

        const descChanged = (draft.description || "") !== (eventData.description || "");

        let recurrenceChanged = false;
        if (draft.mode === "monthly") {
            recurrenceChanged =
                draft.recurrence?.monthlyKind !== eventData.recurrence?.monthlyKind ||
                draft.recurrence?.dayOfMonth !== eventData.recurrence?.dayOfMonth ||
                draft.recurrence?.weekDay !== eventData.recurrence?.weekDay ||
                draft.recurrence?.ordinal !== eventData.recurrence?.ordinal;
        }

        return baseChanged || descChanged || recurrenceChanged;
    });

    /**
     * Pre-effect that ensures a draft exists when entering edit mode.
     *
     * If editing is enabled but no draft is present,
     * a deep clone of the current event data is created.
     *
     * Acts as a safety mechanism against inconsistent state.
     */
    $effect.pre(() => {
        if (isEditing && !draft) {
            draft = JSON.parse(JSON.stringify(eventData));
        }
    });

    /**
     * Persists the current draft to the backend.
     *
     * - Sends a snapshot of the draft to the update API
     * - Exits edit mode and clears the draft
     *
     * Assumes validation has already been handled externally.
     */
    async function updateEventData() {
        isSubmitting = true;
        try {
            await updateEvent($state.snapshot(draft));
        } finally {
            isSubmitting = false;
            onChangeIsEditing(false);
            draft = null;
        }
    }

    /**
     * Navigates back to the events overview page.
     *
     * - Refreshes the raw event list
     * - Performs route navigation to `/events`
     *
     * Ensures the overview reflects the latest persisted state.
     */
    async function routeToEvents() {
        await fetchAndSetRaw();
        await push("/events");
    }

    // ==================
    // MODAL REFERENCES
    // ==================
    /**
     * Reference to the delete confirmation modal.
     * Used to initiate and confirm event deletion flow.
     * @type {import("../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteEventModal = null;

    function setMonthlyKindToWeekday() {
        draft.recurrence.monthlyKind = "weekday";
        draft.recurrence.weekDay = weekDay;
        draft.recurrence.ordinal = ordinal;
    }

    function setMonthlyKindToDate() {
        draft.recurrence.monthlyKind = "date";
        draft.recurrence.dayOfMonth = getDayOfMonthFromDate(draft.date);
    }

    function changeMode(val) {
        draft.mode = modeMap[val];

        if (draft.mode === "monthly") {
            draft.recurrence.monthlyKind = "date";
        }
    }

    function updateTime(value) { draft.time = value; }

    function updateDate(value) {
        draft.date = value
        if (draft.recurrence.monthlyKind === "date") {
            draft.recurrence.dayOfMonth = getDayOfMonthFromDate(draft.date);
        }
    }

    function updateStatus(value) { draft.status = statusMap[value]; }

    function updateType(value) { draft.type = typeMap[value]; }

    function startDeletingEvent() { onChangeIsDeleting(true); confirmDeleteEventModal.startDelete(); }

    function disableIsDeleting() { onChangeIsDeleting(false); }
</script>

<ToastStack isMobile={true}></ToastStack>

<ConfirmDeleteModal expectedInput={`${eventData.title}`} id={eventData.id} isMobile={true}
                    title="Veranstaltung löschen"
                    subTitle="Sind Sie sich sicher das Sie diese Veranstaltung löschen möchten?"
                    action="deleteEvent"
                    onClose={async () => {await push("/events")}}
                    onCancel={disableIsDeleting}
                    bind:this={confirmDeleteEventModal}
/>

<main class="flex h-screen overflow-hidden">
    <div class="flex flex-col min-h-0 w-full p-7 overflow-hidden">
        <PageHeader title="Veranstaltung" subTitle={`Veranstaltung: "${eventData?.title ?? ""}"`}>
            {#if !isEditing}
                <button
                    type="button"
                    class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2"
                    onclick={async () => await routeToEvents()}
                >
                    <span class="material-symbols-rounded text-icon-dt-2">close</span>
                </button>
            {/if}
        </PageHeader>

        <div class="flex-1 min-h-0 overflow-y-auto w-full">
            <div class="flex flex-col items-center gap-5 min-[1500px]:w-1/2 min-[1200px]:w-2/3 w-full mt-5">
                {#if viewport.width < 900 && !isEditing && (user.role === "board_member" || user.role === "admin")}
                    <div class="flex items-center gap-2 w-full">
                        <Button type="delete"
                                onclick={startDeletingEvent}>
                            <span class="material-symbols-rounded mr-2">delete</span>
                            Löschen
                        </Button>
                        <Button type="primary" onclick={startEditing}>
                            <span class="material-symbols-rounded mr-2">person_edit</span>
                            Bearbeiten
                        </Button>
                    </div>
                {/if}

                {#if !isEditing}
                    <Input bind:value={eventData.title} title="Titel" placeholder="Veranstaltung XYZ" readonly={true} />

                    <Input title="Typ" bind:value={typeMap[eventData.type]} readonly={true} />
                    <Input title="Status" bind:value={statusMap[eventData.status]} readonly={true} />

                    <Input bind:value={eventData.location} title="Ort" placeholder="Ort XYZ" readonly={true} />

                    <Textarea bind:value={eventData.description} title="Beschreibung"
                              placeholder="Kurze Beschreibung zur Veranstaltung..." readonly={true} />

                    <Input value={formatISODateString(eventData.date)} title="Datum" placeholder="XX.XX.XXXX"
                           readonly={true} />
                    <Input value={eventData.time} title="Uhrzeit" placeholder="--:--" readonly={true} />

                    <TabBar contents={["Einmalig", "Wöchentlich", "Monatlich"]} selected={modeMap[eventData.mode]}
                            disabled={true} marginTop="3" />

                    {#if eventData.mode === "single"}
                        <p class="text-gv-dark-text text-dt-6 text-left w-full">Diese Veranstaltung findet nur einmal
                            statt.</p>
                    {:else if eventData.mode === "weekly"}
                        <p class="text-gv-dark-text text-dt-6 text-left w-full">Jede Woche am {weekDayMap[weekDay]}</p>
                    {:else if eventData.mode === "monthly"}
                        <div class="w-full flex flex-col items-start justify-start gap-4">
                            <Checkbox
                                title="Am gleichen Datum"
                                isChecked={eventData.recurrence.monthlyKind === "date"}
                                disabled={true}
                            />

                            <Checkbox
                                title="Am gleichen Wochentag"
                                isChecked={eventData.recurrence.monthlyKind === "weekday"}
                                disabled={true}
                            />
                        </div>

                        {#if eventData.recurrence.monthlyKind === "weekday"}
                            <p class="text-gv-dark-text text-dt-6 text-left mt-2 w-full">Jeden Monat
                                am {`${ordinalMap[ordinal]} ${weekDayMap[weekDay]}`}.</p>
                        {:else}
                            <p class="text-gv-dark-text text-dt-6 text-left mt-2 w-full">Jeden Monat
                                am {formatISODateString(eventData.date).split(".")[0]}
                                .</p>
                        {/if}
                    {/if}
                {:else}
                    <Input bind:value={draft.title} title="Titel" placeholder="Veranstaltung XYZ" />

                    <Dropdown title="Typ" options={["Proben", "Meeting", "Konzerte", "Sonstiges"]}
                              onChange={updateType} selected={typeMap[draft.type]}
                              showDropshadow={true} />
                    <Dropdown title="Status" options={["Bevorstehend", "Abgeschlossen"]}
                              onChange={updateStatus}
                              selected={statusMap[draft.status]} showDropshadow={true} />

                    <Input bind:value={draft.location} title="Ort" placeholder="Ort XYZ" />

                    <Textarea bind:value={draft.description} title="Beschreibung (Optional)"
                              placeholder="Kurze Beschreibung zur Veranstaltung..." />

                    <div class="flex flex-col items-start w-full h-full">
                        <p class="text-dt-6 font-medium">Datum</p>
                        <DefaultDatepicker marginTop="1" onChange={updateDate} selected={formatISODateString(draft.date)} />
                    </div>

                    <div class="flex flex-col items-start w-full h-full">
                        <p class="text-dt-6 font-medium">Uhrzeit</p>
                        <TimePicker marginTop="1" selected={draft.time} onChange={updateTime} />
                    </div>

                    <TabBar
                        marginTop="3"
                        contents={["Einmalig", "Wöchentlich", "Monatlich"]}
                        selected={modeMap[draft.mode]}
                        onChange={changeMode}
                    />

                    {#if draft.mode === "single"}
                        <p class="text-gv-dark-text text-dt-6 text-left w-full">Diese Veranstaltung findet nur einmal
                            statt.</p>
                    {:else if draft.mode === "weekly"}
                        <p class="text-gv-dark-text text-dt-6 text-left w-full">Jede Woche am {weekDayMap[weekDay]}</p>
                    {:else if draft.mode === "monthly"}
                        <div class="w-full flex flex-col items-start justify-start gap-4">
                            <Checkbox
                                title="Am gleichen Datum"
                                isChecked={draft.recurrence.monthlyKind === "date"}
                                onChange={setMonthlyKindToDate}
                            />

                            <Checkbox
                                title="Am gleichen Wochentag"
                                isChecked={draft.recurrence.monthlyKind === "weekday"}
                                onChange={setMonthlyKindToWeekday}
                            />
                        </div>

                        {#if draft.recurrence.monthlyKind === "weekday"}
                            <p class="text-gv-dark-text text-dt-6 text-left mt-2 w-full">Jeden Monat
                                am {`${ordinalMap[ordinal]} ${weekDayMap[weekDay]}`}.</p>
                        {:else}
                            <p class="text-gv-dark-text text-dt-6 text-left mt-2 w-full">Jeden Monat
                                am {formatISODateString(draft.date).split(".")[0]}
                                .</p>
                        {/if}
                    {/if}
                {/if}

                {#if isEditing}
                    <div class="flex items-center w-full gap-2">
                        <Button type="secondary" onclick={cancelEditing} isCancel={true}>Abbrechen</Button>
                        <Button type="primary" disabled={!hasChanges || isSubmitting}
                                onclick={async () => await updateEventData()}>
                            {#if isSubmitting}
                                <Spinner light={true} />
                                <p>Speichern...</p>
                            {:else}
                                Speichern
                            {/if}
                        </Button>
                    </div>
                {/if}
            </div>
        </div>
    </div>
</main>