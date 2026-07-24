<script>
    import { push } from "svelte-spa-router";
    import { viewport } from "../../stores/viewport.svelte";
    import { fetchAndSetRaw } from "../../services/filterService.svelte";
    import { user } from "../../stores/user.svelte";

    import ToastStack from "../../components/ToastStack.svelte";
    import PageHeader from "../../components/PageHeader.svelte";
    import Button from "../../components/Button.svelte";
    import Input from "../../components/Input.svelte";
    import ConfirmDeleteModal from "../../components/ConfirmDeleteModal.svelte";
    import Dropdown from "../../components/Dropdown.svelte";
    import TabBar from "../../components/TabBar.svelte";
    import { determineChoirType } from "../../services/utils.js";
    import FileSelector from "../../components/FileSelector.svelte";
    import { appSettings } from "../../stores/appSettings.svelte";
    import {
        downloadScoreFiles,
        getLibraryCategories,
        updateScore,
        voiceMap
    } from "../../services/libraryService.svelte";
    import Spinner from "../../components/Spinner.svelte";
    import ChipPicker from "../../components/ChipPicker.svelte";

    let {
        scoreData,
        isEditing = $bindable(false),
        isDeleting = $bindable(false),
        onChangeIsEditing = () => {},
        onChangeIsDeleting = () => {},
        ...restProps
    } = $props();

    let draft = $state(null);
    let isSubmitting = $state(false);

    let originalSelectedChoirType = $derived(determineChoirType(scoreData.voices[0]) ? "Männerchor" : "Gemischterchor");
    let editedSelectedChoirType = $state("");
    let editTabBarInitialized = $state(false);

    const chipsCache = [];

    let originalSelectedChips = $derived.by(() => {
        chipsCache.length = scoreData.voices.length;

        for (let i = 0; i < scoreData.voices.length; i++) {
            const str = scoreData.voices[i];
            chipsCache[i] = voiceMap[str] ?? str;
        }

        return chipsCache;
    });
    let editSelectedChips = $state([]);

    /**
     * Initializes edit mode for the current score.
     *
     * Creates a deep clone of `scoreData` and assigns it to `draft`
     * to allow non-destructive editing. Enables the editing state.
     */
    function startEditing() {
        draft = JSON.parse(JSON.stringify(scoreData));
        onChangeIsEditing(true);
        editTabBarInitialized = false;
        editedSelectedChoirType = originalSelectedChoirType;
        editSelectedChips = [...originalSelectedChips];
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
        editSelectedChips = [];
    }

    /**
     * Adds or removes a voice from the current draft depending
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

        draft.voices = isChecked
            ? [...new Set([...draft.voices, voiceMap[voice]])]
            : draft.voices.filter((item) => item !== voiceMap[voice]);
    }

    const REQUIRED_SCORE_FIELDS = ["title", "artist", "type", "voices"];

    const hasChanges = $derived.by(() => {
        if (!draft || !scoreData) return false;

        const allFieldsFilled = REQUIRED_SCORE_FIELDS.every(field => {
            const value = draft[field];

            if (Array.isArray(value)) {
                return value.length > 0;
            } else {
                return value !== null && value !== undefined && String(value).trim() !== "";
            }
        });

        if (!allFieldsFilled) return false;

        return REQUIRED_SCORE_FIELDS.some(field => {
            const v1 = draft[field];
            const v2 = scoreData[field];

            if (Array.isArray(v1) && Array.isArray(v2)) {
                return v1.length !== v2.length || v1.some((item, index) => item !== v2[index]);
            }

            return v1 !== v2;
        });
    });

    /**
     * Creates a normalized representation of a score object so it can be
     * reliably compared with another score instance.
     *
     * This function removes insignificant differences that would otherwise
     * break simple equality checks (e.g. via JSON.stringify). In particular:
     * - String fields are trimmed to avoid whitespace-only differences.
     * - Missing values are replaced with consistent defaults.
     * - The `voices` array is sorted so that order differences do not affect
     *   equality checks.
     * - `files` is normalized to an empty array if undefined, but its order
     *   and content are preserved so additions/removals are still detectable.
     *
     * The returned object is intended for comparison purposes only and should
     * not be used as a full replacement for the original score object.
     *
     * @param {Object} score - The score object to normalize.
     * @param {string} [score.title]
     * @param {string} [score.artist]
     * @param {string} [score.type]
     * @param {string[]} [score.voices]
     * @param {Array} [score.files]
     * @returns {Object|undefined|null} A normalized score object, or the
     * original value if `score` is null or undefined.
     */
    function normalizeScore(score) {
        if (!score) return score;

        return {
            title: score.title?.trim() ?? "",
            artists: score.artist?.trim() ?? "",
            type: score.type ?? null,

            voices: [...(score.voices ?? [])].sort(),

            files: score.files ?? []
        };
    }

    /**
     * Pre-effect that ensures a draft exists when entering edit mode.
     *
     * If editing is enabled but no draft is present,
     * a deep clone of the current score data is created.
     *
     * Acts as a safety mechanism against inconsistent state.
     */
    $effect.pre(() => {
        if (isEditing && !draft) {
            startEditing();
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
    async function updateScoreData() {
        isSubmitting = true;
        const score = {
            ...draft,
            originalFiles: scoreData.files,
            voiceCount: draft.voices.length
        };

        try {
            await updateScore(score);
        } finally {
            isSubmitting = false;
            onChangeIsEditing(false);
            draft = null;
        }
    }

    // ==================
    // MODAL REFERENCES
    // ==================
    /**
     * Reference to the delete confirmation modal.
     * Used to initiate and confirm score deletion flow.
     * @type {import("../../components/ConfirmDeleteModal.svelte").default}
     */
    let confirmDeleteScoreModal = null;

    /**
     * Navigates back to the library overview page.
     *
     * - Refreshes the raw library list
     * - Performs route navigation to `/library`
     *
     * Ensures the overview reflects the latest persisted state.
     */
    async function routeToLibrary() {
        try {
            await fetchAndSetRaw();
        } finally {
            await push("/library");
        }
    }

    function startDelete() {
        if (confirmDeleteScoreModal) {
            onChangeIsDeleting(true);
            confirmDeleteScoreModal.startDelete();
        }
    }

    function disableIsDeleting() { onChangeIsDeleting(false); }

    function updateChoirType(value) {
        editedSelectedChoirType = value;
        !editTabBarInitialized ? editTabBarInitialized = true : draft.voices = [];
    }

    function updateCategory(value) { draft.type = appSettings.scoreCategories[value]; }
</script>

<ToastStack isMobile={true}></ToastStack>

<ConfirmDeleteModal expectedInput={`${scoreData.title}`} id={scoreData.id}
                    title="Noten löschen" subTitle="Sind Sie sich sicher das Sie diese Noten löschen möchten?"
                    action="deleteLibEntry"
                    onClose={async () => {await routeToLibrary()}}
                    onCancel={disableIsDeleting}
                    bind:this={confirmDeleteScoreModal} isMobile={true}
/>

<main class="flex h-screen overflow-hidden">
    <div class="flex flex-col min-h-0 w-full p-7 overflow-hidden">
        <PageHeader title="Veranstaltung" subTitle={`Details der Noten: "${scoreData?.title ?? ""}"`}>
            {#if viewport.width > 900}
                {#if !isEditing}
                    <Button type="secondary" onclick={async () => await routeToLibrary()}>
                        <span class="material-symbols-rounded text-icon-dt-5">arrow_back</span>
                        <p class="ml-2 text-dt-3">Zurück</p>
                    </Button>
                {/if}
            {:else}
                <button
                    type="button"
                    class="cursor-pointer ml-auto hover:bg-gv-hover-effect flex items-center justify-center p-2 rounded-2"
                    onclick={async () => await routeToLibrary()}
                >
                    <span class="material-symbols-rounded text-icon-dt-2">close</span>
                </button>
            {/if}
        </PageHeader>

        <div class="flex-1 min-h-0 overflow-y-auto w-full">
            <div class="flex flex-col items-center gap-5 min-[1500px]:w-1/2 min-[1200px]:w-2/3 w-full mt-5">
                {#if viewport.width < 900 && !isEditing && (user.role === "board_member" || user.role === "admin" || user.role === "librarian" || user.role === "conductor")}
                    <div class="flex items-center gap-2 w-full">
                        <Button type="delete" onclick={startDelete}>
                            <span class="material-symbols-rounded mr-2">delete</span>
                            Löschen
                        </Button>
                        <Button type="primary" onclick={startEditing}>
                            <span class="material-symbols-rounded mr-2">person_edit</span>
                            Bearbeiten
                        </Button>
                    </div>
                {/if}

                {#if viewport.width < 900 && !isEditing}
                    <Button type="secondary" onclick={async () => await downloadScoreFiles(scoreData.id)}>
                        <span class="material-symbols-rounded mr-2">download</span>
                        Herunterladen
                    </Button>
                {/if}

                {#if !isEditing}
                    <Input title="Noten ID" placeholder="T01" bind:value={scoreData.scoreId} readonly={true} />
                    <Input title="Titel" placeholder="The Final Countdown" bind:value={scoreData.title}
                           readonly={true} />
                    <Input title="Komponist/Band" placeholder="Europe" bind:value={scoreData.artist} readonly={true} />
                    <Input title="Kategorie" bind:value={appSettings.scoreCategories[scoreData.type]} readonly={true} />
                    <TabBar contents={["Männerchor", "Gemischterchor"]} selected={originalSelectedChoirType}
                            disabled={true} />

                    <div class="w-full flex items-start justify-start gap-8">
                        {#if originalSelectedChoirType === "Männerchor"}
                            <ChipPicker title="Stimmen" options={["1. Tenor", "2. Tenor", "1. Bass", "2. Bass"]}
                                        useLock={true}
                                        lockTooltip="Bitte wählen Sie zuerst alle Stimmen ab, um den Chortyp zu ändern."
                                        disabled={true} />
                        {:else}
                            <ChipPicker title="Stimmen" options={["Tenor", "Bass", "Sopran", "Alt"]} useLock={true}
                                        lockTooltip="Bitte wählen Sie zuerst alle Stimmen ab, um den Chortyp zu ändern."
                                        disabled={true} />
                        {/if}
                    </div>

                    <FileSelector title="Noten"
                                  validTypes={["pdf", "gp", "gp5", "gp3", "gp4", "gpx", "cap", "capx"]} page="library"
                                  bind:files={scoreData.files} disabled={true} />
                {:else}
                    <Input title="Noten ID" placeholder="T01" bind:value={draft.scoreId} />
                    <Input title="Titel" placeholder="The Final Countdown" bind:value={draft.title} />
                    <Input title="Komponist/Band" placeholder="Europe" bind:value={draft.artist} />
                    <Dropdown title="Kategorie" options={getLibraryCategories(false)}
                              selected={appSettings.scoreCategories[draft.type]}
                              onChange={updateCategory} />
                    <TabBar contents={["Männerchor", "Gemischterchor"]} selected={editedSelectedChoirType}
                            onChange={updateChoirType}
                            disabled={editSelectedChips.length > 0} />

                    <div class="w-full flex items-start justify-start gap-8">
                        {#if editedSelectedChoirType === "Männerchor"}
                            <ChipPicker title="Stimmen" options={["1. Tenor", "2. Tenor", "1. Bass", "2. Bass"]}
                                        useLock={true}
                                        onChange={toggleVoice} bind:selectedOptions={editSelectedChips}
                                        lockTooltip="Bitte wählen Sie zuerst alle Stimmen ab, um den Chortyp zu ändern." />
                        {:else}
                            <ChipPicker title="Stimmen" options={["Tenor", "Bass", "Sopran", "Alt"]} useLock={true}
                                        onChange={toggleVoice} bind:selectedOptions={editSelectedChips}
                                        lockTooltip="Bitte wählen Sie zuerst alle Stimmen ab, um den Chortyp zu ändern." />
                        {/if}
                    </div>

                    <FileSelector title="Noten"
                                  validTypes={["pdf", "gp", "gp5", "gp3", "gp4", "gpx", "cap", "capx"]} page="library"
                                  bind:files={draft.files} />
                {/if}

                {#if viewport.width > 900 && !isEditing && (user.role === "board_member" || user.role === "admin" || user.role === "librarian" || user.role === "conductor")}
                    <div class="flex items-center gap-4 w-full">
                        <Button type="delete" onclick={startDelete}>
                            <span class="material-symbols-rounded mr-2">delete</span>
                            Löschen
                        </Button>
                        <Button type="primary" onclick={startEditing}>
                            <span class="material-symbols-rounded mr-2">person_edit</span>
                            Bearbeiten
                        </Button>
                    </div>
                {/if}

                {#if viewport.width > 900 && !isEditing}
                    <Button type="secondary" onclick={async () => await downloadScoreFiles(scoreData.id)}>
                        <span class="material-symbols-rounded mr-2">download</span>
                        Herunterladen
                    </Button>
                {/if}

                {#if isEditing}
                    <div class="flex items-center w-full gap-2">
                        <Button type="secondary" onclick={cancelEditing} isCancel={true}>Abbrechen</Button>
                        <Button type="primary" disabled={!hasChanges || isSubmitting}
                                onclick={async () => await updateScoreData()}>
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