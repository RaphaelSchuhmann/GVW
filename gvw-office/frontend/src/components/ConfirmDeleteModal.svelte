<script>
    import Modal from "./Modal.svelte";
    import Button from "./Button.svelte";
    import Input from "./Input.svelte";
    import Spinner from "./Spinner.svelte";
    import { removeMember } from "../services/membersService.svelte";
    import { deleteEvent } from "../services/eventsService.svelte";
    import { deleteScore } from "../services/libraryService.svelte";
    import { deleteUser } from "../services/userService.svelte";
    import { addToast } from "../stores/toasts.svelte";
    import { deleteArticle, deleteHelpCenterCategory } from "../services/helpCenterService.svelte.js";

    const actionMap = {
        "deleteMember": removeMember,
        "deleteEvent": deleteEvent,
        "deleteLibEntry": deleteScore,
        "deleteUser": deleteUser,
        "deleteHelpCategory": deleteHelpCenterCategory,
        "deleteHelpArticle": deleteArticle
    };

    let {
        action = "none",
        title = "",
        subTitle = "",
        placeholder = "",
        expectedInput = "",
        id = "",
        onClose = () => {},
        onCancel = () => {},
        isMobile = false,
        ...restProps
    } = $props();

    let confirmInput = $state("");

    /** @type {import("./Modal.svelte").default} */
    let confirmDeleteModal = null;

    let isDeleting = $state(false);

    const disableDelete = $derived(!(confirmInput === expectedInput && expectedInput) || isDeleting);

    const validActions = Object.keys(actionMap);
    let activeAction = $derived(validActions.includes(action) ? action : "none");

    /**
     * Shows the confirmation delete modal
     */
    export function startDelete() {
        confirmDeleteModal?.showModal();
    }

    /**
     * Handles the delete operation after confirmation
     */
    async function handleDelete() {
        isDeleting = true;
        confirmDeleteModal?.hideModal();

        if (activeAction !== "none") {
            const apiFunction = actionMap[activeAction];
            if (!apiFunction) {
                addToast({
                    title: "Unerwarteter Fehler",
                    subTitle: !isMobile ? "Aktion wird nicht unterstützt." : "",
                    type: "error"
                });
                onClose();
                return;
            }

            await apiFunction(id);
        }
        isDeleting = false;
        onClose();
    }

    function handleCancelDelete() {
        confirmDeleteModal?.hideModal();
        onCancel();
    }
</script>

<Modal
    bind:this={confirmDeleteModal}
    extraFunction={() => { confirmInput = ""; }}
    onCancel={onCancel}
    {title}
    {subTitle}
    {isMobile}
    width="2/5"
>
    <Input
        marginTop="5"
        bind:value={confirmInput}
        title={`Geben Sie: "${expectedInput}" ein um fortzufahren`}
        {placeholder}
    />

    <div class="w-full flex items-center justify-end mt-5 gap-4">
        <Button
            type="secondary"
            onclick={handleCancelDelete}
        >
            Abbrechen
        </Button>
        <Button
            type="delete"
            onclick={handleDelete}
            disabled={disableDelete}
        >
            {#if isDeleting}
                <Spinner light={true} />
                <p>Löschen...</p>
            {:else}
                Löschen
            {/if}
        </Button>
    </div>
</Modal>