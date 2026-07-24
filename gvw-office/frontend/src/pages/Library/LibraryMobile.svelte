<script>
    import { voiceMap, getLibraryCategories, addScore } from "../../services/libraryService.svelte";
    import { user } from "../../stores/user.svelte.js";
    import { libraryStore } from "../../stores/library.svelte.js";
    import { appSettings } from "../../stores/appSettings.svelte.js";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";
    import { push } from "svelte-spa-router";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import SearchBar from "../../components/SearchBar.svelte";
    import Filter from "../../components/Filter.svelte";
    import Button from "../../components/Button.svelte";
    import Card from "../../components/Card.svelte";
    import Chip from "../../components/Chip.svelte";
    import CategoryModal from "../../components/CategoryModal.svelte";
    import Modal from "../../components/Modal.svelte";
    import Input from "../../components/Input.svelte";
    import Dropdown from "../../components/Dropdown.svelte";
    import TabBar from "../../components/TabBar.svelte";
    import Checkbox from "../../components/Checkbox.svelte";
    import FileSelector from "../../components/FileSelector.svelte";
    import MobileSidebar from "../../components/MobileSidebar.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import ChipPicker from "../../components/ChipPicker.svelte";

    // ==================
    // MODAL REFERENCES
    // ==================
    /**
     * Reference to the category modal.
     * Used to programmatically open the category dialog.
     * @type {import("../../components/CategoryModal.svelte").default}
     */
    let categoryModal = null;

    /**
     * Reference to the add score modal.
     * Used to programmatically open the add score dialog.
     * @type {import("../../components/Modal.svelte").default}
     */
    let addScoreModal = null;

    // ==========
    // CATEGORIES
    // ==========
    /**
     * Derived list of library categories including the default
     * `"Alle Kategorien"` entry.
     *
     * The categories are generated from `appSettings.scoreCategories`
     * via `getLibraryCategories(true)` and automatically update
     * whenever the underlying settings change.
     *
     * @type {string[]}
     */
    let categories = $derived(getLibraryCategories(true));

    // =========
    // ADD SCORE
    // =========
    /**
     * Reactive state object containing all form inputs used when
     * creating a new score entry.
     *
     * `voiceCount` is not directly edited by the user and is derived
     * from the number of selected voices when submitting the form.
     *
     * @type {{
     *   scoreId: string,
     *   title: string,
     *   artist: string,
     *   type: string,
     *   voices: string[],
     *   voiceCount: number,
     *   files: File[]
     * }}
     */
    let scoreInput = $state({
        scoreId: "",
        title: "",
        artist: "",
        type: "",
        voices: [],
        voiceCount: 0, // Not a direct input, is bound to voices array
        files: []
    });

    /**
     * Reactive state representing the currently selected choir type.
     * Used to control which voice options are available in the UI.
     *
     * @type {string}
     */
    let selectedChoirType = $state("Männerchor");

    let selectedChips = $state([]);

    let isSubmitting = $state(false);

    /**
     * Derived boolean indicating whether the save action should be disabled.
     *
     * The save button is disabled unless all required fields are filled:
     * - scoreId
     * - title
     * - artist
     * - type
     * - at least one selected voice
     *
     * @type {boolean}
     */
    const saveDisabled = $derived(!(
        scoreInput.scoreId &&
        scoreInput.title &&
        scoreInput.artist &&
        scoreInput.type &&
        scoreInput.voices.length > 0
    ) || isSubmitting);

    /**
     * Resets all input fields in the "Add Score" form to their default values.
     *
     * This is typically called after successfully submitting a new score
     * or when the form should be cleared before creating another entry.
     *
     * @returns {void}
     */
    function resetAddInputs() {
        scoreInput = {
            scoreId: "",
            title: "",
            artist: "",
            type: "",
            voices: [],
            voiceCount: 0,
            files: []
        };

        selectedChips = [];
    }

    /**
     * Adds or removes a voice from the current score input depending
     * on the checkbox state.
     *
     * When enabled, the voice is added to the `voices` array while
     * ensuring that duplicates cannot occur. When disabled, the voice
     * is removed from the array.
     *
     * @param {string} action - The action for a voice identifier to toggle.
     * @returns {void}
     */
    function toggleVoice(action) {
        const [kind, voice] = action.split(":");
        const isChecked = kind === "add";

        if (!voiceMap[voice]) return;

        scoreInput.voices = isChecked
            ? [...new Set([...scoreInput.voices, voiceMap[voice]])]
            : scoreInput.voices.filter((item) => item !== voiceMap[voice]);
    }

    /**
     * Submits the current score input to the backend to create a new score.
     *
     * The function:
     * 1. Constructs a new score object including the calculated `voiceCount`.
     * 2. Sends the data to the API via `addScore`.
     * 3. Refreshes the library data (`fetchAndSetRaw`).
     * 4. Closes the "Add Score" modal.
     *
     * `$state.snapshot()` is used to pass a plain object to the API
     * instead of the reactive proxy.
     *
     * @returns {Promise<void>}
     */
    async function submitScore() {
        isSubmitting = true;

        const score = {
            ...scoreInput,
            voiceCount: scoreInput.voices.length
        };

        try {
            await addScore($state.snapshot(score));
        } finally {
            isSubmitting = false;
        }

        await fetchAndSetRaw();
        addScoreModal.hideModal();
    }

    let sidebarOpen = $state(false);

    function showAddScoreModal() {
        if (addScoreModal) {
            addScoreModal.showModal();
        }
    }

    function hideAddScoreModal() {
        if (addScoreModal) {
            addScoreModal.hideModal();
        }
    }

    function showCategoryModal() {
        if (categoryModal) {
            categoryModal.showModal();
        }
    }

    function openSidebar() { sidebarOpen = true; }

    function updateCategory(value) { scoreInput.type = appSettings.scoreCategories[value]; }

    function updateChoirType(value) { selectedChoirType = value; scoreInput.voices = []; }
