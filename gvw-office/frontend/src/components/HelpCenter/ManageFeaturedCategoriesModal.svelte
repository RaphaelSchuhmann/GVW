<script>
    import Modal from "../Modal.svelte";
    import Button from "../Button.svelte";
    import Spinner from "../Spinner.svelte";
    import Card from "../Card.svelte";
    import Checkbox from "../Checkbox.svelte";
    import { appSettings } from "../../stores/appSettings.svelte.js";
    import { updateHelpCenterCategoryFeaturedList } from "../../services/helpCenterService.svelte.js";

    let { isMobile = false, ...restProps } = $props();

    let modalRef = null;
    let isSubmitting = $state(false);

    let inputMap = new Map();
    let inputs = $state(inputMap);

    $effect(() => {
        if (appSettings.helpCenterCategories) {
            inputs = new Map(appSettings.helpCenterCategories.map(obj => [obj.id, !!obj.isFeatured]));
        }
    });

    let getEnabledCount = $derived.by(() => {
        let count = 0;

        for (const val of inputs.values()) {
            if (val) count++;
        }

        return count;
    });

    let addDisabled = getEnabledCount < 1 || getEnabledCount > 6;

    function submit() {
        isSubmitting = true;
        try {
            updateHelpCenterCategoryFeaturedList(inputs);
        } finally {
            isSubmitting = false;
            hideModal();
        }
    }

    function initializeInputs() {
        inputs = new Map(
            (appSettings.helpCenterCategories ?? []).map(category => [
                category.id,
                !!category.isFeatured
            ])
        );
    }

    export function showModal() {
        if (modalRef) {
            initializeInputs();
            modalRef.showModal();
        }
    }

    export function hideModal() {
        if (modalRef) modalRef.hideModal();
    }

    function updateCategory(event) {
        const id = event.currentTarget.dataset.id;

        inputs.set(id, !inputs.get(id));
        inputs = new Map(inputs);
    }
</script>

<Modal bind:this={modalRef} title="Kategorien auswählen" hideSubTitle={true} isMobile={isMobile}>
    <div class="flex flex-col w-full items-center justify-start gap-4">
        <div class="flex flex-col w-full items-center justify-start gap-2">
            {#each appSettings.helpCenterCategories as category (category.id)}
                <button class="cursor-pointer disabled:cursor-not-allowed opacity-75 w-full"
                        disabled={getEnabledCount >= 6 && !inputs.get(category.id)}
                        data-id={category.id}
                        onclick={updateCategory}>
                    <Card>
                        <div class="flex items-center justify-start gap-2 w-full">
                            <Checkbox isChecked={inputs.get(category.id)} clickable={false} />

                            <div class="flex items-center justify-center p-2 rounded-2 bg-gv-bg-bar">
                                <span
                                    class="material-symbols-rounded text-gv-primary text-icon-dt-5">{category.icon}</span>
                            </div>
                            <p class="text-gv-dark-text text-nowrap truncate text-dt-4">{category.title}</p>
                        </div>
                    </Card>
                </button>
            {/each}
        </div>

        <div class="w-full flex items-center justify-end gap-4">
            <Button type="secondary" onclick={hideModal}>Abbrechen</Button>
            <Button type="primary" disabled={addDisabled} onclick={submit} isSubmit={true}>
                {#if isSubmitting}
                    <Spinner light={true} />
                    <p>Speichern...</p>
                {:else}
                    Speichern
                {/if}
            </Button>
        </div>
    </div>
</Modal>