<script>
    import { getChangelogs } from "../services/changelogService.svelte.js";
    import Modal from "./Modal.svelte";
    import ChangelogListItem from "./ChangelogListItem.svelte";
    import { changelogsStore } from "../stores/changelogs.svelte.js";
    import AccordionList from "./AccordionList.svelte";
    import Spinner from "./Spinner.svelte";

    let {
        isMobile = false,
        ...restProps
    } = $props();

    /** @type {import("../components/Modal.svelte").default}*/
    let modal = null;

    let isLoading = $state(true);

    export async function showModal() {
        modal.showModal();

        // retrieve latest changelogs
        isLoading = true;
        try {
            await getChangelogs();
        } finally {
            isLoading = false;
        }
    }
</script>

<Modal bind:this={modal} title="Changelogs" subTitle="" isMobile={isMobile}>
    {#if isLoading}
        <div class="w-full flex items-center justify-center">
            <Spinner />
        </div>
    {:else}
        <div class="w-full flex flex-col items-center max-h-[50vh]">
            <AccordionList itemComponent={ChangelogListItem} list={changelogsStore} usePadding={false}/>
        </div>
    {/if}
</Modal>