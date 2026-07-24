<script>
    import {
        typeMap,
        statusMap,
        modeMap,
        weekDayMap,
        ordinalMap,
        addEvent,
        getOrdinalFromDateString,
        getWeekDayFromDateStringMondayFirst, getDayOfMonthFromDate
    } from "../services/eventsService.svelte";
    import { fetchAndSetRaw } from "../services/filterService.svelte";
    import { formatISODateString } from "../services/dateTimeUtils.js";
    import Spinner from "./Spinner.svelte";
    import Button from "./Button.svelte";
    import Checkbox from "./Checkbox.svelte";
    import TabBar from "./TabBar.svelte";
    import TimePicker from "./TimePicker.svelte";
    import DefaultDatepicker from "./DefaultDatepicker.svelte";
    import Textarea from "./Textarea.svelte";
    import Input from "./Input.svelte";
    import Dropdown from "./Dropdown.svelte";
    import Modal from "./Modal.svelte";

    // Destructure properties - including responsive configuration flag
    let { onSaveSuccess, isMobile = false } = $props();

    let modalRef = null;
    let isSubmitting = $state(false);

    let eventInput = $state({
        title: "",
        type: "",
        status: "",
        date: "",
        time: "",
        location: "",
        description: "",
        mode: "single",
        recurrence: {
            monthlyKind: "date",
            dayOfMonth: null,
            weekDay: null,
            ordinal: null
        }
    });

    const saveDisabled = $derived(!(
        eventInput.type &&
        eventInput.status &&
        eventInput.date &&
        eventInput.mode &&
        eventInput.title &&
        eventInput.time &&
        eventInput.location
    ) || isSubmitting);

    const ordinal = $derived(getOrdinalFromDateString(eventInput.date));
    const weekDay = $derived(getWeekDayFromDateStringMondayFirst(eventInput.date));

    function resetAddInputs() {
        eventInput = {
            title: "",
            type: "",
            status: "",
            date: "",
            time: "",
            location: "",
            description: "",
            mode: "single",
            recurrence: {
                monthlyKind: "date",
                dayOfMonth: null,
                weekDay: null,
                ordinal: null
            }
        };
    }

    async function submitEvent() {
        isSubmitting = true;

        if (eventInput.mode === "monthly") {
            if (eventInput.recurrence.monthlyKind === "date") {
                eventInput.recurrence.dayOfMonth = Number(eventInput.date.split(".")[0]);
                delete eventInput.recurrence.weekDay;
                delete eventInput.recurrence.ordinal;
            } else {
                eventInput.recurrence.weekDay = weekDay;
                eventInput.recurrence.ordinal = ordinal;
                delete eventInput.recurrence.dayOfMonth;
            }
        } else {
            eventInput.recurrence = {
                monthlyKind: null,
                dayOfMonth: null,
                weekDay: null,
                ordinal: null,
            };
        }

        const event = {
            ...eventInput,
            description: eventInput.description || "Keine Beschreibung"
        };

        try {
            await addEvent(event);
            await fetchAndSetRaw();
            modalRef?.hideModal();
            onSaveSuccess?.();
        } finally {
            isSubmitting = false;
        }
    }

    export function show() {
        if (modalRef) modalRef.showModal();
    }

    function hide() {
        if (modalRef) modalRef.hideModal();
    }

    function setMonthlyKindToWeekday() {
        eventInput.recurrence.monthlyKind = "weekday";
        eventInput.recurrence.weekDay = weekDay;
        eventInput.recurrence.ordinal = ordinal;
    }

    function setMonthlyKindToDate() {
        eventInput.recurrence.monthlyKind = "date";
        const [day] = eventInput.date.split(".").map(Number);
        eventInput.recurrence.dayOfMonth = day;
    }

    function changeMode(val) { eventInput.mode = modeMap[val] }

    function updateTime(value) { eventInput.time = value }

    function updateDate(value) { eventInput.date = value }

    function updateStatus(value) { eventInput.status = statusMap[value] }

    function updateType(value) { eventInput.type = typeMap[value]; }
</script>

<Modal bind:this={modalRef} extraFunction={resetAddInputs} {isMobile}
       title="Neue Veranstaltung hinzufügen" subTitle="Erfassen Sie hier die Details der Veranstaltung"
       width="2/5">

    <Input bind:value={eventInput.title} title="Titel" placeholder="Veranstaltung XYZ" marginTop="5" />

    <div class="w-full flex flex-col md:flex-row items-center gap-4 mt-5">
        <Dropdown title="Typ" options={["Proben", "Meeting", "Konzerte", "Sonstiges"]}
                  onChange={updateType} showDropshadow={true} />
        <Dropdown title="Status" options={["Bevorstehend", "Abgeschlossen"]}
                  onChange={updateStatus} showDropshadow={true} />
    </div>

    <Input bind:value={eventInput.location} title="Ort" placeholder="Ort XYZ" marginTop="5" />

    <Textarea bind:value={eventInput.description} title="Beschreibung (Optional)"
              placeholder="Kurze Beschreibung zur Veranstaltung..." marginTop="5" />

    <div class="w-full flex flex-col md:flex-row items-center gap-4 mt-5">
        <div class="flex flex-col items-start w-full h-full">
            <p class="text-dt-6 font-medium">Datum</p>
            <DefaultDatepicker marginTop="1" onChange={updateDate} />
        </div>
        <div class="flex flex-col items-start w-full h-full">
            <p class="text-dt-6 font-medium">Uhrzeit</p>
            <TimePicker marginTop="1" selected={eventInput.time} onChange={updateTime} />
        </div>
    </div>

    <div class="h-min mt-5">
        <TabBar
            contents={["Einmalig", "Wöchentlich", "Monatlich"]}
            selected={modeMap[eventInput.mode]}
            onChange={changeMode}
        />
    </div>

    {#if eventInput.mode === "single"}
        <p class="text-gv-dark-text text-dt-6 md:text-dt-4 text-left w-full mt-5">Diese Veranstaltung findet nur einmal statt.</p>
    {:else if eventInput.mode === "weekly"}
        <p class="text-gv-dark-text text-dt-6 md:text-dt-4 text-left w-full mt-5">Jede Woche am {weekDayMap[weekDay]}</p>
    {:else if eventInput.mode === "monthly"}
        <div class="w-full flex flex-col md:flex-row items-start md:items-center justify-start gap-4 mt-5">
            <Checkbox
                title="Am gleichen Datum"
                isChecked={eventInput.recurrence.monthlyKind === "date"}
                onChange={setMonthlyKindToDate}
            />

            <Checkbox
                title="Am gleichen Wochentag"
                isChecked={eventInput.recurrence.monthlyKind === "weekday"}
                onChange={setMonthlyKindToWeekday}
            />
        </div>

        {#if eventInput.recurrence.monthlyKind === "weekday"}
            <p class="text-gv-dark-text text-dt-6 md:text-dt-4 text-left mt-5">Jeden Monat am {`${ordinalMap[ordinal]} ${weekDayMap[weekDay]}`}.</p>
        {:else}
            <p class="text-gv-dark-text text-dt-6 md:text-dt-4 text-left mt-5">Jeden Monat am {formatISODateString(eventInput.date)}.</p>
        {/if}
    {/if}

    <div class="w-full flex items-center justify-end mt-5 gap-4">
        <Button type="secondary" onclick={hide}>Abbrechen</Button>
        <Button type="primary" disabled={saveDisabled} onclick={submitEvent}>
            {#if isSubmitting}
                <Spinner light={true} />
                <p>Speichern...</p>
            {:else}
                Hinzufügen
            {/if}
        </Button>
    </div>
</Modal>