</script>

<ToastStack isMobile={true}/>

<CategoryModal bind:this={categoryModal} isMobile={true} />

<Modal title="Neue Noten hinzufügen" subTitle="Erfassen Sie hier die Details der Noten"
       width="2/5" bind:this={addScoreModal} extraFunction={resetAddInputs} isMobile={true}>
    <Input title="Noten ID" placeholder="T01" bind:value={scoreInput.scoreId} />
    <Input title="Titel" placeholder="The Final Countdown" bind:value={scoreInput.title} marginTop="5" />
    <Input title="Komponist/Band" placeholder="Europe" bind:value={scoreInput.artist} marginTop="5" />
    <Dropdown title="Kategorie" options={getLibraryCategories(false)}
              onChange={updateCategory} marginTop="5" />
    <TabBar contents={["Männerchor", "Gemischterchor"]} selected={selectedChoirType}
            onChange={updateChoirType} marginTop="5"
            disabled={selectedChips.length > 0} />

    <div class="w-full flex items-start justify-start mt-2 gap-8">
        {#if selectedChoirType === "Männerchor"}
            <ChipPicker title="Stimmen" options={["1. Tenor", "2. Tenor", "1. Bass", "2. Bass"]} useLock={true}
                        onChange={toggleVoice} bind:selectedOptions={selectedChips}
                        lockTooltip="Bitte wählen Sie zuerst alle Stimmen ab, um den Chortyp zu ändern." />
        {:else}
            <ChipPicker title="Stimmen" options={["Tenor", "Bass", "Sopran", "Alt"]} useLock={true}
                        onChange={toggleVoice} bind:selectedOptions={selectedChips}
                        lockTooltip="Bitte wählen Sie zuerst alle Stimmen ab, um den Chortyp zu ändern." />
        {/if}
    </div>

    <FileSelector title="Noten" marginTop="5"
                  validTypes={["pdf", "gp", "gp5", "gp3", "gp4", "gpx", "cap", "capx"]} page="library"
                  bind:files={scoreInput.files} />

    <div class="w-full flex items-center gap-4 mt-5">
        <Button type="secondary" onclick={hideAddScoreModal}>Abbrechen</Button>
        <Button type="primary" disabled={saveDisabled} onclick={async () => await submitScore()}>
            {#if isSubmitting}
                <Spinner light={true} />
                <p>Speichern...</p>
            {:else}
                Hinzufügen
            {/if}
        </Button>
    </div>
</Modal>

<MobileSidebar currentPage="library" bind:isOpen={sidebarOpen} />

<main class="flex overflow-hidden">
    <div class="flex flex-col w-full h-dvh overflow-hidden p-7 min-h-0">
        <div class="w-full flex items-center justify-start">
            <button class="flex items-center justify-center" onclick={openSidebar}>
                <span class="material-symbols-rounded text-icon-dt-4 text-gv-dark-text">menu</span>
            </button>
        </div>
        <PageHeader title="Notenbibliothek" subTitle="Verwaltung des gesamten Notenmaterials" showSlot={false}></PageHeader>

        {#if user.role === "board_member" || user.role === "admin" || user.role === "librarian" || user.role === "conductor"}
            <div class="flex flex-col items-center w-full gap-2 mt-5">
                <Button type="primary" onclick={showCategoryModal}>
                    <span class="material-symbols-rounded min-[1000px]:text-icon-dt-4 text-icon-dt-5 mr-2">discover_tune</span>
                    <p class="min-[1000px]:text-dt-4 text-dt-5">Kategorien</p>
                </Button>
                <Button type="primary" onclick={showAddScoreModal}>
                    <span class="material-symbols-rounded min-[1000px]:text-icon-dt-4 text-icon-dt-5 mr-2">add</span>
                    <p class="min-[1000px]:text-dt-4 text-dt-5 text-nowrap">Noten hinzufügen</p>
                </Button>
            </div>
        {/if}

        <div class="flex flex-col w-full gap-4 mt-5">
            <SearchBar placeholder="Noten durchsuchen..." page="library" />
            <div class="h-full w-full">
                <Filter page="library" options={categories} textWrap={false} customDefault="Alle Kategorien" />
            </div>
        </div>

        <div class="flex-1 min-h-0 overflow-y-auto mt-5">
            <div
                class="min-[1470px]:grid min-[1470px]:grid-cols-2 flex flex-col gap-4 overflow-y-auto overflow-x-hidden">
                {#each libraryStore.display as score (score.id)}
                    <button onclick={async () => await push(`/library/details?id=${score.id}&editing=false`)}>
                        <Card>
                            <div class="flex items-start justify-start gap-2 w-full">
                                <span class="material-symbols-rounded text-icon-dt-6 text-gv-primary">music_note</span>
                                <div class="flex flex-col items-start gap-1 max-w-2/3">
                                    <p class="text-gv-dark-text text-dt-5 text-left line-clamp-2 truncate">{score.title}</p>
                                    <p class="text-gv-light-text text-dt-7">{score.artist}</p>
                                </div>
                            </div>

                            <div class="flex w-full items-center justify-start mt-2">
                                <Chip text={appSettings.scoreCategories[score.type]} />
                            </div>

                            <div class="flex w-full items-center justify-start mt-4 gap-2">
                            <span
                                class="material-symbols-rounded text-gv-light-text text-icon-dt-5">import_contacts</span>
                                {#each score.voices as voice, i (i)}
                                    <p class="text-gv-light-text text-dt-7">{voiceMap[voice]}</p>
                                {/each}
                            </div>

                            <div class="flex w-full items-center justify-start mt-2">
                                <p class="text-gv-light-text text-dt-7">{score.voiceCount}-stimmig</p>
                            </div>

                            <div class="flex w-full items-center justify-start mt-2">
                                <p class="text-gv-light-text text-dt-6">
                                    Noten-ID: {score.scoreId || "Keine ID vorhanden"}</p>
                            </div>
                        </Card>
                    </button>
                {/each}
            </div>
        </div>
    </div>
</main>