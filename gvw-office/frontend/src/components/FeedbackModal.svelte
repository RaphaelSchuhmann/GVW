<script>
    import { getDropdownItemsFromMap, severityMap, submitNewItem } from "../services/reportHubService.svelte";
    import { appSettings } from "../stores/appSettings.svelte";
    import Dropdown from "./Dropdown.svelte";
    import Input from "./Input.svelte";
    import Modal from "./Modal.svelte";
    import TabBar from "./TabBar.svelte";
    import Textarea from "./Textarea.svelte";
    import Spinner from "./Spinner.svelte";
    import Button from "./Button.svelte";
    import Sentiment from "./Sentiment.svelte";

    let {
        isMobile = false,
        ...restProps
    } = $props();

    let selectedView = $state("Feedback");

    /** @type {import("../components/Modal.svelte").default} */
    let modal = null;

    let isSubmitting = $state(false);

    const addDisabled = $derived.by(() => {
        const hasEmptyFields = (!inputs.title || inputs.title.trim() === "") ||
            (!inputs.textarea || inputs.textarea.trim() === "");

        const hasUnselectedDropdown = !inputs.dropdown || inputs.dropdown.toLowerCase() === "wählen";

        if (selectedView === "Feedback") {
            return hasEmptyFields || hasUnselectedDropdown || inputs.sentiment <= 0 || isSubmitting;
        } else {
            return hasEmptyFields || hasUnselectedDropdown || isSubmitting;
        }
    });


    let inputs = $state({
        title: "",
        dropdown: "",
        textarea: "",
        sentiment: 0
    });

    function switchView(val) {
        selectedView = val

        inputs = {
            title: "",
            dropdown: "",
            textarea: "",
            sentiment: 0
        };
    }

    function resetInputs() {
        selectedView = "Feedback";
        inputs = {
            title: "",
            dropdown: "",
            textarea: "",
            sentiment: 0
        };
    }

    async function submit() {
        isSubmitting = true;

        try {
            await submitNewItem(inputs, selectedView);
        } finally {
            hideModal();
            isSubmitting = false;
        }
    }

    export function showModal() {
        if (modal) modal.showModal();
    }

    function hideModal() {
        if (modal) modal.hideModal();
    }

    function selectSeverity(val) { inputs.dropdown = severityMap[val] || severityMap["_medium"] }

    function selectCategory(val) { inputs.dropdown = appSettings.feedbackCategories[val] || appSettings.feedbackCategories["_other"] }
</script>

<Modal bind:this={modal} extraFunction={resetInputs} title="Feedback"
       subTitle="Geben Sie hier Feedback zur App oder melden Sie einen Fehler" isMobile={isMobile}>
    <div class="w-full flex flex-col items-start gap-4 mt-5">
        <TabBar
            contents={["Feedback", "Fehler melden"]}
            selected={selectedView}
            onChange={switchView}
        />

        {#if selectedView === "Feedback"}
            {#if isMobile}
                <Input
                    title="Titel"
                    placeholder="Feedback Titel"
                    bind:value={inputs.title}
                />
                <Dropdown
                    options={getDropdownItemsFromMap(appSettings.feedbackCategories)}
                    doCapitalizeWords={false}
                    title="Kategorie"
                    onChange={selectCategory}
                    showDropshadow={true}
                />
            {:else}
                <div class="flex items-center gap-4 w-full">
                    <Input
                        title="Titel"
                        placeholder="Feedback Titel"
                        bind:value={inputs.title}
                    />
                    <Dropdown
                        options={getDropdownItemsFromMap(appSettings.feedbackCategories)}
                        doCapitalizeWords={false}
                        title="Kategorie"
                        onChange={selectCategory}
                        showDropshadow={true}
                    />
                </div>
            {/if}
            <Textarea
                bind:value={inputs.textarea}
                title="Nachricht"
                placeholder="Feedback..."
                height="h-[20vh]"
            />
            <Sentiment title="Bewertung" bind:selected={inputs.sentiment} />
        {:else}
            {#if isMobile}
                <Input
                    title="Titel"
                    placeholder="Tippfehler"
                    bind:value={inputs.title}
                />
                <Dropdown
                    options={getDropdownItemsFromMap(severityMap)}
                    doCapitalizeWords={false}
                    title="Schweregrad"
                    onChange={selectSeverity}
                    showDropshadow={true}
                />
            {:else}
                <div class="flex items-center gap-4 w-full">
                    <Input
                        title="Titel"
                        placeholder="Tippfehler"
                        bind:value={inputs.title}
                    />
                    <Dropdown
                        options={getDropdownItemsFromMap(severityMap)}
                        doCapitalizeWords={false}
                        title="Schweregrad"
                        onChange={selectSeverity}
                        showDropshadow={true}
                    />
                </div>
            {/if}
            <Textarea
                bind:value={inputs.textarea}
                title="Schritte zur Reproduktion"
                placeholder="Was haben Sie gemacht damit es passiert?"
                height="h-[30vh]"
            />
        {/if}

        <div class="w-full flex items-center justify-end gap-4">
            <Button type="secondary" onclick={hideModal}>Abbrechen</Button>
            <Button type="primary" disabled={addDisabled} onclick={submit} isSubmit={true}>
                {#if isSubmitting}
                    <Spinner light={true} />
                    <p>Speichern...</p>
                {:else}
                    {selectedView === "Feedback" ? "Abschicken" : "Fehler melden"}
                {/if}
            </Button>
        </div>
    </div>
</Modal>
